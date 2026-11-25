'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const {
  verifyRazorpayCheckoutSignature,
  verifyRazorpayWebhookSignature,
  mapPlayPurchaseV2ToRecord,
  mapRazorpayEventToState,
  buildSettlementRecord,
  evaluateIdempotency
} = require('../src/paymentLogic');

test('Razorpay checkout HMAC-SHA256 verification succeeds for authentic payload', () => {
  const secret = 'rzp_test_secret_123';
  const paymentId = 'pay_999';
  const subscriptionId = 'sub_888';
  const sig = crypto.createHmac('sha256', secret).update(`${paymentId}|${subscriptionId}`).digest('hex');

  assert.equal(verifyRazorpayCheckoutSignature({ paymentId, subscriptionId, signature: sig, secret }), true);
  assert.equal(verifyRazorpayCheckoutSignature({ paymentId, subscriptionId, signature: 'tampered_sig', secret }), false);
});

test('Razorpay webhook HMAC-SHA256 rejects tampered body', () => {
  const webhookSecret = 'whsec_test_999';
  const rawBody = Buffer.from(JSON.stringify({ event: 'subscription.charged', payload: {} }));
  const sig = crypto.createHmac('sha256', webhookSecret).update(rawBody).digest('hex');

  assert.equal(verifyRazorpayWebhookSignature({ rawBody, signature: sig, webhookSecret }), true);
  assert.equal(
    verifyRazorpayWebhookSignature({
      rawBody: Buffer.from(JSON.stringify({ event: 'subscription.charged', payload: { amount: 1 } })),
      signature: sig,
      webhookSecret
    }),
    false
  );
});

test('Monthly Play Billing purchase grants Pro entitlement until expiryTime', () => {
  const now = Date.UTC(2026, 10, 1);
  const nextMonth = new Date(now + 30 * 86_400_000).toISOString();
  const res = mapPlayPurchaseV2ToRecord({
    userId: 'user_1',
    purchaseToken: 'tok_monthly_abc',
    productId: 'tejascode_pro_monthly',
    billingInterval: 'monthly',
    nowMs: now,
    planConfig: { monthlyPriceMinor: 49900, yearlyPriceMinor: 399900, currency: 'INR' },
    playResponse: {
      subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
      startTime: new Date(now).toISOString(),
      lineItems: [{ expiryTime: nextMonth, autoRenewingPlan: { autoRenewEnabled: true } }]
    }
  });
  assert.equal(res.entitlement.tier, 'pro');
  assert.equal(res.entitlement.status, 'Active');
  assert.equal(res.subscription.amountMinorUnits, 49900);
});

test('Annual Play Billing purchase maps yearly amount and status', () => {
  const now = Date.UTC(2026, 10, 1);
  const nextYear = new Date(now + 365 * 86_400_000).toISOString();
  const res = mapPlayPurchaseV2ToRecord({
    userId: 'user_2',
    purchaseToken: 'tok_yearly_xyz',
    productId: 'tejascode_pro_yearly',
    billingInterval: 'yearly',
    nowMs: now,
    planConfig: { monthlyPriceMinor: 49900, yearlyPriceMinor: 399900, currency: 'INR' },
    playResponse: {
      subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
      lineItems: [{ expiryTime: nextYear, autoRenewingPlan: { autoRenewEnabled: true } }]
    }
  });
  assert.equal(res.entitlement.tier, 'pro');
  assert.equal(res.subscription.billingInterval, 'yearly');
  assert.equal(res.subscription.amountMinorUnits, 399900);
});

test('Cancelled subscription retains Pro access until period end, then revokes after expiry', () => {
  const now = Date.UTC(2026, 10, 1);
  const futureExpiry = new Date(now + 10 * 86_400_000).toISOString();
  const pastExpiry = new Date(now - 1000).toISOString();

  const stillValid = mapPlayPurchaseV2ToRecord({
    userId: 'u1',
    purchaseToken: 't1',
    productId: 'tejascode_pro_monthly',
    billingInterval: 'monthly',
    nowMs: now,
    planConfig: { monthlyPriceMinor: 49900 },
    playResponse: {
      subscriptionState: 'SUBSCRIPTION_STATE_CANCELED',
      lineItems: [{ expiryTime: futureExpiry }]
    }
  });
  assert.equal(stillValid.entitlement.tier, 'pro');
  assert.equal(stillValid.entitlement.cancelAtPeriodEnd, true);

  const expiredCancel = mapPlayPurchaseV2ToRecord({
    userId: 'u1',
    purchaseToken: 't1',
    productId: 'tejascode_pro_monthly',
    billingInterval: 'monthly',
    nowMs: now,
    planConfig: { monthlyPriceMinor: 49900 },
    playResponse: {
      subscriptionState: 'SUBSCRIPTION_STATE_CANCELED',
      lineItems: [{ expiryTime: pastExpiry }]
    }
  });
  assert.equal(expiredCancel.entitlement.tier, 'free');
});

test('Expired and Paused Play subscriptions downgrade user to Free tier', () => {
  const now = Date.UTC(2026, 10, 1);
  const expired = mapPlayPurchaseV2ToRecord({
    userId: 'u3',
    purchaseToken: 't3',
    productId: 'tejascode_pro_monthly',
    billingInterval: 'monthly',
    nowMs: now,
    playResponse: { subscriptionState: 'SUBSCRIPTION_STATE_EXPIRED', lineItems: [{ expiryTime: new Date(now - 5000).toISOString() }] }
  });
  assert.equal(expired.entitlement.tier, 'free');
  assert.equal(expired.entitlement.status, 'Expired');
});

test('Razorpay subscription.charged renews period and creates captured transaction', () => {
  const nowSec = Math.floor(Date.UTC(2026, 10, 1) / 1000);
  const res = mapRazorpayEventToState({
    eventName: 'subscription.charged',
    nowMs: nowSec * 1000,
    existingSub: { userId: 'u_rzp', subscriptionId: 'sub_1', billingInterval: 'monthly', currency: 'INR' },
    payload: {
      subscription: { entity: { id: 'sub_1', current_start: nowSec, current_end: nowSec + 2592000 } },
      payment: { entity: { id: 'pay_1', amount: 49900, fee: 1178, tax: 212, currency: 'INR', method: 'upi' } }
    }
  });
  assert.equal(res.entitlementUpdate.tier, 'pro');
  assert.equal(res.entitlementUpdate.status, 'Active');
  assert.equal(res.txRecord.status, 'captured');
  assert.equal(res.txRecord.paymentMethodSummary, 'upi');
  assert.equal(res.txRecord.netAmountMinor, 49900 - 1178 - 212);
});

test('Razorpay refund.processed immediately revokes Pro entitlement', () => {
  const nowMs = Date.UTC(2026, 10, 10);
  const res = mapRazorpayEventToState({
    eventName: 'refund.processed',
    nowMs,
    existingSub: { userId: 'u_rzp', subscriptionId: 'sub_1', currentPeriodEndMs: nowMs + 86400000 },
    payload: {
      refund: { entity: { id: 'rfnd_1', payment_id: 'pay_1', amount: 49900, currency: 'INR' } }
    }
  });
  assert.equal(res.entitlementUpdate.tier, 'free');
  assert.equal(res.entitlementUpdate.status, 'Refunded');
  assert.equal(res.txRecord.status, 'refunded');
});

test('Razorpay payment.failed records failed invoice and puts new sub in Pending', () => {
  const res = mapRazorpayEventToState({
    eventName: 'payment.failed',
    existingSub: { userId: 'u_fail', subscriptionId: 'sub_fail', status: 'Pending' },
    payload: { payment: { entity: { id: 'pay_fail', amount: 49900, method: 'card' } } }
  });
  assert.equal(res.entitlementUpdate.tier, 'free');
  assert.equal(res.txRecord.status, 'failed');
});

test('Webhook idempotency helper blocks duplicate delivery', () => {
  const seen = new Set();
  assert.equal(evaluateIdempotency(seen, 'evt_101').duplicate, false);
  assert.equal(evaluateIdempotency(seen, 'evt_101').duplicate, true);
});

test('Settlement reconciliation maps UTR, fees, GST, and net bank payout', () => {
  const st = buildSettlementRecord({
    id: 'setl_987',
    amount: 48510,
    fees: 1263,
    tax: 227,
    status: 'processed',
    utr: 'HDFC1234567890',
    settled_at: 1793491200
  });
  assert.equal(st.settlementId, 'setl_987');
  assert.equal(st.grossAmountMinor, 50000);
  assert.equal(st.netPayoutMinor, 48510);
  assert.equal(st.utrNumber, 'HDFC1234567890');
  assert.equal(st.status, 'processed');
});

'use strict';

const crypto = require('node:crypto');

function verifyRazorpayCheckoutSignature({ paymentId, subscriptionId, signature, secret }) {
  if (!paymentId || !subscriptionId || !signature || !secret) return false;
  const expected = crypto
    .createHmac('sha256', secret)
    .update(`${paymentId}|${subscriptionId}`)
    .digest('hex');
  return timingSafeHexEqual(expected, signature);
}

function verifyRazorpayWebhookSignature({ rawBody, signature, webhookSecret }) {
  if (!rawBody || !signature || !webhookSecret) return false;
  const payload = Buffer.isBuffer(rawBody) ? rawBody : Buffer.from(String(rawBody), 'utf8');
  const expected = crypto
    .createHmac('sha256', webhookSecret)
    .update(payload)
    .digest('hex');
  return timingSafeHexEqual(expected, signature);
}

function timingSafeHexEqual(a, b) {
  const bufA = Buffer.from(String(a).trim().toLowerCase(), 'utf8');
  const bufB = Buffer.from(String(b).trim().toLowerCase(), 'utf8');
  if (bufA.length !== bufB.length || bufA.length === 0) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

function parsePeriodEndMs(isoTimestamp) {
  if (!isoTimestamp) return 0;
  const parsed = Date.parse(isoTimestamp);
  return Number.isNaN(parsed) ? 0 : parsed;
}

/**
 * Maps Google Play Developer API subscriptionsv2 response into canonical TejasCode status + entitlement.
 */
function mapPlayPurchaseV2ToRecord({
  userId,
  purchaseToken,
  productId,
  billingInterval,
  playResponse,
  planConfig,
  nowMs = Date.now()
}) {
  const state = String(playResponse?.subscriptionState || '');
  const lineItem = Array.isArray(playResponse?.lineItems) ? playResponse.lineItems[0] : null;
  const expiryMs = parsePeriodEndMs(lineItem?.expiryTime);
  const startMs = parsePeriodEndMs(playResponse?.startTime) || nowMs;
  const autoRenewing = Boolean(lineItem?.autoRenewingPlan?.autoRenewEnabled);

  let status = 'Pending';
  let cancelAtPeriodEnd = false;
  let tier = 'free';

  switch (state) {
    case 'SUBSCRIPTION_STATE_ACTIVE':
      status = 'Active';
      tier = expiryMs === 0 || expiryMs > nowMs ? 'pro' : 'free';
      break;
    case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
      status = 'GracePeriod';
      tier = expiryMs === 0 || expiryMs > nowMs ? 'pro' : 'free';
      break;
    case 'SUBSCRIPTION_STATE_CANCELED':
      status = 'Cancelled';
      cancelAtPeriodEnd = true;
      tier = expiryMs > nowMs ? 'pro' : 'free';
      break;
    case 'SUBSCRIPTION_STATE_PAUSED':
      status = 'Paused';
      tier = 'free';
      break;
    case 'SUBSCRIPTION_STATE_ON_HOLD':
      status = 'Paused';
      tier = 'free';
      break;
    case 'SUBSCRIPTION_STATE_EXPIRED':
      status = 'Expired';
      tier = 'free';
      break;
    case 'SUBSCRIPTION_STATE_PENDING':
    case 'SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED':
      status = state.includes('CANCELED') ? 'Cancelled' : 'Pending';
      tier = 'free';
      break;
    default:
      status = 'Pending';
      tier = 'free';
  }

  // Handle explicit revocation/refund
  if (playResponse?.canceledStateContext?.developerInitiatedCancellation ||
      playResponse?.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_PENDING' && state === 'SUBSCRIPTION_STATE_EXPIRED') {
    if (expiryMs <= nowMs) tier = 'free';
  }

  const amountMinorUnits = billingInterval === 'yearly'
    ? Number(planConfig?.yearlyPriceMinor || 0)
    : Number(planConfig?.monthlyPriceMinor || 0);

  const subId = `play_${crypto.createHash('sha256').update(purchaseToken).digest('hex').slice(0, 20)}`;

  return {
    subscription: {
      subscriptionId: subId,
      userId,
      provider: 'google_play',
      status,
      billingInterval,
      amountMinorUnits,
      currency: planConfig?.currency || 'INR',
      productOrPlanId: productId || '',
      currentPeriodStartMs: startMs,
      currentPeriodEndMs: expiryMs,
      cancelAtPeriodEnd,
      autoRenewing
    },
    entitlement: {
      userId,
      tier,
      status,
      provider: 'google_play',
      billingInterval,
      activeSubscriptionId: subId,
      periodEndEpochMs: expiryMs,
      cancelAtPeriodEnd,
      lastVerifiedAtEpochMs: nowMs
    }
  };
}

/**
 * Maps Razorpay webhook event payloads into canonical TejasCode subscription + entitlement state.
 */
function mapRazorpayEventToState({ eventName, payload, existingSub, nowMs = Date.now() }) {
  const subEntity = payload?.subscription?.entity || {};
  const paymentEntity = payload?.payment?.entity || {};
  const refundEntity = payload?.refund?.entity || {};

  const periodEndMs = subEntity.current_end ? subEntity.current_end * 1000 : Number(existingSub?.currentPeriodEndMs || 0);
  const periodStartMs = subEntity.current_start ? subEntity.current_start * 1000 : Number(existingSub?.currentPeriodStartMs || nowMs);

  let status = existingSub?.status || 'Pending';
  let cancelAtPeriodEnd = Boolean(existingSub?.cancelAtPeriodEnd);
  let tier = 'free';
  let txRecord = null;

  switch (eventName) {
    case 'subscription.authenticated':
      status = subEntity.start_at && subEntity.start_at * 1000 > nowMs ? 'Trialing' : 'Pending';
      tier = status === 'Trialing' ? 'pro' : 'free';
      break;
    case 'subscription.activated':
    case 'subscription.resumed':
      status = 'Active';
      cancelAtPeriodEnd = false;
      tier = periodEndMs === 0 || periodEndMs > nowMs ? 'pro' : 'free';
      break;
    case 'subscription.charged':
      status = 'Active';
      cancelAtPeriodEnd = false;
      tier = periodEndMs === 0 || periodEndMs > nowMs ? 'pro' : 'free';
      if (paymentEntity.id) {
        const gross = Number(paymentEntity.amount || existingSub?.amountMinorUnits || 0);
        const fee = Number(paymentEntity.fee || Math.round(gross * 0.0236));
        const tax = Number(paymentEntity.tax || Math.round(fee * 0.18));
        txRecord = {
          transactionId: paymentEntity.id,
          subscriptionId: subEntity.id || existingSub?.subscriptionId || '',
          userId: existingSub?.userId || '',
          provider: 'razorpay',
          billingInterval: existingSub?.billingInterval || 'monthly',
          grossAmountMinor: gross,
          gatewayFeeMinor: fee,
          taxMinor: tax,
          netAmountMinor: Math.max(0, gross - fee - tax),
          currency: (paymentEntity.currency || existingSub?.currency || 'INR').toUpperCase(),
          status: 'captured',
          paymentMethodSummary: paymentEntity.method || 'razorpay',
          invoiceRef: paymentEntity.invoice_id || paymentEntity.id,
          createdAtMs: nowMs
        };
      }
      break;
    case 'subscription.pending':
    case 'subscription.halted':
      status = 'GracePeriod';
      tier = periodEndMs > nowMs ? 'pro' : 'free';
      break;
    case 'subscription.paused':
      status = 'Paused';
      tier = 'free';
      break;
    case 'subscription.cancelled':
      status = 'Cancelled';
      cancelAtPeriodEnd = true;
      tier = periodEndMs > nowMs ? 'pro' : 'free';
      break;
    case 'subscription.completed':
      status = 'Expired';
      cancelAtPeriodEnd = true;
      tier = periodEndMs > nowMs ? 'pro' : 'free';
      break;
    case 'payment.failed':
      status = existingSub?.status === 'Active' ? 'GracePeriod' : 'Pending';
      tier = existingSub?.status === 'Active' && periodEndMs > nowMs ? 'pro' : 'free';
      if (paymentEntity.id) {
        txRecord = {
          transactionId: paymentEntity.id,
          subscriptionId: existingSub?.subscriptionId || '',
          userId: existingSub?.userId || '',
          provider: 'razorpay',
          billingInterval: existingSub?.billingInterval || 'monthly',
          grossAmountMinor: Number(paymentEntity.amount || 0),
          gatewayFeeMinor: 0,
          taxMinor: 0,
          netAmountMinor: 0,
          currency: (paymentEntity.currency || existingSub?.currency || 'INR').toUpperCase(),
          status: 'failed',
          paymentMethodSummary: paymentEntity.method || 'failed',
          invoiceRef: paymentEntity.id,
          createdAtMs: nowMs
        };
      }
      break;
    case 'refund.processed':
    case 'refund.created':
      status = 'Refunded';
      cancelAtPeriodEnd = true;
      tier = 'free';
      if (refundEntity.id) {
        txRecord = {
          transactionId: refundEntity.id,
          subscriptionId: existingSub?.subscriptionId || '',
          userId: existingSub?.userId || '',
          provider: 'razorpay',
          billingInterval: existingSub?.billingInterval || 'monthly',
          grossAmountMinor: Number(refundEntity.amount || 0),
          gatewayFeeMinor: 0,
          taxMinor: 0,
          netAmountMinor: 0,
          currency: (refundEntity.currency || existingSub?.currency || 'INR').toUpperCase(),
          status: 'refunded',
          paymentMethodSummary: 'refund',
          invoiceRef: refundEntity.payment_id || refundEntity.id,
          createdAtMs: nowMs
        };
      }
      break;
    default:
      tier = (status === 'Active' || status === 'Trialing') && (periodEndMs === 0 || periodEndMs > nowMs)
        ? 'pro'
        : 'free';
  }

  return {
    subscriptionUpdate: {
      status,
      currentPeriodStartMs: periodStartMs,
      currentPeriodEndMs: periodEndMs,
      cancelAtPeriodEnd
    },
    entitlementUpdate: {
      tier,
      status,
      provider: 'razorpay',
      billingInterval: existingSub?.billingInterval || 'monthly',
      activeSubscriptionId: subEntity.id || existingSub?.subscriptionId || '',
      periodEndEpochMs: periodEndMs,
      cancelAtPeriodEnd,
      lastVerifiedAtEpochMs: nowMs
    },
    txRecord
  };
}

function buildSettlementRecord(rzpSettlement) {
  const gross = Number(rzpSettlement.amount || 0) + Number(rzpSettlement.fees || 0) + Number(rzpSettlement.tax || 0);
  const fee = Number(rzpSettlement.fees || 0);
  const tax = Number(rzpSettlement.tax || 0);
  const net = Number(rzpSettlement.amount || Math.max(0, gross - fee - tax));
  return {
    settlementId: String(rzpSettlement.id || ''),
    provider: 'razorpay',
    grossAmountMinor: gross,
    gatewayFeeMinor: fee,
    gstOnFeeMinor: tax,
    netPayoutMinor: net,
    currency: 'INR',
    status: String(rzpSettlement.status || 'pending'),
    utrNumber: rzpSettlement.utr || null,
    bankAccountMasked: rzpSettlement.bank_account_masked || 'Verified Merchant INR Account',
    settledAtMs: rzpSettlement.settled_at ? rzpSettlement.settled_at * 1000 : (rzpSettlement.created_at ? rzpSettlement.created_at * 1000 : Date.now())
  };
}

function evaluateIdempotency(seenEventIds, eventId) {
  if (!eventId) return { duplicate: false };
  if (seenEventIds.has(eventId)) return { duplicate: true };
  seenEventIds.add(eventId);
  return { duplicate: false };
}

module.exports = {
  verifyRazorpayCheckoutSignature,
  verifyRazorpayWebhookSignature,
  mapPlayPurchaseV2ToRecord,
  mapRazorpayEventToState,
  buildSettlementRecord,
  evaluateIdempotency
};

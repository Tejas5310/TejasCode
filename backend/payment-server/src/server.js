'use strict';

require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');
const { google } = require('googleapis');
const Razorpay = require('razorpay');
const {
  verifyRazorpayCheckoutSignature,
  verifyRazorpayWebhookSignature,
  mapPlayPurchaseV2ToRecord,
  mapRazorpayEventToState,
  buildSettlementRecord
} = require('./paymentLogic');

const PORT = Number(process.env.PORT || 8080);
const FIRESTORE_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID || 'ai-studio-android-codeforg-4825a9d8-fdbc-4ef9-b150-1b23f538b011';
const ANDROID_PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME || 'com.aistudio.codeforge.vxkmpq';
const ADMIN_EMAIL = (process.env.ADMIN_EMAIL || 'tejashpatel02010@gmail.com').toLowerCase();

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = getFirestore(FIRESTORE_DATABASE_ID);

const razorpayClient =
  process.env.RAZORPAY_KEY_ID && process.env.RAZORPAY_KEY_SECRET
    ? new Razorpay({
        key_id: process.env.RAZORPAY_KEY_ID,
        key_secret: process.env.RAZORPAY_KEY_SECRET
      })
    : null;

const app = express();
app.use(helmet());
app.use(cors());

// Preserve rawBody ONLY for webhook HMAC verification
app.use(
  express.json({
    verify: (req, _res, buf) => {
      req.rawBody = buf;
    }
  })
);

const apiLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false
});
app.use('/api/', apiLimiter);

async function requireAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization || '';
    if (!authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Missing Bearer Firebase ID token.' });
    }
    const token = authHeader.slice(7).trim();
    const decoded = await admin.auth().verifyIdToken(token);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Invalid or expired Firebase ID token.' });
  }
}

function requireAdmin(req, res, next) {
  const email = String(req.user?.email || '').toLowerCase();
  if (email !== ADMIN_EMAIL || !req.user?.email_verified) {
    return res.status(403).json({ error: 'Admin privileges required.' });
  }
  next();
}

async function getPlanConfig() {
  const snap = await db.collection('plan_configs').doc('active').get();
  return snap.exists ? snap.data() : {};
}

async function getPlayPublisherClient() {
  const auth = new google.auth.GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/androidpublisher']
  });
  return google.androidpublisher({ version: 'v3', auth });
}

app.get('/healthz', (_req, res) => {
  res.json({ ok: true, service: 'tejascode-payment-server', databaseId: FIRESTORE_DATABASE_ID });
});

// 1. Verify Google Play Purchase Token via Android Publisher SubscriptionsV2 API
app.post('/api/billing/play/verify', requireAuth, async (req, res) => {
  try {
    const { purchaseToken, productId, orderId, billingInterval } = req.body || {};
    if (!purchaseToken || !productId) {
      return res.status(400).json({ error: 'purchaseToken and productId are required.' });
    }
    const uid = req.user.uid;
    const planConfig = await getPlanConfig();
    const publisher = await getPlayPublisherClient();

    const playResp = await publisher.purchases.subscriptionsv2.get({
      packageName: ANDROID_PACKAGE_NAME,
      token: purchaseToken
    });

    const mapped = mapPlayPurchaseV2ToRecord({
      userId: uid,
      purchaseToken,
      productId,
      billingInterval: billingInterval === 'yearly' ? 'yearly' : 'monthly',
      playResponse: playResp.data,
      planConfig
    });

    const subRef = db.collection('subscriptions').doc(mapped.subscription.subscriptionId);
    const entRef = db.collection('user_entitlements').doc(uid);

    await db.runTransaction(async (tx) => {
      tx.set(subRef, {
        ...mapped.subscription,
        purchaseTokenHash: require('node:crypto').createHash('sha256').update(purchaseToken).digest('hex'),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      tx.set(entRef, {
        ...mapped.entitlement,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      if (mapped.entitlement.tier === 'pro' && orderId) {
        const txId = `play_${orderId.replace(/[^a-zA-Z0-9._-]/g, '_')}`;
        const gross = mapped.subscription.amountMinorUnits;
        const playFee = Math.round(gross * 0.15);
        tx.set(db.collection('payment_transactions').doc(txId), {
          transactionId: txId,
          subscriptionId: mapped.subscription.subscriptionId,
          userId: uid,
          provider: 'google_play',
          billingInterval: mapped.subscription.billingInterval,
          grossAmountMinor: gross,
          gatewayFeeMinor: playFee,
          taxMinor: 0,
          netAmountMinor: Math.max(0, gross - playFee),
          currency: mapped.subscription.currency,
          status: 'captured',
          paymentMethodSummary: 'Google Play Billing',
          invoiceRef: orderId,
          createdAtMs: Date.now()
        }, { merge: true });
      }
    });

    // Acknowledge if still pending acknowledgement
    if (playResp.data?.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_PENDING') {
      await publisher.purchases.subscriptions.acknowledge({
        packageName: ANDROID_PACKAGE_NAME,
        subscriptionId: productId,
        token: purchaseToken
      });
    }

    return res.json({
      verified: true,
      tier: mapped.entitlement.tier,
      status: mapped.entitlement.status,
      message: mapped.entitlement.tier === 'pro'
        ? 'Google Play subscription verified. TejasCode Pro is now active!'
        : `Subscription status: ${mapped.entitlement.status}`
    });
  } catch (err) {
    console.error('Play verify error:', err);
    return res.status(500).json({ error: err.message || 'Failed to verify Play purchase.' });
  }
});

// 2. Create Razorpay Subscription Order (recurring -> settles to Indian Merchant Bank Account)
app.post('/api/billing/razorpay/create-subscription', requireAuth, async (req, res) => {
  try {
    if (!razorpayClient) {
      return res.status(503).json({ error: 'Razorpay credentials not configured on server.' });
    }
    const { billingInterval } = req.body || {};
    const interval = billingInterval === 'yearly' ? 'yearly' : 'monthly';
    const planConfig = await getPlanConfig();
    const rzpPlanId = interval === 'yearly' ? planConfig.razorpayYearlyPlanId : planConfig.razorpayMonthlyPlanId;
    const amountMinor = interval === 'yearly' ? planConfig.yearlyPriceMinor : planConfig.monthlyPriceMinor;

    if (!planConfig.razorpayEnabled || !rzpPlanId || !amountMinor) {
      return res.status(400).json({ error: 'Razorpay plan ID or pricing is not configured by Admin.' });
    }

    const rzpSub = await razorpayClient.subscriptions.create({
      plan_id: rzpPlanId,
      total_count: interval === 'yearly' ? 10 : 60,
      customer_notify: 1,
      notes: {
        userId: req.user.uid,
        billingInterval: interval
      }
    });

    await db.collection('subscriptions').doc(rzpSub.id).set({
      subscriptionId: rzpSub.id,
      userId: req.user.uid,
      provider: 'razorpay',
      status: 'Pending',
      billingInterval: interval,
      amountMinorUnits: Number(amountMinor),
      currency: planConfig.currency || 'INR',
      productOrPlanId: rzpPlanId,
      currentPeriodStartMs: Date.now(),
      currentPeriodEndMs: 0,
      cancelAtPeriodEnd: false,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.json({
      subscriptionId: rzpSub.id,
      keyId: process.env.RAZORPAY_KEY_ID,
      currency: planConfig.currency || 'INR',
      amountMinor: Number(amountMinor)
    });
  } catch (err) {
    console.error('Razorpay create sub error:', err);
    return res.status(500).json({ error: err.message || 'Could not create Razorpay subscription.' });
  }
});

// 3. Verify Razorpay Checkout HMAC-SHA256 Signature
app.post('/api/billing/razorpay/verify-checkout', requireAuth, async (req, res) => {
  try {
    const { razorpay_payment_id, razorpay_subscription_id, razorpay_signature, billingInterval } = req.body || {};
    const valid = verifyRazorpayCheckoutSignature({
      paymentId: razorpay_payment_id,
      subscriptionId: razorpay_subscription_id,
      signature: razorpay_signature,
      secret: process.env.RAZORPAY_KEY_SECRET
    });

    if (!valid) {
      return res.status(400).json({ error: 'Invalid Razorpay cryptographic signature.' });
    }

    const subRef = db.collection('subscriptions').doc(razorpay_subscription_id);
    const subSnap = await subRef.get();
    if (!subSnap.exists || subSnap.data().userId !== req.user.uid) {
      return res.status(404).json({ error: 'Subscription ownership mismatch.' });
    }

    const interval = billingInterval === 'yearly' ? 'yearly' : 'monthly';
    const rzpSub = razorpayClient ? await razorpayClient.subscriptions.fetch(razorpay_subscription_id) : null;
    const nowMs = Date.now();
    const defaultDurationMs = (interval === 'yearly' ? 365 : 30) * 86_400_000;
    const periodEndMs = rzpSub?.current_end ? rzpSub.current_end * 1000 : nowMs + defaultDurationMs;

    await db.runTransaction(async (tx) => {
      tx.set(subRef, {
        status: 'Active',
        currentPeriodStartMs: rzpSub?.current_start ? rzpSub.current_start * 1000 : nowMs,
        currentPeriodEndMs: periodEndMs,
        cancelAtPeriodEnd: false,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      tx.set(db.collection('user_entitlements').doc(req.user.uid), {
        userId: req.user.uid,
        tier: 'pro',
        status: 'Active',
        provider: 'razorpay',
        billingInterval: interval,
        activeSubscriptionId: razorpay_subscription_id,
        periodEndEpochMs: periodEndMs,
        cancelAtPeriodEnd: false,
        lastVerifiedAtEpochMs: nowMs,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    });

    return res.json({
      verified: true,
      message: 'Razorpay payment verified! TejasCode Pro is now active.'
    });
  } catch (err) {
    console.error('Razorpay verify checkout error:', err);
    return res.status(500).json({ error: err.message || 'Verification failed.' });
  }
});

// 4. Cancel Subscription Renewal (Retains access until currentPeriodEndMs)
app.post('/api/billing/subscriptions/cancel', requireAuth, async (req, res) => {
  try {
    const { subscriptionId, reason } = req.body || {};
    if (!subscriptionId) return res.status(400).json({ error: 'subscriptionId required.' });

    const subRef = db.collection('subscriptions').doc(subscriptionId);
    const snap = await subRef.get();
    if (!snap.exists || snap.data().userId !== req.user.uid) {
      return res.status(404).json({ error: 'Subscription not found.' });
    }
    const sub = snap.data();

    if (sub.provider === 'razorpay' && razorpayClient) {
      await razorpayClient.subscriptions.cancel(subscriptionId, { cancel_at_cycle_end: 1 });
    } else if (sub.provider === 'google_play' && sub.purchaseToken) {
      const publisher = await getPlayPublisherClient();
      await publisher.purchases.subscriptions.cancel({
        packageName: ANDROID_PACKAGE_NAME,
        subscriptionId: sub.productOrPlanId,
        token: sub.purchaseToken
      });
    }

    await db.runTransaction(async (tx) => {
      tx.set(subRef, {
        status: 'Cancelled',
        cancelAtPeriodEnd: true,
        cancellationReason: reason || 'user_requested',
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      tx.set(db.collection('user_entitlements').doc(req.user.uid), {
        status: 'Cancelled',
        cancelAtPeriodEnd: true,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    });

    return res.json({ message: 'Subscription renewal cancelled. Pro access stays active until period end.' });
  } catch (err) {
    return res.status(500).json({ error: err.message || 'Could not cancel subscription.' });
  }
});

// 5. Razorpay Verified Webhook Listener (with Firestore Idempotency)
app.post('/api/webhooks/razorpay', async (req, res) => {
  try {
    const signature = req.headers['x-razorpay-signature'];
    const valid = verifyRazorpayWebhookSignature({
      rawBody: req.rawBody,
      signature,
      webhookSecret: process.env.RAZORPAY_WEBHOOK_SECRET
    });
    if (!valid) {
      return res.status(400).json({ error: 'Invalid webhook signature.' });
    }

    const eventId = req.headers['x-razorpay-event-id'] || req.body?.payload?.payment?.entity?.id || `${req.body?.event}_${req.body?.created_at}`;
    const eventRef = db.collection('billing_events').doc(`rzp_${eventId}`);
    const seen = await eventRef.get();
    if (seen.exists) {
      return res.json({ status: 'duplicate_ignored' });
    }

    const subId = req.body?.payload?.subscription?.entity?.id;
    let subSnap = subId ? await db.collection('subscriptions').doc(subId).get() : null;
    if ((!subSnap || !subSnap.exists) && req.body?.payload?.payment?.entity?.notes?.userId) {
      const fallbackUser = req.body.payload.payment.entity.notes.userId;
      subSnap = { exists: true, data: () => ({ userId: fallbackUser, subscriptionId: subId || '' }) };
    }

    if (subSnap && subSnap.exists) {
      const existingSub = subSnap.data();
      const mapped = mapRazorpayEventToState({
        eventName: req.body?.event,
        payload: req.body?.payload,
        existingSub
      });

      await db.runTransaction(async (tx) => {
        tx.set(eventRef, {
          provider: 'razorpay',
          eventType: req.body?.event,
          processedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        if (subId) {
          tx.set(db.collection('subscriptions').doc(subId), {
            ...mapped.subscriptionUpdate,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          }, { merge: true });
        }
        if (existingSub.userId) {
          tx.set(db.collection('user_entitlements').doc(existingSub.userId), {
            ...mapped.entitlementUpdate,
            userId: existingSub.userId,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          }, { merge: true });
        }
        if (mapped.txRecord && mapped.txRecord.transactionId) {
          tx.set(db.collection('payment_transactions').doc(mapped.txRecord.transactionId), mapped.txRecord, { merge: true });
        }
      });
    }

    return res.json({ status: 'ok' });
  } catch (err) {
    console.error('Webhook error:', err);
    return res.status(500).json({ error: err.message });
  }
});

// 6. Google Play Real-Time Developer Notifications (RTDN Pub/Sub Push)
app.post('/api/webhooks/play-rtdn', async (req, res) => {
  try {
    const token = req.query.token || req.headers['x-play-rtdn-token'];
    if (process.env.PLAY_RTDN_VERIFICATION_TOKEN && token !== process.env.PLAY_RTDN_VERIFICATION_TOKEN) {
      return res.status(403).json({ error: 'Invalid RTDN push token.' });
    }
    const dataB64 = req.body?.message?.data;
    if (!dataB64) return res.status(400).json({ error: 'Missing Pub/Sub message.data' });

    const decoded = JSON.parse(Buffer.from(dataB64, 'base64').toString('utf8'));
    const subNotif = decoded.subscriptionNotification;
    if (!subNotif?.purchaseToken) return res.json({ status: 'ignored' });

    const eventId = `play_${decoded.eventTimeMillis}_${subNotif.notificationType}`;
    const eventRef = db.collection('billing_events').doc(eventId);
    if ((await eventRef.get()).exists) return res.json({ status: 'duplicate_ignored' });

    const planConfig = await getPlanConfig();
    const publisher = await getPlayPublisherClient();
    const playResp = await publisher.purchases.subscriptionsv2.get({
      packageName: ANDROID_PACKAGE_NAME,
      token: subNotif.purchaseToken
    });

    const userId = playResp.data?.externalAccountIdentifiers?.obfuscatedExternalAccountId || '';
    if (!userId) return res.json({ status: 'no_user_mapping' });

    const interval = subNotif.subscriptionId === planConfig.yearlyPlayProductId ? 'yearly' : 'monthly';
    const mapped = mapPlayPurchaseV2ToRecord({
      userId,
      purchaseToken: subNotif.purchaseToken,
      productId: subNotif.subscriptionId,
      billingInterval: interval,
      playResponse: playResp.data,
      planConfig
    });

    await db.runTransaction(async (tx) => {
      tx.set(eventRef, {
        provider: 'google_play',
        notificationType: subNotif.notificationType,
        processedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      tx.set(db.collection('subscriptions').doc(mapped.subscription.subscriptionId), {
        ...mapped.subscription,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
      tx.set(db.collection('user_entitlements').doc(userId), {
        ...mapped.entitlement,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    });

    return res.json({ status: 'processed' });
  } catch (err) {
    console.error('Play RTDN error:', err);
    return res.status(500).json({ error: err.message });
  }
});

// 7. Admin Bank Settlement Sync (fetches Razorpay payouts to verified Indian merchant bank account)
app.post('/api/admin/settlements/reconcile', requireAuth, requireAdmin, async (_req, res) => {
  try {
    if (!razorpayClient) {
      return res.status(503).json({ error: 'Razorpay credentials not configured on server.' });
    }
    const list = await razorpayClient.settlements.all({ count: 25 });
    const items = Array.isArray(list?.items) ? list.items : [];
    const batch = db.batch();
    for (const item of items) {
      const rec = buildSettlementRecord(item);
      if (rec.settlementId) {
        batch.set(db.collection('settlements').doc(rec.settlementId), rec, { merge: true });
      }
    }
    await batch.commit();
    return res.json({ message: `Synced ${items.length} settlement record(s) from Razorpay.` });
  } catch (err) {
    return res.status(500).json({ error: err.message || 'Settlement reconciliation failed.' });
  }
});

// 8. Authenticated Account Deletion
app.post('/api/account/delete', requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const snippets = await db.collection('users').doc(uid).collection('snippets').get();
    const certs = await db.collection('users').doc(uid).collection('certificates').get();
    const batch = db.batch();
    snippets.forEach((doc) => batch.delete(doc.ref));
    certs.forEach((doc) => batch.delete(doc.ref));
    batch.delete(db.collection('users').doc(uid));
    batch.delete(db.collection('leaderboard').doc(uid));
    batch.delete(db.collection('user_entitlements').doc(uid));
    await batch.commit();
    await admin.auth().deleteUser(uid);
    return res.json({ message: 'Account and workspace data permanently deleted.' });
  } catch (err) {
    return res.status(500).json({ error: err.message || 'Account deletion failed.' });
  }
});

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`TejasCode billing server listening on port ${PORT}`);
  });
}

module.exports = app;

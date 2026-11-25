# TejasCode Verification & Webhook Payment Server

This standalone Node.js + Express backend securely handles **Google Play Billing** verification, **Razorpay Subscription** creation/verification, **Webhooks** with idempotency, **Bank Settlement Reconciliation** to your verified Indian merchant bank account, and **Account Deletion** for the **TejasCode** Android app (`com.aistudio.codeforge.vxkmpq`).

---

## 1. Merchant Account & Indian Bank Settlement Setup

### A. Razorpay (UPI AutoPay, Cards, NetBanking -> Your Indian Bank Account)
1. Register/verify a **Razorpay Business Account** with your Indian merchant PAN, GST (if applicable), and **Indian Current Bank Account** (beneficiary verification + penny drop).
2. Payouts from Razorpay settle automatically on Razorpay's **T+2 / T+3 working days** cycle directly into your verified bank account with **UTR numbers**, synced into Admin -> *Payments & Settlements*.
3. In Razorpay Dashboard -> **Subscriptions -> Plans**, create:
   - **Monthly Plan** (`plan_monthly_xxx`)
   - **Annual Plan** (`plan_yearly_xxx`)
4. In Razorpay Dashboard -> **Settings -> Webhooks**, add:
   - Endpoint: `https://<your-deployed-server>/api/webhooks/razorpay`
   - Events: `subscription.authenticated`, `subscription.activated`, `subscription.charged`, `subscription.pending`, `subscription.halted`, `subscription.cancelled`, `subscription.completed`, `payment.failed`, `refund.processed`
   - Save the **Webhook Secret** (`RAZORPAY_WEBHOOK_SECRET`).

### B. Google Play Console Billing
1. In **Google Play Console -> Setup -> Payments profile**, complete merchant verification and link your Indian business bank account for monthly Play Store payouts.
2. In **Monetize -> Subscriptions**, create two subscription products matching Admin -> *Pricing & Plans*:
   - `tejascode_pro_monthly` (Base plan ID: `monthly-base`)
   - `tejascode_pro_yearly` (Base plan ID: `yearly-base`)
3. Create a Google Cloud Service Account with **Android Publisher** access, grant it *View financial data* & *Manage orders/subscriptions* in Play Console API access, and attach the JSON key as `GOOGLE_APPLICATION_CREDENTIALS`.
4. Enable **Real-Time Developer Notifications (RTDN)** via Cloud Pub/Sub push to `https://<your-deployed-server>/api/webhooks/play-rtdn?token=<PLAY_RTDN_VERIFICATION_TOKEN>`.

---

## 2. Deploy & Connect to TejasCode Android App

```bash
cd backend/payment-server
npm install
npm test
npm start
```

Deploy to **Cloud Run**, **Render**, **Railway**, or **Firebase Cloud Run** over HTTPS, then set these in **AI Studio Secrets** for the Android build:
- `TEJASCODE_BACKEND_URL=https://your-payment-server-url`
- `RAZORPAY_KEY_ID=rzp_live_...`
- `PLAY_ALTERNATIVE_BILLING_APPROVED=false` (set to `true` ONLY once enrolled in Play Alternative Billing or User Choice Billing)

Finally, sign into TejasCode as `tejashpatel02010@gmail.com` -> **Admin Control Console -> Pricing & Plans** to set your Monthly & Annual prices live!

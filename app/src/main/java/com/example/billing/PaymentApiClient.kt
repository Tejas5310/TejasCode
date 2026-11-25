package com.example.billing

import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RazorpayInitPayload(
    val subscriptionId: String,
    val keyId: String,
    val currency: String,
    val amountMinor: Long
)

class PaymentApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun baseBackendUrl(): String {
        val raw = BuildConfig.TEJASCODE_BACKEND_URL.trim().trimEnd('/')
        return if (raw.equals("UNCONFIGURED", ignoreCase = true)) "" else raw
    }

    fun isBackendConfigured(): Boolean = baseBackendUrl().startsWith("http")

    fun razorpayKeyId(): String {
        val raw = BuildConfig.RAZORPAY_KEY_ID.trim()
        return if (raw.equals("UNCONFIGURED", ignoreCase = true)) "" else raw
    }

    fun isPlayAlternativeBillingApproved(): Boolean =
        BuildConfig.PLAY_ALTERNATIVE_BILLING_APPROVED.trim().equals("true", ignoreCase = true)

    private suspend fun getFirebaseIdToken(): String {
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("Please sign in with Google before managing subscriptions.")
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("Could not obtain Firebase ID token.")
    }

    private suspend fun postJson(path: String, body: JSONObject): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            val baseUrl = baseBackendUrl()
            if (!baseUrl.startsWith("http")) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Verification backend URL (TEJASCODE_BACKEND_URL) is not configured yet. Configure your deployed TejasCode payment server in AI Studio Secrets."
                    )
                )
            }
            try {
                val idToken = getFirebaseIdToken()
                val req = Request.Builder()
                    .url("$baseUrl$path")
                    .header("Authorization", "Bearer $idToken")
                    .header("Content-Type", "application/json")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
                    if (!resp.isSuccessful) {
                        val errMsg = json.optString("error").ifBlank {
                            "Server returned HTTP ${resp.code}"
                        }
                        return@withContext Result.failure(IllegalStateException(errMsg))
                    }
                    Result.success(json)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.localizedMessage ?: "Network request failed."))
            }
        }

    suspend fun verifyPlayPurchase(
        purchaseToken: String,
        productId: String,
        basePlanId: String?,
        orderId: String?,
        billingInterval: String
    ): Result<String> {
        val payload = JSONObject().apply {
            put("purchaseToken", purchaseToken)
            put("productId", productId)
            if (!basePlanId.isNullOrBlank()) put("basePlanId", basePlanId)
            if (!orderId.isNullOrBlank()) put("orderId", orderId)
            put("billingInterval", billingInterval)
        }
        return postJson("/api/billing/play/verify", payload).map { json ->
            json.optString("message").ifBlank { "Google Play subscription verified." }
        }
    }

    suspend fun createRazorpaySubscription(billingInterval: String): Result<RazorpayInitPayload> {
        val payload = JSONObject().apply {
            put("billingInterval", billingInterval)
        }
        return postJson("/api/billing/razorpay/create-subscription", payload).map { json ->
            RazorpayInitPayload(
                subscriptionId = json.getString("subscriptionId"),
                keyId = json.optString("keyId").ifBlank { razorpayKeyId() },
                currency = json.optString("currency", "INR"),
                amountMinor = json.optLong("amountMinor", 0L)
            )
        }
    }

    suspend fun verifyRazorpayCheckout(
        paymentId: String,
        subscriptionId: String,
        signature: String,
        billingInterval: String
    ): Result<String> {
        val payload = JSONObject().apply {
            put("razorpay_payment_id", paymentId)
            put("razorpay_subscription_id", subscriptionId)
            put("razorpay_signature", signature)
            put("billingInterval", billingInterval)
        }
        return postJson("/api/billing/razorpay/verify-checkout", payload).map { json ->
            json.optString("message").ifBlank { "Razorpay subscription verified." }
        }
    }

    suspend fun cancelSubscription(subscriptionId: String, reason: String? = null): Result<String> {
        val payload = JSONObject().apply {
            put("subscriptionId", subscriptionId)
            if (!reason.isNullOrBlank()) put("reason", reason.take(200))
        }
        return postJson("/api/billing/subscriptions/cancel", payload).map { json ->
            json.optString("message").ifBlank { "Subscription cancelled; access remains until period end." }
        }
    }

    suspend fun reconcileSettlements(): Result<String> {
        return postJson("/api/admin/settlements/reconcile", JSONObject()).map { json ->
            json.optString("message").ifBlank { "Settlements synchronized." }
        }
    }

    suspend fun deleteAccount(): Result<String> {
        return postJson("/api/account/delete", JSONObject()).map { json ->
            json.optString("message").ifBlank { "Account deleted permanently." }
        }
    }
}

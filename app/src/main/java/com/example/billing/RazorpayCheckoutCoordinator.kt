package com.example.billing

import android.app.Activity
import com.example.model.PlanConfig
import com.razorpay.Checkout
import com.razorpay.PaymentData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class RazorpayUiState(
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

class RazorpayCheckoutCoordinator(
    private val apiClient: PaymentApiClient
) {
    private val _state = MutableStateFlow(RazorpayUiState())
    val state: StateFlow<RazorpayUiState> = _state.asStateFlow()

    private var pendingInterval: String = "monthly"
    private var pendingSubscriptionId: String? = null

    fun canUseRazorpay(planConfig: PlanConfig): Boolean {
        return planConfig.razorpayEnabled &&
            apiClient.isPlayAlternativeBillingApproved() &&
            apiClient.isBackendConfigured() &&
            apiClient.razorpayKeyId().isNotBlank()
    }

    fun startSubscriptionCheckout(
        activity: Activity,
        planConfig: PlanConfig,
        billingInterval: String,
        userEmail: String?,
        userName: String?,
        scope: CoroutineScope
    ) {
        if (!planConfig.isConfigured) {
            _state.value = RazorpayUiState(
                isBusy = false,
                statusMessage = "Subscription pricing is not configured by the Admin yet.",
                isError = true
            )
            return
        }
        if (!planConfig.razorpayEnabled) {
            _state.value = RazorpayUiState(
                isBusy = false,
                statusMessage = "Razorpay checkout is currently disabled by Admin.",
                isError = true
            )
            return
        }
        if (!apiClient.isPlayAlternativeBillingApproved()) {
            _state.value = RazorpayUiState(
                isBusy = false,
                statusMessage = "Google Play Policy guard: Razorpay in-app digital checkout requires PLAY_ALTERNATIVE_BILLING_APPROVED=true (Play Alternative Billing or external web enrollment). Use Google Play Billing on this build.",
                isError = true
            )
            return
        }
        if (!apiClient.isBackendConfigured() || apiClient.razorpayKeyId().isBlank()) {
            _state.value = RazorpayUiState(
                isBusy = false,
                statusMessage = "Configure TEJASCODE_BACKEND_URL and RAZORPAY_KEY_ID in AI Studio Secrets before initiating Razorpay checkout.",
                isError = true
            )
            return
        }

        pendingInterval = billingInterval
        _state.value = RazorpayUiState(isBusy = true, statusMessage = "Creating Razorpay subscription order...", isError = false)

        scope.launch {
            apiClient.createRazorpaySubscription(billingInterval)
                .onSuccess { initPayload ->
                    pendingSubscriptionId = initPayload.subscriptionId
                    try {
                        Checkout.preload(activity.applicationContext)
                        val checkout = Checkout()
                        checkout.setKeyID(initPayload.keyId)
                        val options = JSONObject().apply {
                            put("name", "TejasCode Pro")
                            put(
                                "description",
                                if (billingInterval == "yearly") "TejasCode Pro Yearly Subscription"
                                else "TejasCode Pro Monthly Subscription"
                            )
                            put("subscription_id", initPayload.subscriptionId)
                            put("recurring", "1")
                            put("theme", JSONObject().put("color", "#00D2FF"))
                            val prefill = JSONObject()
                            if (!userEmail.isNullOrBlank()) prefill.put("email", userEmail)
                            if (!userName.isNullOrBlank()) prefill.put("name", userName)
                            put("prefill", prefill)
                        }
                        checkout.open(activity, options)
                        _state.value = RazorpayUiState(isBusy = false)
                    } catch (e: Exception) {
                        _state.value = RazorpayUiState(
                            isBusy = false,
                            statusMessage = "Could not open Razorpay Checkout: ${e.localizedMessage}",
                            isError = true
                        )
                    }
                }
                .onFailure { err ->
                    _state.value = RazorpayUiState(
                        isBusy = false,
                        statusMessage = err.localizedMessage ?: "Failed to create Razorpay subscription.",
                        isError = true
                    )
                }
        }
    }

    fun onRazorpaySuccess(
        razorpayPaymentId: String?,
        paymentData: PaymentData?,
        scope: CoroutineScope
    ) {
        val dataJson = paymentData?.data
        val paymentId = dataJson?.optString("razorpay_payment_id").orEmpty()
            .ifBlank { razorpayPaymentId.orEmpty() }
        val subscriptionId = dataJson?.optString("razorpay_subscription_id").orEmpty()
            .ifBlank { pendingSubscriptionId.orEmpty() }
        val signature = dataJson?.optString("razorpay_signature").orEmpty()

        if (paymentId.isBlank() || subscriptionId.isBlank() || signature.isBlank()) {
            _state.value = RazorpayUiState(
                isBusy = false,
                statusMessage = "Incomplete Razorpay signature payload; Pro access remains locked until verified.",
                isError = true
            )
            return
        }

        _state.value = RazorpayUiState(
            isBusy = true,
            statusMessage = "Verifying Razorpay signature with server...",
            isError = false
        )

        scope.launch {
            apiClient.verifyRazorpayCheckout(
                paymentId = paymentId,
                subscriptionId = subscriptionId,
                signature = signature,
                billingInterval = pendingInterval
            ).onSuccess { msg ->
                _state.value = RazorpayUiState(
                    isBusy = false,
                    statusMessage = msg,
                    isError = false
                )
            }.onFailure { err ->
                _state.value = RazorpayUiState(
                    isBusy = false,
                    statusMessage = err.localizedMessage ?: "Signature verification failed.",
                    isError = true
                )
            }
        }
    }

    fun onRazorpayError(code: Int, response: String?) {
        _state.value = RazorpayUiState(
            isBusy = false,
            statusMessage = "Razorpay payment cancelled or failed ($code): ${response ?: "Try again."}",
            isError = true
        )
    }

    fun clearMessage() {
        _state.value = _state.value.copy(statusMessage = null, isError = false)
    }
}

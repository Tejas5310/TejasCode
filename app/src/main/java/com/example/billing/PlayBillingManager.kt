package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.model.PlanConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayBillingState(
    val isConnected: Boolean = false,
    val isBusy: Boolean = false,
    val monthlyProductDetails: ProductDetails? = null,
    val yearlyProductDetails: ProductDetails? = null,
    val monthlyFormattedPrice: String? = null,
    val yearlyFormattedPrice: String? = null,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

class PlayBillingManager(
    context: Context,
    private val apiClient: PaymentApiClient
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayBillingState())
    val state: StateFlow<PlayBillingState> = _state.asStateFlow()

    private var lastRequestedInterval: String = "monthly"
    private var lastPlanConfig: PlanConfig = PlanConfig()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun startConnection(planConfig: PlanConfig) {
        lastPlanConfig = planConfig
        if (billingClient.isReady) {
            querySubscriptionProducts(planConfig)
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(isConnected = true)
                    querySubscriptionProducts(lastPlanConfig)
                } else {
                    Log.w(TAG, "Play Billing setup finished with code ${result.responseCode}: ${result.debugMessage}")
                    _state.value = _state.value.copy(
                        isConnected = false,
                        statusMessage = null
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(isConnected = false)
            }
        })
    }

    fun querySubscriptionProducts(planConfig: PlanConfig) {
        lastPlanConfig = planConfig
        if (!billingClient.isReady) return

        val productIds = listOf(
            planConfig.monthlyPlayProductId,
            planConfig.yearlyPlayProductId
        ).map { it.trim() }.filter { it.isNotBlank() }.distinct()

        if (productIds.isEmpty()) return

        val products = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = queryProductDetailsResult.productDetailsList
                val monthly = list.firstOrNull { it.productId == planConfig.monthlyPlayProductId }
                val yearly = list.firstOrNull { it.productId == planConfig.yearlyPlayProductId }
                _state.value = _state.value.copy(
                    monthlyProductDetails = monthly,
                    yearlyProductDetails = yearly,
                    monthlyFormattedPrice = extractOfferFormattedPrice(monthly, planConfig.monthlyPlayBasePlanId),
                    yearlyFormattedPrice = extractOfferFormattedPrice(yearly, planConfig.yearlyPlayBasePlanId)
                )
            } else {
                Log.w(TAG, "queryProductDetailsAsync failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun extractOfferFormattedPrice(details: ProductDetails?, basePlanId: String): String? {
        val offers = details?.subscriptionOfferDetails ?: return null
        val chosen = offers.firstOrNull { it.basePlanId == basePlanId } ?: offers.firstOrNull() ?: return null
        return chosen.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
    }

    fun launchSubscriptionPurchase(
        activity: Activity,
        userId: String,
        planConfig: PlanConfig,
        billingInterval: String
    ) {
        lastPlanConfig = planConfig
        lastRequestedInterval = billingInterval
        if (!planConfig.isConfigured) {
            _state.value = _state.value.copy(
                statusMessage = "Pricing plans are not configured by the Admin yet.",
                isError = true
            )
            return
        }
        if (!apiClient.isBackendConfigured()) {
            _state.value = _state.value.copy(
                statusMessage = "Payment verification backend (TEJASCODE_BACKEND_URL) is not configured yet. No charge was initiated.",
                isError = true
            )
            return
        }
        if (!billingClient.isReady) {
            startConnection(planConfig)
            _state.value = _state.value.copy(
                statusMessage = "Connecting to Google Play Billing. Please try again in a moment.",
                isError = false
            )
            return
        }

        val details = if (billingInterval == "yearly") _state.value.yearlyProductDetails else _state.value.monthlyProductDetails
        val basePlanId = if (billingInterval == "yearly") planConfig.yearlyPlayBasePlanId else planConfig.monthlyPlayBasePlanId

        if (details == null) {
            _state.value = _state.value.copy(
                statusMessage = "Play Console product ID (${if (billingInterval == "yearly") planConfig.yearlyPlayProductId else planConfig.monthlyPlayProductId}) is not published for this package yet.",
                isError = true
            )
            return
        }

        val offer = details.subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlanId }
            ?: details.subscriptionOfferDetails?.firstOrNull()

        if (offer == null) {
            _state.value = _state.value.copy(
                statusMessage = "No active subscription offer found in Google Play Console.",
                isError = true
            )
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(userId.take(64))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(
                statusMessage = "Google Play Billing could not launch (${result.debugMessage}).",
                isError = true
            )
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            _state.value = _state.value.copy(
                statusMessage = "Google Play Billing service is not connected.",
                isError = true
            )
            return
        }
        _state.value = _state.value.copy(isBusy = true, statusMessage = "Checking Google Play purchases...", isError = false)
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    isBusy = false,
                    statusMessage = "Failed to query Google Play purchases: ${result.debugMessage}",
                    isError = true
                )
                return@queryPurchasesAsync
            }
            if (purchases.isEmpty()) {
                _state.value = _state.value.copy(
                    isBusy = false,
                    statusMessage = "No active Google Play subscriptions found to restore.",
                    isError = false
                )
                return@queryPurchasesAsync
            }
            scope.launch {
                var verifiedCount = 0
                var lastErr: String? = null
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        val prodId = purchase.products.firstOrNull().orEmpty()
                        val interval = if (prodId == lastPlanConfig.yearlyPlayProductId) "yearly" else "monthly"
                        val basePlanId = if (interval == "yearly") lastPlanConfig.yearlyPlayBasePlanId else lastPlanConfig.monthlyPlayBasePlanId
                        apiClient.verifyPlayPurchase(
                            purchaseToken = purchase.purchaseToken,
                            productId = prodId,
                            basePlanId = basePlanId,
                            orderId = purchase.orderId,
                            billingInterval = interval
                        ).onSuccess {
                            acknowledgeIfNeeded(purchase)
                            verifiedCount++
                        }.onFailure { err ->
                            lastErr = err.localizedMessage
                        }
                    }
                }
                _state.value = _state.value.copy(
                    isBusy = false,
                    statusMessage = if (verifiedCount > 0) {
                        "Restored and verified $verifiedCount Google Play subscription(s)."
                    } else {
                        lastErr ?: "Could not verify restored purchases."
                    },
                    isError = verifiedCount == 0
                )
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                if (list.isEmpty()) return
                scope.launch {
                    _state.value = _state.value.copy(isBusy = true, statusMessage = "Verifying purchase with TejasCode server...", isError = false)
                    for (purchase in list) {
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PENDING -> {
                                _state.value = _state.value.copy(
                                    isBusy = false,
                                    statusMessage = "Payment pending confirmation from Google Play. Access activates automatically once cleared.",
                                    isError = false
                                )
                            }
                            Purchase.PurchaseState.PURCHASED -> {
                                val prodId = purchase.products.firstOrNull().orEmpty()
                                val interval = if (prodId == lastPlanConfig.yearlyPlayProductId) "yearly" else lastRequestedInterval
                                val basePlanId = if (interval == "yearly") lastPlanConfig.yearlyPlayBasePlanId else lastPlanConfig.monthlyPlayBasePlanId
                                apiClient.verifyPlayPurchase(
                                    purchaseToken = purchase.purchaseToken,
                                    productId = prodId,
                                    basePlanId = basePlanId,
                                    orderId = purchase.orderId,
                                    billingInterval = interval
                                ).onSuccess { msg ->
                                    acknowledgeIfNeeded(purchase)
                                    _state.value = _state.value.copy(
                                        isBusy = false,
                                        statusMessage = msg,
                                        isError = false
                                    )
                                }.onFailure { err ->
                                    _state.value = _state.value.copy(
                                        isBusy = false,
                                        statusMessage = err.localizedMessage ?: "Purchase verification failed.",
                                        isError = true
                                    )
                                }
                            }
                            else -> {
                                _state.value = _state.value.copy(isBusy = false)
                            }
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value = _state.value.copy(
                    isBusy = false,
                    statusMessage = "Google Play purchase cancelled.",
                    isError = false
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                restorePurchases()
            }
            else -> {
                _state.value = _state.value.copy(
                    isBusy = false,
                    statusMessage = "Google Play Billing error (${billingResult.responseCode}): ${billingResult.debugMessage}",
                    isError = true
                )
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(ackParams) { res ->
            if (res.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Acknowledge warning: ${res.debugMessage}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(statusMessage = null, isError = false)
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private companion object {
        const val TAG = "PlayBillingManager"
    }
}

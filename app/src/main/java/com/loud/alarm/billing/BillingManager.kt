package com.loud.alarm.billing

import android.app.Activity
import android.content.Context
import android.util.Log
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
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.loud.alarm.analytics.AnalyticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsLogger: AnalyticsLogger
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_QR_CODE = "qr_code_challenge"

        // Subscription product IDs
        const val PRODUCT_ID_LIFETIME = "s2w_lifetime"
        const val PRODUCT_ID_MONTHLY = "s2w_monthly"
        const val PRODUCT_ID_YEARLY = "s2w_yearly"

        // All subscription-related product IDs for quick lookup
        val SUBSCRIPTION_PRODUCT_IDS = setOf(PRODUCT_ID_MONTHLY, PRODUCT_ID_YEARLY)
        val ALL_PREMIUM_PRODUCT_IDS = setOf(PRODUCT_ID_LIFETIME, PRODUCT_ID_MONTHLY, PRODUCT_ID_YEARLY)
    }

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    // Product details for each subscription product
    private var lifetimeProductDetails: ProductDetails? = null
    private var monthlyProductDetails: ProductDetails? = null
    private var yearlyProductDetails: ProductDetails? = null

    private val _isQrCodePurchased = MutableStateFlow(false)
    val isQrCodePurchased: StateFlow<Boolean> = _isQrCodePurchased.asStateFlow()

    private val _qrCodePrice = MutableStateFlow<String?>(null)
    val qrCodePrice: StateFlow<String?> = _qrCodePrice.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    // Price flows for each plan
    private val _lifetimePrice = MutableStateFlow<String?>(null)
    val lifetimePrice: StateFlow<String?> = _lifetimePrice.asStateFlow()

    private val _monthlyPrice = MutableStateFlow<String?>(null)
    val monthlyPrice: StateFlow<String?> = _monthlyPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow<String?>(null)
    val yearlyPrice: StateFlow<String?> = _yearlyPrice.asStateFlow()

    // Track which plan is active
    private val _activePlan = MutableStateFlow<String?>(null)
    val activePlan: StateFlow<String?> = _activePlan.asStateFlow()

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()

        startConnection()
    }

    private var retryCount = 0
    private val maxRetries = 5

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    _isBillingReady.value = true
                    retryCount = 0
                    queryProductDetails()
                    querySubscriptionDetails()
                    queryExistingPurchases()
                    queryExistingSubscriptions()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                _isBillingReady.value = false
                if (retryCount < maxRetries) {
                    retryCount++
                    val delay = (retryCount * 3000).toLong()
                    Log.d(TAG, "Retrying billing connection in ${delay}ms (attempt $retryCount/$maxRetries)")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startConnection()
                    }, delay)
                } else {
                    Log.e(TAG, "Max billing connection retries reached")
                }
            }
        })
    }

    private fun queryProductDetails() {
        // Query in-app products: QR code + Lifetime
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_QR_CODE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList
                for (details in productDetailsList) {
                    when (details.productId) {
                        PRODUCT_ID_QR_CODE -> {
                            productDetails = details
                            _qrCodePrice.value = details.oneTimePurchaseOfferDetails?.formattedPrice
                            Log.d(TAG, "QR Code price: ${_qrCodePrice.value}")
                        }
                        PRODUCT_ID_LIFETIME -> {
                            lifetimeProductDetails = details
                            _lifetimePrice.value = details.oneTimePurchaseOfferDetails?.formattedPrice
                            Log.d(TAG, "Lifetime price: ${_lifetimePrice.value}")
                        }
                    }
                }
                // Fallbacks
                if (_qrCodePrice.value == null) _qrCodePrice.value = "$2.99"
                if (_lifetimePrice.value == null) _lifetimePrice.value = "$20.00"
            } else {
                Log.e(TAG, "Failed to query in-app product details: ${billingResult.debugMessage}")
                _qrCodePrice.value = "$2.99"
                _lifetimePrice.value = "$20.00"
            }
        }
    }

    private fun querySubscriptionDetails() {
        val subProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(subProductList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList
                for (details in productDetailsList) {
                    when (details.productId) {
                        PRODUCT_ID_MONTHLY -> {
                            monthlyProductDetails = details
                            val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
                            val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                            _monthlyPrice.value = pricingPhase?.formattedPrice
                            Log.d(TAG, "Monthly price: ${_monthlyPrice.value}")
                        }
                        PRODUCT_ID_YEARLY -> {
                            yearlyProductDetails = details
                            val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
                            val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                            _yearlyPrice.value = pricingPhase?.formattedPrice
                            Log.d(TAG, "Yearly price: ${_yearlyPrice.value}")
                        }
                    }
                }
                // Fallbacks
                if (_monthlyPrice.value == null) _monthlyPrice.value = "$1.49"
                if (_yearlyPrice.value == null) _yearlyPrice.value = "$12.99"
            } else {
                Log.e(TAG, "Failed to query subscription details: ${billingResult.debugMessage}")
                _monthlyPrice.value = "$1.49"
                _yearlyPrice.value = "$12.99"
            }
        }
    }

    private fun queryExistingPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val qrPurchased = purchaseList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_QR_CODE) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    isValidPurchase(purchase)
                }
                _isQrCodePurchased.value = qrPurchased
                Log.d(TAG, "QR Code purchased: $qrPurchased")

                // Check for lifetime purchase
                val lifetimePurchased = purchaseList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_LIFETIME) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    isValidPurchase(purchase)
                }
                if (lifetimePurchased) {
                    _isSubscribed.value = true
                    _activePlan.value = PRODUCT_ID_LIFETIME
                    analyticsLogger.setPremiumPlan(PRODUCT_ID_LIFETIME)
                    Log.d(TAG, "Lifetime purchased: true")
                }

                // Acknowledge any unacknowledged purchases
                purchaseList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && 
                        !purchase.isAcknowledged && isValidPurchase(purchase)) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    private fun queryExistingSubscriptions() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchaseList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && isValidPurchase(purchase)) {
                        when {
                            purchase.products.contains(PRODUCT_ID_MONTHLY) -> {
                                _isSubscribed.value = true
                                _activePlan.value = PRODUCT_ID_MONTHLY
                                analyticsLogger.setPremiumPlan(PRODUCT_ID_MONTHLY)
                                Log.d(TAG, "Monthly subscription active")
                            }
                            purchase.products.contains(PRODUCT_ID_YEARLY) -> {
                                _isSubscribed.value = true
                                _activePlan.value = PRODUCT_ID_YEARLY
                                analyticsLogger.setPremiumPlan(PRODUCT_ID_YEARLY)
                                Log.d(TAG, "Yearly subscription active")
                            }
                        }
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
                Log.d(TAG, "Subscription active: ${_isSubscribed.value}, plan: ${_activePlan.value}")
            } else {
                Log.e(TAG, "Failed to query subscriptions: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = productDetails
        if (details == null) {
            Log.e(TAG, "Product details not available")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        analyticsLogger.logPurchaseFlowStarted(PRODUCT_ID_QR_CODE)
        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Launch subscription purchase flow for a specific plan.
     * @param activity The calling activity
     * @param planType One of PRODUCT_ID_LIFETIME, PRODUCT_ID_MONTHLY, PRODUCT_ID_YEARLY
     */
    fun launchSubscriptionPurchase(activity: Activity, planType: String) {
        val details = when (planType) {
            PRODUCT_ID_LIFETIME -> lifetimeProductDetails
            PRODUCT_ID_MONTHLY -> monthlyProductDetails
            PRODUCT_ID_YEARLY -> yearlyProductDetails
            else -> null
        }

        if (details == null) {
            Log.e(TAG, "Product details not available for plan: $planType")
            return
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        // For subscriptions, we need to set the offer token
        if (planType in SUBSCRIPTION_PRODUCT_IDS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken != null) {
                productDetailsParamsBuilder.setOfferToken(offerToken)
            } else {
                Log.e(TAG, "No offer token available for subscription: $planType")
                return
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        analyticsLogger.logPurchaseFlowStarted(planType)
        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && isValidPurchase(purchase)) {
                        // Check QR code purchase
                        if (purchase.products.contains(PRODUCT_ID_QR_CODE)) {
                            _isQrCodePurchased.value = true
                            analyticsLogger.logPurchaseCompleted(PRODUCT_ID_QR_CODE)
                            Log.d(TAG, "QR Code challenge purchased successfully!")
                        }
                        // Check subscription/lifetime purchases
                        for (productId in purchase.products) {
                            if (productId in ALL_PREMIUM_PRODUCT_IDS) {
                                _isSubscribed.value = true
                                _activePlan.value = productId
                                analyticsLogger.logPurchaseCompleted(productId)
                                Log.d(TAG, "Premium plan purchased: $productId")
                            }
                        }
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                analyticsLogger.logPurchaseCancelled()
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                analyticsLogger.logPurchaseFailed(billingResult.responseCode)
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun restorePurchases() {
        analyticsLogger.logRestorePurchases()
        queryExistingPurchases()
        queryExistingSubscriptions()
    }

    private fun isValidPurchase(purchase: Purchase): Boolean {
        if (Security.BASE_64_ENCODED_PUBLIC_KEY == "PLACEHOLDER_BASE64_PUBLIC_KEY") {
            Log.w(TAG, "Using placeholder public key. Ignoring signature verification for testing.")
            return true
        }
        return Security.verifyPurchase(
            Security.BASE_64_ENCODED_PUBLIC_KEY,
            purchase.originalJson,
            purchase.signature
        )
    }



    fun setSubscribed(subscribed: Boolean) {
        _isSubscribed.value = subscribed
        if (!subscribed) {
            analyticsLogger.setPremiumPlan(null)
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}

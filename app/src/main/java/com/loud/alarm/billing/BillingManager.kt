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

    /**
     * Holds structured info about a subscription offer, including introductory
     * pricing phases (free trial, discounted period) and the regular price.
     */
    data class SubscriptionOfferInfo(
        val hasIntroOffer: Boolean = false,
        val introPrice: String? = null,       // e.g. "$0.75" or "Free"
        val introPeriodDesc: String? = null,   // e.g. "for 3 months", "for 7 days"
        val regularPrice: String? = null,      // e.g. "$1.49"
        val regularPeriodDesc: String? = null,  // e.g. "per month", "per year"
        val discountPercentage: Int? = null,
        val offerToken: String? = null
    )

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    // Product details for each subscription product
    private var lifetimeProductDetails: ProductDetails? = null
    private var monthlyProductDetails: ProductDetails? = null
    private var yearlyProductDetails: ProductDetails? = null

    // Stored offer tokens for the best offer per plan (intro offer preferred)
    private var monthlyOfferToken: String? = null
    private var yearlyOfferToken: String? = null

    // Throttle: track last time purchases were queried to avoid excessive Google Play calls
    private var lastPurchaseQueryTimeMs: Long = 0L
    private val purchaseQueryThrottleMs: Long = 30_000L // 30 seconds

    private val _isQrCodePurchased = MutableStateFlow(false)
    val isQrCodePurchased: StateFlow<Boolean> = _isQrCodePurchased.asStateFlow()

    private val _qrCodePrice = MutableStateFlow<String?>(null)
    val qrCodePrice: StateFlow<String?> = _qrCodePrice.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    // Price flows for each plan (always the regular/base price)
    private val _lifetimePrice = MutableStateFlow<String?>(null)
    val lifetimePrice: StateFlow<String?> = _lifetimePrice.asStateFlow()

    private val _monthlyPrice = MutableStateFlow<String?>(null)
    val monthlyPrice: StateFlow<String?> = _monthlyPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow<String?>(null)
    val yearlyPrice: StateFlow<String?> = _yearlyPrice.asStateFlow()

    // Structured offer info with introductory pricing
    private val _monthlyOfferInfo = MutableStateFlow<SubscriptionOfferInfo?>(null)
    val monthlyOfferInfo: StateFlow<SubscriptionOfferInfo?> = _monthlyOfferInfo.asStateFlow()

    private val _yearlyOfferInfo = MutableStateFlow<SubscriptionOfferInfo?>(null)
    val yearlyOfferInfo: StateFlow<SubscriptionOfferInfo?> = _yearlyOfferInfo.asStateFlow()

    // Track which plan is active
    private val _activePlan = MutableStateFlow<String?>(null)
    val activePlan: StateFlow<String?> = _activePlan.asStateFlow()

    // Error state for UI feedback
    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError.asStateFlow()

    fun clearPurchaseError() {
        _purchaseError.value = null
    }

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
                    queryPurchases()
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
                Log.d(TAG, "Subscription query returned ${productDetailsList.size} products")
                for (details in productDetailsList) {
                    val offers = details.subscriptionOfferDetails
                    Log.d(TAG, "Found subscription product: ${details.productId}, offerCount: ${offers?.size}")
                    offers?.forEach { offer ->
                        Log.d(TAG, "  Offer: id=${offer.offerId}, basePlan=${offer.basePlanId}, " +
                                "phases=${offer.pricingPhases.pricingPhaseList.size}, token=${offer.offerToken.take(20)}...")
                        offer.pricingPhases.pricingPhaseList.forEachIndexed { i, phase ->
                            Log.d(TAG, "    Phase $i: ${phase.formattedPrice}, period=${phase.billingPeriod}, " +
                                    "cycles=${phase.billingCycleCount}, recurrence=${phase.recurrenceMode}")
                        }
                    }

                    when (details.productId) {
                        PRODUCT_ID_MONTHLY -> {
                            monthlyProductDetails = details
                            val offerInfo = processSubscriptionOffers(details, "month")
                            _monthlyOfferInfo.value = offerInfo
                            _monthlyPrice.value = offerInfo.regularPrice
                            monthlyOfferToken = offerInfo.offerToken
                            Log.d(TAG, "Monthly: regular=${offerInfo.regularPrice}, intro=${offerInfo.introPrice}, " +
                                    "hasIntro=${offerInfo.hasIntroOffer}, introPeriod=${offerInfo.introPeriodDesc}")
                        }
                        PRODUCT_ID_YEARLY -> {
                            yearlyProductDetails = details
                            val offerInfo = processSubscriptionOffers(details, "year")
                            _yearlyOfferInfo.value = offerInfo
                            _yearlyPrice.value = offerInfo.regularPrice
                            yearlyOfferToken = offerInfo.offerToken
                            Log.d(TAG, "Yearly: regular=${offerInfo.regularPrice}, intro=${offerInfo.introPrice}, " +
                                    "hasIntro=${offerInfo.hasIntroOffer}, introPeriod=${offerInfo.introPeriodDesc}")
                        }
                    }
                }
                // Fallbacks
                if (_monthlyPrice.value == null) _monthlyPrice.value = "$1.49"
                if (_yearlyPrice.value == null) _yearlyPrice.value = "$12.99"
            } else {
                Log.e(TAG, "Failed to query subscription details: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                _monthlyPrice.value = "$1.49"
                _yearlyPrice.value = "$12.99"
            }
        }
    }

    /**
     * Process all offers for a subscription product and return structured info.
     * Prefers the introductory offer (one with >1 pricing phases) over the base plan offer.
     * The regular price is always extracted from the LAST pricing phase of the best offer
     * (which is the recurring phase after any intro/trial phases).
     */
    private fun processSubscriptionOffers(
        details: ProductDetails,
        defaultPeriodLabel: String
    ): SubscriptionOfferInfo {
        val offers = details.subscriptionOfferDetails ?: return SubscriptionOfferInfo(
            regularPrice = null,
            regularPeriodDesc = "per $defaultPeriodLabel"
        )

        // Find the best offer: prefer intro offers (>1 pricing phase)
        val introOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.size > 1
        }
        // Fallback to base plan offer (1 pricing phase, usually no offerId)
        val basePlanOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.size == 1
        }

        if (introOffer != null) {
            val phases = introOffer.pricingPhases.pricingPhaseList
            // First phase = introductory (discounted/free), last phase = regular recurring
            val introPhase = phases.first()
            val regularPhase = phases.last()

            val introPriceAmountMicros = introPhase.priceAmountMicros
            val regularPriceAmountMicros = regularPhase.priceAmountMicros
            val introFormattedPrice = if (introPriceAmountMicros == 0L) "Free" else introPhase.formattedPrice

            var discountPercentage: Int? = null
            if (regularPriceAmountMicros > 0) {
                val discount = ((regularPriceAmountMicros - introPriceAmountMicros).toDouble() / regularPriceAmountMicros * 100).toInt()
                if (discount in 1..100) {
                    discountPercentage = discount
                }
            }

            // Build intro period description from billing period + cycle count
            val introPeriodDesc = buildIntroPeriodDescription(
                introPhase.billingPeriod,
                introPhase.billingCycleCount
            )

            val regularPeriodDesc = "per ${parseBillingPeriodLabel(regularPhase.billingPeriod, defaultPeriodLabel)}"

            return SubscriptionOfferInfo(
                hasIntroOffer = true,
                introPrice = introFormattedPrice,
                introPeriodDesc = introPeriodDesc,
                regularPrice = regularPhase.formattedPrice,
                regularPeriodDesc = regularPeriodDesc,
                discountPercentage = discountPercentage,
                offerToken = introOffer.offerToken
            )
        }

        if (basePlanOffer != null) {
            val regularPhase = basePlanOffer.pricingPhases.pricingPhaseList.first()
            val regularPeriodDesc = "per ${parseBillingPeriodLabel(regularPhase.billingPeriod, defaultPeriodLabel)}"

            return SubscriptionOfferInfo(
                hasIntroOffer = false,
                regularPrice = regularPhase.formattedPrice,
                regularPeriodDesc = regularPeriodDesc,
                offerToken = basePlanOffer.offerToken
            )
        }

        // Fallback: use whatever first offer is available
        val fallbackOffer = offers.first()
        val fallbackPhase = fallbackOffer.pricingPhases.pricingPhaseList.lastOrNull()
        return SubscriptionOfferInfo(
            regularPrice = fallbackPhase?.formattedPrice,
            regularPeriodDesc = "per $defaultPeriodLabel",
            offerToken = fallbackOffer.offerToken
        )
    }

    /**
     * Builds a human-readable intro period description.
     * E.g., billingPeriod="P1M", cycleCount=3 -> "for 3 months"
     *        billingPeriod="P1Y", cycleCount=1 -> "for 1 year"
     *        billingPeriod="P7D", cycleCount=1 -> "for 7 days"
     */
    private fun buildIntroPeriodDescription(billingPeriod: String, cycleCount: Int): String {
        // ISO 8601 duration: P1M, P3M, P1Y, P1W, P7D, etc.
        val regex = Regex("P(\\d+)([DWMY])")
        val match = regex.find(billingPeriod) ?: return "for $cycleCount period(s)"

        val amount = match.groupValues[1].toIntOrNull() ?: 1
        val unit = match.groupValues[2]

        val totalAmount = amount * cycleCount
        val unitLabel = when (unit) {
            "D" -> if (totalAmount == 1) "day" else "days"
            "W" -> if (totalAmount == 1) "week" else "weeks"
            "M" -> if (totalAmount == 1) "month" else "months"
            "Y" -> if (totalAmount == 1) "year" else "years"
            else -> "period(s)"
        }

        return "for $totalAmount $unitLabel"
    }

    /**
     * Parses ISO 8601 billing period to a human label.
     * E.g., "P1M" -> "month", "P1Y" -> "year", "P1W" -> "week"
     */
    private fun parseBillingPeriodLabel(billingPeriod: String, fallback: String): String {
        val regex = Regex("P(\\d+)([DWMY])")
        val match = regex.find(billingPeriod) ?: return fallback

        val amount = match.groupValues[1].toIntOrNull() ?: 1
        val unit = match.groupValues[2]

        return when (unit) {
            "D" -> if (amount == 1) "day" else "$amount days"
            "W" -> if (amount == 1) "week" else "$amount weeks"
            "M" -> if (amount == 1) "month" else "$amount months"
            "Y" -> if (amount == 1) "year" else "$amount years"
            else -> fallback
        }
    }

    /**
     * Re-query subscription details. Useful when the initial query may have
     * failed due to a timing or network issue.
     */
    fun retryQuerySubscriptionDetails() {
        if (billingClient?.isReady == true) {
            querySubscriptionDetails()
        } else {
            Log.w(TAG, "Billing client not ready, reconnecting...")
            startConnection()
        }
    }

    private fun queryPurchases() {
        lastPurchaseQueryTimeMs = System.currentTimeMillis()
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResultInApp, purchaseListInApp ->
            billingClient?.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { billingResultSubs, purchaseListSubs ->
                if (billingResultInApp.responseCode == BillingClient.BillingResponseCode.OK &&
                    billingResultSubs.responseCode == BillingClient.BillingResponseCode.OK
                ) {
                    var isPremium = false
                    var activePlanId: String? = null

                    // 1. Check lifetime purchase
                    val lifetimePurchased = purchaseListInApp.any { purchase ->
                        purchase.products.contains(PRODUCT_ID_LIFETIME) &&
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                isValidPurchase(purchase)
                    }

                    if (lifetimePurchased) {
                        isPremium = true
                        activePlanId = PRODUCT_ID_LIFETIME
                        Log.d(TAG, "Lifetime purchased: true")
                    }

                    // 2. Check subscriptions if lifetime is not active
                    if (!isPremium) {
                        for (purchase in purchaseListSubs) {
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && isValidPurchase(purchase)) {
                                if (purchase.products.contains(PRODUCT_ID_MONTHLY)) {
                                    isPremium = true
                                    activePlanId = PRODUCT_ID_MONTHLY
                                    Log.d(TAG, "Monthly subscription active")
                                    break
                                } else if (purchase.products.contains(PRODUCT_ID_YEARLY)) {
                                    isPremium = true
                                    activePlanId = PRODUCT_ID_YEARLY
                                    Log.d(TAG, "Yearly subscription active")
                                    break
                                }
                            }
                        }
                    }

                    // Set the subscription state exactly to whether we found an active plan
                    _isSubscribed.value = isPremium
                    _activePlan.value = activePlanId

                    if (isPremium && activePlanId != null) {
                        analyticsLogger.setPremiumPlan(activePlanId)
                    } else {
                        analyticsLogger.setPremiumPlan(null)
                    }

                    Log.d(TAG, "Premium active: ${_isSubscribed.value}, plan: ${_activePlan.value}")

                    // 3. Check QR code purchase
                    val qrPurchased = purchaseListInApp.any { purchase ->
                        purchase.products.contains(PRODUCT_ID_QR_CODE) &&
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                isValidPurchase(purchase)
                    }
                    _isQrCodePurchased.value = qrPurchased
                    Log.d(TAG, "QR Code purchased: $qrPurchased")

                    // 4. Acknowledge any unacknowledged purchases
                    val allPurchases = purchaseListInApp + purchaseListSubs
                    allPurchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            !purchase.isAcknowledged && isValidPurchase(purchase)
                        ) {
                            acknowledgePurchase(purchase)
                        }
                    }
                } else {
                    Log.e(
                        TAG, "Failed to query purchases. " +
                                "InApp Code: ${billingResultInApp.responseCode}, Subs Code: ${billingResultSubs.responseCode}"
                    )
                }
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
        // Clear any previous error
        _purchaseError.value = null

        if (billingClient?.isReady != true) {
            Log.e(TAG, "Billing client not ready")
            _purchaseError.value = "Google Play is not ready. Please check your internet connection and try again."
            startConnection()
            return
        }

        val details = when (planType) {
            PRODUCT_ID_LIFETIME -> lifetimeProductDetails
            PRODUCT_ID_MONTHLY -> monthlyProductDetails
            PRODUCT_ID_YEARLY -> yearlyProductDetails
            else -> null
        }

        if (details == null) {
            Log.e(TAG, "Product details not available for plan: $planType. Retrying query...")
            _purchaseError.value = "Loading plan details. Please wait a moment and try again."
            // Automatically retry fetching product details
            querySubscriptionDetails()
            queryProductDetails()
            return
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        // For subscriptions, use the stored offer token (which prefers intro offers)
        if (planType in SUBSCRIPTION_PRODUCT_IDS) {
            val storedToken = when (planType) {
                PRODUCT_ID_MONTHLY -> monthlyOfferToken
                PRODUCT_ID_YEARLY -> yearlyOfferToken
                else -> null
            }
            val offerToken = storedToken
                ?: details.subscriptionOfferDetails?.firstOrNull()?.offerToken

            if (offerToken != null) {
                productDetailsParamsBuilder.setOfferToken(offerToken)
                Log.d(TAG, "Using offer token for $planType: ${offerToken.take(20)}...")
            } else {
                Log.e(TAG, "No offer token available for subscription: $planType")
                _purchaseError.value = "Subscription offer not available. Please try again or choose a different plan."
                querySubscriptionDetails()
                return
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        analyticsLogger.logPurchaseFlowStarted(planType)
        val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
        if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchBillingFlow failed: code=${result?.responseCode}, msg=${result?.debugMessage}")
            _purchaseError.value = "Could not open purchase dialog. Please try again."
        }
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
        queryPurchases()
    }

    /**
     * Refresh purchases only if enough time has passed since the last query.
     * Use this for automatic lifecycle refreshes (e.g. onResume) to avoid
     * excessive Google Play queries. For manual restore, use [restorePurchases].
     */
    fun refreshPurchasesIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastPurchaseQueryTimeMs >= purchaseQueryThrottleMs) {
            Log.d(TAG, "Refreshing purchases (stale check passed)")
            queryPurchases()
        } else {
            Log.d(TAG, "Skipping purchase refresh (last query ${(now - lastPurchaseQueryTimeMs) / 1000}s ago)")
        }
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

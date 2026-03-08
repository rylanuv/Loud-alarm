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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_QR_CODE = "qr_code_challenge"
    }

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    private val _isQrCodePurchased = MutableStateFlow(false)
    val isQrCodePurchased: StateFlow<Boolean> = _isQrCodePurchased.asStateFlow()

    private val _qrCodePrice = MutableStateFlow<String?>(null)
    val qrCodePrice: StateFlow<String?> = _qrCodePrice.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
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
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                _isBillingReady.value = false
                // Retry connection with backoff
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
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_QR_CODE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    productDetails = productDetailsList[0]
                    _qrCodePrice.value = productDetailsList[0].oneTimePurchaseOfferDetails?.formattedPrice
                    Log.d(TAG, "Product details loaded: price=${_qrCodePrice.value}")
                } else {
                    Log.d(TAG, "No product details found for $PRODUCT_ID_QR_CODE")
                    // Fallback price for display when product isn't set up in Play Console yet
                    _qrCodePrice.value = "$2.99"
                }
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                _qrCodePrice.value = "$2.99"
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
                val purchased = purchaseList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_QR_CODE) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isQrCodePurchased.value = purchased
                Log.d(TAG, "QR Code purchased: $purchased")

                // Acknowledge any unacknowledged purchases
                purchaseList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
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

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (purchase.products.contains(PRODUCT_ID_QR_CODE)) {
                            _isQrCodePurchased.value = true
                            Log.d(TAG, "QR Code challenge purchased successfully!")
                        }
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
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

    fun setDebugPremium(enabled: Boolean) {
        if (enabled) {
            _isQrCodePurchased.value = true
        } else {
            queryExistingPurchases()
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}

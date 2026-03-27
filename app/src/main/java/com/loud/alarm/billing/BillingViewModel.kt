package com.loud.alarm.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val isQrCodePurchased: StateFlow<Boolean> = billingManager.isQrCodePurchased
    val qrCodePrice: StateFlow<String?> = billingManager.qrCodePrice

    val isSubscribed: StateFlow<Boolean> = billingManager.isSubscribed
    val activePlan: StateFlow<String?> = billingManager.activePlan

    // Prices for each plan
    val lifetimePrice: StateFlow<String?> = billingManager.lifetimePrice
    val monthlyPrice: StateFlow<String?> = billingManager.monthlyPrice
    val yearlyPrice: StateFlow<String?> = billingManager.yearlyPrice

    fun purchaseQrCode(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    /**
     * Launch the purchase/subscription flow for a specific plan.
     * @param planType One of BillingManager.PRODUCT_ID_LIFETIME, PRODUCT_ID_MONTHLY, PRODUCT_ID_YEARLY
     */
    fun purchaseSubscription(activity: Activity, planType: String) {
        billingManager.launchSubscriptionPurchase(activity, planType)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun setSubscribed(subscribed: Boolean) {
        billingManager.setSubscribed(subscribed)
    }
}

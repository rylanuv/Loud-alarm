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

    fun purchaseQrCode(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }
    
    fun setSubscribed(subscribed: Boolean) {
        billingManager.setSubscribed(subscribed)
    }
}

package com.loud.alarm

import android.app.Application
import com.loud.alarm.billing.BillingManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LoudAlarmApp : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()
        billingManager.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        billingManager.destroy()
    }
}

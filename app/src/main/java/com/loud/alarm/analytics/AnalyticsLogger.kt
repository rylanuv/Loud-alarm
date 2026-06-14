package com.loud.alarm.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "Firebase is not configured. Add app/google-services.json to enable analytics.")
            null
        } else {
            FirebaseAnalytics.getInstance(context)
        }
    }

    fun logScreen(screenName: String) {
        logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName.take(MAX_SCREEN_NAME_LENGTH),
                FirebaseAnalytics.Param.SCREEN_CLASS to "MainActivity"
            )
        )
    }

    fun logAlarmSaved(isNewAlarm: Boolean, challengeCount: Int, challengeTypes: String, isRepeating: Boolean, wakeUpCheckMinutes: Int) {
        logEvent(
            if (isNewAlarm) Event.ALARM_CREATED else Event.ALARM_UPDATED,
            mapOf(
                Param.CHALLENGE_COUNT to challengeCount.toLong(),
                Param.CHALLENGE_TYPES to challengeTypes,
                Param.IS_REPEATING to isRepeating.toLongValue(),
                Param.WAKE_UP_CHECK_MINUTES to wakeUpCheckMinutes.toLong()
            )
        )
    }

    fun logAlarmToggled(enabled: Boolean, challengeCount: Int, challengeTypes: String, isRepeating: Boolean) {
        logEvent(
            if (enabled) Event.ALARM_ENABLED else Event.ALARM_DISABLED,
            mapOf(
                Param.CHALLENGE_COUNT to challengeCount.toLong(),
                Param.CHALLENGE_TYPES to challengeTypes,
                Param.IS_REPEATING to isRepeating.toLongValue()
            )
        )
    }

    fun logAlarmDeleted(challengeCount: Int, challengeTypes: String, isRepeating: Boolean) {
        logEvent(
            Event.ALARM_DELETED,
            mapOf(
                Param.CHALLENGE_COUNT to challengeCount.toLong(),
                Param.CHALLENGE_TYPES to challengeTypes,
                Param.IS_REPEATING to isRepeating.toLongValue()
            )
        )
    }

    fun logAlarmTriggered(challengeCount: Int, challengeTypes: String, wakeUpCheckMinutes: Int) {
        logEvent(
            Event.ALARM_TRIGGERED,
            mapOf(
                Param.CHALLENGE_COUNT to challengeCount.toLong(),
                Param.CHALLENGE_TYPES to challengeTypes,
                Param.WAKE_UP_CHECK_MINUTES to wakeUpCheckMinutes.toLong()
            )
        )
    }

    fun logAlarmDismissed(challengeCount: Int, challengeTypes: String, wakeUpCheckMinutes: Int) {
        logEvent(
            Event.ALARM_DISMISSED,
            mapOf(
                Param.CHALLENGE_COUNT to challengeCount.toLong(),
                Param.CHALLENGE_TYPES to challengeTypes,
                Param.WAKE_UP_CHECK_MINUTES to wakeUpCheckMinutes.toLong()
            )
        )
    }

    fun logAlarmSnoozed(minutes: Int, challengeCount: Int, challengeTypes: String) {
        logEvent(
            Event.ALARM_SNOOZED,
            mapOf(
                Param.SNOOZE_MINUTES to minutes.toLong(),
                Param.CHALLENGE_COUNT to challengeCount.toLong(),
                Param.CHALLENGE_TYPES to challengeTypes
            )
        )
    }

    fun logPurchaseFlowStarted(productId: String) {
        logEvent(Event.PURCHASE_FLOW_STARTED, mapOf(Param.PRODUCT_ID to productId))
    }

    fun logPurchaseCompleted(productId: String) {
        logEvent(Event.PURCHASE_COMPLETED, mapOf(Param.PRODUCT_ID to productId))
        setUserProperty(UserProperty.PREMIUM_PLAN, productId)
    }

    fun logPurchaseCancelled() {
        logEvent(Event.PURCHASE_CANCELLED)
    }

    fun logPurchaseFailed(responseCode: Int) {
        logEvent(Event.PURCHASE_FAILED, mapOf(Param.RESPONSE_CODE to responseCode.toLong()))
    }

    fun logRestorePurchases() {
        logEvent(Event.RESTORE_PURCHASES)
    }

    fun setPremiumPlan(planId: String?) {
        setUserProperty(UserProperty.PREMIUM_PLAN, planId)
    }

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        val analytics = firebaseAnalytics ?: return
        analytics.logEvent(name, params.toBundle())
    }

    private fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics?.setUserProperty(name, value?.take(MAX_USER_PROPERTY_VALUE_LENGTH))
    }

    private fun Map<String, Any?>.toBundle(): Bundle {
        return Bundle().apply {
            forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value.take(MAX_STRING_PARAM_LENGTH))
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putDouble(key, value.toDouble())
                    is Boolean -> putLong(key, value.toLongValue())
                }
            }
        }
    }

    private fun Boolean.toLongValue(): Long = if (this) 1L else 0L

    private object Event {
        const val ALARM_CREATED = "alarm_created"
        const val ALARM_UPDATED = "alarm_updated"
        const val ALARM_ENABLED = "alarm_enabled"
        const val ALARM_DISABLED = "alarm_disabled"
        const val ALARM_DELETED = "alarm_deleted"
        const val ALARM_TRIGGERED = "alarm_triggered"
        const val ALARM_DISMISSED = "alarm_dismissed"
        const val ALARM_SNOOZED = "alarm_snoozed"
        const val PURCHASE_FLOW_STARTED = "purchase_flow_started"
        const val PURCHASE_COMPLETED = "purchase_completed"
        const val PURCHASE_CANCELLED = "purchase_cancelled"
        const val PURCHASE_FAILED = "purchase_failed"
        const val RESTORE_PURCHASES = "restore_purchases"
    }

    private object Param {
        const val CHALLENGE_COUNT = "challenge_count"
        const val CHALLENGE_TYPES = "challenge_types"
        const val IS_REPEATING = "is_repeating"
        const val WAKE_UP_CHECK_MINUTES = "wake_up_check_minutes"
        const val SNOOZE_MINUTES = "snooze_minutes"
        const val PRODUCT_ID = "product_id"
        const val RESPONSE_CODE = "response_code"
    }

    private object UserProperty {
        const val PREMIUM_PLAN = "premium_plan"
    }

    private companion object {
        private const val TAG = "AnalyticsLogger"
        private const val MAX_SCREEN_NAME_LENGTH = 36
        private const val MAX_STRING_PARAM_LENGTH = 100
        private const val MAX_USER_PROPERTY_VALUE_LENGTH = 36
    }
}

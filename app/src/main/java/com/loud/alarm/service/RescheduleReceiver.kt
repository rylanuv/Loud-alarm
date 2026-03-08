package com.loud.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.loud.alarm.data.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles system events that invalidate scheduled alarms:
 *  - TIME_SET: user changed the clock manually
 *  - TIMEZONE_CHANGED: user changed timezone or traveled
 *  - LOCALE_CHANGED: rare, but some OEMs reset alarms
 *  - MY_PACKAGE_REPLACED: app update wipes all PendingIntents
 *
 * On any of these events we re-schedule every enabled alarm.
 */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlarmRepository

    @Inject
    lateinit var scheduler: AlarmScheduler

    companion object {
        private const val TAG = "RescheduleReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received action: $action — rescheduling all enabled alarms")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarms = repository.getEnabledAlarms()
                Log.d(TAG, "Found ${alarms.size} enabled alarm(s) to reschedule")
                alarms.forEach { alarm ->
                    try {
                        scheduler.schedule(alarm)
                        Log.d(TAG, "Rescheduled alarm ${alarm.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reschedule alarm ${alarm.id}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load alarms for rescheduling", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

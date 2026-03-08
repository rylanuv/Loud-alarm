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
 * Reschedules all enabled alarms after device boot.
 * Handles both standard BOOT_COMPLETED and manufacturer-specific quick-boot intents
 * (HTC, Xiaomi, etc.) since AlarmManager alarms are lost on reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlarmRepository

    @Inject
    lateinit var scheduler: AlarmScheduler

    companion object {
        private const val TAG = "BootReceiver"
        private val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",   // HTC
            "com.htc.intent.action.QUICKBOOT_POWERON",    // HTC alternate
            "android.intent.action.REBOOT",               // Some custom ROMs
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action in BOOT_ACTIONS) {
            Log.d(TAG, "Boot event received ($action) — rescheduling all enabled alarms")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = repository.getEnabledAlarms()
                    Log.d(TAG, "Found ${alarms.size} enabled alarm(s) to reschedule after boot")
                    alarms.forEach { alarm ->
                        try {
                            scheduler.schedule(alarm)
                            Log.d(TAG, "Rescheduled alarm ${alarm.id} after boot")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to reschedule alarm ${alarm.id}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load alarms for rescheduling after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

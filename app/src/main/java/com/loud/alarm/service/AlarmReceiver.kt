package com.loud.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.ui.alarm.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlarmRepository

    @Inject
    lateinit var scheduler: AlarmScheduler

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
        val isVolumeBoostEnabled =
            intent.getBooleanExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, false)
        if (alarmId == -1) return

        Log.d(TAG, "Alarm triggered: $alarmId")

        // 1. Start foreground service to play sound + vibrate
        //    This MUST happen first — service handles audio, vibration, wake lock
        try {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, isVolumeBoostEnabled)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "AlarmService started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmService", e)
        }

        // 2. Receiver launch is intentional: the alarm PendingIntent was delivered by the system.
        try {
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            context.startActivity(activityIntent)
            Log.d(TAG, "AlarmActivity launched directly from AlarmReceiver")
        } catch (e: Exception) {
            Log.w(TAG, "Direct AlarmActivity launch from receiver failed; service watchdog will retry", e)
        }

        // 3. Reschedule if repeating, or disable if one-shot
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = repository.getAlarm(alarmId)
                if (alarm != null && alarm.enabled) {
                    if (alarm.daysOfWeek.isNotEmpty()) {
                         // Repeating alarm — schedule next occurrence
                         scheduler.schedule(alarm)
                         Log.d(TAG, "Repeating alarm ${alarm.id} rescheduled for next occurrence")
                    } else {
                        // Single shot alarm — disable it so it doesn't show as active
                         repository.update(alarm.copy(enabled = false))
                         Log.d(TAG, "One-shot alarm ${alarm.id} disabled after firing")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm reschedule/disable for id=$alarmId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

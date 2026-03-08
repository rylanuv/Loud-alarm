package com.loud.alarm.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives the tap/click on the Wake Up Check notification.
 * When the user taps it, we:
 *   1. Cancel the scheduled re-ring alarm (so the alarm doesn't fire again).
 *   2. Dismiss the notification.
 */
class WakeUpCheckConfirmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WakeUpCheckConfirm"
        /** Request-code offset so re-ring PendingIntents don't clash with other alarms. */
        const val RE_RING_REQUEST_CODE_BASE = 20000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d(TAG, "User confirmed wake-up check for alarm: $alarmId")

        // 1. Cancel the pending re-ring alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reRingIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val reRingPendingIntent = PendingIntent.getBroadcast(
            context,
            RE_RING_REQUEST_CODE_BASE + alarmId,
            reRingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reRingPendingIntent)
        reRingPendingIntent.cancel()
        Log.d(TAG, "Re-ring alarm cancelled for alarm: $alarmId")

        // 2. Dismiss the wake-up check notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(WakeUpCheckReceiver.NOTIFICATION_ID_BASE + alarmId)
        Log.d(TAG, "Wake-up check notification dismissed for alarm: $alarmId")
    }
}

package com.loud.alarm.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.loud.alarm.R

class WakeUpCheckReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "wake_up_check_channel"
        const val NOTIFICATION_ID_BASE = 10000
        private const val TAG = "WakeUpCheckReceiver"
        /** How long the user has to tap the notification before the alarm re-rings (5 minutes). */
        private const val RE_RING_DELAY_MS = 5 * 60 * 1000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: ""
        val isVolumeBoostEnabled = intent.getBooleanExtra("IS_VOLUME_BOOST_ENABLED", false)
        Log.d(TAG, "Wake Up Check triggered for alarm: $alarmId")

        createNotificationChannel(context)

        // --- PendingIntent: tapping notification confirms the user is awake ---
        val confirmIntent = Intent(context, WakeUpCheckConfirmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            WakeUpCheckConfirmReceiver.RE_RING_REQUEST_CODE_BASE + alarmId + 50000, // unique code
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val labelPart = if (alarmLabel.isNotEmpty()) " ($alarmLabel)" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Wake Up Check!")
            .setContentText("Tap this notification to confirm you're awake!$labelPart")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Click on this notification to confirm you're awake!$labelPart\nIf you don't tap within 5 minutes, the alarm will ring again!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(confirmPendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setOngoing(true) // Cannot be swiped away — must be tapped
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + alarmId, notification)
        Log.d(TAG, "Wake Up Check notification posted for alarm: $alarmId")

        // --- Schedule re-ring: if user doesn't tap within 5 minutes, trigger the alarm again ---
        scheduleReRing(context, alarmId, isVolumeBoostEnabled)
    }

    /**
     * Schedules the alarm to fire again in [RE_RING_DELAY_MS] if the user
     * does not confirm they are awake by tapping the notification.
     */
    private fun scheduleReRing(context: Context, alarmId: Int, isVolumeBoostEnabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reRingIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("IS_VOLUME_BOOST_ENABLED", isVolumeBoostEnabled)
        }
        val reRingPendingIntent = PendingIntent.getBroadcast(
            context,
            WakeUpCheckConfirmReceiver.RE_RING_REQUEST_CODE_BASE + alarmId,
            reRingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMs = System.currentTimeMillis() + RE_RING_DELAY_MS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, reRingPendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, reRingPendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, reRingPendingIntent)
        }

        Log.d(TAG, "Re-ring scheduled for alarm $alarmId in 5 minutes (if user doesn't confirm)")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wake Up Check",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Follow-up notification to make sure you're awake"
                enableVibration(true)
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(alarmSound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

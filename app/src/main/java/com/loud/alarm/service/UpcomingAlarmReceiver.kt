package com.loud.alarm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.loud.alarm.R
import com.loud.alarm.MainActivity

/**
 * Shows a heads-up notification ~30 minutes before a scheduled alarm fires,
 * giving the user a gentle reminder that an alarm is approaching.
 *
 * Scheduled/cancelled by [AlarmSchedulerImpl] whenever an alarm is
 * scheduled or cancelled, and only when the user has enabled the
 * "Upcoming Alarm Notification" setting.
 */
class UpcomingAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "upcoming_alarm_channel"
        const val NOTIFICATION_ID_BASE = 20000
        /** Offset added to the alarm ID to produce a unique PendingIntent request code. */
        const val REQUEST_CODE_OFFSET = 30000
        private const val TAG = "UpcomingAlarmReceiver"
        const val EXTRA_ALARM_LABEL = "UPCOMING_ALARM_LABEL"
        const val EXTRA_ALARM_HOUR = "UPCOMING_ALARM_HOUR"
        const val EXTRA_ALARM_MINUTE = "UPCOMING_ALARM_MINUTE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
        if (alarmId == -1) return

        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: ""
        val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, -1)

        Log.d(TAG, "Upcoming alarm notification triggered for alarm $alarmId")

        createNotificationChannel(context)

        // Tapping opens the app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OFFSET + alarmId + 1000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format the alarm time for display
        val timeText = if (hour >= 0 && minute >= 0) {
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format("%d:%02d %s", displayHour, minute, amPm)
        } else {
            ""
        }

        val labelPart = if (label.isNotEmpty()) " — $label" else ""
        val timePart = if (timeText.isNotEmpty()) " at $timeText" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Alarm in 30 minutes")
            .setContentText("Your alarm$timePart$labelPart is coming up soon")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your alarm$timePart$labelPart will ring in about 30 minutes. Get ready to wake up!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + alarmId, notification)
        Log.d(TAG, "Upcoming alarm notification posted for alarm $alarmId")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upcoming Alarm Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you 30 minutes before an alarm rings"
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

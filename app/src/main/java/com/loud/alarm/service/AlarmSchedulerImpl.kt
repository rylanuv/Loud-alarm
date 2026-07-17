package com.loud.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.SettingsRepository
import com.loud.alarm.ui.alarm.AlarmActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmSchedulerImpl"
        private const val UPCOMING_LEAD_TIME_MS = 30 * 60 * 1000L // 30 minutes
    }

    override fun schedule(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, alarm.isVolumeBoostEnabled)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateTriggerTime(alarm)

        // Create a PendingIntent for the AlarmActivity to show in the status bar alarm icon
        val showIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setAlarmClock - this is the KEY for alarm apps:
        // 1. Grants the app permission to start activities from background
        // 2. Shows alarm icon in status bar
        // 3. Immune to Doze mode
        // 4. Fires at exact time
        // 5. Survives battery optimization on ALL manufacturer skins
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Alarm ${alarm.id} scheduled with setAlarmClock for $triggerTime (${
                java.time.Instant.ofEpochMilli(triggerTime).atZone(ZoneId.systemDefault())
            })")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm with setAlarmClock, falling back", e)
            // Fallback for edge cases where setAlarmClock fails
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Alarm ${alarm.id} scheduled with setExactAndAllowWhileIdle (fallback)")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Alarm ${alarm.id} scheduled with setAndAllowWhileIdle (last resort)")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "All scheduling methods failed for alarm ${alarm.id}", e2)
            }
        }

        // Schedule upcoming alarm notification (30 min before)
        scheduleUpcomingNotification(alarm, triggerTime)
    }

    override fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm ${alarm.id} cancelled")

        // Also cancel any upcoming notification for this alarm
        cancelUpcomingNotification(alarm.id)
    }

    override fun scheduleSnooze(alarm: Alarm, delayMinutes: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, alarm.isVolumeBoostEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val showIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Snooze for alarm ${alarm.id} scheduled in $delayMinutes minutes at ${
                java.time.Instant.ofEpochMilli(triggerTime).atZone(ZoneId.systemDefault())
            }")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling snooze, falling back", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.d(TAG, "Snooze for alarm ${alarm.id} scheduled with fallback method")
            } catch (e2: Exception) {
                Log.e(TAG, "All snooze scheduling methods failed for alarm ${alarm.id}", e2)
            }
        }
    }

    private fun calculateTriggerTime(alarm: Alarm): Long {
        val now = LocalDateTime.now()
        var alarmTime = now.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)

        if (alarm.daysOfWeek.isEmpty()) {
            // Single shot
             if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
                alarmTime = alarmTime.plusDays(1)
            }
        } else {
            // Repeating: find next matching day
            var currentCandidate = now.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)
            if (currentCandidate.isBefore(now) || currentCandidate.isEqual(now)) {
                 currentCandidate = currentCandidate.plusDays(1)
            }
            
            for (i in 0..7) {
                 val dayOfWeek = javaDayToCalendarDay(currentCandidate.dayOfWeek.value)
                 if (alarm.daysOfWeek.contains(dayOfWeek)) {
                     return currentCandidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                 }
                 currentCandidate = currentCandidate.plusDays(1)
            }
            // Fallback — should never reach here if daysOfWeek contains valid days
            Log.w(TAG, "Could not find next matching day for alarm ${alarm.id}, daysOfWeek=${alarm.daysOfWeek}")
            return currentCandidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        return alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    // Convert java.time.DayOfWeek (1=Mon) to Calendar constants (1=Sun, 2=Mon)
    private fun javaDayToCalendarDay(javaDay: Int): Int {
        return if (javaDay == 7) 1 else javaDay + 1
    }

    // --- Upcoming Alarm Notification ---

    /**
     * Schedules a notification to fire 30 minutes before [alarmTriggerTimeMs],
     * but only if the user has enabled the "Upcoming Alarm Notification" setting.
     * If the alarm is less than 30 minutes away, no notification is scheduled.
     */
    private fun scheduleUpcomingNotification(alarm: Alarm, alarmTriggerTimeMs: Long) {
        // Read the setting — schedule() is always called from a background thread
        val enabled = try {
            runBlocking { settingsRepository.upcomingAlarmNotificationEnabled.first() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read upcoming alarm notification setting", e)
            false
        }

        // Always cancel any previous upcoming notification for this alarm first
        cancelUpcomingNotification(alarm.id)

        if (!enabled) {
            Log.d(TAG, "Upcoming alarm notification disabled by user, skipping for alarm ${alarm.id}")
            return
        }

        val notifyTime = alarmTriggerTimeMs - UPCOMING_LEAD_TIME_MS
        if (notifyTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Alarm ${alarm.id} is less than 30 min away, skipping upcoming notification")
            return
        }

        val intent = Intent(context, UpcomingAlarmReceiver::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra(UpcomingAlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(UpcomingAlarmReceiver.EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(UpcomingAlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            UpcomingAlarmReceiver.REQUEST_CODE_OFFSET + alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notifyTime,
                pendingIntent
            )
            Log.d(TAG, "Upcoming notification scheduled for alarm ${alarm.id} at ${
                java.time.Instant.ofEpochMilli(notifyTime).atZone(ZoneId.systemDefault())
            }")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule upcoming notification for alarm ${alarm.id}", e)
        }
    }

    /**
     * Cancels any previously scheduled upcoming notification for the given alarm.
     */
    private fun cancelUpcomingNotification(alarmId: Int) {
        val intent = Intent(context, UpcomingAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            UpcomingAlarmReceiver.REQUEST_CODE_OFFSET + alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}

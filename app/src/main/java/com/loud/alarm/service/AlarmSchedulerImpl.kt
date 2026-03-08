package com.loud.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.loud.alarm.data.Alarm
import com.loud.alarm.ui.alarm.AlarmActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmSchedulerImpl"
    }

    override fun schedule(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("IS_VOLUME_BOOST_ENABLED", alarm.isVolumeBoostEnabled)
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
            putExtra("ALARM_ID", alarm.id)
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
}

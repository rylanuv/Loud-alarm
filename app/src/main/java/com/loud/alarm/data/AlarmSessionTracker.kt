package com.loud.alarm.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSessionTracker @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("alarm_session_tracker", Context.MODE_PRIVATE)

    fun startOrGetSession(alarmId: Int): SessionData {
        val lastAlarmId = prefs.getInt("alarm_id", -1)
        val now = System.currentTimeMillis()
        val lastUpdate = prefs.getLong("last_update", 0)

        // If alarm ID changed or it's been more than 2 hours since last update, it's a new session
        if (lastAlarmId != alarmId || (now - lastUpdate) > 2 * 60 * 60 * 1000) {
            prefs.edit()
                .putInt("alarm_id", alarmId)
                .putLong("ring_start_time", now)
                .putInt("snooze_count", 0)
                .putLong("last_update", now)
                .apply()
            return SessionData(now, 0)
        } else {
            val start = prefs.getLong("ring_start_time", now)
            val snoozes = prefs.getInt("snooze_count", 0)
            prefs.edit().putLong("last_update", now).apply()
            return SessionData(start, snoozes)
        }
    }

    fun recordSnooze() {
        val snoozes = prefs.getInt("snooze_count", 0)
        prefs.edit()
            .putInt("snooze_count", snoozes + 1)
            .putLong("last_update", System.currentTimeMillis())
            .apply()
    }

    fun getSessionData(): SessionData {
        val now = System.currentTimeMillis()
        val start = prefs.getLong("ring_start_time", now)
        val snoozes = prefs.getInt("snooze_count", 0)
        return SessionData(start, snoozes)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    data class SessionData(val ringStartTime: Long, val snoozeCount: Int)
}

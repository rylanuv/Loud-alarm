package com.loud.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_sessions")
data class AlarmSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alarmId: Int, // The ID of the alarm that rang
    val ringStartTime: Long, // Epoch millis when it started ringing
    val dismissTime: Long, // Epoch millis when it was fully dismissed
    val snoozeCount: Int, // Number of times snoozed during this session
    val date: Long, // Just to group by day easily (e.g., start of day timestamp)
    val timeToWakeSeconds: Int // pre-calculated for convenience
)

package com.loud.alarm.service

import com.loud.alarm.data.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
    fun scheduleSnooze(alarm: Alarm, delayMinutes: Int)
}

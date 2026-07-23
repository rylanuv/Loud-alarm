package com.loud.alarm.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val alarmSessionDao: AlarmSessionDao
) {
    suspend fun recordSession(
        alarmId: Int,
        ringStartTime: Long,
        dismissTime: Long,
        snoozeCount: Int
    ) {
        val timeToWakeSeconds = ((dismissTime - ringStartTime) / 1000).toInt()
        val date = java.util.Calendar.getInstance().apply {
            timeInMillis = ringStartTime
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val session = AlarmSession(
            alarmId = alarmId,
            ringStartTime = ringStartTime,
            dismissTime = dismissTime,
            snoozeCount = snoozeCount,
            date = date,
            timeToWakeSeconds = timeToWakeSeconds
        )
        alarmSessionDao.insertSession(session)
    }

    fun getAllSessions(): Flow<List<AlarmSession>> = alarmSessionDao.getAllSessions()

    fun getRecentSessions(limit: Int): Flow<List<AlarmSession>> = alarmSessionDao.getRecentSessions(limit)

    fun getTotalSessionsCount(): Flow<Int> = alarmSessionDao.getTotalSessionsCount()

    fun getTotalSnoozesCount(): Flow<Int?> = alarmSessionDao.getTotalSnoozesCount()

    fun getAverageTimeToWake(): Flow<Int?> = alarmSessionDao.getAverageTimeToWake()
    
    fun getFastestTimeToWake(): Flow<Int?> = alarmSessionDao.getFastestTimeToWake()

    fun getSlowestTimeToWake(): Flow<Int?> = alarmSessionDao.getSlowestTimeToWake()

    fun getFlawlessWakesCount(): Flow<Int> = alarmSessionDao.getFlawlessWakesCount()

    fun getAllDismissTimes(): Flow<List<Long>> = alarmSessionDao.getAllDismissTimes()
    
    suspend fun deleteAllSessions() = alarmSessionDao.deleteAllSessions()
}

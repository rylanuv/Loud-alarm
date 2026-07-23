package com.loud.alarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmSessionDao {
    @Insert
    suspend fun insertSession(session: AlarmSession)

    @Query("SELECT * FROM alarm_sessions ORDER BY dismissTime DESC")
    fun getAllSessions(): Flow<List<AlarmSession>>

    @Query("SELECT * FROM alarm_sessions ORDER BY dismissTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<AlarmSession>>

    @Query("SELECT COUNT(*) FROM alarm_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT SUM(snoozeCount) FROM alarm_sessions")
    fun getTotalSnoozesCount(): Flow<Int?>
    
    @Query("SELECT AVG(timeToWakeSeconds) FROM alarm_sessions")
    fun getAverageTimeToWake(): Flow<Int?>
    
    @Query("SELECT MIN(timeToWakeSeconds) FROM alarm_sessions")
    fun getFastestTimeToWake(): Flow<Int?>

    @Query("SELECT MAX(timeToWakeSeconds) FROM alarm_sessions")
    fun getSlowestTimeToWake(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM alarm_sessions WHERE snoozeCount = 0")
    fun getFlawlessWakesCount(): Flow<Int>

    @Query("SELECT dismissTime FROM alarm_sessions")
    fun getAllDismissTimes(): Flow<List<Long>>

    @Query("DELETE FROM alarm_sessions")
    suspend fun deleteAllSessions()
}

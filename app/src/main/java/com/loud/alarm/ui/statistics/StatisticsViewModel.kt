package com.loud.alarm.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.data.AlarmSession
import com.loud.alarm.data.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: StatisticsRepository
) : ViewModel() {

    val totalAlarms: StateFlow<Int> = repository.getTotalSessionsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSnoozes: StateFlow<Int> = repository.getTotalSnoozesCount()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageTimeToWake: StateFlow<Int> = repository.getAverageTimeToWake()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val fastestTimeToWake: StateFlow<Int> = repository.getFastestTimeToWake()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val slowestTimeToWake: StateFlow<Int> = repository.getSlowestTimeToWake()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val flawlessWakesCount: StateFlow<Int> = repository.getFlawlessWakesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageWakeupTimeStr: StateFlow<String> = repository.getAllDismissTimes()
        .map { times ->
            if (times.isEmpty()) return@map "--:--"
            var totalMinutes = 0L
            times.forEach { timeMs ->
                val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
                val hours = cal.get(Calendar.HOUR_OF_DAY)
                val minutes = cal.get(Calendar.MINUTE)
                totalMinutes += (hours * 60) + minutes
            }
            val avgTotalMinutes = (totalMinutes / times.size).toInt()
            val avgHour = avgTotalMinutes / 60
            val avgMinute = avgTotalMinutes % 60
            
            val amPm = if (avgHour >= 12) "PM" else "AM"
            val displayHour = if (avgHour == 0) 12 else if (avgHour > 12) avgHour % 12 else avgHour
            val minuteStr = avgMinute.toString().padStart(2, '0')
            
            "$displayHour:$minuteStr $amPm"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "--:--")

    val recentSessions: StateFlow<List<AlarmSession>> = repository.getRecentSessions(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Generates a list of Pair<DayName, TimeToWakeSeconds> for the chart, padding empty days
    val chartData: StateFlow<List<Pair<String, Int>>> = recentSessions.map { sessions ->
        val result = mutableListOf<Pair<String, Int>>()
        val cal = Calendar.getInstance()
        
        // We'll map the last 7 days. 0 = today, -1 = yesterday, etc.
        for (i in -6..0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }
            val dayName = dayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
            
            // Find session for this day. (Very naive check by comparing YEAR and DAY_OF_YEAR)
            val sessionForDay = sessions.find { 
                val sessionCal = Calendar.getInstance().apply { timeInMillis = it.ringStartTime }
                sessionCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) && 
                sessionCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
            }
            
            result.add(Pair(dayName, sessionForDay?.timeToWakeSeconds ?: 0))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

package com.loud.alarm.ui.home

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.analytics.AnalyticsLogger
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.review.InAppReviewManager
import com.loud.alarm.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val reviewManager: InAppReviewManager,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .map { alarmList ->
            alarmList.sortedWith(
                compareBy<Alarm> { it.hour }
                    .thenBy { it.minute }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logic to find the absolute next alarm
    val nextAlarm: StateFlow<Alarm?> = alarms.map { alarmList ->
        alarmList.filter { it.enabled }.minByOrNull { calculateTimeUntilNext(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Formatted text "in 6h 32m"
    val timeUntilNextAlarmValues: StateFlow<String> = nextAlarm.map { alarm ->
        if (alarm == null) "" else formatDuration(calculateTimeUntilNext(alarm))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // We can't easily tick a flow every minute without a timer, but for now 
    // relying on state updates or just calculating on composition is okay-ish.
    // Ideally we'd have a ticker flow.
    
    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val newAlarm = alarm.copy(enabled = !alarm.enabled)
            repository.update(newAlarm)
            if (newAlarm.enabled) {
                scheduler.schedule(newAlarm)
            } else {
                scheduler.cancel(newAlarm)
            }
            analyticsLogger.logAlarmToggled(
                enabled = newAlarm.enabled,
                challengeCount = alarm.analyticsChallengeCount(),
                isRepeating = alarm.daysOfWeek.isNotEmpty()
            )
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.delete(alarm)
            analyticsLogger.logAlarmDeleted(
                challengeCount = alarm.analyticsChallengeCount(),
                isRepeating = alarm.daysOfWeek.isNotEmpty()
            )
        }
    }

    private fun calculateTimeUntilNext(alarm: Alarm): Long {
        val now = LocalDateTime.now()
        var alarmTime = now.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)

        if (alarm.daysOfWeek.isEmpty()) {
            // Single shot
            if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
                alarmTime = alarmTime.plusDays(1)
            }
        } else {
             // Repeating
             var currentCandidate = now.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)
             if (currentCandidate.isBefore(now) || currentCandidate.isEqual(now)) {
                 currentCandidate = currentCandidate.plusDays(1)
             }
             
             for (i in 0..7) {
                 val dayOfWeek = javaDayToCalendarDay(currentCandidate.dayOfWeek.value)
                 if (alarm.daysOfWeek.contains(dayOfWeek)) {
                     return Duration.between(now, currentCandidate).toMillis()
                 }
                 currentCandidate = currentCandidate.plusDays(1)
             }
             // Should verify logic above matches scheduler exactly
        }
        
        // Simple fallback calculation same as scheduler for sorting
        if (alarmTime.isBefore(now)) {
             alarmTime = alarmTime.plusDays(1)
        }
        return Duration.between(now, alarmTime).toMillis()
    }

    private fun formatDuration(millis: Long): String {
        val duration = Duration.ofMillis(millis)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    private fun javaDayToCalendarDay(javaDay: Int): Int {
        return if (javaDay == 7) 1 else javaDay + 1
    }

    /**
     * Check if conditions are met to show the in-app review prompt.
     * Should be called when HomeScreen resumes (user just came back from dismissing an alarm).
     */
    suspend fun shouldRequestReview(): Boolean {
        return reviewManager.shouldRequestReview()
    }

    /**
     * Launch the in-app review flow. Must be called with an Activity.
     */
    suspend fun requestReview(activity: android.app.Activity) {
        reviewManager.requestReview(activity)
    }

    private fun Alarm.analyticsChallengeCount(): Int {
        return challengeTypes.count { it != ChallengeType.NONE }
    }
}

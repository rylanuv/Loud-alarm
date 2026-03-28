package com.loud.alarm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.data.SettingsRepository
import com.loud.alarm.data.VibrationPattern
import com.loud.alarm.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.data.Alarm
import kotlinx.coroutines.flow.map

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val snoozeEnabled: StateFlow<Boolean> = settingsRepository.snoozeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val snoozeDuration: StateFlow<Int> = settingsRepository.snoozeDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val alarmVolume: StateFlow<Float> = settingsRepository.alarmVolume
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.8f)

    val fadeInEnabled: StateFlow<Boolean> = settingsRepository.fadeInEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fadeInDuration: StateFlow<Int> = settingsRepository.fadeInDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val darkModeEnabled: StateFlow<Boolean> = settingsRepository.darkModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSilenceDuration: StateFlow<Int> = settingsRepository.autoSilenceDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)
        
    val isPremium: StateFlow<Boolean> = billingManager.isSubscribed

    val vibrationPattern: StateFlow<String> = settingsRepository.vibrationPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VibrationPattern.DEVICE_DEFAULT.name)

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
        }
    }

    fun setSnoozeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSnoozeEnabled(enabled)
        }
    }

    fun setSnoozeDuration(duration: Int) {
        viewModelScope.launch {
            settingsRepository.setSnoozeDuration(duration)
        }
    }

    fun setAlarmVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.setAlarmVolume(volume)
        }
    }

    fun setFadeInEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFadeInEnabled(enabled)
        }
    }

    fun setFadeInDuration(duration: Int) {
        viewModelScope.launch {
            settingsRepository.setFadeInDuration(duration)
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkModeEnabled(enabled)
        }
    }

    fun setAutoSilenceDuration(duration: Int) {
        viewModelScope.launch {
            settingsRepository.setAutoSilenceDuration(duration)
        }
    }
    
    fun setDebugPremium(enabled: Boolean) {
        billingManager.setDebugPremium(enabled)
    }

    fun setVibrationPattern(patternName: String) {
        viewModelScope.launch {
            settingsRepository.setVibrationPattern(patternName)
        }
    }

    val nextAlarm: StateFlow<Alarm?> = alarmRepository.allAlarms.map { alarmList ->
        alarmList.filter { it.enabled }.minByOrNull { calculateTimeUntilNext(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun calculateTimeUntilNext(alarm: Alarm): Long {
        val now = java.time.LocalDateTime.now()
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
                     return java.time.Duration.between(now, currentCandidate).toMillis()
                 }
                 currentCandidate = currentCandidate.plusDays(1)
             }
        }
        
        if (alarmTime.isBefore(now)) {
             alarmTime = alarmTime.plusDays(1)
        }
        return java.time.Duration.between(now, alarmTime).toMillis()
    }

    private fun javaDayToCalendarDay(javaDay: Int): Int {
        return if (javaDay == 7) 1 else javaDay + 1
    }
}

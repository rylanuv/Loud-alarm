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

    val isDevModeEnabled: StateFlow<Boolean> = settingsRepository.isDevModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val skeletonOverlayEnabled: StateFlow<Boolean> = settingsRepository.skeletonOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
    


    fun setVibrationPattern(patternName: String) {
        viewModelScope.launch {
            settingsRepository.setVibrationPattern(patternName)
        }
    }

    fun setDevModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDevModeEnabled(enabled)
        }
    }

    fun setSkeletonOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSkeletonOverlayEnabled(enabled)
        }
    }
}

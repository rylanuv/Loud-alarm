package com.loud.alarm.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmActiveViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: com.loud.alarm.service.AlarmScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm.asStateFlow()
    val snoozeEnabled: StateFlow<Boolean> = settingsRepository.snoozeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun loadAlarm(id: Int) {
        viewModelScope.launch {
            _alarm.value = repository.getAlarm(id)
        }
    }

    fun snoozeAlarm(alarm: Alarm, minutes: Int) {
        viewModelScope.launch {
            if (!settingsRepository.snoozeEnabled.first()) return@launch

            val now = java.time.LocalTime.now()
            val snoozeTime = now.plusMinutes(minutes.toLong())
            
            val snoozedAlarm = Alarm(
                hour = snoozeTime.hour,
                minute = snoozeTime.minute,
                daysOfWeek = emptySet(),
                label = "Snooze: ${alarm.label}",
                soundUri = alarm.soundUri,
                challengeTypes = alarm.challengeTypes,
                mathDifficulty = alarm.mathDifficulty,
                barcodeValue = alarm.barcodeValue,
                isVolumeBoostEnabled = alarm.isVolumeBoostEnabled,
                sinkImageUri = alarm.sinkImageUri,
                scanObjectLabel = alarm.scanObjectLabel,
                enabled = true
            )
            
            val newId = repository.insert(snoozedAlarm)
            scheduler.schedule(snoozedAlarm.copy(id = newId.toInt()))
        }
    }
}

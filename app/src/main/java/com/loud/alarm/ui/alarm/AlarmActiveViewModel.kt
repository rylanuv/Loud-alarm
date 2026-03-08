package com.loud.alarm.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmActiveViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: com.loud.alarm.service.AlarmScheduler
) : ViewModel() {

    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm.asStateFlow()

    fun loadAlarm(id: Int) {
        viewModelScope.launch {
            _alarm.value = repository.getAlarm(id)
        }
    }

    fun snoozeAlarm(alarm: Alarm, minutes: Int) {
        viewModelScope.launch {
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
                enabled = true
            )
            
            val newId = repository.insert(snoozedAlarm)
            scheduler.schedule(snoozedAlarm.copy(id = newId.toInt()))
        }
    }
}

package com.loud.alarm.ui.alarm

import android.util.Log
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
    companion object {
        private const val TAG = "AlarmActiveViewModel"
    }

    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm.asStateFlow()
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()
    val snoozeEnabled: StateFlow<Boolean> = settingsRepository.snoozeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun loadAlarm(id: Int) {
        viewModelScope.launch {
            _loadError.value = null
            _alarm.value = null
            try {
                val loaded = repository.getAlarm(id)
                if (loaded == null) {
                    _loadError.value = "Alarm not found"
                } else {
                    _alarm.value = loaded
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load alarm id=$id", e)
                _loadError.value = "Could not load this alarm"
            }
        }
    }

    fun snoozeAlarm(alarm: Alarm, minutes: Int) {
        viewModelScope.launch {
            if (!settingsRepository.snoozeEnabled.first()) return@launch

            // Schedule the same alarm to fire again after the snooze duration.
            // No new alarm is created — we just re-schedule the existing one.
            scheduler.scheduleSnooze(alarm, minutes)
            Log.d(TAG, "Snoozed alarm ${alarm.id} for $minutes minutes")
        }
    }
}

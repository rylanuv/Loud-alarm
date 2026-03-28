package com.loud.alarm.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.MathDifficulty
import com.loud.alarm.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState: MutableStateFlow<AlarmUiState>
    val uiState: StateFlow<AlarmUiState>

    private var currentAlarmId: Int? = null

    init {
        val alarmId = savedStateHandle.get<Int>("alarmId")
        if (alarmId != null && alarmId != -1) {
            currentAlarmId = alarmId
            // Start with current time; loadAlarm will overwrite once fetched
            val calendar = Calendar.getInstance()
            _uiState = MutableStateFlow(AlarmUiState(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            ))
            loadAlarm(alarmId)
        } else {
            // New alarm — default to current time
            val calendar = Calendar.getInstance()
            _uiState = MutableStateFlow(AlarmUiState(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            ))
        }
        uiState = _uiState.asStateFlow()
    }

    private fun loadAlarm(id: Int) {
        viewModelScope.launch {
            repository.getAlarm(id)?.let { alarm ->
                _uiState.value = AlarmUiState(
                    hour = alarm.hour,
                    minute = alarm.minute,
                    daysOfWeek = alarm.daysOfWeek,
                    label = alarm.label,
                    soundUri = alarm.soundUri,
                    challengeTypes = alarm.challengeTypes,
                    mathDifficulty = alarm.mathDifficulty,
                    mazeDifficulty = alarm.mazeDifficulty,
                    barcodeValue = alarm.barcodeValue,
                    isVolumeBoostEnabled = alarm.isVolumeBoostEnabled,
                    wakeUpCheckMinutes = alarm.wakeUpCheckMinutes,
                    rewriteText = alarm.rewriteText,
                    stepCount = alarm.stepCount,
                    shakeCount = alarm.shakeCount,
                    sinkImageUri = alarm.sinkImageUri,
                    scanObjectLabel = alarm.scanObjectLabel,
                    timePickerVersion = _uiState.value.timePickerVersion + 1
                )
            }
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(hour = hour, minute = minute)
    }

    fun updateLabel(label: String) {
        _uiState.value = _uiState.value.copy(label = label)
    }

    fun updateSoundUri(soundUri: String?) {
        _uiState.value = _uiState.value.copy(soundUri = soundUri)
    }

    fun toggleDay(day: Int) {
        val currentDays = _uiState.value.daysOfWeek.toMutableSet()
        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
        }
        _uiState.value = _uiState.value.copy(daysOfWeek = currentDays)
    }

    fun setDays(days: Set<Int>) {
        _uiState.value = _uiState.value.copy(daysOfWeek = days)
    }

    fun updateVolumeBoost(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isVolumeBoostEnabled = enabled)
    }
    
    fun toggleChallengeType(
        type: ChallengeType,
        maxActiveChallenges: Int = Int.MAX_VALUE
    ): Boolean {
        val current = _uiState.value.challengeTypes.toMutableSet()
        if (type == ChallengeType.NONE) {
            // Selecting NONE clears everything else
            _uiState.value = _uiState.value.copy(challengeTypes = setOf(ChallengeType.NONE))
            return true
        }
        // Remove NONE when selecting any real challenge
        current.remove(ChallengeType.NONE)
        if (current.contains(type)) {
            current.remove(type)
        } else {
            if (current.size >= maxActiveChallenges) {
                return false
            }
            current.add(type)
        }
        // If nothing selected, default back to NONE
        if (current.isEmpty()) {
            current.add(ChallengeType.NONE)
        }
        _uiState.value = _uiState.value.copy(challengeTypes = current)
        return true
    }

    fun updateMathDifficulty(difficulty: MathDifficulty) {
        _uiState.value = _uiState.value.copy(mathDifficulty = difficulty)
    }

    fun updateMazeDifficulty(difficulty: MathDifficulty) {
        _uiState.value = _uiState.value.copy(mazeDifficulty = difficulty)
    }

    fun updateBarcodeValue(value: String?) {
        _uiState.value = _uiState.value.copy(barcodeValue = value)
    }

    fun updateWakeUpCheckMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(wakeUpCheckMinutes = minutes)
    }

    fun updateRewriteText(text: String) {
        _uiState.value = _uiState.value.copy(rewriteText = text)
    }

    fun updateStepCount(count: Int) {
        _uiState.value = _uiState.value.copy(stepCount = count)
    }

    fun updateShakeCount(count: Int) {
        _uiState.value = _uiState.value.copy(shakeCount = count)
    }

    fun updateSinkImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(sinkImageUri = uri)
    }

    fun updateScanObjectLabel(label: String) {
        _uiState.value = _uiState.value.copy(scanObjectLabel = label)
    }

    fun saveAlarm(onSaved: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val alarm = Alarm(
                id = currentAlarmId ?: 0,
                hour = state.hour,
                minute = state.minute,
                daysOfWeek = state.daysOfWeek,
                label = state.label,
                soundUri = state.soundUri,
                challengeTypes = state.challengeTypes,
                mathDifficulty = state.mathDifficulty,
                mazeDifficulty = state.mazeDifficulty,
                barcodeValue = state.barcodeValue,
                isVolumeBoostEnabled = state.isVolumeBoostEnabled,
                wakeUpCheckMinutes = state.wakeUpCheckMinutes,
                rewriteText = state.rewriteText,
                stepCount = state.stepCount,
                shakeCount = state.shakeCount,
                sinkImageUri = state.sinkImageUri,
                scanObjectLabel = state.scanObjectLabel,
                enabled = true
            )

            if (currentAlarmId == null || currentAlarmId == 0) {
                val newId = repository.insert(alarm)
                scheduler.schedule(alarm.copy(id = newId.toInt()))
            } else {
                repository.update(alarm)
                scheduler.schedule(alarm)
            }
            onSaved()
        }
    }
}

data class AlarmUiState(
    val hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val minute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val label: String = "",
    val soundUri: String? = null,
    val challengeTypes: Set<ChallengeType> = setOf(ChallengeType.NONE),
    val mathDifficulty: MathDifficulty = MathDifficulty.EASY,
    val mazeDifficulty: MathDifficulty = MathDifficulty.EASY,
    val barcodeValue: String? = null,
    val isVolumeBoostEnabled: Boolean = false,
    val wakeUpCheckMinutes: Int = 0,
    val rewriteText: String = "",
    val stepCount: Int = 30,
    val shakeCount: Int = 30,
    val sinkImageUri: String? = null,
    val scanObjectLabel: String = "",
    val timePickerVersion: Int = 0
)

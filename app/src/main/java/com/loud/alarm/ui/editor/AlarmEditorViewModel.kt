package com.loud.alarm.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.analytics.AnalyticsLogger
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.MathDifficulty
import com.loud.alarm.data.SquatDetectionMode
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
    private val analyticsLogger: AnalyticsLogger,
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
                    puzzleDifficulty = alarm.puzzleDifficulty,
                    barcodeValue = alarm.barcodeValue,
                    isVolumeBoostEnabled = alarm.isVolumeBoostEnabled,
                    wakeUpCheckMinutes = alarm.wakeUpCheckMinutes,
                    rewriteText = alarm.rewriteText,
                    stepCount = alarm.stepCount,
                    shakeCount = alarm.shakeCount,
                    tapCount = alarm.tapCount,
                    squatCount = alarm.squatCount,
                    squatDetectionMode = alarm.squatDetectionMode,
                    pushUpCount = alarm.pushUpCount,
                    reverseTypingCount = alarm.reverseTypingCount,
                    mathQuestionCount = alarm.mathQuestionCount,
                    sinkImageUri = alarm.sinkImageUri,
                    scanObjectLabel = alarm.scanObjectLabel,
                    scanObjectExcluded = alarm.scanObjectExcluded,
                    memoryDifficulty = alarm.memoryDifficulty,
                    memoryChallengeCount = alarm.memoryChallengeCount,
                    spellBeeDifficulty = alarm.spellBeeDifficulty,
                    spellBeeCount = alarm.spellBeeCount,
                    audioMemoryDifficulty = alarm.audioMemoryDifficulty,
                    audioMemoryChallengeCount = alarm.audioMemoryChallengeCount,
                    clockReadingDifficulty = alarm.clockReadingDifficulty,
                    clockReadingCount = alarm.clockReadingCount,
                    roomLightTargetLux = alarm.roomLightTargetLux,
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
            analyticsLogger.logChallengeSelected(type.name)
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

    fun updatePuzzleDifficulty(difficulty: MathDifficulty) {
        _uiState.value = _uiState.value.copy(puzzleDifficulty = difficulty)
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

    fun updateTapCount(count: Int) {
        _uiState.value = _uiState.value.copy(tapCount = count)
    }

    fun updateSquatCount(count: Int) {
        _uiState.value = _uiState.value.copy(squatCount = count)
    }

    fun updateSquatDetectionMode(mode: SquatDetectionMode) {
        _uiState.value = _uiState.value.copy(squatDetectionMode = mode)
    }

    fun updatePushUpCount(count: Int) {
        _uiState.value = _uiState.value.copy(pushUpCount = count)
    }

    fun updateReverseTypingCount(count: Int) {
        _uiState.value = _uiState.value.copy(reverseTypingCount = count)
    }

    fun updateMathQuestionCount(count: Int) {
        _uiState.value = _uiState.value.copy(mathQuestionCount = count)
    }

    fun updateMemoryDifficulty(difficulty: MathDifficulty) {
        _uiState.value = _uiState.value.copy(memoryDifficulty = difficulty)
    }

    fun updateMemoryChallengeCount(count: Int) {
        _uiState.value = _uiState.value.copy(memoryChallengeCount = count)
    }

    fun updateSpellBeeDifficulty(difficulty: MathDifficulty) {
        _uiState.value = _uiState.value.copy(spellBeeDifficulty = difficulty)
    }

    fun updateSpellBeeCount(count: Int) {
        _uiState.value = _uiState.value.copy(spellBeeCount = count)
    }

    fun updateAudioMemoryDifficulty(difficulty: MathDifficulty) {
        _uiState.value = _uiState.value.copy(audioMemoryDifficulty = difficulty)
    }

    fun updateAudioMemoryChallengeCount(count: Int) {
        _uiState.value = _uiState.value.copy(audioMemoryChallengeCount = count)
    }

    fun updateClockReadingDifficulty(difficulty: MathDifficulty) {
        _uiState.value = _uiState.value.copy(clockReadingDifficulty = difficulty)
    }

    fun updateClockReadingCount(count: Int) {
        _uiState.value = _uiState.value.copy(clockReadingCount = count)
    }

    fun updateRoomLightTargetLux(lux: Int) {
        _uiState.value = _uiState.value.copy(roomLightTargetLux = lux.coerceIn(10, 800))
    }

    fun updateSinkImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(sinkImageUri = uri)
    }

    fun updateScanObjectLabel(label: String) {
        _uiState.value = _uiState.value.copy(scanObjectLabel = label)
    }

    fun toggleScanObjectExcluded(label: String) {
        val current = _uiState.value.scanObjectExcluded.toMutableSet()
        if (current.contains(label)) current.remove(label) else current.add(label)
        _uiState.value = _uiState.value.copy(scanObjectExcluded = current)
    }

    fun saveAlarm(onSaved: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val isNewAlarm = currentAlarmId == null || currentAlarmId == 0
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
                puzzleDifficulty = state.puzzleDifficulty,
                barcodeValue = state.barcodeValue,
                isVolumeBoostEnabled = state.isVolumeBoostEnabled,
                wakeUpCheckMinutes = state.wakeUpCheckMinutes,
                rewriteText = state.rewriteText,
                stepCount = state.stepCount,
                shakeCount = state.shakeCount,
                tapCount = state.tapCount,
                squatCount = state.squatCount,
                squatDetectionMode = state.squatDetectionMode,
                pushUpCount = state.pushUpCount,
                reverseTypingCount = state.reverseTypingCount,
                mathQuestionCount = state.mathQuestionCount,
                sinkImageUri = state.sinkImageUri,
                scanObjectLabel = state.scanObjectLabel,
                scanObjectExcluded = state.scanObjectExcluded,
                memoryDifficulty = state.memoryDifficulty,
                memoryChallengeCount = state.memoryChallengeCount,
                spellBeeDifficulty = state.spellBeeDifficulty,
                spellBeeCount = state.spellBeeCount,
                audioMemoryDifficulty = state.audioMemoryDifficulty,
                audioMemoryChallengeCount = state.audioMemoryChallengeCount,
                clockReadingDifficulty = state.clockReadingDifficulty,
                clockReadingCount = state.clockReadingCount,
                roomLightTargetLux = state.roomLightTargetLux,
                enabled = true
            )

            if (isNewAlarm) {
                val newId = repository.insert(alarm)
                scheduler.schedule(alarm.copy(id = newId.toInt()))
            } else {
                repository.update(alarm)
                scheduler.schedule(alarm)
            }
            analyticsLogger.logAlarmSaved(
                isNewAlarm = isNewAlarm,
                challengeCount = state.challengeTypes.count { it != ChallengeType.NONE },
                challengeTypes = state.challengeTypes.joinToString(",") { it.name },
                isRepeating = state.daysOfWeek.isNotEmpty(),
                wakeUpCheckMinutes = state.wakeUpCheckMinutes
            )
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
    val tapCount: Int = 30,
    val squatCount: Int = 15,
    val squatDetectionMode: SquatDetectionMode = SquatDetectionMode.CAMERA,
    val pushUpCount: Int = 15,
    val reverseTypingCount: Int = 3,
    val mathQuestionCount: Int = 1,
    val sinkImageUri: String? = null,
    val scanObjectLabel: String = "RANDOM",
    val scanObjectExcluded: Set<String> = emptySet(),
    val puzzleDifficulty: MathDifficulty = MathDifficulty.EASY,
    val memoryDifficulty: MathDifficulty = MathDifficulty.EASY,
    val memoryChallengeCount: Int = 3,
    val spellBeeDifficulty: MathDifficulty = MathDifficulty.EASY,
    val spellBeeCount: Int = 3,
    val audioMemoryDifficulty: MathDifficulty = MathDifficulty.EASY,
    val audioMemoryChallengeCount: Int = 3,
    val clockReadingDifficulty: MathDifficulty = MathDifficulty.EASY,
    val clockReadingCount: Int = 1,
    val roomLightTargetLux: Int = 100,
    val timePickerVersion: Int = 0
)

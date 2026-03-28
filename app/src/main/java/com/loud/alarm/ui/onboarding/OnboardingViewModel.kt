package com.loud.alarm.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.SettingsRepository
import com.loud.alarm.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            minute = Calendar.getInstance().get(Calendar.MINUTE)
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val onboardingCompleted: Flow<Boolean> = settingsRepository.onboardingCompleted

    fun updateTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(hour = hour, minute = minute)
    }

    fun setDays(days: Set<Int>) {
        _uiState.value = _uiState.value.copy(daysOfWeek = days)
    }

    fun selectFreeChallenge(type: ChallengeType) {
        val nextSelection = when (type) {
            ChallengeType.MATH, ChallengeType.QR_CODE, ChallengeType.REWRITE, ChallengeType.NONE -> setOf(type)
            else -> setOf(ChallengeType.NONE)
        }
        _uiState.value = _uiState.value.copy(challengeTypes = nextSelection)
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val state = _uiState.value
                val alarm = Alarm(
                    hour = state.hour,
                    minute = state.minute,
                    enabled = true,
                    daysOfWeek = state.daysOfWeek,
                    label = "",
                    challengeTypes = state.challengeTypes
                )
                val alarmId = alarmRepository.insert(alarm).toInt()
                alarmScheduler.schedule(alarm.copy(id = alarmId))
                settingsRepository.setOnboardingCompleted(true)
                onCompleted()
            } finally {
                _isSaving.value = false
            }
        }
    }
}

data class OnboardingUiState(
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val challengeTypes: Set<ChallengeType> = setOf(ChallengeType.MATH)
)

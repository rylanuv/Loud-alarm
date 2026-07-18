package com.loud.alarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SNOOZE_ENABLED = booleanPreferencesKey("snooze_enabled")
        val SNOOZE_DURATION = intPreferencesKey("snooze_duration")
        val ALARM_VOLUME = floatPreferencesKey("alarm_volume")
        val FADE_IN_ENABLED = booleanPreferencesKey("fade_in_enabled")
        val FADE_IN_DURATION = intPreferencesKey("fade_in_duration")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val AUTO_SILENCE_DURATION = intPreferencesKey("auto_silence_duration")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val VIBRATION_PATTERN = stringPreferencesKey("vibration_pattern")
        val ALARM_DISMISS_COUNT = intPreferencesKey("alarm_dismiss_count")
        val REVIEW_SHOWN = booleanPreferencesKey("review_shown")
        val NEXT_REVIEW_DISMISS_MILESTONE = intPreferencesKey("next_review_dismiss_milestone")
        val IS_DEV_MODE_ENABLED = booleanPreferencesKey("is_dev_mode_enabled")
        val SKELETON_OVERLAY_ENABLED = booleanPreferencesKey("skeleton_overlay_enabled")
        val SHOW_LABELS_ENABLED = booleanPreferencesKey("show_labels_enabled")
        val PREVENT_POWER_OFF_ENABLED = booleanPreferencesKey("prevent_power_off_enabled")
        val PREVENT_UNINSTALL_ENABLED = booleanPreferencesKey("prevent_uninstall_enabled")
        val LAST_SHARE_PROMPT_TIME = longPreferencesKey("last_share_prompt_time")
        val UPCOMING_ALARM_NOTIFICATION = booleanPreferencesKey("upcoming_alarm_notification")
        val PREVENT_LAST_MINUTE_EDITS_ENABLED = booleanPreferencesKey("prevent_last_minute_edits_enabled")
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
        }

    val snoozeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.SNOOZE_ENABLED] ?: true
        }

    val snoozeDuration: Flow<Int> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.SNOOZE_DURATION] ?: 5
        }

    val alarmVolume: Flow<Float> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.ALARM_VOLUME] ?: 0.8f
        }

    val fadeInEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.FADE_IN_ENABLED] ?: true
        }

    val fadeInDuration: Flow<Int> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.FADE_IN_DURATION] ?: 25
        }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] ?: false
        }

    val autoSilenceDuration: Flow<Int> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.AUTO_SILENCE_DURATION] ?: 15
        }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    val vibrationPattern: Flow<String> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.VIBRATION_PATTERN] ?: VibrationPattern.DEVICE_DEFAULT.name
        }

    val isDevModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.IS_DEV_MODE_ENABLED] ?: false
        }

    val skeletonOverlayEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.SKELETON_OVERLAY_ENABLED] ?: false
        }

    val showLabelsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.SHOW_LABELS_ENABLED] ?: false
        }

    val preventUninstallEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.PREVENT_UNINSTALL_ENABLED] ?: false
        }

    val upcomingAlarmNotificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.UPCOMING_ALARM_NOTIFICATION] ?: false
        }

    val preventLastMinuteEditsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.PREVENT_LAST_MINUTE_EDITS_ENABLED] ?: false
        }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setSnoozeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.SNOOZE_ENABLED] = enabled
        }
    }

    suspend fun setSnoozeDuration(duration: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.SNOOZE_DURATION] = duration
        }
    }

    suspend fun setAlarmVolume(volume: Float) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.ALARM_VOLUME] = volume
        }
    }

    suspend fun setFadeInEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.FADE_IN_ENABLED] = enabled
        }
    }

    suspend fun setFadeInDuration(duration: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.FADE_IN_DURATION] = duration
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] = enabled
        }
    }

    suspend fun setAutoSilenceDuration(duration: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.AUTO_SILENCE_DURATION] = duration
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setVibrationPattern(patternName: String) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.VIBRATION_PATTERN] = patternName
        }
    }

    suspend fun setDevModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.IS_DEV_MODE_ENABLED] = enabled
        }
    }

    suspend fun setSkeletonOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.SKELETON_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setShowLabelsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.SHOW_LABELS_ENABLED] = enabled
        }
    }

    suspend fun setPreventUninstallEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.PREVENT_UNINSTALL_ENABLED] = enabled
        }
    }

    suspend fun setUpcomingAlarmNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.UPCOMING_ALARM_NOTIFICATION] = enabled
        }
    }

    suspend fun setPreventLastMinuteEditsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.PREVENT_LAST_MINUTE_EDITS_ENABLED] = enabled
        }
    }

    // --- In-App Review tracking ---

    val alarmDismissCount: Flow<Int> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.ALARM_DISMISS_COUNT] ?: 0
        }

    val reviewShown: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.REVIEW_SHOWN] ?: false
        }

    val nextReviewDismissMilestone: Flow<Int> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.NEXT_REVIEW_DISMISS_MILESTONE] ?: 0
        }

    suspend fun incrementAlarmDismissCount() {
        context.dataStore.edit { preferences: MutablePreferences ->
            val current = preferences[PreferencesKeys.ALARM_DISMISS_COUNT] ?: 0
            preferences[PreferencesKeys.ALARM_DISMISS_COUNT] = current + 1
        }
    }

    suspend fun setNextReviewDismissMilestone(milestone: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.NEXT_REVIEW_DISMISS_MILESTONE] = milestone
        }
    }

    // --- Share Prompt tracking ---

    val lastSharePromptTime: Flow<Long> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.LAST_SHARE_PROMPT_TIME] ?: 0L
        }

    suspend fun setLastSharePromptTime(timeMillis: Long) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.LAST_SHARE_PROMPT_TIME] = timeMillis
        }
    }
}

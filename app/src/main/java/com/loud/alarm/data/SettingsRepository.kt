package com.loud.alarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] ?: false
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
}

package com.loud.alarm.share

import android.util.Log
import com.loud.alarm.data.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the periodic "Share with Friends" prompt.
 *
 * Strategy:
 * - Prompts the user every [PROMPT_INTERVAL_DAYS] days (10 days) to share the app.
 * - The timestamp of the last prompt is persisted in DataStore.
 * - On the very first launch (timestamp == 0), the current time is recorded
 *   so the user isn't prompted immediately on install.
 */
@Singleton
class SharePromptManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "SharePromptManager"
        private const val PROMPT_INTERVAL_DAYS = 10
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }

    /**
     * Returns true if at least [PROMPT_INTERVAL_DAYS] days have passed
     * since the last share prompt (or since the first app open).
     */
    suspend fun shouldShowSharePrompt(): Boolean {
        val lastPromptTime = settingsRepository.lastSharePromptTime.first()

        if (lastPromptTime == 0L) {
            // First time — seed the timestamp so the user isn't prompted immediately
            Log.d(TAG, "First check — seeding initial share prompt timestamp")
            settingsRepository.setLastSharePromptTime(System.currentTimeMillis())
            return false
        }

        val elapsed = System.currentTimeMillis() - lastPromptTime
        val thresholdMillis = PROMPT_INTERVAL_DAYS * MILLIS_PER_DAY
        val shouldShow = elapsed >= thresholdMillis

        Log.d(
            TAG,
            "shouldShowSharePrompt: elapsed=${elapsed / MILLIS_PER_DAY} days, " +
                    "threshold=$PROMPT_INTERVAL_DAYS days, shouldShow=$shouldShow"
        )

        return shouldShow
    }

    /**
     * Records that the share prompt was shown (or dismissed).
     * Resets the 10-day countdown.
     */
    suspend fun onSharePromptShown() {
        settingsRepository.setLastSharePromptTime(System.currentTimeMillis())
        Log.d(TAG, "Share prompt timestamp updated")
    }
}

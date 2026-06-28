package com.loud.alarm.review

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.loud.alarm.data.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manages the Google Play In-App Review flow.
 *
 * Strategy:
 * - First prompt after the user has successfully dismissed at least
 *   [FIRST_DISMISS_THRESHOLD] alarms, proving they are a genuine, engaged user.
 * - After each review attempt, wait [REPEAT_DISMISS_INTERVAL] more successful
 *   dismissals before requesting again.
 * - The prompt is shown when the user returns to the HomeScreen after dismissing an alarm,
 *   which is a calm, positive moment (they just woke up successfully).
 */
@Singleton
class InAppReviewManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "InAppReviewManager"
        private const val FIRST_DISMISS_THRESHOLD = 4
        private const val REPEAT_DISMISS_INTERVAL = 40
    }

    /**
     * Returns true if the conditions are met to show the review prompt:
     * - User has dismissed at least the next scheduled dismiss milestone
     */
    suspend fun shouldRequestReview(): Boolean {
        val dismissCount = settingsRepository.alarmDismissCount.first()
        val nextMilestone = getOrInitializeNextMilestone(dismissCount)

        Log.d(TAG, "shouldRequestReview: dismissCount=$dismissCount, nextMilestone=$nextMilestone")

        return dismissCount >= nextMilestone
    }

    /**
     * Launches the Google Play In-App Review flow.
     * Must be called from an Activity context.
     * Advances the next review milestone regardless of whether the user actually left a
     * review, because Google does not reveal the outcome of the review flow.
     */
    suspend fun requestReview(activity: Activity) {
        if (!shouldRequestReview()) {
            Log.d(TAG, "Conditions not met for review. Skipping.")
            return
        }

        val currentDismissCount = settingsRepository.alarmDismissCount.first()
        val currentMilestone = getOrInitializeNextMilestone(currentDismissCount)
        val nextMilestone = calculateNextDismissMilestone(currentDismissCount)

        try {
            val reviewManager = ReviewManagerFactory.create(activity)

            // Request the review flow info (suspend-friendly via suspendCoroutine)
            val reviewInfo = suspendCoroutine { cont ->
                reviewManager.requestReviewFlow()
                    .addOnSuccessListener { info -> cont.resume(info) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }

            Log.d(TAG, "Review flow info obtained. Launching review dialog...")

            // Advance the milestone before launching in case the activity is destroyed.
            settingsRepository.setNextReviewDismissMilestone(nextMilestone)

            // Launch the review flow
            suspendCoroutine { cont ->
                reviewManager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener { cont.resume(Unit) }
            }

            Log.d(TAG, "Review flow completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch review flow", e)
            // Restore the current milestone so the app can retry next time.
            settingsRepository.setNextReviewDismissMilestone(currentMilestone)
        }
    }

    /**
     * Forces the Google Play In-App Review flow for debugging.
     */
    suspend fun forceRequestReview(activity: Activity) {
        try {
            val reviewManager = ReviewManagerFactory.create(activity)

            val reviewInfo = suspendCoroutine { cont ->
                reviewManager.requestReviewFlow()
                    .addOnSuccessListener { info -> cont.resume(info) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }

            Log.d(TAG, "Review flow info obtained. Launching review dialog (forced)...")

            suspendCoroutine { cont ->
                reviewManager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener { cont.resume(Unit) }
            }

            Log.d(TAG, "Forced review flow completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch forced review flow", e)
        }
    }

    private fun calculateNextDismissMilestone(currentDismissCount: Int): Int {
        return if (currentDismissCount < FIRST_DISMISS_THRESHOLD) {
            FIRST_DISMISS_THRESHOLD
        } else {
            currentDismissCount + REPEAT_DISMISS_INTERVAL
        }
    }

    private suspend fun getOrInitializeNextMilestone(dismissCount: Int): Int {
        val storedMilestone = settingsRepository.nextReviewDismissMilestone.first()
        if (storedMilestone > 0) {
            return storedMilestone
        }

        val initialMilestone = if (settingsRepository.reviewShown.first()) {
            dismissCount + REPEAT_DISMISS_INTERVAL
        } else {
            FIRST_DISMISS_THRESHOLD
        }

        settingsRepository.setNextReviewDismissMilestone(initialMilestone)
        return initialMilestone
    }
}

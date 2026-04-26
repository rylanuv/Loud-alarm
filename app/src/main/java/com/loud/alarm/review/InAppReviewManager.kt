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
 * - Only prompt after the user has successfully dismissed at least [DISMISS_THRESHOLD] alarms,
 *   proving they are a genuine, engaged user.
 * - Only prompt once. If the review sheet has been shown before, never show again.
 * - The prompt is shown when the user returns to the HomeScreen after dismissing an alarm,
 *   which is a calm, positive moment (they just woke up successfully).
 */
@Singleton
class InAppReviewManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "InAppReviewManager"
        private const val DISMISS_THRESHOLD = 7
    }

    /**
     * Returns true if the conditions are met to show the review prompt:
     * - User has dismissed >= [DISMISS_THRESHOLD] alarms
     * - Review prompt has not been shown before
     */
    suspend fun shouldRequestReview(): Boolean {
        val dismissCount = settingsRepository.alarmDismissCount.first()
        val reviewAlreadyShown = settingsRepository.reviewShown.first()

        Log.d(TAG, "shouldRequestReview: dismissCount=$dismissCount, reviewAlreadyShown=$reviewAlreadyShown")

        return dismissCount >= DISMISS_THRESHOLD && !reviewAlreadyShown
    }

    /**
     * Launches the Google Play In-App Review flow.
     * Must be called from an Activity context.
     * Marks the review as shown regardless of whether the user actually left a review
     * (Google does not tell us the outcome for anti-manipulation reasons).
     */
    suspend fun requestReview(activity: Activity) {
        if (!shouldRequestReview()) {
            Log.d(TAG, "Conditions not met for review. Skipping.")
            return
        }

        try {
            val reviewManager = ReviewManagerFactory.create(activity)

            // Request the review flow info (suspend-friendly via suspendCoroutine)
            val reviewInfo = suspendCoroutine { cont ->
                reviewManager.requestReviewFlow()
                    .addOnSuccessListener { info -> cont.resume(info) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }

            Log.d(TAG, "Review flow info obtained. Launching review dialog...")

            // Mark as shown BEFORE launching (in case the activity is destroyed)
            settingsRepository.setReviewShown(true)

            // Launch the review flow
            suspendCoroutine { cont ->
                reviewManager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener { cont.resume(Unit) }
            }

            Log.d(TAG, "Review flow completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch review flow", e)
            // Don't mark as shown on failure — let it retry next time
            settingsRepository.setReviewShown(false)
        }
    }
}

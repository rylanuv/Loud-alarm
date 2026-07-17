package com.loud.alarm.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin receiver used solely to prevent uninstallation of the app.
 *
 * No policies are enforced — we only leverage the side-effect that an active
 * device administrator cannot be uninstalled by the user.
 *
 * When device admin is revoked (from system Settings or programmatically),
 * the SettingsScreen re-checks admin status on resume and syncs the preference.
 */
class LoudAlarmDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // No-op — preference is synced from the UI on resume
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // No-op — SettingsScreen re-checks admin status via LifecycleObserver
    }
}

package com.loud.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmServiceRestartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmServiceRestartRcvr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmService.ACTION_RESTART_ALARM_SERVICE) {
            return
        }

        val alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
        if (alarmId == -1) {
            Log.e(TAG, "Missing alarm id in restart intent; skipping restart")
            return
        }

        val isVolumeBoostEnabled =
            intent.getBooleanExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, false)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, isVolumeBoostEnabled)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.w(TAG, "Restarted AlarmService for alarmId=$alarmId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart AlarmService for alarmId=$alarmId", e)
        }
    }
}

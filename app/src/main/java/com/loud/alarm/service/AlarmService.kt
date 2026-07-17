package com.loud.alarm.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.loud.alarm.R
import com.loud.alarm.data.AlarmRepository
import com.loud.alarm.data.VibrationPattern
import com.loud.alarm.ui.alarm.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject


@AndroidEntryPoint
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var fadeAnimator: android.animation.ValueAnimator? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var currentAlarmId: Int = -1
    private var currentVolumeBoostEnabled: Boolean = false
    private var stopRequested: Boolean = false
    private var restartScheduled: Boolean = false
    @Volatile
    private var lastActivityLaunchTimeMs: Long = 0L

    private data class RingingContext(
        val alarmId: Int,
        val isVolumeBoostEnabled: Boolean
    )

    companion object {
        const val CHANNEL_ID = "alarm_ringing_v2"
        const val NOTIFICATION_ID = 1
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_IS_VOLUME_BOOST_ENABLED = "IS_VOLUME_BOOST_ENABLED"
        const val ACTION_STOP_ALARM = "com.loud.alarm.action.STOP_ALARM"
        const val ACTION_RESHOW_ALARM = "com.loud.alarm.action.RESHOW_ALARM"
        const val ACTION_RESTART_ALARM_SERVICE = "com.loud.alarm.action.RESTART_ALARM_SERVICE"
        const val ACTION_PREVIEW_ALARM = "com.loud.alarm.action.PREVIEW_ALARM"
        private const val TAG = "AlarmService"
        private const val STATE_PREFS = "alarm_service_state"
        private const val PREF_LAST_ALARM_ID = "last_alarm_id"
        private const val PREF_LAST_VOLUME_BOOST = "last_volume_boost"
        private const val BOOST_TARGET_GAIN_MB = 1500 // 15 dB — clearly audible boost
        private const val RESTART_DELAY_MS = 1_000L
        private const val HOME_RESHOW_DELAY_MS = 450L
        private const val VOLUME_ENFORCER_INTERVAL_MS = 750L
        private const val ACTIVITY_WATCHDOG_INTERVAL_MS = 5_000L
        private const val LAUNCH_COOLDOWN_MS = 4_000L

        /** Reference to active instance so challenges can dim/restore alarm volume */
        @Volatile
        private var activeInstance: AlarmService? = null

        /** True while the alarm is actively ringing */
        @Volatile
        @JvmStatic
        var isRinging: Boolean = false
            private set

        /** Saved MUSIC stream volume before TTS boost */
        private var savedMusicVolume: Int = -1

        /** Dim the alarm ringtone and boost TTS stream so words are loud and clear */
        fun dimAlarmVolume() {
            activeInstance?.let { svc ->
                svc.isVolumeDimmed = true
                svc.mediaPlayer?.setVolume(0.02f, 0.02f)
                svc.loudnessEnhancer?.enabled = false
                // Pause vibration so user can hear TTS clearly
                svc.vibrator?.cancel()
                // Crank TTS stream (MUSIC) to max volume
                try {
                    val am = svc.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    savedMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                    Log.d(TAG, "TTS stream boosted to max ($maxMusic), was $savedMusicVolume")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to boost TTS stream volume", e)
                }
                Log.d(TAG, "Alarm volume dimmed + vibration paused for TTS")
            }
        }

        /** Restore the alarm ringtone and TTS stream to original levels */
        fun restoreAlarmVolume() {
            activeInstance?.let { svc ->
                svc.isVolumeDimmed = false
                svc.mediaPlayer?.setVolume(1f, 1f)
                if (svc.currentVolumeBoostEnabled) {
                    svc.loudnessEnhancer?.enabled = true
                }
                // Restore TTS stream (MUSIC) to previous volume
                if (savedMusicVolume >= 0) {
                    try {
                        val am = svc.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusicVolume, 0)
                        Log.d(TAG, "TTS stream restored to $savedMusicVolume")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restore TTS stream volume", e)
                    }
                    savedMusicVolume = -1
                }
                Log.d(TAG, "Alarm volume restored after TTS")
            }
        }
    }

    /** When true, the volume enforcer skips re-cranking volume */
    private var isVolumeDimmed = false

    private val autoSilenceRunnable = Runnable {
        Log.w(TAG, "Auto-silencing alarm after timeout: user never dismissed")
        stopRequested = true
        clearPersistedRingingContext()
        stopSelf()
    }

    private val volumeEnforcerRunnable = object : Runnable {
        override fun run() {
            if (stopRequested) return
            // Skip enforcing volume while alarm is dimmed for TTS
            if (isVolumeDimmed) {
                timeoutHandler.postDelayed(this, VOLUME_ENFORCER_INTERVAL_MS)
                return
            }
            enforceAlarmVolume(currentVolumeBoostEnabled)
            if (currentVolumeBoostEnabled) {
                loudnessEnhancer?.let {
                    if (!it.enabled) {
                        Log.w(TAG, "Re-enabling LoudnessEnhancer (was disabled by system)")
                        it.enabled = true
                    }
                }
            }
            timeoutHandler.postDelayed(this, VOLUME_ENFORCER_INTERVAL_MS)
        }
    }

    private val activityWatchdogRunnable = object : Runnable {
        override fun run() {
            if (stopRequested || currentAlarmId == -1) return

            val now = System.currentTimeMillis()
            val elapsed = now - lastActivityLaunchTimeMs
            if (!isAlarmActivityVisible() && elapsed >= LAUNCH_COOLDOWN_MS) {
                Log.w(TAG, "AlarmActivity not visible after ${elapsed}ms; relaunching")
                launchAlarmActivity(currentAlarmId)
            }

            timeoutHandler.postDelayed(this, ACTIVITY_WATCHDOG_INTERVAL_MS)
        }
    }

    @Inject
    lateinit var settingsRepository: com.loud.alarm.data.SettingsRepository

    @Inject
    lateinit var alarmRepository: AlarmRepository

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESHOW_ALARM) {
            val requestedAlarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
            val alarmIdToRelaunch = when {
                requestedAlarmId != -1 -> requestedAlarmId
                currentAlarmId != -1 -> currentAlarmId
                else -> resolveRingingContext(intent)?.alarmId ?: -1
            }

            if (!stopRequested && alarmIdToRelaunch != -1) {
                timeoutHandler.postDelayed(
                    { if (!stopRequested) launchAlarmActivity(alarmIdToRelaunch) },
                    HOME_RESHOW_DELAY_MS
                )
                Log.w(TAG, "Received re-show request; relaunching alarm screen soon")
            } else {
                Log.w(TAG, "Ignoring re-show request: no active alarm context")
            }
            return START_STICKY
        }

        if (intent?.action == ACTION_STOP_ALARM) {
            Log.d(TAG, "Received explicit stop action")
            stopRequested = true
            clearPersistedRingingContext()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_PREVIEW_ALARM) {
            val isVolumeBoostEnabled = intent.getBooleanExtra(EXTRA_IS_VOLUME_BOOST_ENABLED, false)
            val soundUri = intent.getStringExtra("EXTRA_SOUND_URI")
            currentAlarmId = -1
            currentVolumeBoostEnabled = isVolumeBoostEnabled
            stopRequested = false
            activeInstance = this
            isRinging = true
            restartScheduled = false
            Log.d(TAG, "AlarmService started for PREVIEW")
            
            Thread {
                val isVibrationEnabled = readSettingOrDefault("vibrationEnabled", true) {
                    kotlinx.coroutines.runBlocking { settingsRepository.vibrationEnabled.first() }
                }
                val isFadeInEnabled = readSettingOrDefault("fadeInEnabled", true) {
                    kotlinx.coroutines.runBlocking { settingsRepository.fadeInEnabled.first() }
                }
                val fadeInDuration = readSettingOrDefault("fadeInDuration", 25) {
                    kotlinx.coroutines.runBlocking { settingsRepository.fadeInDuration.first() }
                }
                val autoSilenceDuration = readSettingOrDefault("autoSilenceDuration", 30) {
                    kotlinx.coroutines.runBlocking { settingsRepository.autoSilenceDuration.first() }
                }
                val vibrationPatternName = readSettingOrDefault(
                    "vibrationPattern",
                    VibrationPattern.DEVICE_DEFAULT.name
                ) {
                    kotlinx.coroutines.runBlocking { settingsRepository.vibrationPattern.first() }
                }


                Handler(Looper.getMainLooper()).post {
                    startAlarm(
                        isVolumeBoostEnabled = isVolumeBoostEnabled,
                        isVibrationEnabled = isVibrationEnabled,
                        isFadeInEnabled = isFadeInEnabled,
                        fadeInDuration = fadeInDuration,
                        autoSilenceDuration = autoSilenceDuration,
                        vibrationPatternName = vibrationPatternName,
                        selectedSoundUri = soundUri
                    )
                }
            }.start()
            return START_NOT_STICKY
        }

        val ringingContext = resolveRingingContext(intent)
        if (ringingContext == null) {
            Log.e(TAG, "AlarmService started without alarm context; stopping")
            stopRequested = true
            clearPersistedRingingContext()
            stopSelf()
            return START_NOT_STICKY
        }

        currentAlarmId = ringingContext.alarmId
        currentVolumeBoostEnabled = ringingContext.isVolumeBoostEnabled
        stopRequested = false
        activeInstance = this
        isRinging = true
        restartScheduled = false
        Log.d(TAG, "AlarmService started for alarm: ${ringingContext.alarmId}")

        acquireWakeLock()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14+: Alarm apps with USE_EXACT_ALARM should use SYSTEM_EXEMPTED (1024)
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification(ringingContext.alarmId),
                    1024 or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification(ringingContext.alarmId),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification(ringingContext.alarmId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground, app might be in strict background state", e)
        }

        Thread {
            val isVibrationEnabled = readSettingOrDefault("vibrationEnabled", true) {
                kotlinx.coroutines.runBlocking { settingsRepository.vibrationEnabled.first() }
            }
            val isFadeInEnabled = readSettingOrDefault("fadeInEnabled", true) {
                kotlinx.coroutines.runBlocking { settingsRepository.fadeInEnabled.first() }
            }
            val fadeInDuration = readSettingOrDefault("fadeInDuration", 25) {
                kotlinx.coroutines.runBlocking { settingsRepository.fadeInDuration.first() }
            }
            val autoSilenceDuration = readSettingOrDefault("autoSilenceDuration", 30) {
                kotlinx.coroutines.runBlocking { settingsRepository.autoSilenceDuration.first() }
            }
            val vibrationPatternName = readSettingOrDefault(
                "vibrationPattern",
                VibrationPattern.DEVICE_DEFAULT.name
            ) {
                kotlinx.coroutines.runBlocking { settingsRepository.vibrationPattern.first() }
            }
            val selectedSoundUri = try {
                kotlinx.coroutines.runBlocking {
                    alarmRepository.getAlarm(ringingContext.alarmId)?.soundUri
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load alarm sound; using default", e)
                null
            }

            Handler(Looper.getMainLooper()).post {
                startAlarm(
                    isVolumeBoostEnabled = ringingContext.isVolumeBoostEnabled,
                    isVibrationEnabled = isVibrationEnabled,
                    isFadeInEnabled = isFadeInEnabled,
                    fadeInDuration = fadeInDuration,
                    autoSilenceDuration = autoSilenceDuration,
                    vibrationPatternName = vibrationPatternName,
                    selectedSoundUri = selectedSoundUri
                )
            }
        }.start()

        launchAlarmActivity(ringingContext.alarmId)
        startActivityWatchdog()

        return START_STICKY
    }

    private fun resolveRingingContext(intent: Intent?): RingingContext? {
        val intentAlarmId = intent?.getIntExtra(EXTRA_ALARM_ID, -1) ?: -1
        if (intentAlarmId != -1) {
            val intentVolumeBoost =
                intent?.getBooleanExtra(EXTRA_IS_VOLUME_BOOST_ENABLED, false) ?: false
            persistRingingContext(intentAlarmId, intentVolumeBoost)
            return RingingContext(intentAlarmId, intentVolumeBoost)
        }

        val prefs = getSharedPreferences(STATE_PREFS, MODE_PRIVATE)
        val storedAlarmId = prefs.getInt(PREF_LAST_ALARM_ID, -1)
        if (storedAlarmId == -1) {
            return null
        }

        val storedVolumeBoost = prefs.getBoolean(PREF_LAST_VOLUME_BOOST, false)
        Log.w(TAG, "Recovered alarm context from persistent state (alarmId=$storedAlarmId)")
        return RingingContext(storedAlarmId, storedVolumeBoost)
    }

    private fun persistRingingContext(alarmId: Int, isVolumeBoostEnabled: Boolean) {
        getSharedPreferences(STATE_PREFS, MODE_PRIVATE)
            .edit()
            .putInt(PREF_LAST_ALARM_ID, alarmId)
            .putBoolean(PREF_LAST_VOLUME_BOOST, isVolumeBoostEnabled)
            .apply()
    }

    private fun clearPersistedRingingContext() {
        getSharedPreferences(STATE_PREFS, MODE_PRIVATE)
            .edit()
            .remove(PREF_LAST_ALARM_ID)
            .remove(PREF_LAST_VOLUME_BOOST)
            .apply()
    }

    private fun scheduleAutoSilence(durationMinutes: Int) {
        timeoutHandler.removeCallbacks(autoSilenceRunnable)
        val timeoutMs = durationMinutes * 60 * 1000L
        timeoutHandler.postDelayed(autoSilenceRunnable, timeoutMs)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LoudAlarm::AlarmWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
        Log.d(TAG, "WakeLock acquired (PARTIAL)")
    }

    private fun createNotification(alarmId: Int): android.app.Notification {
        createNotificationChannel()

        val openAlarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val openAlarmPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            openAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm Ringing!")
            .setContentText("Tap to solve challenge & dismiss")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAlarmPendingIntent)
            .setFullScreenIntent(openAlarmPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.deleteNotificationChannel("alarm_channel")
            notificationManager.deleteNotificationChannel("alarm_service_silent")

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Ringing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows when alarm is ringing"
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun <T> readSettingOrDefault(
        settingName: String,
        defaultValue: T,
        reader: () -> T
    ): T {
        return try {
            reader()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read $settingName; using default: $defaultValue", e)
            defaultValue
        }
    }

    private fun startAlarm(
        isVolumeBoostEnabled: Boolean,
        isVibrationEnabled: Boolean,
        isFadeInEnabled: Boolean,
        fadeInDuration: Int,
        autoSilenceDuration: Int,
        vibrationPatternName: String = VibrationPattern.DEVICE_DEFAULT.name,
        selectedSoundUri: String? = null
    ) {
        fadeAnimator?.cancel()
        fadeAnimator = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        mediaPlayer?.let { existingPlayer ->
            runCatching { existingPlayer.stop() }
            runCatching { existingPlayer.release() }
        }
        mediaPlayer = null
        vibrator?.cancel()

        enforceAlarmVolume(isVolumeBoostEnabled)
        startVolumeEnforcer()

        try {
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val customUri = selectedSoundUri
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
            val alarmUri = customUri ?: fallbackUri

            if (alarmUri == null) {
                Log.e(TAG, "No alarm/notification/ringtone URI found on this device")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                val playbackUri = try {
                    setDataSource(applicationContext, alarmUri)
                    alarmUri
                } catch (e: Exception) {
                    if (fallbackUri != null && alarmUri != fallbackUri) {
                        Log.w(TAG, "Selected alarm sound unavailable; falling back to default", e)
                        reset()
                        setDataSource(applicationContext, fallbackUri)
                        fallbackUri
                    } else {
                        throw e
                    }
                }

                Log.d(TAG, "Playing alarm sound uri: $playbackUri")
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()

                if (isFadeInEnabled) {
                    setVolume(0f, 0f)
                } else {
                    setVolume(1f, 1f)
                }

                start()

                if (isVolumeBoostEnabled) {
                    val boostApplied = applyVolumeBoost(this)
                    if (!boostApplied) {
                        Log.e(TAG, "Volume boost could not be applied — LoudnessEnhancer unavailable")
                    }
                }

                if (isFadeInEnabled) {
                    fadeAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = fadeInDuration * 1000L
                        interpolator = android.view.animation.LinearInterpolator()
                        addUpdateListener { animator ->
                            val v = animator.animatedValue as Float
                            mediaPlayer?.setVolume(v, v)
                        }
                        start()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }

        if (isVibrationEnabled) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            vibrator?.let {
                if (it.hasVibrator()) {
                    val selectedPattern = VibrationPattern.fromName(vibrationPatternName)
                    val pattern = selectedPattern.pattern
                    Log.d(TAG, "Using vibration pattern: ${selectedPattern.displayName}")

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        @Suppress("DEPRECATION")
                        it.vibrate(VibrationEffect.createWaveform(pattern, 0), audioAttributes)
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(pattern, 0, audioAttributes)
                    }
                }
            }
        }

        scheduleAutoSilence(autoSilenceDuration)
    }

    private fun applyVolumeBoost(player: MediaPlayer): Boolean {
        val audioSessionId = player.audioSessionId
        if (audioSessionId <= 0) {
            Log.w(TAG, "Skipping volume boost: invalid audio session id=$audioSessionId")
            return false
        }

        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(BOOST_TARGET_GAIN_MB)
                enabled = true
            }
            Log.d(TAG, "Volume boost applied: ${BOOST_TARGET_GAIN_MB} mB gain (session=$audioSessionId)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Session boost failed (session=$audioSessionId), retrying global mix", e)
        }

        return try {
            loudnessEnhancer?.release()
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(0).apply {
                setTargetGain(BOOST_TARGET_GAIN_MB)
                enabled = true
            }
            Log.d(TAG, "Global mix boost applied: ${BOOST_TARGET_GAIN_MB} mB gain")
            true
        } catch (e: Exception) {
            Log.e(TAG, "All volume boost methods failed (${BOOST_TARGET_GAIN_MB} mB)", e)
            false
        }
    }

    private fun enforceAlarmVolume(isVolumeBoostEnabled: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        runCatching {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        }

        val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val currentAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        if (currentAlarmVolume < maxAlarmVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)
        }

        if (isVolumeBoostEnabled) {
            val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentMusicVolume < maxMusicVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVolume, 0)
            }
        }
    }

    private fun startVolumeEnforcer() {
        timeoutHandler.removeCallbacks(volumeEnforcerRunnable)
        timeoutHandler.post(volumeEnforcerRunnable)
    }

    private fun startActivityWatchdog() {
        timeoutHandler.removeCallbacks(activityWatchdogRunnable)
        timeoutHandler.postDelayed(activityWatchdogRunnable, ACTIVITY_WATCHDOG_INTERVAL_MS)
    }

    private fun isAlarmActivityVisible(): Boolean {
        return AlarmActivity.isAlarmScreenVisible
    }

    private fun launchAlarmActivity(alarmId: Int) {
        if (alarmId == -1) return
        if (isAlarmActivityVisible()) {
            Log.d(TAG, "AlarmActivity already visible; skipping launch")
            return
        }
        lastActivityLaunchTimeMs = System.currentTimeMillis()

        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        try {
            startActivity(activityIntent)
            Log.d(TAG, "AlarmActivity launched from service via startActivity")
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity failed (likely background restriction); falling back to PendingIntent", e)
            try {
                val launchPendingIntent = PendingIntent.getActivity(
                    this,
                    alarmId + 20_000,
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                launchPendingIntent.send()
                Log.d(TAG, "AlarmActivity launched from service via PendingIntent")
            } catch (pendingError: Exception) {
                Log.e(TAG, "Failed to launch AlarmActivity via PendingIntent fallback", pendingError)
            }
        }
    }

    private fun scheduleServiceRestart() {
        if (restartScheduled || currentAlarmId == -1) return

        restartScheduled = true
        val restartIntent = Intent(this, AlarmServiceRestartReceiver::class.java).apply {
            action = ACTION_RESTART_ALARM_SERVICE
            putExtra(EXTRA_ALARM_ID, currentAlarmId)
            putExtra(EXTRA_IS_VOLUME_BOOST_ENABLED, currentVolumeBoostEnabled)
        }

        val requestCode = 70_000 + currentAlarmId
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + RESTART_DELAY_MS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        Log.w(TAG, "Scheduled AlarmService restart for alarmId=$currentAlarmId")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "onTaskRemoved called while alarm is active")
        if (!stopRequested && currentAlarmId != -1) {
            scheduleServiceRestart()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeInstance = null
        isRinging = false
        Log.d(TAG, "AlarmService onDestroy: cleaning up resources")
        timeoutHandler.removeCallbacksAndMessages(null)
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        fadeAnimator?.cancel()
        fadeAnimator = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null

        if (!stopRequested && currentAlarmId != -1) {
            Log.w(TAG, "AlarmService destroyed unexpectedly; scheduling restart")
            scheduleServiceRestart()
        } else {
            clearPersistedRingingContext()
        }

        currentAlarmId = -1
        currentVolumeBoostEnabled = false
    }
}

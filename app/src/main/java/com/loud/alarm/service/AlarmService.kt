package com.loud.alarm.service

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
import com.loud.alarm.ui.alarm.AlarmActivity
import com.loud.alarm.data.VibrationPattern
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.roundToInt

@AndroidEntryPoint
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var fadeAnimator: android.animation.ValueAnimator? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())

    companion object {
        const val CHANNEL_ID = "alarm_ringing_v2"
        const val NOTIFICATION_ID = 1
        private const val TAG = "AlarmService"
        private const val BOOST_MULTIPLIER = 1.50f
        private const val MAX_LOUDNESS_ENHANCER_GAIN_MB = 10_000
    }

    @Inject
    lateinit var settingsRepository: com.loud.alarm.data.SettingsRepository

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val isVolumeBoostEnabled = intent?.getBooleanExtra("IS_VOLUME_BOOST_ENABLED", false) ?: false
        Log.d(TAG, "AlarmService started for alarm: $alarmId")

        // Read settings off main thread using a coroutine, but we need values now.
        // Use a background thread to read, then post back to start alarm.
        // However, for foreground service we MUST call startForeground ASAP (within 5s).
        // So we call startForeground FIRST with the notification, then read settings async.
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification(alarmId))

        // Read settings and start alarm on a background thread to avoid ANR.
        // Each setting read falls back independently so one failure does not
        // accidentally force unrelated defaults (for example fade-in ON).
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
                    vibrationPatternName = vibrationPatternName
                )
            }
        }.start()

        launchAlarmActivity(alarmId)

        // We will schedule auto silence in startAlarm instead
        // scheduleAutoSilence()

        return START_STICKY
    }

    private fun scheduleAutoSilence(durationMinutes: Int) {
        timeoutHandler.removeCallbacksAndMessages(null)
        val timeoutMs = durationMinutes * 60 * 1000L
        timeoutHandler.postDelayed({
            Log.w(TAG, "Auto-silencing alarm after timeout — user never dismissed")
            stopSelf()
        }, timeoutMs)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LoudAlarm::AlarmWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 min max, released on destroy
        }
        Log.d(TAG, "WakeLock acquired (PARTIAL)")
    }

    private fun createNotification(alarmId: Int): android.app.Notification {
        createNotificationChannel()

        // This PendingIntent opens AlarmActivity when notification is tapped
        val openAlarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
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
            .setContentIntent(openAlarmPendingIntent) // Tapping opens AlarmActivity
            .setFullScreenIntent(openAlarmPendingIntent, true) // Auto-opens when screen is off
            .setOngoing(true) // Cannot be swiped away
            .setAutoCancel(false) // Don't dismiss on tap
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete ALL old channels
            notificationManager.deleteNotificationChannel("alarm_channel")
            notificationManager.deleteNotificationChannel("alarm_service_silent")

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Ringing",
                NotificationManager.IMPORTANCE_HIGH // HIGH = heads-up + fullScreenIntent works
            ).apply {
                description = "Shows when alarm is ringing"
                setSound(null, null) // Sound is handled by MediaPlayer
                enableVibration(false) // Vibration is handled manually
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

    private fun startAlarm(isVolumeBoostEnabled: Boolean, isVibrationEnabled: Boolean, isFadeInEnabled: Boolean, fadeInDuration: Int, autoSilenceDuration: Int, vibrationPatternName: String = VibrationPattern.DEVICE_DEFAULT.name) {
        // Defensive cleanup if service receives another start before being destroyed.
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

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Force max alarm volume — this is the #1 reason alarms are "too quiet"
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        if (isVolumeBoostEnabled) {
            val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVolume, 0)
        }

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (alarmUri == null) {
                Log.e(TAG, "No alarm/notification/ringtone URI found on this device!")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
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
                    applyVolumeBoost(this, BOOST_MULTIPLIER)
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
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
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

    private fun applyVolumeBoost(player: MediaPlayer, multiplier: Float): Boolean {
        if (multiplier <= 1f) return false

        val audioSessionId = player.audioSessionId
        if (audioSessionId <= 0) {
            Log.w(TAG, "Skipping volume boost: invalid audio session id=$audioSessionId")
            return false
        }

        val targetGainMb = (20.0 * log10(multiplier.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, MAX_LOUDNESS_ENHANCER_GAIN_MB)

        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(targetGainMb)
                enabled = true
            }
            Log.d(TAG, "Volume boost applied: ${multiplier}x ($targetGainMb mB)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Session boost failed, retrying global mix boost ($targetGainMb mB)", e)
        }

        return try {
            loudnessEnhancer?.release()
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(0).apply {
                setTargetGain(targetGainMb)
                enabled = true
            }
            Log.d(TAG, "Global mix boost applied: ${multiplier}x ($targetGainMb mB)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force volume boost ($targetGainMb mB)", e)
            false
        }
    }

    private fun launchAlarmActivity(alarmId: Int) {
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        try {
            startActivity(activityIntent)
            Log.d(TAG, "AlarmActivity launched from service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmActivity from service", e)
        }
    }

    /**
     * Called when the user swipes the app away from recent apps.
     * We must NOT let the service die — the alarm must keep ringing!
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "onTaskRemoved called — alarm service continues ringing")
        // Service has START_STICKY, so it will be restarted if killed.
        // Nothing to do here — just log it. Do NOT call stopSelf().
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlarmService onDestroy — cleaning up resources")
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
    }
}

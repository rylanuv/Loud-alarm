package com.loud.alarm.ui.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private const val GRAVITY_ALPHA = 0.85f
private const val MAGNITUDE_SMOOTHING_ALPHA = 0.85f // Increased smoothing to filter vibration
private const val STEP_UPPER_THRESHOLD = 1.6f // Increased to avoid vibration triggers
private const val STEP_LOWER_THRESHOLD = 0.6f // Increased to ensure valley is reached despite vibration
private const val MIN_STEP_INTERVAL_MS = 250L
private const val MAX_STEP_INTERVAL_MS = 1800L
private const val ACCEL_WARMUP_MS = 800L
private const val PEAK_TO_VALLEY_TIMEOUT_MS = 800L
private const val STATIONARY_RESET_MS = 3000L
private const val REQUIRED_CADENCE_STREAK = 1L // Start counting from first clear step to feel more responsive

@Composable
fun StepChallengeScreen(
    targetSteps: Int,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var currentSteps by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(currentSteps) {
        if (currentSteps >= targetSteps) {
            onSuccess()
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val gravity = floatArrayOf(0f, 0f, 0f)
        var smoothedMagnitude = 0f
        var waitingForValley = false
        var peakTimestampMs = 0L
        var lastCandidateStepTimestampMs = 0L
        var lastAcceptedStepTimestampMs = 0L
        var cadenceStreak = 0L
        var accelStartTimestampMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val nowMs = event.timestamp / 1_000_000L
                    if (accelStartTimestampMs == 0L) accelStartTimestampMs = nowMs

                    // Remove gravity, then smooth magnitude to reduce noise from small hand jitter.
                    for (index in 0..2) {
                        gravity[index] =
                            (GRAVITY_ALPHA * gravity[index]) + ((1f - GRAVITY_ALPHA) * event.values[index])
                    }

                    val linearX = event.values[0] - gravity[0]
                    val linearY = event.values[1] - gravity[1]
                    val linearZ = event.values[2] - gravity[2]
                    val linearMagnitude =
                        sqrt((linearX * linearX + linearY * linearY + linearZ * linearZ).toDouble()).toFloat()

                    smoothedMagnitude =
                        (MAGNITUDE_SMOOTHING_ALPHA * smoothedMagnitude) +
                            ((1f - MAGNITUDE_SMOOTHING_ALPHA) * linearMagnitude)

                    if (nowMs - accelStartTimestampMs < ACCEL_WARMUP_MS) return

                    if (!waitingForValley) {
                        val enoughTimeSinceLastPeak = nowMs - peakTimestampMs > MIN_STEP_INTERVAL_MS
                        if (smoothedMagnitude > STEP_UPPER_THRESHOLD && enoughTimeSinceLastPeak) {
                            peakTimestampMs = nowMs
                            waitingForValley = true
                        }
                    } else {
                        if (smoothedMagnitude < STEP_LOWER_THRESHOLD) {
                            val intervalSinceCandidate = nowMs - lastCandidateStepTimestampMs
                            cadenceStreak = if (intervalSinceCandidate in MIN_STEP_INTERVAL_MS..MAX_STEP_INTERVAL_MS) {
                                cadenceStreak + 1
                            } else {
                                1L
                            }
                            lastCandidateStepTimestampMs = nowMs

                            if (cadenceStreak >= REQUIRED_CADENCE_STREAK) {
                                val intervalSinceAccepted = nowMs - lastAcceptedStepTimestampMs
                                val shouldAccept =
                                    lastAcceptedStepTimestampMs == 0L ||
                                        intervalSinceAccepted in MIN_STEP_INTERVAL_MS..MAX_STEP_INTERVAL_MS
                                if (shouldAccept) {
                                    currentSteps += 1
                                    lastAcceptedStepTimestampMs = nowMs
                                }
                            }

                            waitingForValley = false
                        } else if (nowMs - peakTimestampMs > PEAK_TO_VALLEY_TIMEOUT_MS) {
                            waitingForValley = false
                        }
                    }

                    if (lastCandidateStepTimestampMs != 0L &&
                        nowMs - lastCandidateStepTimestampMs > STATIONARY_RESET_MS
                    ) {
                        cadenceStreak = 0
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Walk to wake up!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hold your phone and take steps",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentSteps",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ $targetSteps steps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

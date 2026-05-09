package com.loud.alarm.ui.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "StepChallenge"

// ── Hardware step-detector fallback debouncing ──
private const val HW_MIN_STEP_INTERVAL_MS = 250L

// ── Accelerometer fallback constants ──
private const val GRAVITY_ALPHA = 0.8f
private const val MAGNITUDE_SMOOTHING_ALPHA = 0.65f
// Moderated thresholds for more reliable step detection
private const val STEP_UPPER_THRESHOLD = 1.2f
private const val STEP_LOWER_THRESHOLD = 0.5f
private const val MIN_STEP_INTERVAL_MS = 250L
private const val ACCEL_WARMUP_MS = 600L
private const val PEAK_TO_VALLEY_TIMEOUT_MS = 1000L
// Require the gravity-aligned axis to carry some of the acceleration (walking = vertical bounce)
private const val GRAVITY_AXIS_RATIO_MIN = 0.2f

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

        // Prefer the hardware step detector
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val useHardwareDetector = stepDetector != null

        Log.d(TAG, "Hardware step detector available: $useHardwareDetector")

        val listener: SensorEventListener

        if (useHardwareDetector) {
            // ── Hardware step detector with simple debouncing ──
            var lastHwStepMs = 0L

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type != Sensor.TYPE_STEP_DETECTOR) return

                    val nowMs = System.currentTimeMillis()

                    // Basic debounce to prevent rapid shake triggering, while relying 
                    // on the OS hardware detector for actual cadence and filtering
                    if (lastHwStepMs == 0L || (nowMs - lastHwStepMs) >= HW_MIN_STEP_INTERVAL_MS) {
                        currentSteps += 1
                        lastHwStepMs = nowMs
                        Log.d(TAG, "HW step detected, total=$currentSteps")
                    } else {
                        Log.d(TAG, "HW step rejected: debounced")
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, stepDetector, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            // ── Accelerometer fallback with anti-shake protection ──
            Log.d(TAG, "Falling back to accelerometer-based step detection")
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val gravity = floatArrayOf(0f, 0f, 0f)
            var smoothedMagnitude = 0f
            var waitingForValley = false
            var peakTimestampMs = 0L
            var lastStepTimestampMs = 0L
            var accelStartTimestampMs = 0L
            // Track the gravity-axis component of acceleration at the peak
            var peakGravityAxisRatio = 0f

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                    val nowMs = event.timestamp / 1_000_000L
                    if (accelStartTimestampMs == 0L) accelStartTimestampMs = nowMs

                    // Isolate linear acceleration by removing gravity
                    for (i in 0..2) {
                        gravity[i] = GRAVITY_ALPHA * gravity[i] + (1f - GRAVITY_ALPHA) * event.values[i]
                    }

                    val lx = event.values[0] - gravity[0]
                    val ly = event.values[1] - gravity[1]
                    val lz = event.values[2] - gravity[2]
                    val magnitude = sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()

                    smoothedMagnitude =
                        MAGNITUDE_SMOOTHING_ALPHA * smoothedMagnitude +
                            (1f - MAGNITUDE_SMOOTHING_ALPHA) * magnitude

                    // Let gravity filter settle
                    if (nowMs - accelStartTimestampMs < ACCEL_WARMUP_MS) return

                    // Determine how much of the acceleration is along the gravity axis.
                    // Walking produces a vertical bounce; shaking is multi-axis.
                    val gravityMag = sqrt(
                        (gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]).toDouble()
                    ).toFloat().coerceAtLeast(0.01f)
                    // Project linear acceleration onto the gravity direction
                    val gravityAxisAccel = abs(
                        (lx * gravity[0] + ly * gravity[1] + lz * gravity[2]) / gravityMag
                    )
                    val currentGravityRatio = if (magnitude > 0.01f) gravityAxisAccel / magnitude else 0f

                    if (!waitingForValley) {
                        // Detect peak
                        if (smoothedMagnitude > STEP_UPPER_THRESHOLD &&
                            nowMs - peakTimestampMs > MIN_STEP_INTERVAL_MS
                        ) {
                            peakTimestampMs = nowMs
                            peakGravityAxisRatio = currentGravityRatio
                            waitingForValley = true
                        }
                    } else {
                        // Detect valley → potential step
                        if (smoothedMagnitude < STEP_LOWER_THRESHOLD) {
                            val interval = nowMs - lastStepTimestampMs

                            // Validate: interval not too fast (debounce) & acceleration was mostly vertical
                            val intervalValid = lastStepTimestampMs == 0L || interval >= MIN_STEP_INTERVAL_MS
                            val axisValid = peakGravityAxisRatio >= GRAVITY_AXIS_RATIO_MIN

                            if (intervalValid && axisValid) {
                                currentSteps += 1
                                Log.d(TAG, "Accel step detected, gravRatio=%.2f, total=$currentSteps".format(peakGravityAxisRatio))
                                lastStepTimestampMs = nowMs
                            } else if (!intervalValid) {
                                Log.d(TAG, "Accel step rejected: interval too fast (${interval}ms)")
                            } else {
                                Log.d(TAG, "Accel step rejected: bad axis (ratio=%.2f)".format(peakGravityAxisRatio))
                            }
                            waitingForValley = false
                        } else if (nowMs - peakTimestampMs > PEAK_TO_VALLEY_TIMEOUT_MS) {
                            // Timed out waiting for valley — reset
                            waitingForValley = false
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            accelerometer?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

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
            text = "Hold your phone and walk around",
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

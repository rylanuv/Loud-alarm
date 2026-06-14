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

// ── Hardware step-detector debouncing ──
private const val HW_MIN_STEP_INTERVAL_MS = 300L

// ── Accelerometer fallback constants ──
private const val GRAVITY_ALPHA = 0.8f
private const val MAGNITUDE_SMOOTHING_ALPHA = 0.55f

// Thresholds for peak/valley detection
private const val STEP_UPPER_THRESHOLD = 1.4f
private const val STEP_LOWER_THRESHOLD = 0.6f
private const val MIN_STEP_INTERVAL_MS = 300L
private const val ACCEL_WARMUP_MS = 600L
private const val PEAK_TO_VALLEY_TIMEOUT_MS = 1000L

// Walking produces vertical bounce; shaking is multi-axis
private const val GRAVITY_AXIS_RATIO_MIN = 0.30f

// Cadence validation: real walking is ~1.5–2.5 steps/sec (400–670ms intervals)
private const val CADENCE_TOLERANCE = 0.40f           // ±40% from rolling average
private const val CADENCE_MIN_STEPS_FOR_VALIDATION = 3 // start enforcing cadence after this many steps

// Consecutive-step gating: reject isolated jolts
private const val CONSECUTIVE_GATE_COUNT = 2   // first N candidate steps are tentative
private const val INACTIVITY_TIMEOUT_MS = 3000L // reset gate after no steps for this long

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
            // ── Hardware step detector with debouncing ──
            var lastHwStepMs = 0L

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type != Sensor.TYPE_STEP_DETECTOR) return

                    val nowMs = System.currentTimeMillis()

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

            sensorManager.registerListener(listener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
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
            var peakGravityAxisRatio = 0f

            // Cadence tracking: rolling average of recent step intervals
            val recentIntervals = ArrayDeque<Long>(8)
            var candidateStepsInBout = 0   // consecutive valid candidates in current walking bout
            var pendingSteps = 0           // steps waiting for gating confirmation

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                    val nowMs = event.timestamp / 1_000_000L
                    if (accelStartTimestampMs == 0L) accelStartTimestampMs = nowMs

                    // ── Inactivity timeout: reset walking bout state ──
                    if (lastStepTimestampMs != 0L && (nowMs - lastStepTimestampMs) > INACTIVITY_TIMEOUT_MS) {
                        candidateStepsInBout = 0
                        pendingSteps = 0
                        recentIntervals.clear()
                        Log.d(TAG, "Inactivity timeout: reset walking bout")
                    }

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
                    val gravityMag = sqrt(
                        (gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]).toDouble()
                    ).toFloat().coerceAtLeast(0.01f)
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

                            // ── Validation checks ──
                            val intervalValid = lastStepTimestampMs == 0L || interval >= MIN_STEP_INTERVAL_MS
                            val axisValid = peakGravityAxisRatio >= GRAVITY_AXIS_RATIO_MIN

                            // Cadence validation: after enough steps, check rhythm consistency
                            val cadenceValid = if (recentIntervals.size >= CADENCE_MIN_STEPS_FOR_VALIDATION && lastStepTimestampMs != 0L) {
                                val avgInterval = recentIntervals.average().toLong()
                                val minAllowed = (avgInterval * (1.0 - CADENCE_TOLERANCE)).toLong()
                                val maxAllowed = (avgInterval * (1.0 + CADENCE_TOLERANCE)).toLong()
                                interval in minAllowed..maxAllowed
                            } else {
                                true // not enough data yet to validate cadence
                            }

                            if (intervalValid && axisValid && cadenceValid) {
                                candidateStepsInBout += 1

                                // Track interval for cadence
                                if (lastStepTimestampMs != 0L) {
                                    recentIntervals.addLast(interval)
                                    if (recentIntervals.size > 6) recentIntervals.removeFirst()
                                }
                                lastStepTimestampMs = nowMs

                                if (candidateStepsInBout <= CONSECUTIVE_GATE_COUNT) {
                                    // Tentative step — don't count yet
                                    pendingSteps += 1
                                    Log.d(TAG, "Accel step tentative (#$candidateStepsInBout), pending=$pendingSteps, gravRatio=%.2f".format(peakGravityAxisRatio))
                                } else {
                                    // We're past the gate — flush pending + count this one
                                    if (pendingSteps > 0) {
                                        currentSteps += pendingSteps
                                        Log.d(TAG, "Accel gate passed: flushed $pendingSteps pending steps")
                                        pendingSteps = 0
                                    }
                                    currentSteps += 1
                                    Log.d(TAG, "Accel step confirmed, gravRatio=%.2f, total=$currentSteps".format(peakGravityAxisRatio))
                                }
                            } else {
                                // Invalid step candidate — if we haven't passed the gate, reset the bout
                                if (candidateStepsInBout <= CONSECUTIVE_GATE_COUNT) {
                                    if (pendingSteps > 0) {
                                        Log.d(TAG, "Accel bout invalidated: discarding $pendingSteps pending steps")
                                    }
                                    candidateStepsInBout = 0
                                    pendingSteps = 0
                                    recentIntervals.clear()
                                }

                                when {
                                    !intervalValid -> Log.d(TAG, "Accel step rejected: interval too fast (${interval}ms)")
                                    !axisValid -> Log.d(TAG, "Accel step rejected: bad axis (ratio=%.2f)".format(peakGravityAxisRatio))
                                    !cadenceValid -> Log.d(TAG, "Accel step rejected: cadence mismatch (interval=${interval}ms)")
                                }
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

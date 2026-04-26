package com.loud.alarm.ui.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "SquatChallenge"

/**
 * Squat detection via accelerometer — velocity-based with **temporal gating**.
 *
 * A squat is distinguished from a shake by its TIME SCALE:
 *   - Shake:  down+up completes in ~200ms
 *   - Squat:  down takes 0.4-2s, up takes 0.3-2s
 *
 * Algorithm:
 * 1. Compute orientation-independent vertical acceleration
 * 2. Integrate to estimate vertical velocity
 * 3. 3-phase state machine with MINIMUM DURATION requirements:
 *      IDLE → GOING_DOWN (must stay ≥ 350ms) → GOING_UP (must stay ≥ 300ms) → count
 * 4. Phase timeouts prevent getting stuck if velocity drifts
 *
 * The time gates make it physically impossible to trigger by shaking.
 */

// Gravity isolation
private const val GRAVITY_ALPHA = 0.8f

// Velocity drift correction
private const val VELOCITY_DECAY = 0.96f

// Velocity thresholds to enter each phase (m/s)
// Raised above noise floor (~0.1 m/s) to avoid false phase transitions
private const val VELOCITY_DOWN_ENTER = -0.25f  // Start of descent
private const val VELOCITY_UP_ENTER = 0.25f     // Start of ascent

// Threshold for "velocity has settled" (lower than enter thresholds)
private const val VELOCITY_SETTLED = 0.15f

// ── TIME GATES ──
// Minimum time the user must be in the DOWN phase before we accept UP transition.
// A real squat descent takes at least 0.4-1.5 seconds. A shake is < 0.2s.
private const val MIN_DOWN_DURATION_MS = 350L

// Minimum time in UP phase before counting. Ensures the user actually stood back up.
private const val MIN_UP_DURATION_MS = 300L

// Maximum time in each phase before we assume it's a false trigger and reset
private const val MAX_DOWN_DURATION_MS = 4000L
private const val MAX_UP_DURATION_MS = 3000L

// Minimum gap between two counted squats
private const val MIN_SQUAT_INTERVAL_MS = 800L

// Warmup time for gravity filter
private const val WARMUP_MS = 800L

private enum class SquatPhase {
    IDLE,        // Waiting for downward velocity
    GOING_DOWN,  // User is descending — must persist for MIN_DOWN_DURATION_MS
    GOING_UP     // User is ascending — will count after MIN_UP_DURATION_MS + velocity settles
}

@Composable
fun SquatChallengeScreen(
    targetSquats: Int,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var currentSquats by rememberSaveable { mutableIntStateOf(0) }
    var pulseKey by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(currentSquats) {
        if (currentSquats >= targetSquats) {
            onSuccess()
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (pulseKey % 2 == 0) 1f else 1.12f,
        animationSpec = tween(durationMillis = 200),
        label = "pulse"
    )

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val gravity = floatArrayOf(0f, 0f, 0f)
        var verticalVelocity = 0f
        var phase = SquatPhase.IDLE
        var phaseStartMs = 0L       // When current phase began
        var lastSquatMs = 0L
        var startMs = 0L
        var prevTimestampNs = 0L
        // Track whether we ever saw strong enough velocity in each phase
        var sawStrongDown = false
        var sawStrongUp = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                val nowNs = event.timestamp
                val nowMs = nowNs / 1_000_000L
                if (startMs == 0L) startMs = nowMs

                // ── 1. Isolate gravity ──
                for (i in 0..2) {
                    gravity[i] = GRAVITY_ALPHA * gravity[i] + (1f - GRAVITY_ALPHA) * event.values[i]
                }

                if (nowMs - startMs < WARMUP_MS) {
                    prevTimestampNs = nowNs
                    return
                }

                // ── 2. Compute dt ──
                if (prevTimestampNs == 0L) {
                    prevTimestampNs = nowNs
                    return
                }
                val dtSeconds = (nowNs - prevTimestampNs) / 1_000_000_000f
                prevTimestampNs = nowNs
                if (dtSeconds <= 0f || dtSeconds > 0.5f) return

                // ── 3. Orientation-independent vertical acceleration ──
                val gx = gravity[0]; val gy = gravity[1]; val gz = gravity[2]
                val gMag = sqrt(gx * gx + gy * gy + gz * gz)
                if (gMag < 0.1f) return

                val lx = event.values[0] - gx
                val ly = event.values[1] - gy
                val lz = event.values[2] - gz

                // Dot product: project linear accel onto gravity direction
                // Positive = accelerating upward, Negative = accelerating downward
                val verticalAccel = (lx * gx + ly * gy + lz * gz) / gMag

                // ── 4. Integrate to velocity + drift correction ──
                verticalVelocity = (verticalVelocity + verticalAccel * dtSeconds) * VELOCITY_DECAY

                // ── 5. State machine with time gates ──
                val timeInPhaseMs = nowMs - phaseStartMs

                when (phase) {
                    SquatPhase.IDLE -> {
                        if (verticalVelocity < VELOCITY_DOWN_ENTER) {
                            phase = SquatPhase.GOING_DOWN
                            phaseStartMs = nowMs
                            sawStrongDown = true
                            Log.d(TAG, "→ GOING_DOWN  vel=$verticalVelocity")
                        }
                    }

                    SquatPhase.GOING_DOWN -> {
                        // Track peak downward velocity
                        if (verticalVelocity < VELOCITY_DOWN_ENTER) {
                            sawStrongDown = true
                        }

                        // Timeout — probably a false trigger or user abandoned
                        if (timeInPhaseMs > MAX_DOWN_DURATION_MS) {
                            phase = SquatPhase.IDLE
                            sawStrongDown = false
                            Log.d(TAG, "→ IDLE (down phase timed out: ${timeInPhaseMs}ms)")
                        }
                        // If velocity reverses (going upward now)
                        else if (verticalVelocity > VELOCITY_UP_ENTER) {
                            if (timeInPhaseMs >= MIN_DOWN_DURATION_MS && sawStrongDown) {
                                // Legit descent lasted long enough → transition to UP
                                phase = SquatPhase.GOING_UP
                                phaseStartMs = nowMs
                                sawStrongUp = true
                                Log.d(TAG, "→ GOING_UP  vel=$verticalVelocity  downDuration=${timeInPhaseMs}ms")
                            } else {
                                // Too fast — was a shake, not a squat
                                phase = SquatPhase.IDLE
                                sawStrongDown = false
                                Log.d(TAG, "→ IDLE (down too short: ${timeInPhaseMs}ms, rejected)")
                            }
                        }
                        // If velocity settles near zero for long enough while in DOWN, transition
                        // This handles slow squats where velocity decays before reversal
                        else if (timeInPhaseMs >= MIN_DOWN_DURATION_MS && sawStrongDown
                            && abs(verticalVelocity) < VELOCITY_SETTLED) {
                            // Velocity decayed but we had a real descent — wait for upward motion
                            // Stay in GOING_DOWN, the upward reversal will come
                        }
                    }

                    SquatPhase.GOING_UP -> {
                        // Track peak upward velocity
                        if (verticalVelocity > VELOCITY_UP_ENTER) {
                            sawStrongUp = true
                        }

                        // Timeout — prevent getting stuck
                        if (timeInPhaseMs > MAX_UP_DURATION_MS) {
                            // If we had strong upward motion and enough time, still count it
                            if (sawStrongUp && timeInPhaseMs >= MIN_UP_DURATION_MS) {
                                if (nowMs - lastSquatMs >= MIN_SQUAT_INTERVAL_MS) {
                                    currentSquats += 1
                                    lastSquatMs = nowMs
                                    pulseKey += 1
                                    Log.d(TAG, "✓ SQUAT COUNTED (timeout)  total=$currentSquats  upDuration=${timeInPhaseMs}ms")
                                }
                            }
                            phase = SquatPhase.IDLE
                            sawStrongDown = false
                            sawStrongUp = false
                            Log.d(TAG, "→ IDLE (up phase timed out: ${timeInPhaseMs}ms)")
                        }
                        // Count once we've been ascending long enough AND velocity settles
                        else if (timeInPhaseMs >= MIN_UP_DURATION_MS && abs(verticalVelocity) < VELOCITY_SETTLED) {
                            if (nowMs - lastSquatMs >= MIN_SQUAT_INTERVAL_MS) {
                                currentSquats += 1
                                lastSquatMs = nowMs
                                pulseKey += 1
                                Log.d(TAG, "✓ SQUAT COUNTED  total=$currentSquats  upDuration=${timeInPhaseMs}ms")
                            }
                            phase = SquatPhase.IDLE
                            sawStrongDown = false
                            sawStrongUp = false
                        } else if (verticalVelocity < VELOCITY_DOWN_ENTER && timeInPhaseMs < MIN_UP_DURATION_MS) {
                            // Reversed again too quickly — was oscillation/shake
                            phase = SquatPhase.IDLE
                            sawStrongDown = false
                            sawStrongUp = false
                            Log.d(TAG, "→ IDLE (up too short: ${timeInPhaseMs}ms, rejected)")
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
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
            text = "Squat to wake up!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hold your phone and do squats",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentSquats",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ $targetSquats squats",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

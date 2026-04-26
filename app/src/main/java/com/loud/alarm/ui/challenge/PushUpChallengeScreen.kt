package com.loud.alarm.ui.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val TAG = "PushUpChallenge"

/**
 * Push-up detection using the proximity sensor.
 *
 * When the user places the phone face-down on the floor and does push-ups,
 * their face/chest approaches the screen on the way down (proximity = NEAR)
 * and moves away on the way up (proximity = FAR).
 *
 * Each NEAR→FAR transition counts as one push-up rep.
 *
 * Falls back to accelerometer-based detection if no proximity sensor is available.
 */
private const val MIN_PUSHUP_INTERVAL_MS = 600L

// Accelerometer fallback constants
private const val GRAVITY_ALPHA = 0.8f
private const val SMOOTHING_ALPHA = 0.7f
private const val ACCEL_DOWN_THRESHOLD = -2.0f
private const val ACCEL_UP_THRESHOLD = 2.0f
private const val ACCEL_WARMUP_MS = 600L

@Composable
fun PushUpChallengeScreen(
    targetPushUps: Int,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var currentPushUps by rememberSaveable { mutableStateOf(0) }
    var useProximity by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(currentPushUps) {
        if (currentPushUps >= targetPushUps) {
            onSuccess()
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (currentPushUps > 0) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pulse"
    )

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val listener: SensorEventListener

        if (proximitySensor != null) {
            // ── Proximity-based detection ──
            val maxRange = proximitySensor.maximumRange
            var wasNear = false
            var lastPushUpMs = 0L

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return

                    val distance = event.values[0]
                    val isNear = distance < maxRange / 2f
                    val nowMs = System.currentTimeMillis()

                    if (wasNear && !isNear && nowMs - lastPushUpMs >= MIN_PUSHUP_INTERVAL_MS) {
                        // NEAR → FAR = one push-up completed
                        currentPushUps += 1
                        lastPushUpMs = nowMs
                        Log.d(TAG, "Proximity push-up detected, total=$currentPushUps")
                    }
                    wasNear = isNear
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            // ── Accelerometer fallback (Z-axis for face-down phone) ──
            useProximity = false
            Log.d(TAG, "No proximity sensor, falling back to accelerometer")
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val gravity = floatArrayOf(0f, 0f, 0f)
            var smoothedZ = 0f
            var wentDown = false
            var lastPushUpMs = 0L
            var startMs = 0L

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                    val nowMs = event.timestamp / 1_000_000L
                    if (startMs == 0L) startMs = nowMs
                    if (nowMs - startMs < ACCEL_WARMUP_MS) return

                    for (i in 0..2) {
                        gravity[i] = GRAVITY_ALPHA * gravity[i] + (1f - GRAVITY_ALPHA) * event.values[i]
                    }

                    val linearZ = event.values[2] - gravity[2]
                    smoothedZ = SMOOTHING_ALPHA * smoothedZ + (1f - SMOOTHING_ALPHA) * linearZ

                    if (!wentDown) {
                        if (smoothedZ < ACCEL_DOWN_THRESHOLD) {
                            wentDown = true
                        }
                    } else {
                        if (smoothedZ > ACCEL_UP_THRESHOLD && nowMs - lastPushUpMs >= MIN_PUSHUP_INTERVAL_MS) {
                            currentPushUps += 1
                            lastPushUpMs = nowMs
                            wentDown = false
                            Log.d(TAG, "Accel push-up detected, total=$currentPushUps")
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
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
            text = "Push-ups to wake up!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (useProximity)
                "Place phone face-up on the floor and do push-ups over it"
            else
                "Hold your phone and do push-ups",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
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
                    text = "$currentPushUps",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ $targetPushUps push-ups",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

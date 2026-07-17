package com.loud.alarm.ui.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Room Light Challenge: The user must turn on the room light (or move the phone
 * near a bright light source). We use the ambient light sensor to detect a
 * significant increase in lux from the baseline reading captured when the
 * challenge first loads.
 *
 * - The challenge completes when the sensor reads a lux value that meets or exceeds the target lux.
 */

private const val DARK_ROOM_THRESHOLD_LUX = 20f
private const val DARK_ROOM_MIN_INCREASE = 80f   // absolute lux increase for dark rooms
private const val LIT_ROOM_MULTIPLIER = 3f        // must reach 3× baseline for already-lit rooms
private const val BASELINE_SAMPLE_COUNT = 5        // number of readings to average for baseline

@Composable
fun LightChallengeScreen(
    targetLux: Int,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isDone by rememberSaveable { mutableStateOf(false) }
    var currentLux by rememberSaveable { mutableFloatStateOf(0f) }
    var maxLuxReached by rememberSaveable { mutableFloatStateOf(0f) }
    var hasSensor by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isDone) {
        if (isDone) {
            onSuccess()
        }
    }

    DisposableEffect(isDone) {
        if (isDone) {
            onDispose { }
        } else {
            val sensorManager =
                context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

            if (lightSensor == null) {
                // Device has no light sensor – fall back to immediate success
                hasSensor = false
                isDone = true
                onDispose { }
            } else {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val lux = event?.values?.getOrNull(0) ?: return
                        currentLux = lux

                        if (lux > maxLuxReached) {
                            maxLuxReached = lux
                        }

                        // Check if the lux target is met
                        if (lux >= targetLux) {
                            isDone = true
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                sensorManager.registerListener(
                    listener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_UI
                )

                onDispose {
                    try {
                        sensorManager.unregisterListener(listener)
                    } catch (_: Exception) { }
                }
            }
        }
    }

    // ─── UI ───
    val infiniteTransition = rememberInfiniteTransition(label = "light_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val bulbOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bulb_bounce"
    )

    val isCalibrating = false // No longer calibrating
    val primaryColor = Color(0xFFFDD835)
    val secondaryColor = Color(0xFFFBC02D)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Turn on the lights",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Reach $targetLux lux to dismiss the alarm",
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
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.3f),
                            secondaryColor.copy(alpha = 0.08f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = bulbOffset.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Progress indicator
        AnimatedVisibility(
            visible = !isDone,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val progress = (currentLux / targetLux).coerceIn(0f, 1f)

                Text(
                    text = "${currentLux.toInt()} / $targetLux lux",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Brightness: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        if (!hasSensor) {
            Text(
                text = "No light sensor detected — challenge skipped.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF6B6B),
                textAlign = TextAlign.Center
            )
        }
    }
}

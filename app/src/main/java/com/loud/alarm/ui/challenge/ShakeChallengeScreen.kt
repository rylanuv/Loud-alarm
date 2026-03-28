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

private const val TARGET_SHAKES = 12
private const val SHAKE_THRESHOLD = 8.5f
private const val SHAKE_DEBOUNCE_MS = 300L
private const val SENSOR_WARMUP_MS = 500L
private const val GRAVITY_ALPHA = 0.8f

@Composable
fun ShakeChallengeScreen(
    targetShakes: Int = TARGET_SHAKES,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var shakeCount by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(shakeCount) {
        if (shakeCount >= targetShakes) {
            onSuccess()
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravity = floatArrayOf(0f, 0f, 0f)
        var sensorStartMs = 0L
        var lastShakeMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val nowMs = event.timestamp / 1_000_000L
                if (sensorStartMs == 0L) sensorStartMs = nowMs
                if (nowMs - sensorStartMs < SENSOR_WARMUP_MS) return

                for (index in 0..2) {
                    gravity[index] =
                        (GRAVITY_ALPHA * gravity[index]) + ((1f - GRAVITY_ALPHA) * event.values[index])
                }

                val linearX = event.values[0] - gravity[0]
                val linearY = event.values[1] - gravity[1]
                val linearZ = event.values[2] - gravity[2]
                val linearMagnitude =
                    sqrt((linearX * linearX + linearY * linearY + linearZ * linearZ).toDouble()).toFloat()

                if (linearMagnitude >= SHAKE_THRESHOLD && nowMs - lastShakeMs >= SHAKE_DEBOUNCE_MS) {
                    lastShakeMs = nowMs
                    if (shakeCount < targetShakes) shakeCount += 1
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
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
            text = "Shake to wake up!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Shake your phone firmly to dismiss the alarm",
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
                    text = "$shakeCount",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ $targetShakes shakes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

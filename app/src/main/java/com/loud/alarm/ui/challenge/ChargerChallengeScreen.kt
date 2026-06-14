package com.loud.alarm.ui.challenge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class ChargerState {
    WAITING_FOR_PLUG,      // Phone is not charging, waiting for user to plug in
    WAITING_FOR_UNPLUG,    // Phone is already charging, waiting for user to unplug first
    WAITING_FOR_REPLUG,    // User unplugged, now waiting for them to plug back in
    DONE                   // Challenge complete
}

@Composable
fun ChargerChallengeScreen(
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var chargerState by rememberSaveable { mutableStateOf<String?>(null) }

    // Initialize state based on current charging status
    if (chargerState == null) {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = plugged != 0
        chargerState = if (isCharging) {
            ChargerState.WAITING_FOR_UNPLUG.name
        } else {
            ChargerState.WAITING_FOR_PLUG.name
        }
    }

    val currentState = try {
        ChargerState.valueOf(chargerState!!)
    } catch (_: Exception) {
        ChargerState.WAITING_FOR_PLUG
    }

    LaunchedEffect(currentState) {
        if (currentState == ChargerState.DONE) {
            onSuccess()
        }
    }

    DisposableEffect(currentState) {
        if (currentState == ChargerState.DONE) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_POWER_CONNECTED -> {
                            if (chargerState == ChargerState.WAITING_FOR_PLUG.name ||
                                chargerState == ChargerState.WAITING_FOR_REPLUG.name
                            ) {
                                chargerState = ChargerState.DONE.name
                            }
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            if (chargerState == ChargerState.WAITING_FOR_UNPLUG.name) {
                                chargerState = ChargerState.WAITING_FOR_REPLUG.name
                            }
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            context.registerReceiver(receiver, filter)

            onDispose {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) { }
            }
        }
    }

    // ─── UI ───
    val infiniteTransition = rememberInfiniteTransition(label = "charger_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val boltOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bolt_bounce"
    )

    val isUnplugPhase = currentState == ChargerState.WAITING_FOR_UNPLUG
    val primaryColor = if (isUnplugPhase) Color(0xFFFF6B6B) else Color(0xFF4ADE80)
    val secondaryColor = if (isUnplugPhase) Color(0xFFFF3B3B) else Color(0xFF22C55E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (currentState) {
                ChargerState.WAITING_FOR_PLUG -> "Plug in your charger!"
                ChargerState.WAITING_FOR_UNPLUG -> "Unplug your charger first!"
                ChargerState.WAITING_FOR_REPLUG -> "Now plug it back in!"
                ChargerState.DONE -> "Charger connected!"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (currentState) {
                ChargerState.WAITING_FOR_PLUG -> "Connect your phone to a charger to dismiss the alarm"
                ChargerState.WAITING_FOR_UNPLUG -> "Your phone is already charging — unplug it first, then plug it back in"
                ChargerState.WAITING_FOR_REPLUG -> "Great! Now connect the charger to dismiss"
                ChargerState.DONE -> "Challenge complete!"
            },
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
                imageVector = if (isUnplugPhase) Icons.Default.PowerOff
                else Icons.Default.Power,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = boltOffset.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Step indicator for the unplug->replug flow
        AnimatedVisibility(
            visible = currentState == ChargerState.WAITING_FOR_UNPLUG ||
                    currentState == ChargerState.WAITING_FOR_REPLUG,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val step1Done = currentState == ChargerState.WAITING_FOR_REPLUG
                Text(
                    text = if (step1Done) "✓ Step 1: Unplug charger" else "→ Step 1: Unplug charger",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!step1Done) FontWeight.Bold else FontWeight.Normal,
                    color = if (step1Done) Color(0xFF4ADE80) else Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (step1Done) "→ Step 2: Plug it back in" else "  Step 2: Plug it back in",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (step1Done) FontWeight.Bold else FontWeight.Normal,
                    color = if (step1Done) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

package com.loud.alarm.ui.alarm

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CongratsScreen(onAnimationFinished: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val rotation = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(30f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse_anim"
    )

    LaunchedEffect(Unit) {
        // Sun scale up and rotate
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
            )
        }

        // Text fade and slide up
        launch {
            delay(400)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(400)
            textOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        
        delay(3000) // Hold the screen for a bit before dismissing
        
        // Quick fade out at the end
        launch {
            textAlpha.animateTo(0f, tween(300))
            scale.animateTo(0f, tween(300))
        }
        delay(300)
        
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // AMOLED Pure Black
        contentAlignment = Alignment.Center
    ) {
        // Subtle morning glow
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2.5f

            // Soft radiating glow from the sun
            drawCircle(
                color = secondaryColor.copy(alpha = (1f - pulse) * 0.15f),
                radius = radius + (radius * pulse * 0.5f),
                center = center
            )
            
            // Outer pulse
            drawCircle(
                color = primaryColor.copy(alpha = (1f - pulse) * 0.05f),
                radius = radius + (radius * pulse * 1.5f),
                center = center
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                    },
                contentAlignment = Alignment.Center
            ) {
                // Sun Icon representing morning / waking up
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Awake",
                    tint = primaryColor,
                    modifier = Modifier.size(96.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "ALARM\nDISMISSED",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAlpha.value
                        translationY = textOffset.value
                    }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Good morning. You're awake.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAlpha.value
                        translationY = textOffset.value
                    }
            )
        }
    }
}

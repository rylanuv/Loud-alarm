package com.loud.alarm.ui.challenge

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.random.Random

private const val DEFAULT_TAP_COUNT = 30
private const val MIN_TARGET_JUMP_DISTANCE = 0.58f

@Composable
fun TapChallengeScreen(
    targetTaps: Int = DEFAULT_TAP_COUNT,
    onSuccess: () -> Unit
) {
    val requiredTaps = targetTaps.coerceAtLeast(1)
    var tapsDone by rememberSaveable { mutableIntStateOf(0) }
    var targetX by rememberSaveable { mutableStateOf(randomTargetFraction()) }
    var targetY by rememberSaveable { mutableStateOf(randomTargetFraction()) }

    val progress by animateFloatAsState(
        targetValue = tapsDone.toFloat() / requiredTaps.toFloat(),
        animationSpec = tween(durationMillis = 180),
        label = "tapProgress"
    )

    fun moveTarget() {
        val nextTarget = randomFarTargetFraction(targetX, targetY)
        targetX = nextTarget.first
        targetY = nextTarget.second
    }

    fun registerTap() {
        val nextCount = tapsDone + 1
        tapsDone = nextCount
        if (nextCount >= requiredTaps) {
            onSuccess()
        } else {
            moveTarget()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val targetSize = 62.dp
        val availableWidth = (maxWidth - targetSize).coerceAtLeast(0.dp)
        val availableHeight = (maxHeight - targetSize).coerceAtLeast(0.dp)
        val targetOffsetX by animateDpAsState(
            targetValue = (availableWidth.value * targetX).dp,
            animationSpec = tween(durationMillis = 70),
            label = "tapTargetX"
        )
        val targetOffsetY by animateDpAsState(
            targetValue = (availableHeight.value * targetY).dp,
            animationSpec = tween(durationMillis = 70),
            label = "tapTargetY"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.035f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
        ) {
            Box(
                modifier = Modifier
                    .offset(x = targetOffsetX, y = targetOffsetY)
                    .size(targetSize)
                    .shadow(14.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                    .clickable { registerTap() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Tap target",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tap Challenge",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The target jumps far after every tap",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$tapsDone / $requiredTaps taps",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
    }
}

private fun randomTargetFraction(): Float = Random.nextFloat()

private fun randomFarTargetFraction(previousX: Float, previousY: Float): Pair<Float, Float> {
    var bestX = randomTargetFraction()
    var bestY = randomTargetFraction()
    var bestDistance = distance(previousX, previousY, bestX, bestY)

    repeat(24) {
        val candidateX = randomTargetFraction()
        val candidateY = randomTargetFraction()
        val candidateDistance = distance(previousX, previousY, candidateX, candidateY)
        if (candidateDistance >= MIN_TARGET_JUMP_DISTANCE) {
            return candidateX to candidateY
        }
        if (candidateDistance > bestDistance) {
            bestX = candidateX
            bestY = candidateY
            bestDistance = candidateDistance
        }
    }

    return bestX to bestY
}

private fun distance(fromX: Float, fromY: Float, toX: Float, toY: Float): Float {
    val deltaX = toX - fromX
    val deltaY = toY - fromY
    return sqrt((deltaX * deltaX) + (deltaY * deltaY))
}

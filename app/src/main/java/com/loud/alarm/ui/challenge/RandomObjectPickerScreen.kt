package com.loud.alarm.ui.challenge

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loud.alarm.ui.theme.*
import kotlinx.coroutines.delay

data class ScanObject(
    val icon: ImageVector,
    val label: String,
    val color: Color
)

val allScanObjects = listOf(
    ScanObject(Icons.Default.Brush, "Toothbrush", IconTeal),
    ScanObject(Icons.Default.Wash, "Sink", IconBlue),
    ScanObject(Icons.Default.Coffee, "Coffee cup", IconOrange),
    ScanObject(Icons.Default.LocalDining, "Bowl", IconRed),
    ScanObject(Icons.Default.SportsMma, "Shoe", IconGreen),
    ScanObject(Icons.AutoMirrored.Filled.MenuBook, "Book", IconPurple),
    ScanObject(Icons.Default.Yard, "Plant", IconLime),
    ScanObject(Icons.Default.Laptop, "Laptop", Color.LightGray),
    ScanObject(Icons.Default.Fastfood, "Fruit", IconYellow),
    ScanObject(Icons.Default.LocalBar, "Bottle", IconCyan),
    ScanObject(Icons.Default.Watch, "Watch", IconIndigo),
    ScanObject(Icons.Default.VpnKey, "Key", IconAmber),
    ScanObject(Icons.Default.Backpack, "Backpack", IconPink),
    ScanObject(Icons.Default.Chair, "Chair", SecondaryOrange),
    ScanObject(Icons.Default.DoorFront, "Door", PrimaryAccent),
    ScanObject(Icons.Default.Tv, "Television", IconBlue),
    ScanObject(Icons.Default.Computer, "Monitor", IconTeal),
    ScanObject(Icons.Default.Mouse, "Mouse", IconPink),
    ScanObject(Icons.Default.Keyboard, "Keyboard", IconOrange),
    ScanObject(Icons.Default.ContentCut, "Scissors", IconRed),
    ScanObject(Icons.Default.Smartphone, "Phone", IconGreen),
    ScanObject(Icons.Default.Umbrella, "Umbrella", IconPurple),
    ScanObject(Icons.Default.Calculate, "Calculator", IconLime),
    ScanObject(Icons.Default.AccountBalanceWallet, "Wallet", IconCyan),
    ScanObject(Icons.Default.Kitchen, "Refrigerator", PrimaryAccent),
    ScanObject(Icons.Default.Bed, "Bed", IconAmber),
    ScanObject(Icons.Default.DirectionsBike, "Bicycle", SecondaryOrange),
    ScanObject(Icons.Default.Wc, "Toilet", IconIndigo),
    ScanObject(Icons.Default.AccessTime, "Clock", IconYellow),
    ScanObject(Icons.Default.Headphones, "Headphones", Color.LightGray)
)

/**
 * Slot-machine style roulette animation that cycles through objects
 * and lands on the final random pick. Calls [onObjectPicked] when done.
 */
@Composable
fun RandomObjectPickerScreen(
    excludedLabels: Set<String>,
    onObjectPicked: (String) -> Unit
) {
    val available = remember(excludedLabels) {
        allScanObjects.filter { it.label !in excludedLabels }.ifEmpty { allScanObjects }
    }

    val finalPick = remember { available.random() }

    // Build the roulette sequence: ~20 fast items then slow decel into final
    val rouletteSequence = remember {
        val seq = mutableListOf<ScanObject>()
        // Fast shuffle phase: 20 random items
        repeat(20) { seq.add(available.random()) }
        // Slow down phase: 8 items with the final one at the end
        repeat(7) { seq.add(available.random()) }
        seq.add(finalPick)
        seq
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    var showGlow by remember { mutableStateOf(false) }

    // Animate through the sequence with increasing delays
    LaunchedEffect(Unit) {
        for (i in rouletteSequence.indices) {
            currentIndex = i
            // Start fast (50ms), decelerate toward end
            val progress = i.toFloat() / rouletteSequence.size
            val delayMs = when {
                progress < 0.6f -> 60L   // Fast phase
                progress < 0.75f -> 120L  // Medium
                progress < 0.85f -> 200L  // Slowing
                progress < 0.93f -> 350L  // Nearly there
                else -> 500L              // Final stops
            }
            delay(delayMs)
        }
        // Done!
        isFinished = true
        delay(200)
        showGlow = true
        delay(1200) // Let user see the result
        onObjectPicked(finalPick.label)
    }

    val currentObj = rouletteSequence[currentIndex]

    // Pulse animation when finished
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Bounce effect when item changes
    val bounceScale by animateFloatAsState(
        targetValue = if (isFinished && showGlow) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Title
            Text(
                text = if (isFinished) "YOUR OBJECT IS..." else "PICKING OBJECT...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = if (isFinished) "Find this to dismiss your alarm!"
                       else "Stand by...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Outer glow ring (visible when finished)
            Box(contentAlignment = Alignment.Center) {
                if (showGlow) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(glowScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        currentObj.color.copy(alpha = glowAlpha * 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Main icon circle
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(bounceScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    currentObj.color.copy(alpha = 0.3f),
                                    currentObj.color.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            width = 3.dp,
                            color = currentObj.color.copy(alpha = if (showGlow) 0.9f else 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentObj.icon,
                        contentDescription = currentObj.label,
                        tint = currentObj.color,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Object name
            Text(
                text = currentObj.label,
                style = MaterialTheme.typography.headlineMedium,
                color = if (showGlow) currentObj.color else Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (!isFinished) {
                Spacer(modifier = Modifier.height(24.dp))
                // Spinning dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { dotIndex ->
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, delayMillis = dotIndex * 150),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot$dotIndex"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(dotAlpha)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

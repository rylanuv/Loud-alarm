package com.loud.alarm.ui.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.MathDifficulty
import com.loud.alarm.data.SquatDetectionMode
import com.loud.alarm.ui.theme.*

// ──────────────────────────────────────────────────────────────
// Reusable wrapper for all challenge settings dialogs
// ──────────────────────────────────────────────────────────────
@Composable
fun ChallengeSettingsDialog(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onDismiss: () -> Unit,
    onPreviewClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1E2E),
                            Color(0xFF16162A)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            iconColor.copy(alpha = 0.35f),
                                            iconColor.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(1.dp, iconColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                content()

                Spacer(modifier = Modifier.height(16.dp))

                if (onPreviewClick != null) {
                    OutlinedButton(
                        onClick = {
                            onPreviewClick()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview Alarm", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Done button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Difficulty selector used by Math, Maze, Puzzle, Memory
// ──────────────────────────────────────────────────────────────
@Composable
fun DifficultySelector(
    label: String,
    selected: MathDifficulty,
    onSelect: (MathDifficulty) -> Unit,
    descriptions: Map<MathDifficulty, String>,
    examples: Map<MathDifficulty, String>? = null
) {
    Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MathDifficulty.values().forEach { diff ->
            val isSelected = diff == selected
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f), label = "bg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.1f), label = "border"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(diff) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            diff.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            letterSpacing = 1.sp
                        )
                        descriptions[diff]?.let { desc ->
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // Example box for selected
                if (isSelected && examples != null && examples.containsKey(diff)) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Show example below the selected card
            AnimatedVisibility(
                visible = isSelected && examples != null && examples.containsKey(diff),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                examples?.get(diff)?.let { ex ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            ex,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Count/option chip row, used by many challenges
// ──────────────────────────────────────────────────────────────
@Composable
fun CountChipRow(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    suffix: String = ""
) {
    Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { count ->
            val isSelected = selected == count
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(count) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.1f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count $suffix",
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Challenge Preview Dialog (for locked premium challenges)
// ──────────────────────────────────────────────────────────────
@Composable
fun ChallengePreviewDialog(
    challengeName: String,
    icon: ImageVector,
    iconColor: Color,
    description: String,
    features: List<String>,
    isLocked: Boolean = true,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1E2E),
                            Color(0xFF16162A)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                // Big icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape, ambientColor = iconColor.copy(0.5f), spotColor = iconColor.copy(0.5f))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(iconColor.copy(0.4f), iconColor.copy(0.1f))
                            )
                        )
                        .border(2.dp, iconColor.copy(0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    challengeName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Features list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                feature,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLocked) {
                    // Unlock button
                    Button(
                        onClick = {
                            onDismiss()
                            onSubscribe()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Unlock with Subscription",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    // Close button for unlocked challenges
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Got it", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Challenge preview descriptions
// ──────────────────────────────────────────────────────────────
fun getChallengePreviewInfo(type: ChallengeType): Pair<String, List<String>> {
    return when (type) {
        ChallengeType.STEP -> "Walk a set number of steps to dismiss your alarm. Gets you moving!" to listOf(
            "Customizable step targets (10-100)",
            "Uses phone's built-in pedometer",
            "Forces you out of bed"
        )
        ChallengeType.MAZE -> "Navigate through a tricky maze to wake up. Dead ends included!" to listOf(
            "4 difficulty levels",
            "Real dead ends to challenge you",
            "Reset button if you get stuck"
        )
        ChallengeType.MEMORY -> "Remember and repeat a tile sequence on a grid." to listOf(
            "3×3 to 4×4 grids",
            "Sequences from 3 to 6 tiles",
            "Multiple rounds for extra challenge"
        )
        ChallengeType.SHAKE -> "Shake your phone vigorously to dismiss the alarm!" to listOf(
            "Adjustable shake count (15-100)",
            "Uses accelerometer sensor",
            "Quick and physical wake-up"
        )
        ChallengeType.TAP_CHALLENGE -> "Tap a target that jumps to a random spot after every tap." to listOf(
            "Adjustable tap target (10-100)",
            "Random target position after each tap",
            "No extra permissions needed"
        )
        ChallengeType.SPELL_BEE -> "Spell words correctly to dismiss your alarm." to listOf(
            "4 difficulty levels",
            "Tests your spelling skills",
            "Engaging brain exercise"
        )
        ChallengeType.PUZZLE -> "Solve a sliding tile puzzle to wake up." to listOf(
            "2×2 to 5×5 grid sizes",
            "Classic 15-puzzle mechanic",
            "4 difficulty levels"
        )
        ChallengeType.SCAN_SINK -> "Scan your bathroom sink to prove you're up!" to listOf(
            "AI-powered sink detection",
            "Any sink or specific sink modes",
            "Forces you to the bathroom"
        )
        ChallengeType.SCAN_OBJECT -> "Find and scan a specific object around your home." to listOf(
            "30 different objects to scan",
            "Random or pick-one modes",
            "AI-powered object recognition"
        )
        ChallengeType.SQUAT -> "Do squats tracked by camera or motion sensor." to listOf(
            "Camera-based pose tracking",
            "Motion sensor alternative",
            "Adjustable squat targets (5-100)"
        )
        ChallengeType.PUSH_UP -> "Do push-ups tracked by your front camera." to listOf(
            "AI pose detection",
            "Easy & Hard difficulty modes",
            "Adjustable targets (5-100)"
        )
        ChallengeType.REVERSE_TYPING -> "Type displayed words in reverse order." to listOf(
            "Multiple rounds available",
            "Tests focus and attention",
            "Adjustable round count"
        )
        ChallengeType.AUDIO_MEMORY -> "Listen to and replay audio sequences." to listOf(
            "4 difficulty levels",
            "Tests auditory memory",
            "Engaging and fun"
        )
        ChallengeType.MATH -> "Solve math problems to dismiss your alarm." to listOf(
            "4 difficulty levels (Easy to Extreme)",
            "Addition, multiplication, algebra",
            "Configurable question count"
        )
        ChallengeType.ADVANCED_MATH -> "Solve advanced math problems to dismiss." to listOf(
            "Polynomials, Matrix, Calculus...",
            "Decimals and negative numbers",
            "Configurable question count"
        )
        ChallengeType.QR_CODE -> "Scan a QR or barcode to dismiss." to listOf(
            "Scan any QR code or a specific one",
            "Forces you to get up and find it",
            "Great for placing QR in another room"
        )
        ChallengeType.REWRITE -> "Type out a phrase to prove you're awake." to listOf(
            "Custom or random text",
            "Tests motor skills and focus",
            "Quick and effective"
        )
        ChallengeType.CHARGER -> "Plug your phone into a charger to dismiss." to listOf(
            "Forces you to get up and find a charger",
            "Must unplug and re-plug if already charging",
            "No extra permissions needed"
        )
        ChallengeType.ROOM_LIGHT -> "Turn on the room light to dismiss." to listOf(
            "Uses the ambient light sensor",
            "Forces you to get out of bed",
            "Works best in a dark room"
        )
        ChallengeType.CLOCK_READING -> "Read an analogue clock and type the time to dismiss." to listOf(
            "Beautiful canvas-drawn clock face",
            "4 difficulty levels",
            "Configurable round count",
            "Trains your analogue time reading"
        )
        ChallengeType.NONE -> "No challenge — alarm dismisses normally." to listOf(
            "Simple swipe to dismiss",
            "No extra effort required"
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Get short summary for selected challenge settings
// ──────────────────────────────────────────────────────────────
fun getChallengeSettingsSummary(type: ChallengeType, uiState: AlarmUiState): String? {
    return when (type) {
        ChallengeType.MATH -> {
            val d = uiState.mathDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            val q = uiState.mathQuestionCount
            "$d · ${q}Q"
        }
        ChallengeType.ADVANCED_MATH -> {
            val d = uiState.advancedMathDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            val q = uiState.advancedMathQuestionCount
            val t = uiState.advancedMathTopics.size
            "$d · $t topics · ${q}Q"
        }
        ChallengeType.MAZE -> uiState.mazeDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
        ChallengeType.PUZZLE -> uiState.puzzleDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
        ChallengeType.MEMORY -> {
            val d = uiState.memoryDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            val r = uiState.memoryChallengeCount
            "$d · ${r}R"
        }
        ChallengeType.QR_CODE -> if (uiState.barcodeValue == null) "Any" else "Specific"
        ChallengeType.REWRITE -> if (uiState.rewriteText.isBlank()) "Random" else "Custom"
        ChallengeType.STEP -> "${uiState.stepCount} steps"
        ChallengeType.SHAKE -> "${uiState.shakeCount} shakes"
        ChallengeType.TAP_CHALLENGE -> "${uiState.tapCount} taps"
        ChallengeType.SQUAT -> "${uiState.squatCount} squats"
        ChallengeType.PUSH_UP -> "${uiState.pushUpCount} push-ups"
        ChallengeType.REVERSE_TYPING -> "${uiState.reverseTypingCount} rounds"
        ChallengeType.SCAN_SINK -> if (uiState.sinkImageUri.isNullOrBlank()) "Any sink" else "Specific"
        ChallengeType.SCAN_OBJECT -> if (uiState.scanObjectLabel == "RANDOM") "Random" else uiState.scanObjectLabel
        ChallengeType.SPELL_BEE -> uiState.spellBeeDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
        ChallengeType.AUDIO_MEMORY -> {
            val d = uiState.audioMemoryDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            val r = uiState.audioMemoryChallengeCount
            "$d · ${r}R"
        }
        ChallengeType.CHARGER -> null
        ChallengeType.ROOM_LIGHT -> "${uiState.roomLightTargetLux} lux"
        ChallengeType.CLOCK_READING -> {
            val d = uiState.clockReadingDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            val r = uiState.clockReadingCount
            "$d · ${r}R"
        }
        else -> null
    }
}

// ──────────────────────────────────────────────────────────────
// Short subtitle for compact challenge tiles
// ──────────────────────────────────────────────────────────────
fun getChallengeTileSubtitle(type: ChallengeType): String {
    return when (type) {
        ChallengeType.NONE -> "Dismiss normally"
        ChallengeType.MATH -> "Solve sums"
        ChallengeType.ADVANCED_MATH -> "For Maths Lovers"
        ChallengeType.QR_CODE -> "Scan code"
        ChallengeType.REWRITE -> "Type phrase"
        ChallengeType.TAP_CHALLENGE -> "Tap target"
        ChallengeType.STEP -> "Walk steps"
        ChallengeType.MAZE -> "Find exit"
        ChallengeType.MEMORY -> "Repeat tiles"
        ChallengeType.SHAKE -> "Shake phone"
        ChallengeType.SPELL_BEE -> "Spell words"
        ChallengeType.PUZZLE -> "Slide tiles"
        ChallengeType.SCAN_SINK -> "Use camera"
        ChallengeType.SCAN_OBJECT -> "Find object"
        ChallengeType.SQUAT -> "Do squats"
        ChallengeType.PUSH_UP -> "Do push-ups"
        ChallengeType.REVERSE_TYPING -> "Reverse text"
        ChallengeType.AUDIO_MEMORY -> "Replay sounds"
        ChallengeType.CHARGER -> "Plug in"
        ChallengeType.ROOM_LIGHT -> "Turn on Lights"
        ChallengeType.CLOCK_READING -> "Read time"
    }
}

// Check if a challenge type has configurable settings
fun challengeHasSettings(type: ChallengeType): Boolean {
    return type != ChallengeType.NONE
}

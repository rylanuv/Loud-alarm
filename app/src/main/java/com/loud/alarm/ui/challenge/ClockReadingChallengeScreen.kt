package com.loud.alarm.ui.challenge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loud.alarm.data.MathDifficulty
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ──────────────────────────────────────────────────────────────
// Data class for a generated clock time
// ──────────────────────────────────────────────────────────────
private data class ClockTime(val hour: Int, val minute: Int) {
    /** Display string like "3:05" or "12:45" */
    val displayString: String
        get() = "$hour:${minute.toString().padStart(2, '0')}"
}

// ──────────────────────────────────────────────────────────────
// Random clock time generator based on difficulty
// ──────────────────────────────────────────────────────────────
private object ClockTimeGenerator {
    fun generate(difficulty: MathDifficulty): ClockTime {
        val hour = (1..12).random()
        val minute = when (difficulty) {
            MathDifficulty.EASY -> {
                // 5-minute intervals: 0, 5, 10, ..., 55
                listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).random()
            }
            MathDifficulty.MEDIUM -> {
                // Any minute value
                (0..59).random()
            }
            MathDifficulty.HARD -> {
                // Tricky positions: hands close together or near overlapping
                // Minutes where the minute hand is near the hour hand position
                val trickyMinutes = listOf(
                    // Near the hour mark (hands close)
                    hour * 5 - 2, hour * 5 - 1, hour * 5, hour * 5 + 1, hour * 5 + 2,
                    // Near the opposite side (less tricky but still challenging)
                    ((hour * 5 + 30) % 60) - 1, ((hour * 5 + 30) % 60), ((hour * 5 + 30) % 60) + 1
                ).map { (it + 60) % 60 }.distinct()
                trickyMinutes.random()
            }
            MathDifficulty.EXTREME -> {
                // Any minute, but these will require multiple rounds
                (0..59).random()
            }
        }
        return ClockTime(hour, minute)
    }
}

// ──────────────────────────────────────────────────────────────
// Main Challenge Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun ClockReadingChallengeScreen(
    difficulty: MathDifficulty,
    questionCount: Int = 1,
    onSuccess: () -> Unit
) {
    var currentQuestion by remember { mutableStateOf(1) }
    var clockTime by remember { mutableStateOf(ClockTimeGenerator.generate(difficulty)) }
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    // Auto-advance to next question after correct flash
    LaunchedEffect(isCorrect) {
        if (isCorrect) {
            kotlinx.coroutines.delay(500)
            if (currentQuestion >= questionCount) {
                onSuccess()
            } else {
                currentQuestion++
                clockTime = ClockTimeGenerator.generate(difficulty)
                input = ""
                isCorrect = false
            }
        }
    }

    // Provide feedback via color
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCorrect -> Color(0xFF1B5E20).copy(alpha = 0.3f)
            isError -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.background
        },
        animationSpec = tween(300),
        label = "bgColor"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: progress + instruction
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))

                // Progress indicator
                if (questionCount > 1) {
                    Text(
                        text = "Round $currentQuestion / $questionCount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(currentQuestion.toFloat() / questionCount.toFloat())
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "What time is it?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Middle: Clock face
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                AnalogueClockFace(
                    hour = clockTime.hour,
                    minute = clockTime.minute
                )
            }

            // Input display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (input.isEmpty()) "H:MM" else formatClockInput(input),
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (input.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 3.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Keypad
            ClockKeypad(
                onNumberClick = { num ->
                    // Max 4 digits (H:MM or HH:MM → we store raw digits, max 4)
                    if (input.length < 4 && !isCorrect) {
                        input += num
                        isError = false
                    }
                },
                onDeleteClick = {
                    if (input.isNotEmpty() && !isCorrect) {
                        input = input.dropLast(1)
                        isError = false
                    }
                },
                onEnterClick = {
                    if (!isCorrect && input.isNotEmpty()) {
                        val parsed = parseClockInput(input)
                        if (parsed != null && parsed.first == clockTime.hour && parsed.second == clockTime.minute) {
                            isCorrect = true
                        } else {
                            isError = true
                            input = ""
                        }
                    }
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Format raw digit input as clock display (e.g., "345" → "3:45")
// ──────────────────────────────────────────────────────────────
private fun formatClockInput(raw: String): String {
    return when (raw.length) {
        1 -> raw                      // "3"
        2 -> raw                      // "34"
        3 -> "${raw[0]}:${raw.substring(1)}"  // "3:45"
        4 -> "${raw.substring(0, 2)}:${raw.substring(2)}" // "12:45"
        else -> raw
    }
}

// ──────────────────────────────────────────────────────────────
// Parse raw digit input to (hour, minute)
// ──────────────────────────────────────────────────────────────
private fun parseClockInput(raw: String): Pair<Int, Int>? {
    return when (raw.length) {
        3 -> {
            val h = raw[0].digitToIntOrNull() ?: return null
            val m = raw.substring(1).toIntOrNull() ?: return null
            if (h in 1..12 && m in 0..59) Pair(h, m) else null
        }
        4 -> {
            val h = raw.substring(0, 2).toIntOrNull() ?: return null
            val m = raw.substring(2).toIntOrNull() ?: return null
            if (h in 1..12 && m in 0..59) Pair(h, m) else null
        }
        else -> null
    }
}

// ──────────────────────────────────────────────────────────────
// Analogue Clock Face — Canvas-drawn
// ──────────────────────────────────────────────────────────────
@Composable
private fun AnalogueClockFace(
    hour: Int,
    minute: Int
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.88f

        // ─── Outer glow ring ───
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.12f),
                    primaryColor.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = radius * 1.2f
            ),
            radius = radius * 1.2f,
            center = Offset(centerX, centerY)
        )

        // ─── Clock face background ───
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A1A2E),
                    Color(0xFF0F0F1A)
                ),
                center = Offset(centerX, centerY),
                radius = radius
            ),
            radius = radius,
            center = Offset(centerX, centerY)
        )

        // ─── Outer ring border ───
        drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3f)
        )

        // ─── Minute tick marks ───
        for (i in 0 until 60) {
            val angle = (i * 6.0 - 90) * PI / 180
            val isHourMark = i % 5 == 0
            val outerRadius = radius * 0.92f
            val innerRadius = if (isHourMark) radius * 0.82f else radius * 0.87f
            val tickWidth = if (isHourMark) 2.5f else 1f
            val tickColor = if (isHourMark)
                primaryColor.copy(alpha = 0.8f)
            else
                onSurfaceColor.copy(alpha = 0.2f)

            drawLine(
                color = tickColor,
                start = Offset(
                    centerX + (innerRadius * cos(angle)).toFloat(),
                    centerY + (innerRadius * sin(angle)).toFloat()
                ),
                end = Offset(
                    centerX + (outerRadius * cos(angle)).toFloat(),
                    centerY + (outerRadius * sin(angle)).toFloat()
                ),
                strokeWidth = tickWidth,
                cap = StrokeCap.Round
            )
        }

        // ─── Hour numerals ───
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = radius * 0.18f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }

        for (i in 1..12) {
            val angle = (i * 30.0 - 90) * PI / 180
            val textRadius = radius * 0.72f
            val x = centerX + (textRadius * cos(angle)).toFloat()
            val y = centerY + (textRadius * sin(angle)).toFloat()

            // Adjust vertical centering
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(i.toString(), 0, i.toString().length, textBounds)
            val textHeight = textBounds.height()

            drawContext.canvas.nativeCanvas.drawText(
                i.toString(),
                x,
                y + textHeight / 2f,
                textPaint
            )
        }

        // ─── Hour hand ───
        val hourAngle = ((hour % 12 + minute / 60f) * 30f - 90f) * PI.toFloat() / 180f
        val hourHandLength = radius * 0.48f
        val hourHandWidth = 7f

        // Hand shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = Offset(centerX + 2f, centerY + 2f),
            end = Offset(
                centerX + 2f + hourHandLength * cos(hourAngle),
                centerY + 2f + hourHandLength * sin(hourAngle)
            ),
            strokeWidth = hourHandWidth + 2f,
            cap = StrokeCap.Round
        )
        // Actual hour hand
        drawLine(
            color = Color.White,
            start = Offset(centerX, centerY),
            end = Offset(
                centerX + hourHandLength * cos(hourAngle),
                centerY + hourHandLength * sin(hourAngle)
            ),
            strokeWidth = hourHandWidth,
            cap = StrokeCap.Round
        )

        // ─── Minute hand ───
        val minuteAngle = (minute * 6f - 90f) * PI.toFloat() / 180f
        val minuteHandLength = radius * 0.72f
        val minuteHandWidth = 4f

        // Hand shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = Offset(centerX + 2f, centerY + 2f),
            end = Offset(
                centerX + 2f + minuteHandLength * cos(minuteAngle),
                centerY + 2f + minuteHandLength * sin(minuteAngle)
            ),
            strokeWidth = minuteHandWidth + 2f,
            cap = StrokeCap.Round
        )
        // Actual minute hand
        drawLine(
            color = primaryColor,
            start = Offset(centerX, centerY),
            end = Offset(
                centerX + minuteHandLength * cos(minuteAngle),
                centerY + minuteHandLength * sin(minuteAngle)
            ),
            strokeWidth = minuteHandWidth,
            cap = StrokeCap.Round
        )

        // ─── Center dot ───
        drawCircle(
            color = primaryColor,
            radius = 8f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(centerX, centerY)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Clock Keypad (numbers 0-9, DEL, OK)
// ──────────────────────────────────────────────────────────────
@Composable
private fun ClockKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("DEL", "0", "OK")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    ClockKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                "DEL" -> onDeleteClick()
                                "OK" -> onEnterClick()
                                else -> onNumberClick(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isAction = label == "DEL" || label == "OK"
    val containerColor = if (isAction)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface
    val contentColor = if (isAction)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == "DEL") {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Delete",
                tint = contentColor
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

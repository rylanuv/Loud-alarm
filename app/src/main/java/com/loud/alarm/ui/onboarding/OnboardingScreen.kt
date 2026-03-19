package com.loud.alarm.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loud.alarm.R
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.ui.editor.RepeatPickerDialog
import com.loud.alarm.ui.editor.WheelTimePicker
import java.util.Locale

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    var showRepeatDialog by remember { mutableStateOf(false) }

    val totalPages = 5
    val repeatSummary = remember(uiState.daysOfWeek) { getRepeatSummary(uiState.daysOfWeek) }
    val challengeLabel = if (uiState.challengeTypes.contains(ChallengeType.MATH)) "Maths" else "None"

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.onboarding),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.74f),
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            OnboardingProgress(
                step = pageIndex + 1,
                total = totalPages
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (pageIndex) {
                    0 -> IntroTimelinePage()
                    1 -> WelcomePage()
                    2 -> WakeTimePage(
                        hour = uiState.hour,
                        minute = uiState.minute,
                        repeatSummary = repeatSummary,
                        onTimeChanged = viewModel::updateTime,
                        onPickDates = { showRepeatDialog = true }
                    )
                    3 -> ChallengePage(
                        selected = uiState.challengeTypes,
                        onSelect = viewModel::selectFreeChallenge
                    )
                    else -> ReadyPage(
                        hour = uiState.hour,
                        minute = uiState.minute,
                        repeatSummary = repeatSummary,
                        challengeLabel = challengeLabel
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pageIndex > 0) {
                    OutlinedButton(
                        onClick = { pageIndex -= 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.35f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Back")
                    }
                }

                val actionLabel = if (pageIndex == totalPages - 1) {
                    if (isSaving) "Creating..." else "Start Using App"
                } else {
                    "Continue"
                }

                Button(
                    onClick = {
                        if (pageIndex == totalPages - 1) {
                            viewModel.completeOnboarding(onFinished)
                        } else {
                            pageIndex += 1
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showRepeatDialog) {
        RepeatPickerDialog(
            selectedDays = uiState.daysOfWeek,
            onDaysChanged = { viewModel.setDays(it) },
            onDismiss = { showRepeatDialog = false }
        )
    }
}

@Composable
private fun OnboardingProgress(step: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Setup $step/$total",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index < step) 18.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (index < step) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun IntroTimelinePage() {
    Column {
        Text(
            text = "Wake up on your first ring",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "A smarter routine that gets you up without endless snoozes.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.height(18.dp))

        MorningComparisonCard()
    }
}

@Composable
private fun WelcomePage() {
    Column {
        Text(
            text = "Welcome to Loud Alarm",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Let's set up your first wake-up plan in less than a minute.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.84f)
        )
        Spacer(modifier = Modifier.height(22.dp))
        OnboardingGlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "What we will set now",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                WelcomePoint(text = "Your wake-up time")
                WelcomePoint(text = "Repeat dates (same picker as edit alarm)")
                WelcomePoint(text = "Free wake-up challenges")
                WelcomePoint(text = "Done. Alarm gets created automatically")
            }
        }
    }
}

@Composable
private fun WakeTimePage(
    hour: Int,
    minute: Int,
    repeatSummary: String,
    onTimeChanged: (Int, Int) -> Unit,
    onPickDates: () -> Unit
) {
    Column {
        Text(
            text = "What time do you want to wake up?",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))
        OnboardingGlassCard {
            Column(modifier = Modifier.padding(14.dp)) {
                WheelTimePicker(hour = hour, minute = minute, onTimeChanged = onTimeChanged)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingGlassCard(
            modifier = Modifier.clickable(onClick = onPickDates)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dates",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = repeatSummary,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Edit",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ChallengePage(
    selected: Set<ChallengeType>,
    onSelect: (ChallengeType) -> Unit
) {
    Column {
        Text(
            text = "Pick your challenge",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Free challenges",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.height(14.dp))

        FreeChallengeItem(
            label = "Maths",
            description = "Solve quick math before dismissing alarm",
            icon = {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White)
            },
            selected = selected.contains(ChallengeType.MATH),
            onClick = { onSelect(ChallengeType.MATH) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        FreeChallengeItem(
            label = "None",
            description = "No challenge, stop alarm normally",
            icon = {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color.White)
            },
            selected = selected.contains(ChallengeType.NONE),
            onClick = { onSelect(ChallengeType.NONE) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You can unlock and select more challenges inside the app anytime.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.74f)
        )
    }
}

@Composable
private fun ReadyPage(
    hour: Int,
    minute: Int,
    repeatSummary: String,
    challengeLabel: String
) {
    Column {
        Text(
            text = "You're ready to go",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We'll create your first alarm with these settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.height(18.dp))

        OnboardingGlassCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryRow("Wake time", formatTime12h(hour, minute))
                SummaryRow("Dates", repeatSummary)
                SummaryRow("Challenge", challengeLabel)
            }
        }
    }
}

@Composable
private fun OnboardingGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
private fun WelcomePoint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun FreeChallengeItem(
    label: String,
    description: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    OnboardingGlassCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        else Color.White.copy(alpha = 0.1f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.22f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MorningComparisonCard() {
    OnboardingGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Typical Morning",
                    color = Color(0xFFFF8A6C),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Solve2Wake Morning",
                    color = Color(0xFF4CD9A1),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
            ) {
                MorningTrack(
                    modifier = Modifier.weight(1f),
                    points = listOf(
                        MorningPoint(0.34f, 0.06f, "7:00", "ALARM", Icons.Default.Alarm, Color(0xFFF3B24A)),
                        MorningPoint(0.57f, 0.36f, "7:08", "SNOOZE", Icons.Default.Snooze, Color(0xFFFFA970)),
                        MorningPoint(0.38f, 0.66f, "7:16", "SNOOZE", Icons.Default.Snooze, Color(0xFFF27DA0)),
                        MorningPoint(0.26f, 0.9f, "7:24", "PANIC", Icons.Default.Warning, Color(0xFFFF6F85))
                    ),
                    segmentColors = listOf(
                        Color(0xFFF3B24A),
                        Color(0xFFFF9B6E),
                        Color(0xFFF07197)
                    ),
                    showGainBadge = false
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.18f))
                )

                MorningTrack(
                    modifier = Modifier.weight(1f),
                    points = listOf(
                        MorningPoint(0.28f, 0.06f, "7:00", "ALARM", Icons.Default.Alarm, Color(0xFF4CD9A1)),
                        MorningPoint(0.28f, 0.34f, "7:01", "MISSION", Icons.Default.Check, Color(0xFF4CD9A1)),
                        MorningPoint(0.28f, 0.62f, "7:03", "STARTED", Icons.Default.Flag, Color(0xFF4CD9A1))
                    ),
                    segmentColors = listOf(Color(0xFF37D198), Color(0xFF37D198)),
                    showGainBadge = true
                )
            }
        }
    }
}

@Composable
private fun MorningTrack(
    modifier: Modifier,
    points: List<MorningPoint>,
    segmentColors: List<Color>,
    showGainBadge: Boolean
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    for (i in 0 until (points.size - 1)) {
                        val start = points[i]
                        val end = points[i + 1]
                        drawLine(
                            color = segmentColors.getOrElse(i) { Color.White },
                            start = androidx.compose.ui.geometry.Offset(
                                x = size.width * start.xFraction,
                                y = size.height * start.yFraction
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                x = size.width * end.xFraction,
                                y = size.height * end.yFraction
                            ),
                            strokeWidth = 9f,
                            cap = StrokeCap.Round
                        )
                    }
                }
        )

        points.forEach { point ->
            val iconSize = 42.dp
            val iconX = width * point.xFraction - (iconSize / 2)
            val iconY = height * point.yFraction - (iconSize / 2)
            val textX = iconX + 52.dp
            val textY = iconY + 2.dp

            Box(
                modifier = Modifier
                    .offset(x = iconX, y = iconY)
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Color(0xFF141A24))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = point.icon,
                    contentDescription = null,
                    tint = point.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.offset(x = textX, y = textY)
            ) {
                Text(
                    text = point.time,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = point.label,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (showGainBadge) {
            Badge(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E6A50))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                containerColor = Color.Transparent,
                contentColor = Color(0xFF4CD9A1)
            ) {
                Text(
                    text = "25 MIN GAINED",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class MorningPoint(
    val xFraction: Float,
    val yFraction: Float,
    val time: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color
)

private fun getRepeatSummary(days: Set<Int>): String {
    if (days.isEmpty()) return "Once"
    if (days == setOf(1, 2, 3, 4, 5, 6, 7)) return "Every Day"
    if (days == setOf(2, 3, 4, 5, 6)) return "Weekdays"
    if (days == setOf(1, 7)) return "Weekends"

    val shortNames = mapOf(
        1 to "Sun", 2 to "Mon", 3 to "Tue",
        4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat"
    )
    val ordered = listOf(2, 3, 4, 5, 6, 7, 1).filter { it in days }
    return ordered.joinToString(", ") { shortNames[it] ?: "" }
}

private fun formatTime12h(hour: Int, minute: Int): String {
    val isAm = hour < 12
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val suffix = if (isAm) "AM" else "PM"
    return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, suffix)
}

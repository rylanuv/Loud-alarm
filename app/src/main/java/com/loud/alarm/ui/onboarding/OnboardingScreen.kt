package com.loud.alarm.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.loud.alarm.ui.permissions.PermissionSetupPage
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
    val challengeLabel = when {
        uiState.challengeTypes.contains(ChallengeType.MATH) -> "Maths"
        uiState.challengeTypes.contains(ChallengeType.QR_CODE) -> "QR Code"
        uiState.challengeTypes.contains(ChallengeType.REWRITE) -> "Rewrite"
        else -> "None"
    }

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
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.8f)
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
                    .then(
                        // PermissionSetupPage (page 3) has its own verticalScroll;
                        // adding one here too causes a nested-scroll crash.
                        if (pageIndex != 3) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    )
            ) {
                when (pageIndex) {
                    0 -> IntroTimelinePage()
                    1 -> WakeTimePage(
                        hour = uiState.hour,
                        minute = uiState.minute,
                        repeatSummary = repeatSummary,
                        onTimeChanged = viewModel::updateTime,
                        onPickDates = { showRepeatDialog = true }
                    )
                    2 -> ChallengePage(
                        selected = uiState.challengeTypes,
                        onSelect = viewModel::selectFreeChallenge
                    )
                    3 -> PermissionSetupPage(
                        title = "Enable required permissions",
                        description = "Tap each item once and make sure they all show On.",
                        isVisible = true
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
                    if (isSaving) "Creating..." else "Let's Go"
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
            text = "Step $step/$total",
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
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val cardOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 26f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "intro_card_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Text(
            text = "One alarm. Zero excuses.",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.graphicsLayer(alpha = alphaAnim),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Lock in your morning before your brain starts bargaining.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.84f),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = alphaAnim).padding(horizontal = 24.dp),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(20.dp))

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier.graphicsLayer(
                alpha = alphaAnim,
                translationY = cardOffset
            )
        ) {
            MorningMomentumCard()
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
    Column(modifier = Modifier.padding(top = 64.dp)) {
        Text(
            text = "What time do you want to wake up?",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        WheelTimePicker(hour = hour, minute = minute, onTimeChanged = onTimeChanged)

        Spacer(modifier = Modifier.height(32.dp))

        OnboardingGlassCard(
            modifier = Modifier.clickable(onClick = onPickDates)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Event",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Repeat",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = repeatSummary,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Text(
                    text = "Edit",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
    Column(modifier = Modifier.padding(top = 64.dp)) {
        Text(
            text = "Pick your challenge",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Challenges",
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
            label = "QR Code",
            description = "Scan a barcode to stop alarm",
            icon = {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
            },
            selected = selected.contains(ChallengeType.QR_CODE),
            onClick = { onSelect(ChallengeType.QR_CODE) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        FreeChallengeItem(
            label = "Rewrite",
            description = "Rewrite generated text exactly",
            icon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
            },
            selected = selected.contains(ChallengeType.REWRITE),
            onClick = { onSelect(ChallengeType.REWRITE) }
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
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "ready_alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "ready_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ready_pulse_transition")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ready_pulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = alphaAnim),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(scaleX = pulse, scaleY = pulse, alpha = 0.3f / pulse)
                    .clip(CircleShape)
                    .background(Color(0xFF4CD9A1))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4CD9A1),
                                Color(0xFF2EB77F)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp).graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "Your first alarm has been configured for success.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.76f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(42.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E2632).copy(alpha = 0.45f),
                                Color(0xFF0F151E).copy(alpha = 0.65f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                // A glowing orb behind the content
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CD9A1).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                radius = 700f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                    text = "WAKE UP AT",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF4CD9A1),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTime12h(hour, minute),
                    fontSize = 54.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ReadyPill(icon = Icons.Default.Alarm, label = repeatSummary)
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "You can add more challenges and enable Wake-Up Checks later in the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer(alpha = alphaAnim)
        )
    }
}

@Composable
private fun ReadyPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CD9A1),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
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
private fun MorningMomentumCard() {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "showcaseAlpha"
    )
    val wakeFlowProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.08f,
        animationSpec = tween(1100, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "wake_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = cardAlpha),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0B1118).copy(alpha = 0.88f),
                            Color(0xFF111C25).copy(alpha = 0.94f),
                            Color(0xFF0A1017).copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(30.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF65F0BE).copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(540f, 840f),
                            radius = 760f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFBF72).copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(120f, 180f),
                            radius = 520f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IntroModeChip(
                        label = "OLD PATTERN",
                        accent = Color(0xFFFFBF72)
                    )
                    IntroModeChip(
                        label = "SOLVE2WAKE",
                        accent = Color(0xFF65F0BE)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Easy alarms are easy to cheat. This path locks in your morning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MorningPathCard(
                        modifier = Modifier.weight(1f),
                        title = "Snooze spiral",
                        accent = Color(0xFFFFBF72),
                        steps = listOf("Alarm", "Snooze", "Late"),
                        stepIcons = listOf(
                            Icons.Default.Alarm,
                            Icons.Default.Bedtime,
                            Icons.Default.Warning
                        ),
                        progress = 1f,
                        isPositive = false
                    )
                    MorningPathCard(
                        modifier = Modifier.weight(1f),
                        title = "Wake flow",
                        accent = Color(0xFF65F0BE),
                        steps = listOf("Alarm", "Solve", "Up"),
                        stepIcons = listOf(
                            Icons.Default.Alarm,
                            Icons.Default.Calculate,
                            Icons.Default.Check
                        ),
                        progress = wakeFlowProgress,
                        isPositive = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MomentumMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = "Avg +24 min lost",
                        accent = Color(0xFFFFBF72)
                    )
                    MomentumMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Check,
                        label = "Start with a win",
                        accent = Color(0xFF65F0BE)
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroModeChip(
    label: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
private fun MorningPathCard(
    modifier: Modifier = Modifier,
    title: String,
    accent: Color,
    steps: List<String>,
    stepIcons: List<androidx.compose.ui.graphics.vector.ImageVector>,
    progress: Float,
    isPositive: Boolean
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
        ) {
            // Path Line
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .width(3.dp)
                    .height(130.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            // Progress Filler
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 35.dp)
                    .width(3.dp)
                    .height(130.dp * progress)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accent,
                                if (isPositive) Color(0xFF65F0BE) else Color(0xFFFF7C6D)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 15.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(3) { index ->
                    Row(
                        modifier = Modifier.graphicsLayer(
                           alpha = if (progress >= (index.toFloat() / 2f)) 1f else 0.3f
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MorningStepNode(
                            icon = stepIcons[index],
                            tint = if (index == 2 && !isPositive) Color(0xFFFF7C6D)
                                   else if (index == 2 && isPositive) Color(0xFF65F0BE)
                                   else accent,
                            size = 32.dp,
                            iconSize = 16.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = steps[index],
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MorningStepNode(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF121A23))
            .border(1.dp, tint.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun MomentumMetric(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

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

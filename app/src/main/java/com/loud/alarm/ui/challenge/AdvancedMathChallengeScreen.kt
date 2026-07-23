package com.loud.alarm.ui.challenge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loud.alarm.data.AdvancedMathTopic
import kotlinx.coroutines.delay
import com.loud.alarm.data.MathDifficulty

@Composable
fun AdvancedMathChallengeScreen(
    topics: Set<AdvancedMathTopic>,
    difficulty: MathDifficulty = MathDifficulty.EASY,
    questionCount: Int = 1,
    muteWhileSolving: Boolean = false,
    onMuteChanged: (Boolean) -> Unit = {},
    onSuccess: () -> Unit
) {
    var currentQuestion by rememberSaveable { mutableIntStateOf(1) }
    var problem by remember { mutableStateOf(AdvancedMathProblemGenerator.generateProblem(topics, difficulty)) }
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var wrongAttempts by remember { mutableIntStateOf(0) }
    var showHint by remember { mutableStateOf(false) }

    // Mute-while-solving state
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var muteCount by rememberSaveable { mutableIntStateOf(0) }
    var showMuteConfirmDialog by remember { mutableStateOf(false) }
    val maxMuteCount = 3
    val muteDurationMs = 4 * 60 * 1000L // 4 minutes

    // Auto-advance to next question after correct flash
    LaunchedEffect(isCorrect) {
        if (isCorrect) {
            kotlinx.coroutines.delay(400)
            if (currentQuestion >= questionCount) {
                // Unmute when challenge is complete
                if (isMuted) {
                    isMuted = false
                    onMuteChanged(false)
                }
                onSuccess()
            } else {
                currentQuestion++
                problem = AdvancedMathProblemGenerator.generateProblem(topics, difficulty)
                input = ""
                isCorrect = false
                wrongAttempts = 0
                showHint = false
            }
        }
    }

    // Show hint after 2 wrong attempts
    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts >= 2 && problem.hint.isNotBlank()) {
            showHint = true
        }
    }

    // Mute timer: unmute after duration, ask "are you there?"
    LaunchedEffect(isMuted, muteCount) {
        if (isMuted) {
            delay(muteDurationMs)
            
            if (muteCount < maxMuteCount) {
                // Ask user if they're still there
                showMuteConfirmDialog = true
                
                // Wait 10 seconds before turning the audio back on
                delay(10_000L)
                
                if (showMuteConfirmDialog) {
                    isMuted = false
                    onMuteChanged(false)
                }
            } else {
                // Unmute the alarm so user knows the time is up
                isMuted = false
                onMuteChanged(false)
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

    // "Are you there?" confirmation dialog
    if (showMuteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showMuteConfirmDialog = false
                isMuted = false
                onMuteChanged(false)
            },
            title = { Text("Are you still there?") },
            text = { Text("The alarm volume will resume in 10 seconds. Do you need more time to solve in silence?") },
            confirmButton = {
                TextButton(onClick = {
                    showMuteConfirmDialog = false
                    isMuted = true
                    muteCount++
                    onMuteChanged(true)
                }) {
                    Text("Yes, mute again")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMuteConfirmDialog = false
                    isMuted = false
                    onMuteChanged(false)
                }) {
                    Text("No, leave volume on")
                }
            }
        )
    }

    // Auto-mute when entering the challenge
    LaunchedEffect(muteWhileSolving) {
        if (muteWhileSolving && !isMuted && muteCount == 0) {
            isMuted = true
            muteCount++
            onMuteChanged(true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Problem Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Progress indicator
                if (questionCount > 1) {
                    Text(
                        text = "Question $currentQuestion / $questionCount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Progress bar
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

                // Topic badge
                val topicInfo = getTopicDisplayInfo(problem.topic)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = topicInfo.first,
                            contentDescription = topicInfo.second,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = topicInfo.second,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Mute button
                if (muteWhileSolving) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = {
                            if (!isMuted && muteCount < maxMuteCount) {
                                isMuted = true
                                muteCount++
                                onMuteChanged(true)
                            } else if (isMuted) {
                                isMuted = false
                                onMuteChanged(false)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isMuted) Color(0xFF1B5E20).copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isMuted) Color(0xFF4CAF50).copy(alpha = 0.5f)
                                    else Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMuted) "Muted (${maxMuteCount - muteCount} left)"
                                       else if (muteCount >= maxMuteCount) "Mute limit reached"
                                       else "Mute alarm",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isMuted) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = problem.question,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Input Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = input,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 4.sp
                    )
                }

                // Hint display
                AnimatedVisibility(
                    visible = showHint,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A237E).copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = problem.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Keypad (shared from MathChallengeScreen.kt)
            NumericKeypad(
                onNumberClick = { num ->
                    if (input.length < 10 && !isCorrect) {
                        // Prevent multiple decimals
                        if (num == "." && input.contains(".")) return@NumericKeypad
                        // Prevent negative sign anywhere but the start
                        if (num == "-" && input.isNotEmpty()) return@NumericKeypad
                        
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
                    if (!isCorrect) {
                        if (input == problem.answer) {
                            // Correct answer!
                            isCorrect = true
                        } else if (input.isNotEmpty()) {
                            // Wrong answer - clear and show error feedback
                            isError = true
                            wrongAttempts++
                            input = ""
                        }
                    }
                }
            )
        }
    }
}

/** Returns (icon, label) for the given topic */
private fun getTopicDisplayInfo(topic: AdvancedMathTopic): Pair<ImageVector, String> {
    return when (topic) {
        AdvancedMathTopic.POLYNOMIAL -> Icons.Default.Functions to "Polynomial"
        AdvancedMathTopic.GEOMETRY -> Icons.Default.Architecture to "Geometry"
        AdvancedMathTopic.TRIGONOMETRY -> Icons.Default.SquareFoot to "Trigonometry"
        AdvancedMathTopic.CALCULUS -> Icons.Default.Timeline to "Calculus"
        AdvancedMathTopic.MATRIX -> Icons.Default.GridOn to "Matrix"
        AdvancedMathTopic.LOGARITHM -> Icons.Default.ShowChart to "Logarithm"
        AdvancedMathTopic.PROBABILITY -> Icons.Default.Casino to "Probability"
        AdvancedMathTopic.SEQUENCE -> Icons.Default.LinearScale to "Sequence"
    }
}

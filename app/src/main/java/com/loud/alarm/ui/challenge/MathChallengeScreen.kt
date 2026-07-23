package com.loud.alarm.ui.challenge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loud.alarm.data.MathDifficulty

@Composable
fun MathChallengeScreen(
    difficulty: MathDifficulty,
    questionCount: Int = 1,
    onSuccess: () -> Unit
) {
    var currentQuestion by remember { mutableStateOf(1) }
    var problem by remember { mutableStateOf(MathProblemGenerator.generateProblem(difficulty)) }
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    // Auto-advance to next question after correct flash
    LaunchedEffect(isCorrect) {
        if (isCorrect) {
            kotlinx.coroutines.delay(400)
            if (currentQuestion >= questionCount) {
                onSuccess()
            } else {
                currentQuestion++
                problem = MathProblemGenerator.generateProblem(difficulty)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Solve to Dismiss",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = problem.question,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Input Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
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
            }

            // Keypad
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
                        if (input == problem.answer.toString()) {
                            // Correct answer!
                            isCorrect = true
                        } else if (input.isNotEmpty()) {
                            // Wrong answer - clear and show error feedback
                            isError = true
                            input = ""
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "-"),
            listOf("DEL", "OK")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { key ->
                    Key(
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
fun Key(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isAction = label == "DEL" || label == "OK"
    val containerColor = if (isAction) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isAction) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .aspectRatio(if (isAction) 3.5f else 1.5f) // Action keys shorter than numbers
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == "DEL") {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = contentColor)
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

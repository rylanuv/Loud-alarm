package com.loud.alarm.ui.challenge

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loud.alarm.data.MathDifficulty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MemoryChallengeScreen(
    difficulty: MathDifficulty = MathDifficulty.EASY,
    challengeCount: Int = 3,
    onSuccess: () -> Unit
) {
    // Difficulty controls grid size and starting sequence length
    val gridSize = when (difficulty) {
        MathDifficulty.EASY -> 3
        MathDifficulty.MEDIUM -> 3
        MathDifficulty.HARD -> 4
        MathDifficulty.EXTREME -> 4
    }
    val startingSequenceLength = when (difficulty) {
        MathDifficulty.EASY -> 3
        MathDifficulty.MEDIUM -> 4
        MathDifficulty.HARD -> 5
        MathDifficulty.EXTREME -> 6
    }
    val totalLevels = challengeCount

    var sequence by remember { mutableStateOf(listOf<Int>()) }
    var userSequence by remember { mutableStateOf(listOf<Int>()) }
    var activeTile by remember { mutableStateOf<Int?>(null) }
    var isShowingSequence by remember { mutableStateOf(false) }
    var sequenceLength by remember { mutableStateOf(startingSequenceLength) }
    var level by remember { mutableStateOf(1) }
    
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(level) {
        delay(800)
        // Generate new sequence
        isShowingSequence = true
        userSequence = emptyList()
        val newSeq = List(sequenceLength) { (0 until gridSize * gridSize).random() }
        sequence = newSeq
        
        // Play sequence
        for (tile in sequence) {
            activeTile = tile
            delay(400)
            activeTile = null
            delay(200)
        }
        isShowingSequence = false
    }
    
    LaunchedEffect(userSequence) {
        if (userSequence.isNotEmpty()) {
            val isCorrectSoFar = userSequence.indices.all { i -> userSequence[i] == sequence[i] }
            if (!isCorrectSoFar) {
                // Incorrect input: show error without text, then replay.
                isShowingSequence = true
                activeTile = null
                isError = true
                delay(500)
                isError = false
                delay(300)
                
                // Replay the sequence
                for (tile in sequence) {
                    activeTile = tile
                    delay(400)
                    activeTile = null
                    delay(200)
                }
                userSequence = emptyList()
                isShowingSequence = false
            } else if (userSequence.size == sequence.size) {
                // Correct
                isShowingSequence = true
                activeTile = null
                isSuccess = true
                delay(500)
                isSuccess = false
                
                if (level >= totalLevels) {
                    onSuccess()
                } else {
                    sequenceLength += 1
                    level += 1
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Memory Test",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show status text or hide it during error/success states for cleaner UX
        val statusText = when {
            isError -> ""
            isSuccess -> "Good Job!"
            isShowingSequence -> "Watch the pattern"
            else -> "Repeat the pattern"
        }
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Level $level / $totalLevels",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until gridSize) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0 until gridSize) {
                            val tileIndex = r * gridSize + c
                            val isActive = activeTile == tileIndex
                            
                            val targetColor = when {
                                isError -> Color(0xFFE53935) // Red
                                isSuccess -> Color(0xFF43A047) // Green
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFF4C4C4E)
                            }
                            
                            val tileColor by animateColorAsState(
                                targetValue = targetColor,
                                animationSpec = tween(durationMillis = 150),
                                label = "tileColor"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tileColor)
                                    .clickable(enabled = !isShowingSequence) {
                                        activeTile = tileIndex
                                        coroutineScope.launch {
                                            delay(150)
                                            if (activeTile == tileIndex) {
                                                activeTile = null
                                            }
                                        }
                                        userSequence = userSequence + tileIndex
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}


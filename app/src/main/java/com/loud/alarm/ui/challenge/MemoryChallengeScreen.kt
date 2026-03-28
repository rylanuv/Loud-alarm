package com.loud.alarm.ui.challenge

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MemoryChallengeScreen(onSuccess: () -> Unit) {
    val gridSize = 3
    var sequence by remember { mutableStateOf(listOf<Int>()) }
    var userSequence by remember { mutableStateOf(listOf<Int>()) }
    var activeTile by remember { mutableStateOf<Int?>(null) }
    var isShowingSequence by remember { mutableStateOf(false) }
    var sequenceLength by remember { mutableStateOf(4) }
    var level by remember { mutableStateOf(1) }
    
    LaunchedEffect(level) {
        delay(1000)
        // Generate new sequence
        isShowingSequence = true
        userSequence = emptyList()
        val newSeq = List(sequenceLength) { (0 until gridSize * gridSize).random() }
        sequence = newSeq
        
        // Play sequence
        for (tile in sequence) {
            activeTile = tile
            delay(500)
            activeTile = null
            delay(200)
        }
        isShowingSequence = false
    }
    
    LaunchedEffect(userSequence) {
        if (userSequence.isNotEmpty()) {
            val isCorrectSoFar = userSequence.indices.all { i -> userSequence[i] == sequence[i] }
            if (!isCorrectSoFar) {
                // Incorrect input: replay the same sequence, then clear input.
                isShowingSequence = true
                for (tile in sequence) {
                    activeTile = tile
                    delay(500)
                    activeTile = null
                    delay(200)
                }
                activeTile = null
                userSequence = emptyList()
                isShowingSequence = false
            } else if (userSequence.size == sequence.size) {
                if (level >= 3) {
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
        Text(
            text = if (isShowingSequence) "Watch the pattern" else "Repeat the pattern",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Level $level / 3",
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
                            // Or temporarily blink on tap
                            
                            val tileColor = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF4C4C4E)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tileColor)
                                    .clickable {
                                        if (!isShowingSequence) {
                                            activeTile = tileIndex
                                            userSequence = userSequence + tileIndex
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val PUZZLE_SIZE = 2
private const val SCRAMBLE_MOVES = 6

private fun solvedBoard(size: Int): List<Int> {
    return (1 until size * size).toList() + 0
}

private fun adjacentIndices(index: Int, size: Int): List<Int> {
    val row = index / size
    val col = index % size
    val result = mutableListOf<Int>()
    if (row > 0) result += index - size
    if (row < size - 1) result += index + size
    if (col > 0) result += index - 1
    if (col < size - 1) result += index + 1
    return result
}

private fun generateScrambledBoard(size: Int): List<Int> {
    val board = solvedBoard(size).toMutableList()
    var blankIndex = board.lastIndex
    var previousBlankIndex = -1

    repeat(SCRAMBLE_MOVES) {
        val options = adjacentIndices(blankIndex, size).filter { it != previousBlankIndex }
        val nextIndex = (if (options.isNotEmpty()) options else adjacentIndices(blankIndex, size)).random()
        val temp = board[nextIndex]
        board[nextIndex] = board[blankIndex]
        board[blankIndex] = temp
        previousBlankIndex = blankIndex
        blankIndex = nextIndex
    }

    if (board == solvedBoard(size)) {
        val options = adjacentIndices(blankIndex, size)
        val nextIndex = options.first()
        val temp = board[nextIndex]
        board[nextIndex] = board[blankIndex]
        board[blankIndex] = temp
    }

    return board
}

@Composable
fun PuzzleChallengeScreen(onSuccess: () -> Unit) {
    val targetBoard = remember { solvedBoard(PUZZLE_SIZE) }
    var board by rememberSaveable { mutableStateOf(generateScrambledBoard(PUZZLE_SIZE)) }
    var moves by rememberSaveable { mutableStateOf(0) }
    val isSolved = board == targetBoard

    LaunchedEffect(isSolved) {
        if (isSolved) onSuccess()
    }

    fun onTileTap(index: Int) {
        val blankIndex = board.indexOf(0)
        if (index !in adjacentIndices(blankIndex, PUZZLE_SIZE)) return
        val next = board.toMutableList()
        val temp = next[index]
        next[index] = next[blankIndex]
        next[blankIndex] = temp
        board = next
        moves += 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sliding Puzzle",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Arrange numbers 1 to ${PUZZLE_SIZE * PUZZLE_SIZE - 1} in order",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Moves: $moves",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until PUZZLE_SIZE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (c in 0 until PUZZLE_SIZE) {
                            val index = r * PUZZLE_SIZE + c
                            val value = board[index]
                            val isBlank = value == 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isBlank) Color(0xFF1E1E20)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                    )
                                    .clickable(enabled = !isBlank) { onTileTap(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isBlank) {
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    if (r < PUZZLE_SIZE - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.loud.alarm.data.MathDifficulty

private fun puzzleConfig(difficulty: MathDifficulty): Pair<Int, Int> {
    // Returns (gridSize, scrambleMoves)
    return when (difficulty) {
        MathDifficulty.EASY -> 2 to 6       // 2×2 grid (3 tiles)
        MathDifficulty.MEDIUM -> 3 to 20    // 3×3 grid (8 tiles)
        MathDifficulty.HARD -> 4 to 40      // 4×4 grid (15 tiles)
        MathDifficulty.EXTREME -> 5 to 80   // 5×5 grid (24 tiles)
    }
}

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

private fun generateScrambledBoard(size: Int, scrambleMoves: Int): List<Int> {
    val board = solvedBoard(size).toMutableList()
    var blankIndex = board.lastIndex
    var previousBlankIndex = -1

    repeat(scrambleMoves) {
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
fun PuzzleChallengeScreen(
    difficulty: MathDifficulty = MathDifficulty.EASY,
    onSuccess: () -> Unit
) {
    val (puzzleSize, scrambleMoves) = remember(difficulty) { puzzleConfig(difficulty) }
    val targetBoard = remember(puzzleSize) { solvedBoard(puzzleSize) }
    var board by rememberSaveable { mutableStateOf(generateScrambledBoard(puzzleSize, scrambleMoves)) }
    var moves by rememberSaveable { mutableStateOf(0) }
    val isSolved = board == targetBoard
    var showError by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (showError) Color.Red.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "ErrorBackground"
    )

    LaunchedEffect(isSolved) {
        if (isSolved) {
            delay(300)
            onSuccess()
        }
    }

    LaunchedEffect(showError) {
        if (showError) {
            delay(300)
            showError = false
        }
    }

    fun onTileTap(index: Int) {
        val blankIndex = board.indexOf(0)
        if (index !in adjacentIndices(blankIndex, puzzleSize)) {
            showError = true
            return
        }
        val next = board.toMutableList()
        val temp = next[index]
        next[index] = next[blankIndex]
        next[blankIndex] = temp
        board = next
        moves += 1
    }

    // Choose font style based on grid size so numbers fit
    val tileTextStyle = when {
        puzzleSize <= 3 -> MaterialTheme.typography.headlineMedium
        puzzleSize == 4 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
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
            text = "Arrange numbers 1 to ${puzzleSize * puzzleSize - 1} in order",
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            val spacing = if (puzzleSize <= 3) 8.dp else 4.dp
            val tileSize = (maxWidth - spacing * (puzzleSize - 1)) / puzzleSize

            val tileColor by animateColorAsState(
                targetValue = if (isSolved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                animationSpec = tween(300),
                label = "tileColor"
            )

            for (value in 1 until puzzleSize * puzzleSize) {
                val index = board.indexOf(value)
                if (index == -1) continue
                val row = index / puzzleSize
                val col = index % puzzleSize

                val offsetX = (tileSize + spacing) * col
                val offsetY = (tileSize + spacing) * row

                val animOffsetX by animateDpAsState(
                    targetValue = offsetX,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "offsetX"
                )
                val animOffsetY by animateDpAsState(
                    targetValue = offsetY,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "offsetY"
                )

                Box(
                    modifier = Modifier
                        .offset(animOffsetX, animOffsetY)
                        .size(tileSize)
                        .clip(RoundedCornerShape(if (puzzleSize <= 3) 12.dp else 8.dp))
                        .background(tileColor)
                        .clickable(enabled = !isSolved) { onTileTap(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        style = tileTextStyle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

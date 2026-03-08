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
import androidx.compose.foundation.layout.size
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
import kotlin.math.abs

private val MAZE_LEVELS = listOf(
    listOf(
        "S....",
        "###.#",
        "....#",
        "#.###",
        "....E"
    ),
    listOf(
        "S#...",
        ".#.##",
        "....#",
        "###.#",
        "E..."
    ),
    listOf(
        "S....",
        ".###.",
        ".#.#.",
        "....#",
        "###.E"
    )
)

@Composable
fun MazeChallengeScreen(onSuccess: () -> Unit) {
    val mazeRows = remember { MAZE_LEVELS.random() }
    val height = mazeRows.size
    val width = mazeRows[0].length

    var startPos by remember { mutableStateOf(Pair(0, 0)) }
    var endPos by remember { mutableStateOf(Pair(height - 1, width - 1)) }
    
    LaunchedEffect(Unit) {
        for (r in 0 until height) {
            for (c in 0 until width) {
                if (mazeRows[r][c] == 'S') startPos = Pair(r, c)
                if (mazeRows[r][c] == 'E') endPos = Pair(r, c)
            }
        }
    }

    var path by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    LaunchedEffect(startPos) {
        path = listOf(startPos)
    }

    LaunchedEffect(path) {
        if (path.isNotEmpty() && path.last() == endPos) {
            onSuccess()
        }
    }

    fun onCellTap(r: Int, c: Int) {
        val lastPos = path.last()
        // Check if adjacent
        val isAdjacent = abs(lastPos.first - r) + abs(lastPos.second - c) == 1
        if (!isAdjacent) return
        
        // Prevent going into walls
        if (mazeRows[r][c] == '#') {
            // Reset to start on hit wall
            path = listOf(startPos)
            return
        }

        // Backtrack or advance
        if (path.size >= 2 && path[path.size - 2] == Pair(r, c)) {
            path = path.dropLast(1) // backtrack
        } else if (!path.contains(Pair(r, c))) {
            path = path + Pair(r, c)
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
            text = "Solve the Maze",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Guide the path from S to E",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until height) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        for (c in 0 until width) {
                            val isWall = mazeRows[r][c] == '#'
                            val isStart = mazeRows[r][c] == 'S'
                            val isEnd = mazeRows[r][c] == 'E'
                            val isPath = path.contains(Pair(r, c))

                            val bgColor = when {
                                isStart -> Color.Green
                                isEnd -> Color.Red
                                isPath -> MaterialTheme.colorScheme.primary
                                isWall -> Color(0xFF4C4C4E)
                                else -> Color(0xFF1E1E20)
                            }
                            
                            val textStr = when {
                                isStart -> "S"
                                isEnd -> "E"
                                else -> ""
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .clickable { onCellTap(r, c) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (textStr.isNotEmpty()) {
                                    Text(
                                        text = textStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

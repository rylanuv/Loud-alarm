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
import com.loud.alarm.data.MathDifficulty
import kotlin.math.abs
import kotlin.random.Random

// --- Procedural maze generator (Randomized DFS / Recursive Backtracker) ---

/**
 * Grid size for each difficulty (rows x cols of the *cell* grid).
 * The actual rendered grid is (2*cells+1) because walls sit between cells.
 */
private fun gridSizeForDifficulty(d: MathDifficulty): Pair<Int, Int> = when (d) {
    MathDifficulty.EASY    -> 5 to 5
    MathDifficulty.MEDIUM  -> 6 to 6
    MathDifficulty.HARD    -> 7 to 7
    MathDifficulty.EXTREME -> 8 to 8
}

/**
 * Generates a guaranteed-solvable maze using randomized DFS and returns it
 * as a list of strings (one per row) using:
 *   'S' = start, 'E' = end, '.' = open, '#' = wall
 *
 * [rows] and [cols] are the *logical* cell dimensions.
 * The returned grid is (2*rows+1) x (2*cols+1) with walls on even indices.
 */
private fun generateMaze(rows: Int, cols: Int, rng: Random = Random): List<String> {
    val gridH = 2 * rows + 1
    val gridW = 2 * cols + 1

    // Initialize everything as wall
    val grid = Array(gridH) { CharArray(gridW) { '#' } }

    val visited = Array(rows) { BooleanArray(cols) }

    // Directions: up, right, down, left
    val dr = intArrayOf(-1, 0, 1, 0)
    val dc = intArrayOf(0, 1, 0, -1)

    // Iterative DFS with explicit stack (avoids StackOverflow on large grids)
    val stack = ArrayDeque<Pair<Int, Int>>()
    visited[0][0] = true
    grid[1][1] = '.'
    stack.addLast(0 to 0)

    while (stack.isNotEmpty()) {
        val (cr, cc) = stack.last()
        // Collect unvisited neighbours
        val neighbours = mutableListOf<Int>()
        for (d in 0..3) {
            val nr = cr + dr[d]
            val nc = cc + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]) {
                neighbours.add(d)
            }
        }
        if (neighbours.isEmpty()) {
            stack.removeLast()
        } else {
            val d = neighbours[rng.nextInt(neighbours.size)]
            val nr = cr + dr[d]
            val nc = cc + dc[d]
            // Carve the wall between current and neighbour
            val wallR = 2 * cr + 1 + dr[d]
            val wallC = 2 * cc + 1 + dc[d]
            grid[wallR][wallC] = '.'
            // Mark neighbour cell as open & visited
            grid[2 * nr + 1][2 * nc + 1] = '.'
            visited[nr][nc] = true
            stack.addLast(nr to nc)
        }
    }

    // Place start & end markers
    grid[1][1] = 'S'
    grid[2 * rows - 1][2 * cols - 1] = 'E'

    return grid.map { String(it) }
}

// -----------------------------------------------------------------------

@Composable
fun MazeChallengeScreen(
    difficulty: MathDifficulty,
    onSuccess: () -> Unit
) {
    val (cellRows, cellCols) = remember(difficulty) { gridSizeForDifficulty(difficulty) }

    val mazeRows = remember(difficulty) {
        generateMaze(cellRows, cellCols)
    }

    val height = mazeRows.size
    val width = mazeRows.maxOfOrNull { it.length } ?: 0

    fun cellAt(r: Int, c: Int): Char {
        val row = mazeRows.getOrNull(r) ?: return '#'
        return row.getOrNull(c) ?: '#'
    }

    var startPos by remember { mutableStateOf(Pair(0, 0)) }
    var endPos by remember {
        mutableStateOf(
            Pair(
                (height - 1).coerceAtLeast(0),
                (width - 1).coerceAtLeast(0)
            )
        )
    }

    LaunchedEffect(Unit) {
        for (r in 0 until height) {
            for (c in mazeRows[r].indices) {
                if (cellAt(r, c) == 'S') startPos = Pair(r, c)
                if (cellAt(r, c) == 'E') endPos = Pair(r, c)
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
        if (cellAt(r, c) == '#') {
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
            text = "Guide the path from S to E (${difficulty.name})",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(width.toFloat() / height.toFloat())
                .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until height) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        for (c in 0 until width) {
                            val cell = cellAt(r, c)
                            val isWall = cell == '#'
                            val isStart = cell == 'S'
                            val isEnd = cell == 'E'
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
                                    .padding(1.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
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

package com.loud.alarm.ui.challenge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loud.alarm.data.MathDifficulty
import kotlin.math.abs
import kotlin.random.Random

// --- Procedural maze generator (Randomized DFS / Recursive Backtracker) ---

/**
 * Grid size for each difficulty (rows x cols of the *cell* grid).
 * Kept small but the complexity comes from dead-end injection.
 * The actual rendered grid is (2*cells+1) because walls sit between cells.
 */
private fun gridSizeForDifficulty(d: MathDifficulty): Pair<Int, Int> = when (d) {
    MathDifficulty.EASY    -> 5 to 5
    MathDifficulty.MEDIUM  -> 6 to 6
    MathDifficulty.HARD    -> 7 to 7
    MathDifficulty.EXTREME -> 8 to 8
}

/**
 * How many extra dead-end branches to inject per difficulty.
 */
private fun deadEndCountForDifficulty(d: MathDifficulty): Int = when (d) {
    MathDifficulty.EASY    -> 3
    MathDifficulty.MEDIUM  -> 5
    MathDifficulty.HARD    -> 7
    MathDifficulty.EXTREME -> 10
}

/**
 * BFS shortest-path check – returns true if a path exists from start to end
 * through open cells ('.', 'S', 'E').
 */
private fun hasPath(grid: Array<CharArray>, startR: Int, startC: Int, endR: Int, endC: Int): Boolean {
    val h = grid.size
    val w = grid[0].size
    val visited = Array(h) { BooleanArray(w) }
    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.addLast(startR to startC)
    visited[startR][startC] = true
    val dr = intArrayOf(-1, 0, 1, 0)
    val dc = intArrayOf(0, 1, 0, -1)
    while (queue.isNotEmpty()) {
        val (cr, cc) = queue.removeFirst()
        if (cr == endR && cc == endC) return true
        for (i in 0..3) {
            val nr = cr + dr[i]
            val nc = cc + dc[i]
            if (nr in 0 until h && nc in 0 until w && !visited[nr][nc] && grid[nr][nc] != '#') {
                visited[nr][nc] = true
                queue.addLast(nr to nc)
            }
        }
    }
    return false
}

/**
 * Generates a guaranteed-solvable maze using randomized DFS and then
 * injects deliberate dead-end branches to frustrate the solver.
 *
 * Returns a list of strings (one per row) using:
 *   'S' = start, 'E' = end, '.' = open, '#' = wall
 *
 * [rows] and [cols] are the *logical* cell dimensions.
 * The returned grid is (2*rows+1) x (2*cols+1) with walls on even indices.
 */
private fun generateMaze(
    rows: Int,
    cols: Int,
    deadEndBranches: Int,
    rng: Random = Random
): List<String> {
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
    val startR = 1
    val startC = 1
    val endR = 2 * rows - 1
    val endC = 2 * cols - 1
    grid[startR][startC] = 'S'
    grid[endR][endC] = 'E'

    // --- Inject deliberate dead-end branches ---
    // Strategy: find wall cells adjacent to exactly one open cell and carve
    // a short branch (1-3 cells deep) that leads nowhere.
    val dirR = intArrayOf(-1, 0, 1, 0)
    val dirC = intArrayOf(0, 1, 0, -1)

    var branchesCreated = 0
    val attempts = deadEndBranches * 20 // max attempts

    for (attempt in 0 until attempts) {
        if (branchesCreated >= deadEndBranches) break

        // Pick a random wall cell (not on the border)
        val wr = rng.nextInt(1, gridH - 1)
        val wc = rng.nextInt(1, gridW - 1)
        if (grid[wr][wc] != '#') continue

        // Count how many open neighbours this wall has
        var openNeighbours = 0
        var openDir = -1
        for (d in 0..3) {
            val nr = wr + dirR[d]
            val nc = wc + dirC[d]
            if (nr in 0 until gridH && nc in 0 until gridW && grid[nr][nc] != '#') {
                openNeighbours++
                openDir = d
            }
        }

        // We want wall cells that touch exactly one open cell –
        // carving here creates a pocket / dead-end spur
        if (openNeighbours != 1) continue

        // Carve this cell
        grid[wr][wc] = '.'

        // Try to extend the dead end 1-2 cells deeper in the opposite direction
        val extendDir = (openDir + 2) % 4  // opposite direction
        var curR = wr
        var curC = wc
        val depth = rng.nextInt(1, 3)
        for (step in 0 until depth) {
            val nr = curR + dirR[extendDir]
            val nc = curC + dirC[extendDir]
            if (nr in 1 until gridH - 1 && nc in 1 until gridW - 1 && grid[nr][nc] == '#') {
                // Check carving this won't connect to another open area
                // (would remove the dead-end effect)
                var adjacentOpen = 0
                for (dd in 0..3) {
                    val ar = nr + dirR[dd]
                    val ac = nc + dirC[dd]
                    if (ar in 0 until gridH && ac in 0 until gridW && grid[ar][ac] != '#') {
                        adjacentOpen++
                    }
                }
                // Only carve if it would touch exactly one open cell (the previous one)
                if (adjacentOpen == 1) {
                    grid[nr][nc] = '.'
                    curR = nr
                    curC = nc
                } else {
                    break
                }
            } else {
                break
            }
        }

        // Verify the maze is still solvable after carving
        if (hasPath(grid, startR, startC, endR, endC)) {
            branchesCreated++
        } else {
            // Revert (shouldn't happen often, but safety)
            grid[wr][wc] = '#'
        }
    }

    return grid.map { String(it) }
}

// -----------------------------------------------------------------------

@Composable
fun MazeChallengeScreen(
    difficulty: MathDifficulty,
    onSuccess: () -> Unit
) {
    val (cellRows, cellCols) = remember(difficulty) { gridSizeForDifficulty(difficulty) }
    val deadEnds = remember(difficulty) { deadEndCountForDifficulty(difficulty) }

    // Track regeneration key to allow reset
    var mazeKey by remember { mutableIntStateOf(0) }

    val mazeRows = remember(difficulty, mazeKey) {
        generateMaze(cellRows, cellCols, deadEnds)
    }

    val height = mazeRows.size
    val width = mazeRows.maxOfOrNull { it.length } ?: 0

    fun cellAt(r: Int, c: Int): Char {
        val row = mazeRows.getOrNull(r) ?: return '#'
        return row.getOrNull(c) ?: '#'
    }

    var startPos by remember(mazeKey) { mutableStateOf(Pair(0, 0)) }
    var endPos by remember(mazeKey) {
        mutableStateOf(
            Pair(
                (height - 1).coerceAtLeast(0),
                (width - 1).coerceAtLeast(0)
            )
        )
    }

    LaunchedEffect(mazeKey) {
        for (r in 0 until height) {
            for (c in mazeRows[r].indices) {
                if (cellAt(r, c) == 'S') startPos = Pair(r, c)
                if (cellAt(r, c) == 'E') endPos = Pair(r, c)
            }
        }
    }

    var playerPos by remember(mazeKey) { mutableStateOf(startPos) }
    var path by remember(mazeKey) { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var deadEndHit by remember(mazeKey) { mutableStateOf(false) }

    LaunchedEffect(startPos, mazeKey) {
        playerPos = startPos
        path = listOf(startPos)
    }

    LaunchedEffect(playerPos) {
        if (playerPos == endPos) {
            onSuccess()
        }
    }

    // Check if player is in a dead end (no unvisited open neighbours)
    LaunchedEffect(playerPos, path) {
        if (path.size > 1) {
            val (pr, pc) = playerPos
            val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
            val hasUnvisitedOpen = dirs.any { (dr, dc) ->
                val nr = pr + dr
                val nc = pc + dc
                val cell = cellAt(nr, nc)
                cell != '#' && !path.contains(nr to nc)
            }
            deadEndHit = !hasUnvisitedOpen && playerPos != endPos
        } else {
            deadEndHit = false
        }
    }

    fun resetMaze() {
        playerPos = startPos
        path = listOf(startPos)
        deadEndHit = false
    }

    fun movePlayer(dr: Int, dc: Int) {
        val (pr, pc) = playerPos
        val nr = pr + dr
        val nc = pc + dc
        if (nr < 0 || nr >= height || nc < 0 || nc >= width) {
            resetMaze()
            return
        }
        if (cellAt(nr, nc) == '#') {
            resetMaze()
            return
        }

        // If backtracking
        if (path.size >= 2 && path[path.size - 2] == (nr to nc)) {
            path = path.dropLast(1)
            playerPos = nr to nc
        } else if (!path.contains(nr to nc)) {
            path = path + (nr to nc)
            playerPos = nr to nc
        }
    }

    // Colors
    val wallColor = Color(0xFF1A1A2E)
    val pathOpenColor = Color(0xFF16213E)
    val playerTrailColor = Color(0xFF0F3460)
    val playerColor = Color(0xFF00D2FF)
    val startColor = Color(0xFF00E676)
    val endColor = Color(0xFFFF5252)
    val deadEndGlowColor by animateColorAsState(
        targetValue = if (deadEndHit) Color(0x44FF5252) else Color.Transparent,
        animationSpec = tween(500),
        label = "deadEndGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧩 Solve the Maze",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Navigate from S → E  •  ${difficulty.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E99A4)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dead-end warning
        if (deadEndHit) {
            Text(
                text = "💀 Dead end! Backtrack or reset.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF5252),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Maze grid with swipe support
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(width.toFloat() / height.toFloat())
                .clip(RoundedCornerShape(16.dp))
                .background(deadEndGlowColor)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF16213E), Color(0xFF0A0A1A))
                    )
                )
                .border(1.dp, Color(0xFF1A3A5C), RoundedCornerShape(16.dp))
                .padding(6.dp)
                .pointerInput(mazeKey) {
                    var totalDx = 0f
                    var totalDy = 0f
                    detectDragGestures(
                        onDragStart = {
                            totalDx = 0f
                            totalDy = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDx += dragAmount.x
                            totalDy += dragAmount.y
                            val threshold = 60f
                            when {
                                totalDx > threshold -> {
                                    movePlayer(0, 1)
                                    totalDx = 0f
                                    totalDy = 0f
                                }
                                totalDx < -threshold -> {
                                    movePlayer(0, -1)
                                    totalDx = 0f
                                    totalDy = 0f
                                }
                                totalDy > threshold -> {
                                    movePlayer(1, 0)
                                    totalDx = 0f
                                    totalDy = 0f
                                }
                                totalDy < -threshold -> {
                                    movePlayer(-1, 0)
                                    totalDx = 0f
                                    totalDy = 0f
                                }
                            }
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until height) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (c in 0 until width) {
                            val cell = cellAt(r, c)
                            val isWall = cell == '#'
                            val isStart = cell == 'S'
                            val isEnd = cell == 'E'
                            val isPlayer = playerPos == (r to c)
                            val isOnPath = path.contains(r to c)

                            val bgColor = when {
                                isPlayer -> playerColor
                                isStart -> startColor
                                isEnd -> endColor
                                isOnPath -> playerTrailColor
                                isWall -> wallColor
                                else -> pathOpenColor
                            }

                            val textStr = when {
                                isPlayer -> "●"
                                isStart -> "S"
                                isEnd -> "E"
                                else -> ""
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(0.5.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(bgColor)
                                    .then(
                                        if (!isWall) {
                                            Modifier.clickable {
                                                val lastPos = playerPos
                                                val isAdj =
                                                    abs(lastPos.first - r) + abs(lastPos.second - c) == 1
                                                if (isAdj) {
                                                    movePlayer(r - lastPos.first, c - lastPos.second)
                                                }
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (textStr.isNotEmpty()) {
                                    Text(
                                        text = textStr,
                                        fontSize = if (isPlayer) 10.sp else 8.sp,
                                        color = if (isPlayer) Color(0xFF0A0A1A) else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // D-pad controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Up
            DpadButton(Icons.Default.KeyboardArrowUp, "Up") { movePlayer(-1, 0) }
            // Left, Reset, Right
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DpadButton(Icons.Default.KeyboardArrowLeft, "Left") { movePlayer(0, -1) }

                // Reset button in center of D-pad
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (deadEndHit) Color(0xFFFF5252) else Color(0xFF2A2A3E)
                        )
                        .clickable { resetMaze() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DpadButton(Icons.Default.KeyboardArrowRight, "Right") { movePlayer(0, 1) }
            }
            // Down
            DpadButton(Icons.Default.KeyboardArrowDown, "Down") { movePlayer(1, 0) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap arrows, swipe on maze, or tap cells",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5A6A7A)
        )
    }
}

@Composable
private fun DpadButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2A3A))
            .border(1.dp, Color(0xFF2A3A4A), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color(0xFF00D2FF),
            modifier = Modifier.size(28.dp)
        )
    }
}

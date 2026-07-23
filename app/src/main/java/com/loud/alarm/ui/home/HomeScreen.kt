package com.loud.alarm.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.loud.alarm.service.AlarmReceiver
import com.loud.alarm.service.AlarmService
import com.loud.alarm.R
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.ui.components.SkeuomorphicSwitch
import com.loud.alarm.ui.permissions.RequiredPermissionsStatus
import com.loud.alarm.ui.permissions.rememberRequiredPermissionsStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (Int?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissionSetup: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    val nextAlarm by viewModel.nextAlarm.collectAsState()
    val timeUntilNext by viewModel.timeUntilNextAlarmValues.collectAsState()
    val permissionsStatus = rememberRequiredPermissionsStatus()
    val localContext = LocalContext.current

    // In-App Review: check eligibility when HomeScreen is composed
    // (user may have just returned from dismissing an alarm)
    val activity = androidx.activity.compose.LocalActivity.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (activity != null && viewModel.shouldRequestReview()) {
            viewModel.requestReview(activity)
        }
    }

    // Share Prompt: check if 10 days have passed
    var showShareDialog by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (viewModel.shouldShowSharePrompt()) {
            showShareDialog = true
            viewModel.onSharePromptShown()
        }
    }

    // Share Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            containerColor = Color(0xFF1A181C),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            icon = {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Enjoying Loud Alarm?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Share the app with your friends and help them never oversleep again!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Check out Loud Alarm!")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Hey! I use Loud Alarm to make sure I never oversleep. " +
                                        "It has crazy challenges that force you to wake up! " +
                                        "Try it out: https://play.google.com/store/apps/details?id=${localContext.packageName}"
                            )
                        }
                        localContext.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                ) {
                    Text("Share Now", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Maybe Later", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Home-specific background image + overlay
        Image(
            painter = painterResource(id = R.drawable.menu),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(
                            onClick = onNavigateToStatistics,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Statistics",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToEditor(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                }
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!permissionsStatus.allGranted) {
                PermissionWarningCard(
                    status = permissionsStatus,
                    onClick = onNavigateToPermissionSetup
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Next Alarm Card
            AnimatedVisibility(visible = nextAlarm != null) {
                Column {
                    NextAlarmCard(nextAlarm = nextAlarm, timeUntil = timeUntilNext)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Alarm List
            if (alarms.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        SwipeableAlarmItem(
                            alarm = alarm,
                            onToggle = { 
                                viewModel.handleAlarmAction(alarm, { viewModel.toggleAlarm(alarm) }, {
                                    android.widget.Toast.makeText(localContext, "Cannot edit alarm within 30 minutes of ringing", android.widget.Toast.LENGTH_SHORT).show()
                                }) 
                            },
                            onClick = { 
                                viewModel.handleAlarmAction(alarm, { onNavigateToEditor(alarm.id) }, {
                                    android.widget.Toast.makeText(localContext, "Cannot edit alarm within 30 minutes of ringing", android.widget.Toast.LENGTH_SHORT).show()
                                })
                            },
                            onDelete = { 
                                viewModel.handleAlarmAction(alarm, { viewModel.deleteAlarm(alarm) }, {
                                    android.widget.Toast.makeText(localContext, "Cannot delete alarm within 30 minutes of ringing", android.widget.Toast.LENGTH_SHORT).show()
                                })
                            }
                        )
                    }
                    // Spacer for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
    }
}

@Composable
private fun PermissionWarningCard(
    status: RequiredPermissionsStatus,
    onClick: () -> Unit
) {
    val missingLabels = status.missingItems.map { it.title }
    val summary = when (missingLabels.size) {
        0 -> ""
        1 -> "${missingLabels.first()} is still off."
        2 -> "${missingLabels[0]} and ${missingLabels[1]} are still off."
        else -> "${missingLabels.dropLast(1).joinToString(", ")}, and ${missingLabels.last()} are still off."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33FFB27A),
                            Color(0x1AFFB27A)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0x66FFB27A),
                    shape = RoundedCornerShape(22.dp)
                )
                .clip(RoundedCornerShape(22.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFB27A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFC289)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permissions needed",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.ifEmpty {
                            "Turn on the required permissions so alarms keep working properly."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }

                Text(
                    text = "Fix",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFC289),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NextAlarmCard(nextAlarm: Alarm?, timeUntil: String) {
    if (nextAlarm == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
        ) {
            // Content
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .clickable {
                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra(AlarmService.EXTRA_ALARM_ID, nextAlarm.id)
                            putExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, nextAlarm.isVolumeBoostEnabled)
                        }
                        context.sendBroadcast(intent)
                    }
            )
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next alarm in $timeUntil",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(nextAlarm.hour, nextAlarm.minute),
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 42.sp),
                    color = Color.White
                )
                if (nextAlarm.label.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextAlarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableAlarmItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled || dismissState.currentValue != SwipeToDismissBoxValue.Settled
            val isOverThreshold = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            
            val color by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSwiping) MaterialTheme.colorScheme.error else Color.Transparent,
                label = "backgroundColor"
            )
            
            val scale by animateFloatAsState(
                targetValue = if (isOverThreshold) 1.2f else if (isSwiping) 0.8f else 0.0f,
                label = "iconScale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isSwiping) 1f else 0f,
                label = "iconAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = alpha),
                    modifier = Modifier.scale(scale)
                )
            }
        },
        content = {
            AlarmItemContent(alarm, onToggle, onClick, onDelete)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmItemContent(
    alarm: Alarm,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Alarm") },
            text = { Text("Are you sure you want to delete this alarm?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Alarm") },
                    onClick = { 
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Alarm", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        showDeleteConfirmation = true
                    }
                )
            }
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatTime(alarm.hour, alarm.minute),
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                            color = if (alarm.enabled) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = getAmPm(alarm.hour),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (alarm.enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (alarm.label.isNotEmpty()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (alarm.enabled) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (alarm.daysOfWeek.isNotEmpty()) {
                        DayChipsSmall(alarm.daysOfWeek, isEnabled = alarm.enabled)
                    } else {
                        Text(
                            text = "Once",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (alarm.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                    
                    val activeChallenges = alarm.challengeTypes.filter { it != ChallengeType.NONE }
                    if (activeChallenges.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val typeName = activeChallenges.joinToString(", ") { type ->
                            when (type) {
                                ChallengeType.QR_CODE -> "QR Code"
                                ChallengeType.MATH -> "Maths"
                                ChallengeType.REWRITE -> "Rewrite"
                                ChallengeType.STEP -> "Step"
                                ChallengeType.MAZE -> "Maze"
                                ChallengeType.MEMORY -> "Memory"
                                ChallengeType.SHAKE -> "Shake"
                                ChallengeType.TAP_CHALLENGE -> "Tap"
                                ChallengeType.SPELL_BEE -> "Spell Bee"
                                ChallengeType.PUZZLE -> "Puzzle"
                                ChallengeType.SCAN_SINK -> "Scan Sink"
                                ChallengeType.SCAN_OBJECT -> "Scan Object"
                                ChallengeType.SQUAT -> "Squat"
                                ChallengeType.PUSH_UP -> "Push Up"
                                ChallengeType.REVERSE_TYPING -> "Reverse Typing"
                                ChallengeType.AUDIO_MEMORY -> "Audio Memory"
                                ChallengeType.CLOCK_READING -> "Clock Reading"
                                ChallengeType.CHARGER -> "Charger"
                                ChallengeType.ROOM_LIGHT -> "Lights on"
                                else -> type.name
                            }
                        }
                        Text(
                            text = "Challenge: $typeName",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (alarm.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }

                SkeuomorphicSwitch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@Composable
fun DayChipsSmall(days: Set<Int>, isEnabled: Boolean) {
    // 1=Sun, 2=Mon, ...
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        dayLabels.forEachIndexed { index, label ->
            val dayId = index + 1
            val isActive = days.contains(dayId)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) {
                     if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                     Color.White.copy(alpha = 0.3f)
                },
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Alarms Set",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Add an alarm to wake up!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// Helpers
fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0 || hour == 12) 12 else hour % 12
    return String.format(Locale.getDefault(), "%d:%02d", h, minute)
}

fun getAmPm(hour: Int): String {
    return if (hour < 12) "AM" else "PM"
}

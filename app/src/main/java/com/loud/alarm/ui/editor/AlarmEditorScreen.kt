package com.loud.alarm.ui.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Wash
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.loud.alarm.billing.BillingViewModel
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.MathDifficulty
import com.loud.alarm.ui.challenge.CameraPreview
import com.loud.alarm.ui.challenge.ScannerOverlay
import com.loud.alarm.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.util.concurrent.Executors

private const val FREE_CHALLENGE_LIMIT = 2

private fun resolveRingtoneTitle(context: Context, soundUri: String?): String {
    val ringtoneUri = soundUri
        ?.takeIf { it.isNotBlank() }
        ?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    if (ringtoneUri == null) return "Default alarm sound"

    return runCatching {
        RingtoneManager.getRingtone(context, ringtoneUri)?.getTitle(context)
    }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: "Default alarm sound"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AlarmEditorScreen(
    onBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: AlarmEditorViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isSubscribed by billingViewModel.isSubscribed.collectAsState()
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        if (!data.hasExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)) {
            return@rememberLauncherForActivityResult
        }
        val pickedUri = IntentCompat.getParcelableExtra(
            data,
            RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
            Uri::class.java
        )
        viewModel.updateSoundUri(pickedUri?.toString())
    }
    val ringtoneTitle = remember(context, uiState.soundUri) {
        resolveRingtoneTitle(context, uiState.soundUri)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.loud.alarm.R.drawable.settings),
            contentDescription = "Background",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Edit Alarm") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val requiresSinkPhoto =
                                uiState.challengeTypes.contains(ChallengeType.SCAN_SINK) &&
                                    uiState.sinkImageUri.isNullOrBlank()
                            if (requiresSinkPhoto) {
                                Toast.makeText(
                                    context,
                                    "Add a sink photo to save this alarm.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@TextButton
                            }
                            val activeChallengeCount = uiState.challengeTypes.count { it != ChallengeType.NONE }
                            if (!isSubscribed && activeChallengeCount > FREE_CHALLENGE_LIMIT) {
                                Toast.makeText(
                                    context,
                                    "Free users can use up to $FREE_CHALLENGE_LIMIT challenges per alarm.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateToSubscription()
                            } else {
                                viewModel.saveAlarm(onBack)
                            }
                        }) {
                            Text(
                                "Save",
                                 fontWeight = FontWeight.Bold,
                                 color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            
            // ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            key(uiState.timePickerVersion) {
                WheelTimePicker(
                    hour = uiState.hour,
                    minute = uiState.minute,
                    onTimeChanged = { h, m -> viewModel.updateTime(h, m) }
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // ──────────────────────────────────────────────────
            var showRepeatDialog by remember { mutableStateOf(false) }
            val repeatSummary = remember(uiState.daysOfWeek) {
                getRepeatSummary(uiState.daysOfWeek)
            }
            
            GlassyCard(onClick = { showRepeatDialog = true }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Repeat",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            repeatSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ──────────────────────────────────────────────────
            if (showRepeatDialog) {
                RepeatPickerDialog(
                    selectedDays = uiState.daysOfWeek,
                    onDaysChanged = { newDays ->
                        viewModel.setDays(newDays)
                    },
                    onDismiss = { showRepeatDialog = false }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ──────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.label,
                onValueChange = { viewModel.updateLabel(it) },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassyCard(
                onClick = {
                    val existingUri = uiState.soundUri
                        ?.takeIf { it.isNotBlank() }
                        ?.let(Uri::parse)
                    val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultAlarmUri)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            existingUri ?: defaultAlarmUri
                        )
                    }
                    ringtonePickerLauncher.launch(pickerIntent)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Ringtone",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Ringtone",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                ringtoneTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ──────────────────────────────────────────────────
            GlassyCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Boost Volume",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (uiState.isVolumeBoostEnabled) "150% volume - extra loud"
                                else "Normal volume",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isVolumeBoostEnabled)
                                    MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isVolumeBoostEnabled,
                        onCheckedChange = { viewModel.updateVolumeBoost(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ──────────────────────────────────────────────────
            Text(
                "Wake Up Challenge",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = Color.White
            )
            
            GlassyCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    // ──────────────────────────────────────────────────
                    val challengeOptions = listOf(
                        ChallengeType.NONE to Triple(Icons.Default.Bedtime, "None", IconBlue),
                        ChallengeType.MATH to Triple(Icons.Default.Calculate, "Maths", IconRed),
                        ChallengeType.QR_CODE to Triple(Icons.Default.QrCodeScanner, "QR Code", IconPurple),
                        ChallengeType.REWRITE to Triple(Icons.Default.Edit, "Rewrite", IconYellow),
                        ChallengeType.STEP to Triple(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps", IconOrange),
                        ChallengeType.MAZE to Triple(Icons.Default.Gamepad, "Maze", IconGreen),
                        ChallengeType.MEMORY to Triple(Icons.Default.Psychology, "Memory", IconPink),
                        ChallengeType.SHAKE to Triple(Icons.Default.Vibration, "Shake", IconCyan),
                        ChallengeType.SPELL_BEE to Triple(Icons.Default.Spellcheck, "Spell Bee", IconAmber),
                        ChallengeType.PUZZLE to Triple(Icons.Default.Extension, "Puzzle", IconIndigo),
                        ChallengeType.SCAN_SINK to Triple(Icons.Default.Wash, "Scan Sink", IconTeal),
                        ChallengeType.SCAN_OBJECT to Triple(Icons.Default.CameraAlt, "Scan Object", IconLime)
                    )
                    val premiumChallengeTypes = setOf(
                        ChallengeType.STEP,
                        ChallengeType.MAZE,
                        ChallengeType.MEMORY,
                        ChallengeType.SHAKE,
                        ChallengeType.SPELL_BEE,
                        ChallengeType.PUZZLE,
                        ChallengeType.SCAN_SINK,
                        ChallengeType.SCAN_OBJECT
                    )
                    val columns = 2
                    for (i in challengeOptions.indices step columns) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (j in 0 until columns) {
                                if (i + j < challengeOptions.size) {
                                    val (type, extraArgs) = challengeOptions[i + j]
                                    val (icon, title, iconColor) = extraArgs
                                    val selected = uiState.challengeTypes.contains(type)
                                    val requiresSubscription = type in premiumChallengeTypes
                                    val isLocked = requiresSubscription && !isSubscribed && !selected
                                    val maxActiveChallenges = if (isSubscribed) Int.MAX_VALUE else FREE_CHALLENGE_LIMIT

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                if (isLocked) {
                                                    onNavigateToSubscription()
                                                } else {
                                                    val cameraChallenges = setOf(
                                                        ChallengeType.QR_CODE,
                                                        ChallengeType.SCAN_SINK,
                                                        ChallengeType.SCAN_OBJECT
                                                    )
                                                    
                                                    if (type in cameraChallenges && !cameraPermissionState.status.isGranted) {
                                                        cameraPermissionState.launchPermissionRequest()
                                                    }

                                                    val didUpdate = viewModel.toggleChallengeType(
                                                        type = type,
                                                        maxActiveChallenges = maxActiveChallenges
                                                    )
                                                    if (!didUpdate && !isSubscribed) {
                                                        Toast.makeText(
                                                            context,
                                                            "Free users can select up to $FREE_CHALLENGE_LIMIT challenges.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                            .padding(vertical = 14.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .shadow(
                                                            elevation = 8.dp,
                                                            shape = CircleShape,
                                                            ambientColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else iconColor.copy(alpha = 0.6f),
                                                            spotColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else iconColor.copy(alpha = 0.6f)
                                                        )
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.radialGradient(
                                                                colors = if (selected) listOf(
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                                ) else listOf(
                                                                    iconColor.copy(alpha = 0.35f),
                                                                    iconColor.copy(alpha = 0.08f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            width = 1.5.dp,
                                                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else iconColor.copy(alpha = 0.4f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = title,
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else iconColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                if (isLocked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = "Locked",
                                                        tint = Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .align(Alignment.BottomEnd)
                                                            .offset(x = 10.dp, y = 4.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (i + columns < challengeOptions.size) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    
                    // ──────────────────────────────────────────────────
                    if (uiState.challengeTypes.contains(ChallengeType.MATH)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Difficulty", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.animateContentSize()
                        ) {
                            MathDifficulty.values().forEach { diff ->
                                val selected = diff == uiState.mathDifficulty
                                val (description, example) = when (diff) {
                                    MathDifficulty.EASY -> "Addition & Subtraction" to "e.g.  18 + 9 = ?"
                                    MathDifficulty.MEDIUM -> "Multi-step expressions" to "e.g.  (14 + 8) × 3 = ?"
                                    MathDifficulty.HARD -> "Solve for x — equations" to "e.g.  3x + 7 = 22,  x = ?"
                                    MathDifficulty.EXTREME -> "Paper-worthy problems" to "e.g.  347 × 28 = ?  or  7x + 32 × 5 = 811"
                                }
                                
                                val scale by animateFloatAsState(
                                    targetValue = if (selected) 1.02f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "scale"
                                )
                                val bgColor by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                    label = "bgColor"
                                )
                                val borderColor by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                    label = "borderColor"
                                )
                                val dotSize by animateDpAsState(
                                    targetValue = if (selected) 12.dp else 8.dp,
                                    label = "dotSize"
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clickable { viewModel.updateMathDifficulty(diff) }
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(dotSize)
                                                .clip(CircleShape)
                                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f))
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                diff.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.7f)
                                            )
                                            
                                            AnimatedVisibility(
                                                visible = selected,
                                                enter = expandVertically(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                ) + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Black.copy(alpha = 0.3f))
                                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                            .padding(12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            example,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                                letterSpacing = 0.5.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            textAlign = TextAlign.Center
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
                    
                    // ──────────────────────────────────────────────────
                    if (uiState.challengeTypes.contains(ChallengeType.MAZE)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Maze Difficulty", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.animateContentSize()
                        ) {
                            MathDifficulty.values().forEach { diff ->
                                val selected = diff == uiState.mazeDifficulty
                                val description = when (diff) {
                                    MathDifficulty.EASY -> "Short paths and open routes"
                                    MathDifficulty.MEDIUM -> "Longer route with more blockers"
                                    MathDifficulty.HARD -> "Dense walls and dead ends"
                                    MathDifficulty.EXTREME -> "Maximum maze complexity"
                                }

                                val scale by animateFloatAsState(
                                    targetValue = if (selected) 1.02f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "scale"
                                )
                                val bgColor by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                    label = "bgColor"
                                )
                                val borderColor by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                    label = "borderColor"
                                )
                                val dotSize by animateDpAsState(
                                    targetValue = if (selected) 12.dp else 8.dp,
                                    label = "dotSize"
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clickable { viewModel.updateMazeDifficulty(diff) }
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(dotSize)
                                                .clip(CircleShape)
                                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f))
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                diff.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (uiState.challengeTypes.contains(ChallengeType.QR_CODE)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("QR Code Mode", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        var showSpecificQrDialog by remember { mutableStateOf(false) }
                        val isAnyQrCode = uiState.barcodeValue == null

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // ──────────────────────────────────────────────────
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateBarcodeValue(null) }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isAnyQrCode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.1f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isAnyQrCode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Any QR Code",
                                        tint = if (isAnyQrCode) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Any QR Code",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isAnyQrCode) MaterialTheme.colorScheme.primary
                                               else Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // ──────────────────────────────────────────────────
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showSpecificQrDialog = true }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (!isAnyQrCode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.1f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (!isAnyQrCode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = "Specific QR Code",
                                        tint = if (!isAnyQrCode) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Specific QR Code",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (!isAnyQrCode) MaterialTheme.colorScheme.primary
                                               else Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // ──────────────────────────────────────────────────
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isAnyQrCode) {
                            Text(
                                "Scan any QR code to dismiss alarm",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else {
                            Text(
                                "Required code: ${uiState.barcodeValue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // ──────────────────────────────────────────────────
                        if (showSpecificQrDialog) {
                            BarcodeScannerOverlay(
                                onBarcodeScanned = { scannedValue ->
                                    viewModel.updateBarcodeValue(scannedValue)
                                    showSpecificQrDialog = false
                                    Toast.makeText(context, "QR Code saved: $scannedValue", Toast.LENGTH_SHORT).show()
                                },
                                onDismiss = { showSpecificQrDialog = false }
                            )
                        }
                    }

                    // ──────────────────────────────────────────────────
                    if (uiState.challengeTypes.contains(ChallengeType.REWRITE)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Rewrite Text Mode", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = uiState.rewriteText,
                            onValueChange = { viewModel.updateRewriteText(it) },
                            label = { Text("Custom text to rewrite") },
                            placeholder = { Text("Leave empty for random words") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3
                        )
                    }

                    // ──────────────────────────────────────────────────
                    if (uiState.challengeTypes.contains(ChallengeType.STEP)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Step Target", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val stepOptions = listOf(10, 20, 30, 50, 100)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stepOptions.forEach { count ->
                                val isSelected = uiState.stepCount == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateStepCount(count) }
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.White.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count steps",
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────
                    if (uiState.challengeTypes.contains(ChallengeType.SHAKE)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Shake Count", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        val shakeOptions = listOf(15, 30, 50, 100)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            shakeOptions.forEach { count ->
                                val isSelected = uiState.shakeCount == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateShakeCount(count) }
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.White.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count shakes",
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.challengeTypes.contains(ChallengeType.SCAN_SINK)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scan Sink Setup", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        var showSinkCameraDialog by remember { mutableStateOf(false) }

                        Column {
                            Text(
                                "Take a photo of your sink so we know what to look for!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.sinkImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Sink image captured",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                "Point your camera at your sink to dismiss",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .clickable { showSinkCameraDialog = true }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Retake Photo", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                            .clickable { viewModel.updateSinkImageUri(null) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { showSinkCameraDialog = true }
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Wash,
                                            contentDescription = "Sink",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Tap to add photo of your sink",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            "Required to enable this challenge",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        if (showSinkCameraDialog) {
                            SinkPhotoCaptureOverlay(
                                onPhotoCaptured = { photoPath ->
                                    viewModel.updateSinkImageUri(photoPath)
                                    showSinkCameraDialog = false
                                    Toast.makeText(context, "Sink photo saved!", Toast.LENGTH_SHORT).show()
                                },
                                onDismiss = { showSinkCameraDialog = false }
                            )
                        }
                    }

                    // ──────────────────────────────────────────────────
                    if (uiState.challengeTypes.contains(ChallengeType.SCAN_OBJECT)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select Object to Scan", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Pick an object you have at home. You'll need to point your camera at it to dismiss the alarm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val objectOptions = listOf(
                            Triple(Icons.Default.Brush, "Toothbrush", IconTeal),
                            Triple(Icons.Default.Wash, "Sink", IconBlue),
                            Triple(Icons.Default.Coffee, "Coffee cup", IconOrange),
                            Triple(Icons.Default.LocalDining, "Bowl", IconRed),
                            Triple(Icons.Default.SportsMma, "Shoe", IconGreen),
                            Triple(Icons.AutoMirrored.Filled.MenuBook, "Book", IconPurple),
                            Triple(Icons.Default.Yard, "Plant", IconLime),
                            Triple(Icons.Default.Laptop, "Laptop", Color.LightGray),
                            Triple(Icons.Default.Fastfood, "Fruit", IconYellow),
                            Triple(Icons.Default.LocalBar, "Bottle", IconCyan),
                            Triple(Icons.Default.Watch, "Watch", IconIndigo),
                            Triple(Icons.Default.VpnKey, "Key", IconAmber),
                            Triple(Icons.Default.Backpack, "Backpack", IconPink),
                            Triple(Icons.Default.Chair, "Chair", SecondaryOrange),
                            Triple(Icons.Default.DoorFront, "Door", PrimaryAccent)
                        )

                        val objColumns = 3
                        for (i in objectOptions.indices step objColumns) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (j in 0 until objColumns) {
                                    if (i + j < objectOptions.size) {
                                        val (objIcon, label, iconColor) = objectOptions[i + j]
                                        val isSelected = uiState.scanObjectLabel.equals(label, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else Color.White.copy(alpha = 0.05f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { viewModel.updateScanObjectLabel(label) }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .shadow(
                                                            elevation = 6.dp,
                                                            shape = CircleShape,
                                                            ambientColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else iconColor.copy(alpha = 0.5f),
                                                            spotColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else iconColor.copy(alpha = 0.5f)
                                                        )
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.radialGradient(
                                                                colors = if (isSelected) listOf(
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                                ) else listOf(
                                                                    iconColor.copy(alpha = 0.35f),
                                                                    iconColor.copy(alpha = 0.08f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else iconColor.copy(alpha = 0.35f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = objIcon,
                                                        contentDescription = label,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else iconColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            if (i + objColumns < objectOptions.size) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        if (uiState.scanObjectLabel.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Selected: ${uiState.scanObjectLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Please select an object",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ──────────────────────────────────────────────────
            var showWakeUpCheckInfo by remember { mutableStateOf(false) }
            
            Text(
                "Wake Up Check",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = Color.White
            )
            
            GlassyCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Follow-up Notification",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap notification to confirm or alarm rings again",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { showWakeUpCheckInfo = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val options = listOf(0, 1, 2, 5, 10, 15, 30)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { mins ->
                            val isSelected = uiState.wakeUpCheckMinutes == mins
                            val text = if (mins == 0) "Off" else "${mins}m"
                            val isLocked = mins > 0 && !isSubscribed
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { 
                                        if (isLocked) onNavigateToSubscription()
                                        else viewModel.updateWakeUpCheckMinutes(mins) 
                                    }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.1f)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = text,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isLocked) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showWakeUpCheckInfo) {
                WakeUpCheckInfoDialog(onDismiss = { showWakeUpCheckInfo = false })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }

}

@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
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
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        content()
    }
}

// ──────────────────────────────────────────────────
// ──────────────────────────────────────────────────
// ──────────────────────────────────────────────────
@Composable
fun WheelTimePicker(
    hour: Int,           // ──────────────────────────────────────────────────
    minute: Int,         // ──────────────────────────────────────────────────
    onTimeChanged: (Int, Int) -> Unit
) {
    val initDisplay12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val initIsAm = hour < 12

    val hourItems = (1..12).map { it.toString() }
    val minuteItems = (0..59).map { String.format("%02d", it) }
    val amPmItems = listOf("AM", "PM")

    val initialHourIndex = initDisplay12 - 1
    val initialMinuteIndex = minute
    val initialAmPmIndex = if (initIsAm) 0 else 1

    // ──────────────────────────────────────────────────
    // ──────────────────────────────────────────────────
    // ──────────────────────────────────────────────────
    val current12HourRef = remember { mutableStateOf(initDisplay12) }
    val currentMinuteRef = remember { mutableStateOf(minute) }
    val currentIsAmRef  = remember { mutableStateOf(initIsAm) }

    // ──────────────────────────────────────────────────
    // ──────────────────────────────────────────────────
    // ──────────────────────────────────────────────────
    val initCount = remember { mutableStateOf(0) }

    // ──────────────────────────────────────────────────
    fun emitTime() {
        if (initCount.value < 3) return          // ──────────────────────────────────────────────────
        val h12 = current12HourRef.value
        val min = currentMinuteRef.value
        val am  = currentIsAmRef.value
        val h24 = if (am) {
            if (h12 == 12) 0 else h12
        } else {
            if (h12 == 12) 12 else h12 + 12
        }
        onTimeChanged(h24, min)
    }

    val itemHeight = 44.dp
    val visibleCount = 5
    
    // ──────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // ──────────────────────────────────────────────────
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black,
                        0.6f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(itemHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ──────────────────────────────────────────────────
            WheelColumn(
                items = hourItems,
                initialIndex = initialHourIndex,
                itemHeight = itemHeight,
                visibleCount = visibleCount,
                isLooping = true,
                modifier = Modifier.width(64.dp),
                onSelectedIndexChanged = { idx, isInit ->
                    val selected12h = idx + 1
                    current12HourRef.value = selected12h
                    if (isInit) initCount.value++ else emitTime()
                }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // ──────────────────────────────────────────────────
            WheelColumn(
                items = minuteItems,
                initialIndex = initialMinuteIndex,
                itemHeight = itemHeight,
                visibleCount = visibleCount,
                isLooping = true,
                modifier = Modifier.width(64.dp),
                onSelectedIndexChanged = { idx, isInit ->
                    currentMinuteRef.value = idx
                    if (isInit) initCount.value++ else emitTime()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ──────────────────────────────────────────────────
            WheelColumn(
                items = amPmItems,
                initialIndex = initialAmPmIndex,
                itemHeight = itemHeight,
                visibleCount = visibleCount,
                isLooping = false,
                modifier = Modifier.width(64.dp),
                onSelectedIndexChanged = { idx, isInit ->
                    currentIsAmRef.value = (idx == 0)
                    if (isInit) initCount.value++ else emitTime()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    itemHeight: Dp,
    visibleCount: Int,
    isLooping: Boolean = true,
    modifier: Modifier = Modifier,
    onSelectedIndexChanged: (index: Int, isInitial: Boolean) -> Unit
) {
    val paddingCount = visibleCount / 2
    val repeatCount = 1000
    val totalLoopSize = items.size * repeatCount

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (isLooping) {
            // ──────────────────────────────────────────────────
            val middleBase = (repeatCount / 2) * items.size
            middleBase + initialIndex - paddingCount
        } else {
            initialIndex  // ──────────────────────────────────────────────────
        }
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // ──────────────────────────────────────────────────
    val hasEmittedInit = remember { mutableStateOf(false) }

    // ──────────────────────────────────────────────────
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index
        }.distinctUntilChanged().collect { centerIndex ->
            if (centerIndex != null) {
                val realIndex = if (isLooping) {
                    centerIndex % items.size
                } else {
                    centerIndex - paddingCount
                }
                if (realIndex in items.indices) {
                    val isInit = !hasEmittedInit.value
                    hasEmittedInit.value = true
                    onSelectedIndexChanged(realIndex, isInit)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(itemHeight * visibleCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val listSize = if (isLooping) totalLoopSize else items.size + paddingCount * 2
        items(listSize) { index ->
            val realIndex = if (isLooping) {
                index % items.size
            } else {
                index - paddingCount
            }

            val text = if (realIndex in items.indices) items[realIndex] else ""

            val isCenter by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter = layoutInfo.viewportStartOffset +
                            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                    val closestItem = layoutInfo.visibleItemsInfo
                        .minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - viewportCenter) }
                    closestItem?.index == index
                }
            }

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = if (isCenter) FontWeight.Medium else FontWeight.Normal,
                            letterSpacing = 1.sp
                        ),
                        color = if (isCenter) Color.White else Color(0xFF6B6B6B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────
// ──────────────────────────────────────────────────
// ──────────────────────────────────────────────────
@Composable
fun RepeatPickerDialog(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val allDays = setOf(1, 2, 3, 4, 5, 6, 7)
    val weekdays = setOf(2, 3, 4, 5, 6)
    val weekends = setOf(1, 7)
    val presets = listOf(emptySet(), allDays, weekdays, weekends)

    var localDays by remember { mutableStateOf(selectedDays) }

    // Custom is expanded when current selection doesn't match any preset
    val isCustom = localDays !in presets
    var customExpanded by remember { mutableStateOf(isCustom) }

    // When a preset is selected, collapse custom
    // When custom is toggled to match a preset, keep custom open (user is actively editing)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    "Repeat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Preset options
                RepeatOptionRow(
                    label = "Once",
                    subtitle = "Ring only one time",
                    isSelected = localDays.isEmpty() && !customExpanded,
                    onClick = {
                        localDays = emptySet()
                        customExpanded = false
                    }
                )

                RepeatOptionRow(
                    label = "Every Day",
                    subtitle = "Mon – Sun",
                    isSelected = localDays == allDays && !customExpanded,
                    onClick = {
                        localDays = allDays
                        customExpanded = false
                    }
                )

                RepeatOptionRow(
                    label = "Weekdays",
                    subtitle = "Mon – Fri",
                    isSelected = localDays == weekdays && !customExpanded,
                    onClick = {
                        localDays = weekdays
                        customExpanded = false
                    }
                )

                RepeatOptionRow(
                    label = "Weekends",
                    subtitle = "Sat & Sun",
                    isSelected = localDays == weekends && !customExpanded,
                    onClick = {
                        localDays = weekends
                        customExpanded = false
                    }
                )

                // Custom option as a radio row (like the presets)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            customExpanded = true
                            // If currently a preset with no days, seed with empty for user to pick
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = customExpanded,
                        onClick = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Custom",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (customExpanded) MaterialTheme.colorScheme.onSurface
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Pick specific days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    // Chevron indicator
                    Icon(
                        imageVector = if (customExpanded) Icons.Default.KeyboardArrowUp
                                     else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (customExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Expandable custom day toggles
                AnimatedVisibility(
                    visible = customExpanded,
                    enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 34.dp, top = 4.dp) // Indent under the radio button
                    ) {
                        val dayNames = listOf(
                            2 to "Monday",
                            3 to "Tuesday",
                            4 to "Wednesday",
                            5 to "Thursday",
                            6 to "Friday",
                            7 to "Saturday",
                            1 to "Sunday"
                        )
                        dayNames.forEach { (dayId, name) ->
                            val isOn = localDays.contains(dayId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        localDays = if (isOn) localDays - dayId else localDays + dayId
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isOn) MaterialTheme.colorScheme.onSurface
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = isOn,
                                    onCheckedChange = {
                                        localDays = if (it) localDays + dayId else localDays - dayId
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDaysChanged(localDays)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatOptionRow(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getRepeatSummary(days: Set<Int>): String {
    if (days.isEmpty()) return "Once"
    if (days == setOf(1, 2, 3, 4, 5, 6, 7)) return "Every Day"
    if (days == setOf(2, 3, 4, 5, 6)) return "Weekdays"
    if (days == setOf(1, 7)) return "Weekends"

    val shortNames = mapOf(
        1 to "Sun", 2 to "Mon", 3 to "Tue",
        4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat"
    )
    // ──────────────────────────────────────────────────
    val ordered = listOf(2, 3, 4, 5, 6, 7, 1).filter { it in days }
    return ordered.joinToString(", ") { shortNames[it] ?: "" }
}

val androidx.compose.material3.ColorScheme.success: Color
    get() = Color(0xFF4CAF50) // ──────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerOverlay(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    var hasScanned by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(
                    onBarcodeScanned = { barcode ->
                        val raw = barcode.rawValue
                        if (raw != null && !hasScanned) {
                            hasScanned = true
                            onBarcodeScanned(raw)
                        }
                    }
                )

                ScannerOverlay()

                // ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan Your QR Code",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Point the camera at the QR code you want to use for this alarm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            } else {
                // ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Camera permission is required to scan QR codes",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SinkPhotoCaptureOverlay(
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCapturing by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted, lifecycleOwner, previewView) {
        if (!cameraPermissionState.status.isGranted) {
            isCameraReady = false
            return@LaunchedEffect
        }
        val currentPreviewView = previewView ?: return@LaunchedEffect
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(currentPreviewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                isCameraReady = true
            } catch (_: Exception) {
                isCameraReady = false
                Toast.makeText(
                    context,
                    "Unable to open camera right now",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                ScannerOverlay()

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Capture Your Sink",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Point the camera at your sink and tap Capture",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (!isCameraReady) {
                                Toast.makeText(
                                    context,
                                    "Camera is still getting ready",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            if (isCapturing) return@Button
                            isCapturing = true

                            val photoFile = File(
                                context.filesDir,
                                "sink_reference_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions
                                .Builder(photoFile)
                                .build()

                            imageCapture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(
                                        outputFileResults: ImageCapture.OutputFileResults
                                    ) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            onPhotoCaptured(photoFile.absolutePath)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            Toast.makeText(
                                                context,
                                                "Failed to capture photo: ${exception.message ?: "Unknown error"}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        },
                        enabled = !isCapturing && isCameraReady,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        val captureLabel = when {
                            isCapturing -> "Capturing..."
                            !isCameraReady -> "Preparing Camera..."
                            else -> "Capture"
                        }
                        Text(captureLabel)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Camera permission is required to capture your sink photo",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradeFeatureRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ──────────────────────────────────────────────────

@Composable
fun WakeUpCheckInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Wake Up Check",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "After dismissing the alarm, tap the follow-up notification. If you don't respond within 5 minutes, the alarm rings again!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    var stepIndex by remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(2000)
                            stepIndex = (stepIndex + 1) % 6
                        }
                    }
                    
                    val steps = listOf(
                        Triple(Icons.Default.NotificationsActive, "Alarm Rings", IconOrange),
                        Triple(Icons.Default.CheckCircle, "You dismiss it", IconGreen),
                        Triple(Icons.Default.Hotel, "Delay minutes pass...", IconPurple),
                        Triple(Icons.Default.Smartphone, "Notification: Tap to confirm!", IconBlue),
                        Triple(Icons.Default.TouchApp, "Tap within 5 min → Confirmed!", IconTeal),
                        Triple(Icons.Default.NotificationsActive, "Didn't tap? Alarm rings again!", IconRed)
                    )
                    
                    AnimatedContent(
                        targetState = stepIndex,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith 
                            (slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "WakeUpAnimation"
                    ) { index ->
                        val (icon, text, stepColor) = steps[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = CircleShape,
                                        ambientColor = stepColor.copy(alpha = 0.6f),
                                        spotColor = stepColor.copy(alpha = 0.6f)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                stepColor.copy(alpha = 0.4f),
                                                stepColor.copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = stepColor.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = stepColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text, 
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it")
                }
            }
        }
    }
}





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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp as vecDp
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
import androidx.compose.ui.text.withStyle
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.loud.alarm.billing.BillingViewModel
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.MathDifficulty
import com.loud.alarm.data.SquatDetectionMode
import com.loud.alarm.ui.challenge.CameraPreview
import com.loud.alarm.ui.challenge.ScannerOverlay
import com.loud.alarm.ui.challenge.allScanObjects
import com.loud.alarm.ui.challenge.hasTargetLabelMatch
import com.loud.alarm.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.util.concurrent.Executors

private const val SINK_REFERENCE_LABELER_CONFIDENCE = 0.05f

/**
 * Custom squat icon — a stick-figure person in a squatting position
 * (bent knees, lowered body) rendered in a 24×24 viewport.
 */
private val SquatIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Squat",
        defaultWidth = 24.vecDp,
        defaultHeight = 24.vecDp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Head (circle)
        path(fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)) {
            // Circle at (12, 4) radius 2.2
            moveTo(12f, 1.8f)
            arcTo(2.2f, 2.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 6.2f)
            arcTo(2.2f, 2.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 1.8f)
            close()
        }
        // Body — torso leaning forward + bent legs in squat
        path(
            fill = null,
            stroke = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
        ) {
            // Torso: from neck down, leaning forward slightly
            moveTo(12f, 6.5f)
            lineTo(12f, 13f)

            // Left leg: thigh goes out-left and down, then shin bends back
            moveTo(12f, 13f)
            lineTo(8.5f, 16f)
            lineTo(7f, 20f)

            // Right leg: thigh goes out-right and down, then shin bends back
            moveTo(12f, 13f)
            lineTo(15.5f, 16f)
            lineTo(17f, 20f)

            // Left arm: reaching forward/down (balance pose)
            moveTo(12f, 8.5f)
            lineTo(8f, 11f)

            // Right arm: reaching forward/down
            moveTo(12f, 8.5f)
            lineTo(16f, 11f)
        }
    }.build()
}

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
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
    val activityRecognitionPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        rememberPermissionState(android.Manifest.permission.ACTIVITY_RECOGNITION)
    } else null
    
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
    var ringtoneTitle by remember { mutableStateOf("Default alarm sound") }
    LaunchedEffect(uiState.soundUri) {
        ringtoneTitle = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            resolveRingtoneTitle(context, uiState.soundUri)
        }
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

                            val activeChallengeCount = uiState.challengeTypes.count { it != ChallengeType.NONE }
                            if (!isSubscribed && activeChallengeCount > FREE_CHALLENGE_LIMIT) {
                                Toast.makeText(
                                    context,
                                    "Go premium to select more challenges at once",
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                color = Color.White
            )
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    append("Tap to select • ")
                    withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Long press for settings")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = Color.White.copy(alpha = 0.5f)
            )
            
            GlassyCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    // ──────────────────────────────────────────────────
                    val challengeOptions = listOf(
                        ChallengeType.NONE to Triple(Icons.Default.Bedtime, "None", IconBlue),
                        ChallengeType.MATH to Triple(Icons.Default.Calculate, "Maths", IconRed),
                        ChallengeType.QR_CODE to Triple(Icons.Default.QrCodeScanner, "QR Code", IconPurple),
                        ChallengeType.REWRITE to Triple(Icons.Default.Edit, "Rewrite", IconYellow),
                        ChallengeType.TAP_CHALLENGE to Triple(Icons.Default.TouchApp, "Tap", IconGreen),
                        ChallengeType.CHARGER to Triple(Icons.Default.Power, "Charger", IconGreen),
                        ChallengeType.CLOCK_READING to Triple(Icons.Default.AccessTime, "Clock Reading", IconAmber),
                        ChallengeType.STEP to Triple(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps", IconOrange),
                        ChallengeType.MAZE to Triple(Icons.Default.Gamepad, "Maze", IconGreen),
                        ChallengeType.MEMORY to Triple(Icons.Default.Psychology, "Memory", IconPink),
                        ChallengeType.SHAKE to Triple(Icons.Default.Vibration, "Shake", IconCyan),
                        ChallengeType.SPELL_BEE to Triple(Icons.Default.Spellcheck, "Spell Bee", IconAmber),
                        ChallengeType.PUZZLE to Triple(Icons.Default.Extension, "Puzzle", IconIndigo),
                        ChallengeType.SCAN_SINK to Triple(Icons.Default.Wash, "Scan Sink", IconTeal),
                        ChallengeType.SCAN_OBJECT to Triple(Icons.Default.CameraAlt, "Scan Object", IconLime),
                        ChallengeType.SQUAT to Triple(SquatIcon, "Squat", IconDeepOrange),
                        ChallengeType.PUSH_UP to Triple(Icons.Default.FitnessCenter, "Push Up", IconDeepPurple),
                        ChallengeType.REVERSE_TYPING to Triple(Icons.Default.Keyboard, "Reverse Typing", IconBrown),
                        ChallengeType.AUDIO_MEMORY to Triple(Icons.Default.Headphones, "Audio Memory", IconLightBlue)
                    )
                    val premiumChallengeTypes = setOf(
                        ChallengeType.STEP,
                        ChallengeType.MAZE,
                        ChallengeType.MEMORY,
                        ChallengeType.SHAKE,
                        ChallengeType.SPELL_BEE,
                        ChallengeType.PUZZLE,
                        ChallengeType.SCAN_SINK,
                        ChallengeType.SCAN_OBJECT,
                        ChallengeType.SQUAT,
                        ChallengeType.PUSH_UP,
                        ChallengeType.REVERSE_TYPING,
                        ChallengeType.AUDIO_MEMORY
                    )
                    var showSettingsForChallenge by remember { mutableStateOf<ChallengeType?>(null) }
                    var showPreviewForChallenge by remember { mutableStateOf<ChallengeType?>(null) }
                    
                    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
                    val customViewConfig = remember(viewConfig) {
                        object : androidx.compose.ui.platform.ViewConfiguration by viewConfig {
                            override val longPressTimeoutMillis: Long
                                get() = 700L
                        }
                    }
                    
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalViewConfiguration provides customViewConfig
                    ) {
                        val columns = 3
                        for (i in challengeOptions.indices step columns) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            .graphicsLayer { alpha = if (isLocked) 0.65f else 1f }
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    if (isLocked) {
                                                        onNavigateToSubscription()
                                                    } else {
                                                        val cameraChallenges = setOf(
                                                            ChallengeType.QR_CODE,
                                                            ChallengeType.SCAN_SINK,
                                                            ChallengeType.SCAN_OBJECT,
                                                            ChallengeType.PUSH_UP
                                                        )
                                                        if (type in cameraChallenges && !cameraPermissionState.status.isGranted) {
                                                            cameraPermissionState.launchPermissionRequest()
                                                        }
                                                        if (type == ChallengeType.STEP && activityRecognitionPermissionState != null &&
                                                            !activityRecognitionPermissionState.status.isGranted
                                                        ) {
                                                            activityRecognitionPermissionState.launchPermissionRequest()
                                                        }
                                                        val didUpdate = viewModel.toggleChallengeType(
                                                            type = type,
                                                            maxActiveChallenges = maxActiveChallenges
                                                        )
                                                        if (!didUpdate && !isSubscribed) {
                                                            Toast.makeText(
                                                                context,
                                                                "Go premium to select more challenges at once",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (type != ChallengeType.NONE) {
                                                        showSettingsForChallenge = type
                                                    }
                                                }
                                            )
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .shadow(
                                                            elevation = 6.dp,
                                                            shape = CircleShape,
                                                            ambientColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else iconColor.copy(alpha = 0.5f),
                                                            spotColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else iconColor.copy(alpha = 0.5f)
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
                                                        modifier = Modifier.size(20.dp)
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
                                                            .offset(x = 6.dp, y = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val subtitle = if (selected && type != ChallengeType.NONE) {
                                                getChallengeSettingsSummary(type, uiState)
                                                    ?: getChallengeTileSubtitle(type)
                                            } else {
                                                getChallengeTileSubtitle(type)
                                            }

                                            // Fixed-height text area so all cards stay the same size
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.height(38.dp)
                                            ) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = subtitle,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = if (selected && type != ChallengeType.NONE) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                                    } else {
                                                        Color.White.copy(alpha = 0.52f)
                                                    },
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (i + columns < challengeOptions.size) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    }

                    // ── Selected challenges: configure strip ──
                    val selectedWithSettings = uiState.challengeTypes.filter { challengeHasSettings(it) }
                    if (selectedWithSettings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Configure Challenges",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Medium
                                )
                                selectedWithSettings.forEach { type ->
                                    val opt = challengeOptions.firstOrNull { it.first == type }
                                    if (opt != null) {
                                        val (_, info) = opt
                                        val (icn, ttl, clr) = info
                                        val sum = getChallengeSettingsSummary(type, uiState)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showSettingsForChallenge = type }
                                                .background(Color.White.copy(alpha = 0.04f))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(icn, null, tint = clr, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(ttl, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                                                if (sum != null) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(sum, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                }
                                            }
                                            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isSubscribed) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                androidx.compose.material.icons.Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Free users can long-press premium challenges to preview them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // ── Settings dialog routing ──
                    showSettingsForChallenge?.let { type ->
                        val opt = challengeOptions.firstOrNull { it.first == type }
                        if (opt != null) {
                            val (_, info) = opt
                            val (icn, ttl, clr) = info
                            ChallengeSettingsDialog(
                                title = "$ttl Settings",
                                icon = icn,
                                iconColor = clr,
                                onDismiss = { showSettingsForChallenge = null },
                                onPreviewClick = { showPreviewForChallenge = type }
                            ) {
                                when (type) {
                                    ChallengeType.MATH -> {
                                        DifficultySelector(
                                            label = "Difficulty",
                                            selected = uiState.mathDifficulty,
                                            onSelect = { viewModel.updateMathDifficulty(it) },
                                            descriptions = mapOf(
                                                MathDifficulty.EASY to "Addition & Subtraction",
                                                MathDifficulty.MEDIUM to "Multi-step expressions",
                                                MathDifficulty.HARD to "Solve for x — equations",
                                                MathDifficulty.EXTREME to "Paper-worthy problems"
                                            ),
                                            examples = mapOf(
                                                MathDifficulty.EASY to "e.g.  18 + 9 = ?",
                                                MathDifficulty.MEDIUM to "e.g.  (14 + 8) × 3 = ?",
                                                MathDifficulty.HARD to "e.g.  3x + 7 = 22,  x = ?",
                                                MathDifficulty.EXTREME to "e.g.  347 × 28 = ?"
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CountChipRow(
                                            label = "Number of Questions",
                                            options = listOf(1, 2, 3, 5, 10),
                                            selected = uiState.mathQuestionCount,
                                            onSelect = { viewModel.updateMathQuestionCount(it) },
                                            suffix = "questions"
                                        )
                                    }
                                    ChallengeType.MAZE -> {
                                        DifficultySelector(
                                            label = "Maze Difficulty",
                                            selected = uiState.mazeDifficulty,
                                            onSelect = { viewModel.updateMazeDifficulty(it) },
                                            descriptions = mapOf(
                                                MathDifficulty.EASY to "Short paths, open routes",
                                                MathDifficulty.MEDIUM to "Longer route, more blockers",
                                                MathDifficulty.HARD to "Dense walls and dead ends",
                                                MathDifficulty.EXTREME to "Maximum maze complexity"
                                            )
                                        )
                                    }
                                    ChallengeType.PUZZLE -> {
                                        DifficultySelector(
                                            label = "Puzzle Difficulty",
                                            selected = uiState.puzzleDifficulty,
                                            onSelect = { viewModel.updatePuzzleDifficulty(it) },
                                            descriptions = mapOf(
                                                MathDifficulty.EASY to "2×2 grid — quick warm-up",
                                                MathDifficulty.MEDIUM to "3×3 grid — classic puzzle",
                                                MathDifficulty.HARD to "4×4 grid — serious challenge",
                                                MathDifficulty.EXTREME to "5×5 grid — brain melter"
                                            )
                                        )
                                    }
                                    ChallengeType.MEMORY -> {
                                        DifficultySelector(
                                            label = "Memory Difficulty",
                                            selected = uiState.memoryDifficulty,
                                            onSelect = { viewModel.updateMemoryDifficulty(it) },
                                            descriptions = mapOf(
                                                MathDifficulty.EASY to "3×3 grid, 3 tile sequence",
                                                MathDifficulty.MEDIUM to "3×3 grid, 4 tile sequence",
                                                MathDifficulty.HARD to "4×4 grid, 5 tile sequence",
                                                MathDifficulty.EXTREME to "4×4 grid, 6 tile sequence"
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CountChipRow(
                                            label = "Number of Rounds",
                                            options = listOf(1, 2, 3, 5),
                                            selected = uiState.memoryChallengeCount,
                                            onSelect = { viewModel.updateMemoryChallengeCount(it) },
                                            suffix = "rounds"
                                        )
                                    }
                                    ChallengeType.STEP -> {
                                        CountChipRow(label = "Step Target", options = listOf(10, 20, 30, 50, 100), selected = uiState.stepCount, onSelect = { viewModel.updateStepCount(it) }, suffix = "steps")
                                    }
                                    ChallengeType.SHAKE -> {
                                        CountChipRow(label = "Shake Count", options = listOf(15, 30, 50, 100), selected = uiState.shakeCount, onSelect = { viewModel.updateShakeCount(it) }, suffix = "shakes")
                                    }
                                    ChallengeType.TAP_CHALLENGE -> {
                                        CountChipRow(label = "Tap Target", options = listOf(10, 20, 30, 50, 75, 100), selected = uiState.tapCount, onSelect = { viewModel.updateTapCount(it) }, suffix = "taps")
                                    }
                                    ChallengeType.SQUAT -> {
                                        Text("Detection Mode", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(SquatDetectionMode.CAMERA to "Camera", SquatDetectionMode.MOTION to "Motion").forEach { (mode, label) ->
                                                val mSel = uiState.squatDetectionMode == mode
                                                Box(
                                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                        .clickable { viewModel.updateSquatDetectionMode(mode); if (mode == SquatDetectionMode.CAMERA && !cameraPermissionState.status.isGranted) cameraPermissionState.launchPermissionRequest() }
                                                        .background(if (mSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                                        .border(1.dp, if (mSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                        .padding(12.dp), contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        label,
                                                        color = if (mSel) MaterialTheme.colorScheme.primary else Color.White,
                                                        fontWeight = if (mSel) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "📷 Camera mode is recommended for best accuracy",
                                            color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 11.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CountChipRow(label = "Squat Target", options = listOf(5, 10, 15, 20, 30, 50, 100), selected = uiState.squatCount, onSelect = { viewModel.updateSquatCount(it) }, suffix = "squats")
                                    }
                                    ChallengeType.PUSH_UP -> {
                                        CountChipRow(label = "Push Up Target", options = listOf(5, 10, 15, 20, 30, 50, 100), selected = uiState.pushUpCount, onSelect = { viewModel.updatePushUpCount(it) }, suffix = "push-ups")
                                    }
                                    ChallengeType.REVERSE_TYPING -> {
                                        CountChipRow(label = "Typing Rounds", options = listOf(1, 2, 3, 5, 7), selected = uiState.reverseTypingCount, onSelect = { viewModel.updateReverseTypingCount(it) }, suffix = "rounds")
                                    }
                                    ChallengeType.REWRITE -> {
                                        var customSelected by remember { mutableStateOf(uiState.rewriteText.isNotEmpty()) }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(true to "Random Text", false to "Custom Text").forEach { (isRand, label) ->
                                                val isSelected = isRand == !customSelected
                                                Box(
                                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                                                        customSelected = !isRand
                                                        if (isRand) viewModel.updateRewriteText("")
                                                    }
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp), contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                }
                                            }
                                        }
                                        if (customSelected) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = uiState.rewriteText,
                                                onValueChange = { viewModel.updateRewriteText(it) },
                                                label = { Text("Custom text") },
                                                placeholder = { Text("Enter your text") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = false,
                                                maxLines = 3
                                            )
                                        }
                                    }
                                    ChallengeType.QR_CODE -> {
                                        var showQrScanner by remember { mutableStateOf(false) }
                                        val isAnyQr = uiState.barcodeValue == null
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(true to "Any QR", false to "Specific QR").forEach { (isAny, label) ->
                                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { if (isAny) viewModel.updateBarcodeValue(null) else showQrScanner = true }
                                                    .background(if (isAnyQr == isAny) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                                    .border(1.dp, if (isAnyQr == isAny) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp), contentAlignment = Alignment.Center
                                                ) { Text(label, color = if (isAnyQr == isAny) MaterialTheme.colorScheme.primary else Color.White, fontWeight = if (isAnyQr == isAny) FontWeight.Bold else FontWeight.Normal) }
                                            }
                                        }
                                        if (!isAnyQr) { Spacer(modifier = Modifier.height(8.dp)); Text("Saved: ${uiState.barcodeValue}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                                        if (showQrScanner) { BarcodeScannerOverlay(onBarcodeScanned = { v -> viewModel.updateBarcodeValue(v); showQrScanner = false; Toast.makeText(context, "QR saved: $v", Toast.LENGTH_SHORT).show() }, onDismiss = { showQrScanner = false }) }
                                    }
                                    ChallengeType.SCAN_SINK -> {
                                        var showSinkCamera by remember { mutableStateOf(false) }
                                        val isAnySink = uiState.sinkImageUri.isNullOrBlank()
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(true to "Any Sink", false to "Specific Sink").forEach { (isAny, label) ->
                                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { if (isAny) viewModel.updateSinkImageUri(null) else showSinkCamera = true }
                                                    .background(if (isAnySink == isAny) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                                    .border(1.dp, if (isAnySink == isAny) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp), contentAlignment = Alignment.Center
                                                ) { Text(label, color = if (isAnySink == isAny) MaterialTheme.colorScheme.primary else Color.White, fontWeight = if (isAnySink == isAny) FontWeight.Bold else FontWeight.Normal) }
                                            }
                                        }
                                        if (showSinkCamera) { SinkPhotoCaptureOverlay(onPhotoCaptured = { p -> viewModel.updateSinkImageUri(p); showSinkCamera = false; Toast.makeText(context, "Specific sink saved!", Toast.LENGTH_SHORT).show() }, onDismiss = { showSinkCamera = false }) }
                                    }
                                    ChallengeType.SCAN_OBJECT -> {
                                        val isRandom = uiState.scanObjectLabel == "RANDOM"
                                        val defaultScanObjectLabel = allScanObjects.first().label
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(false to "Pick One", true to "Random").forEach { (rand, label) ->
                                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                                                    viewModel.updateScanObjectLabel(
                                                        if (rand) {
                                                            "RANDOM"
                                                        } else {
                                                            uiState.scanObjectLabel
                                                                .takeUnless { it == "RANDOM" || it.isBlank() }
                                                                ?: defaultScanObjectLabel
                                                        }
                                                    )
                                                }
                                                    .background(if (isRandom == rand) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                                    .border(1.dp, if (isRandom == rand) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp), contentAlignment = Alignment.Center
                                                ) { Text(label, color = if (isRandom == rand) MaterialTheme.colorScheme.primary else Color.White, fontWeight = if (isRandom == rand) FontWeight.Bold else FontWeight.Normal) }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(IconAmber.copy(alpha = 0.12f))
                                                .border(
                                                    width = 1.dp,
                                                    color = IconAmber.copy(alpha = 0.32f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = IconAmber,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Scan Object is still in development and may malfunction. A math fallback is included if detection fails.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.86f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = if (isRandom) "Exclude objects from random pick" else "Choose the object to scan",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isRandom) {
                                                val available = allScanObjects.size - uiState.scanObjectExcluded.size
                                                "${uiState.scanObjectExcluded.size} excluded, ${available.coerceAtLeast(0)} available"
                                            } else {
                                                "Selected: ${uiState.scanObjectLabel.ifBlank { allScanObjects.first().label }}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            allScanObjects.chunked(2).forEach { rowObjects ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowObjects.forEach { scanObject ->
                                                        val isExcluded = scanObject.label in uiState.scanObjectExcluded
                                                        val isSelected = !isRandom && uiState.scanObjectLabel == scanObject.label
                                                        val isHighlighted = if (isRandom) isExcluded else isSelected
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    if (isRandom) {
                                                                        if (!isExcluded && uiState.scanObjectExcluded.size >= allScanObjects.lastIndex) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "Keep at least one object available for random mode",
                                                                                Toast.LENGTH_SHORT
                                                                            ).show()
                                                                        } else {
                                                                            viewModel.toggleScanObjectExcluded(scanObject.label)
                                                                        }
                                                                    } else {
                                                                        viewModel.updateScanObjectLabel(scanObject.label)
                                                                    }
                                                                }
                                                                .background(
                                                                    when {
                                                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                                        isExcluded -> IconRed.copy(alpha = 0.16f)
                                                                        else -> Color.White.copy(alpha = 0.06f)
                                                                    }
                                                                )
                                                                .border(
                                                                    width = if (isHighlighted) 1.5.dp else 1.dp,
                                                                    color = when {
                                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                                        isExcluded -> IconRed.copy(alpha = 0.8f)
                                                                        else -> Color.White.copy(alpha = 0.1f)
                                                                    },
                                                                    shape = RoundedCornerShape(10.dp)
                                                                )
                                                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .clip(CircleShape)
                                                                        .background(scanObject.color.copy(alpha = if (isExcluded) 0.12f else 0.22f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = scanObject.icon,
                                                                        contentDescription = null,
                                                                        tint = if (isExcluded) Color.White.copy(alpha = 0.45f) else scanObject.color,
                                                                        modifier = Modifier.size(17.dp)
                                                                    )
                                                                }
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = scanObject.label,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = when {
                                                                            isSelected -> MaterialTheme.colorScheme.primary
                                                                            isExcluded -> Color.White.copy(alpha = 0.5f)
                                                                            else -> Color.White
                                                                        },
                                                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                                                        maxLines = 2
                                                                    )
                                                                    if (isRandom && isExcluded) {
                                                                        Text(
                                                                            text = "Excluded",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = IconRed.copy(alpha = 0.85f),
                                                                            maxLines = 1
                                                                        )
                                                                    }
                                                                }
                                                                if (isSelected) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckCircle,
                                                                        contentDescription = "Selected",
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                } else if (isRandom && isExcluded) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Block,
                                                                        contentDescription = "Excluded",
                                                                        tint = IconRed,
                                                                        modifier = Modifier.size(17.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (rowObjects.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    ChallengeType.SPELL_BEE -> {
                                        DifficultySelector(
                                            label = "Spell Bee Difficulty",
                                            selected = uiState.spellBeeDifficulty,
                                            onSelect = { viewModel.updateSpellBeeDifficulty(it) },
                                            descriptions = mapOf(
                                                MathDifficulty.EASY to "5-6 letter words",
                                                MathDifficulty.MEDIUM to "6-8 letter words",
                                                MathDifficulty.HARD to "8-10 letter words",
                                                MathDifficulty.EXTREME to "10+ letter words"
                                            ),
                                            examples = mapOf(
                                                MathDifficulty.EASY to "e.g.  S U N R I S E",
                                                MathDifficulty.MEDIUM to "e.g.  B A L A N C E D",
                                                MathDifficulty.HARD to "e.g.  E L E C T R I C A L",
                                                MathDifficulty.EXTREME to "e.g.  E X T R A O R D I N A R Y"
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CountChipRow(
                                            label = "Number of Rounds",
                                            options = listOf(1, 2, 3, 5, 7),
                                            selected = uiState.spellBeeCount,
                                            onSelect = { viewModel.updateSpellBeeCount(it) },
                                            suffix = "rounds"
                                        )
                                    }
                                    ChallengeType.AUDIO_MEMORY -> {
                                        DifficultySelector(
                                            label = "Audio Memory Difficulty",
                                            selected = uiState.audioMemoryDifficulty,
                                            onSelect = { viewModel.updateAudioMemoryDifficulty(it) },
                                            descriptions = mapOf(
                                                MathDifficulty.EASY to "3 words",
                                                MathDifficulty.MEDIUM to "3-4 words",
                                                MathDifficulty.HARD to "4-5 words",
                                                MathDifficulty.EXTREME to "5-6 words"
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CountChipRow(
                                            label = "Number of Rounds",
                                            options = listOf(1, 2, 3, 5, 7),
                                            selected = uiState.audioMemoryChallengeCount,
                                            onSelect = { viewModel.updateAudioMemoryChallengeCount(it) },
                                            suffix = "rounds"
                                        )
                                    }
                                    ChallengeType.CHARGER -> {
                                        // No configurable settings — show description only
                                        Text(
                                            "Plug your phone into a charger to dismiss the alarm.\n\nIf already charging when the alarm fires, you'll need to unplug first and then plug back in.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    ChallengeType.CLOCK_READING -> {
                                        DifficultySelector(
                                            label = "Clock Reading Difficulty",
                                            selected = uiState.clockReadingDifficulty,
                                            onSelect = { viewModel.updateClockReadingDifficulty(it) },
                                            descriptions = mapOf(
                                                MathDifficulty.EASY to "5-minute intervals (3:15, 7:30)",
                                                MathDifficulty.MEDIUM to "Any minute value (4:23, 9:47)",
                                                MathDifficulty.HARD to "Tricky hand positions (overlapping)",
                                                MathDifficulty.EXTREME to "Any minute, all positions"
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CountChipRow(
                                            label = "Number of Rounds",
                                            options = listOf(1, 2, 3, 5),
                                            selected = uiState.clockReadingCount,
                                            onSelect = { viewModel.updateClockReadingCount(it) },
                                            suffix = "rounds"
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    // ── Launch challenge preview ──
                    showPreviewForChallenge?.let { type ->
                        LaunchedEffect(type) {
                            val previewIntent = Intent(context, com.loud.alarm.ui.alarm.AlarmActivity::class.java).apply {
                                putExtra(com.loud.alarm.ui.alarm.AlarmActivity.EXTRA_PREVIEW_MODE, true)
                                putExtra(com.loud.alarm.ui.alarm.AlarmActivity.EXTRA_PREVIEW_CHALLENGE, type.name)
                                putExtra("isVolumeBoostEnabled", uiState.isVolumeBoostEnabled)
                                putExtra("soundUri", uiState.soundUri)
                                putExtra("mathDifficulty", uiState.mathDifficulty.name)
                                putExtra("mathQuestionCount", uiState.mathQuestionCount)
                                putExtra("mazeDifficulty", uiState.mazeDifficulty.name)
                                putExtra("puzzleDifficulty", uiState.puzzleDifficulty.name)
                                putExtra("memoryDifficulty", uiState.memoryDifficulty.name)
                                putExtra("memoryChallengeCount", uiState.memoryChallengeCount)
                                putExtra("stepCount", uiState.stepCount)
                                putExtra("shakeCount", uiState.shakeCount)
                                putExtra("tapCount", uiState.tapCount)
                                putExtra("squatCount", uiState.squatCount)
                                putExtra("squatDetectionMode", uiState.squatDetectionMode.name)
                                putExtra("pushUpCount", uiState.pushUpCount)
                                putExtra("reverseTypingCount", uiState.reverseTypingCount)
                                putExtra("rewriteText", uiState.rewriteText)
                                putExtra("barcodeValue", uiState.barcodeValue)
                                putExtra("scanObjectLabel", uiState.scanObjectLabel)
                                putStringArrayListExtra(
                                    "scanObjectExcluded",
                                    java.util.ArrayList(uiState.scanObjectExcluded)
                                )
                                putExtra("spellBeeDifficulty", uiState.spellBeeDifficulty.name)
                                putExtra("spellBeeCount", uiState.spellBeeCount)
                                putExtra("audioMemoryDifficulty", uiState.audioMemoryDifficulty.name)
                                putExtra("audioMemoryChallengeCount", uiState.audioMemoryChallengeCount)
                                putExtra("clockReadingDifficulty", uiState.clockReadingDifficulty.name)
                                putExtra("clockReadingCount", uiState.clockReadingCount)
                            }
                            context.startActivity(previewIntent)
                            showPreviewForChallenge = null
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
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val sinkLabeler = remember {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(SINK_REFERENCE_LABELER_CONFIDENCE)
                .build()
        )
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCapturing by remember { mutableStateOf(false) }
    var isVerifyingSink by remember { mutableStateOf(false) }
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
            sinkLabeler.close()
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
                        text = "Capture Specific Sink",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Take a clear photo of the sink this alarm should recognize",
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
                            if (isCapturing || isVerifyingSink) return@Button
                            isCapturing = true
                            isVerifyingSink = false

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
                                        mainExecutor.execute {
                                            isCapturing = false
                                            isVerifyingSink = true
                                        }

                                        val inputImage = runCatching {
                                            InputImage.fromFilePath(context, Uri.fromFile(photoFile))
                                        }.getOrElse {
                                            mainExecutor.execute {
                                                isVerifyingSink = false
                                                photoFile.delete()
                                                Toast.makeText(
                                                    context,
                                                    "Couldn't check the photo. Try again.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            return
                                        }

                                        sinkLabeler.process(inputImage)
                                            .addOnSuccessListener(mainExecutor) { labels ->
                                                val isSink = hasTargetLabelMatch(
                                                    labels.map { it.text to it.confidence },
                                                    "Sink"
                                                )

                                                isVerifyingSink = false
                                                if (isSink) {
                                                    onPhotoCaptured(photoFile.absolutePath)
                                                } else {
                                                    photoFile.delete()
                                                    Toast.makeText(
                                                        context,
                                                        "That doesn't look like a sink. Try again.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            .addOnFailureListener(mainExecutor) {
                                                isVerifyingSink = false
                                                photoFile.delete()
                                                Toast.makeText(
                                                    context,
                                                    "Couldn't check the photo. Try again.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        mainExecutor.execute {
                                            isCapturing = false
                                            isVerifyingSink = false
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
                        enabled = !isCapturing && !isVerifyingSink && isCameraReady,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        val captureLabel = when {
                            isCapturing -> "Capturing..."
                            isVerifyingSink -> "Checking..."
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

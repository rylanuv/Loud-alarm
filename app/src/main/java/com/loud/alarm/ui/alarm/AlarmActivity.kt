package com.loud.alarm.ui.alarm

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.loud.alarm.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loud.alarm.data.Alarm
import com.loud.alarm.data.ChallengeType
import com.loud.alarm.data.MathDifficulty
import com.loud.alarm.service.AlarmService
import com.loud.alarm.ui.challenge.QrCodeChallengeScreen
import com.loud.alarm.ui.challenge.MathChallengeScreen
import com.loud.alarm.ui.challenge.ScanChallengeScreen
import com.loud.alarm.ui.home.formatTime
import com.loud.alarm.ui.home.getAmPm
import com.loud.alarm.ui.theme.LoudAlarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private val viewModel: AlarmActiveViewModel by viewModels()

    companion object {
        private const val TAG = "AlarmActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // showOnLockScreen uses window flags, safe before super
        showOnLockScreen()
        super.onCreate(savedInstanceState)
        
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d(TAG, "AlarmActivity onCreate, alarmId=$alarmId")
        if (alarmId != -1) {
            viewModel.loadAlarm(alarmId)
        }

        setContent {
            LoudAlarmTheme(darkTheme = true) { // Always dark for alarm ringing
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val alarmState by viewModel.alarm.collectAsState()
                    val snoozeEnabled by viewModel.snoozeEnabled.collectAsState()
                    
                    if (alarmState != null) {
                        Log.d(TAG, "Alarm loaded: id=${alarmState!!.id}, challengeTypes=${alarmState!!.challengeTypes}")
                        ActiveAlarmScreen(
                            alarm = alarmState!!,
                            snoozeEnabled = snoozeEnabled,
                            viewModel = viewModel,
                            onStopSound = {
                                stopAlarmService()
                            },
                            onDismissActivity = {
                                if (alarmState!!.wakeUpCheckMinutes > 0) {
                                    scheduleWakeUpCheck(alarmState!!)
                                }
                                stopAlarmService() // Stop service again just in case
                                finish()
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading Alarm...", color = Color.White)
                        }
                    }
                }
            }
        }

        // hideSystemUI must be called AFTER super.onCreate() and setContent
        // because it needs the DecorView to exist
        hideSystemUI()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d(TAG, "AlarmActivity onNewIntent, alarmId=$alarmId")
        if (alarmId != -1) {
            viewModel.loadAlarm(alarmId)
        }
    }

    private fun showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun scheduleWakeUpCheck(alarm: Alarm) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, com.loud.alarm.service.WakeUpCheckReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("IS_VOLUME_BOOST_ENABLED", alarm.isVolumeBoostEnabled)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMs = System.currentTimeMillis() + (alarm.wakeUpCheckMinutes * 60 * 1000L)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
        
        Log.d(TAG, "Scheduled Wake Up Check for alarm ${alarm.id} in ${alarm.wakeUpCheckMinutes} minutes")
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true // Block volume keys
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun hideSystemUI() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
            }
        } catch (e: Exception) {
            Log.e(TAG, "hideSystemUI failed (DecorView may not be ready)", e)
        }
    }

    @Suppress("DEPRECATION")
    @android.annotation.SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back button — user must solve the challenge
        // Intentionally not calling super to prevent back navigation
    }
}

@Composable
fun ActiveAlarmScreen(
    alarm: Alarm,
    snoozeEnabled: Boolean,
    viewModel: AlarmActiveViewModel,
    onStopSound: () -> Unit,
    onDismissActivity: () -> Unit
) {
    Log.d("ActiveAlarmScreen", "=== RENDERING === alarm.id=${alarm.id}, challengeTypes=${alarm.challengeTypes}")

    // Key state to this specific alarm so recomposition doesn't mix up states
    key(alarm.id) {
        var mathSolved by remember { mutableStateOf(false) }
        var barcodeScanned by remember { mutableStateOf(false) }
        var useMathFallback by remember { mutableStateOf(false) }
        var rewriteSolved by remember { mutableStateOf(false) }
        var stepSolved by remember { mutableStateOf(false) }
        var mazeSolved by remember { mutableStateOf(false) }
        var memorySolved by remember { mutableStateOf(false) }
        var scanSinkSolved by remember { mutableStateOf(false) }
        var scanObjectSolved by remember { mutableStateOf(false) }
        
        var isRingingScreenDismissed by rememberSaveable { mutableStateOf(false) }

        // Build the ordered list of challenges the user must complete
        val activeChallenges = remember(alarm.challengeTypes) {
            alarm.challengeTypes.filter { it != ChallengeType.NONE }
        }

        // Determine if ALL challenges are complete
        val isChallengeComplete = activeChallenges.isEmpty() || activeChallenges.all { type ->
            when (type) {
                ChallengeType.MATH -> mathSolved
                ChallengeType.QR_CODE -> barcodeScanned
                ChallengeType.REWRITE -> rewriteSolved
                ChallengeType.STEP -> stepSolved
                ChallengeType.MAZE -> mazeSolved
                ChallengeType.MEMORY -> memorySolved
                ChallengeType.SCAN_SINK -> scanSinkSolved
                ChallengeType.SCAN_OBJECT -> scanObjectSolved
                ChallengeType.SHAKE, ChallengeType.SPELL_BEE, ChallengeType.PUZZLE -> true // TODO Handle them
                ChallengeType.NONE -> true
            }
        }

        // Find the NEXT unsolved challenge to show
        val currentChallenge = activeChallenges.firstOrNull { type ->
            when (type) {
                ChallengeType.MATH -> !mathSolved
                ChallengeType.QR_CODE -> !barcodeScanned
                ChallengeType.REWRITE -> !rewriteSolved
                ChallengeType.STEP -> !stepSolved
                ChallengeType.MAZE -> !mazeSolved
                ChallengeType.MEMORY -> !memorySolved
                ChallengeType.SCAN_SINK -> !scanSinkSolved
                ChallengeType.SCAN_OBJECT -> !scanObjectSolved
                ChallengeType.SHAKE, ChallengeType.SPELL_BEE, ChallengeType.PUZZLE -> false
                ChallengeType.NONE -> false
            }
        }

        Log.d("ActiveAlarmScreen", "mathSolved=$mathSolved, barcodeScanned=$barcodeScanned, useMathFallback=$useMathFallback, isChallengeComplete=$isChallengeComplete")

        LaunchedEffect(isRingingScreenDismissed, isChallengeComplete) {
            if (isRingingScreenDismissed && isChallengeComplete) {
                onDismissActivity()
            }
        }

        if (!isRingingScreenDismissed) {
            Log.d("ActiveAlarmScreen", "Showing DismissOrSnoozeScreen")
            // Show Dismiss / Snooze Options
            DismissOrSnoozeScreen(
                alarm = alarm,
                snoozeEnabled = snoozeEnabled,
                onDismiss = {
                    Log.d("ActiveAlarmScreen", "User tapped DISMISS")
                    onStopSound()
                    isRingingScreenDismissed = true
                },
                onSnooze = { mins ->
                    if (snoozeEnabled) {
                        Log.d("ActiveAlarmScreen", "User tapped SNOOZE for $mins minutes")
                        viewModel.snoozeAlarm(alarm, mins)
                        onDismissActivity() // Snooze dismisses the current ring
                    } else {
                        Log.w("ActiveAlarmScreen", "Snooze tapped while snooze is disabled; ignoring")
                    }
                }
            )
        } else if (!isChallengeComplete && currentChallenge != null) {
            Log.d("ActiveAlarmScreen", "Showing Challenge screen for type=$currentChallenge")
            // Show Challenge
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SOLVE TO DISMISS",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentChallenge) {
                        ChallengeType.MATH -> {
                            Log.d("ActiveAlarmScreen", "Rendering MathChallengeScreen")
                            MathChallengeScreen(
                                difficulty = alarm.mathDifficulty,
                                onSuccess = {
                                    Log.d("ActiveAlarmScreen", "Math challenge SOLVED!")
                                    mathSolved = true
                                }
                            )
                        }
                        ChallengeType.QR_CODE -> {
                            if (useMathFallback) {
                                Log.d("ActiveAlarmScreen", "Rendering MathChallengeScreen (QR code fallback)")
                                MathChallengeScreen(
                                    difficulty = MathDifficulty.MEDIUM,
                                    onSuccess = {
                                        Log.d("ActiveAlarmScreen", "Math fallback SOLVED!")
                                        barcodeScanned = true
                                    }
                                )
                            } else {
                                Log.d("ActiveAlarmScreen", "Rendering QrCodeChallengeScreen")
                                QrCodeChallengeScreen(
                                    targetBarcodeValue = alarm.barcodeValue,
                                    onSuccess = {
                                        Log.d("ActiveAlarmScreen", "QR Code challenge SOLVED!")
                                        barcodeScanned = true
                                    },
                                    onFallbackToMath = {
                                        Log.d("ActiveAlarmScreen", "User chose math fallback for QR code")
                                        useMathFallback = true
                                    }
                                )
                            }
                        }
                        ChallengeType.REWRITE -> {
                            com.loud.alarm.ui.challenge.RewriteChallengeScreen(
                                customText = alarm.rewriteText,
                                onSuccess = { rewriteSolved = true }
                            )
                        }
                        ChallengeType.STEP -> {
                            com.loud.alarm.ui.challenge.StepChallengeScreen(
                                targetSteps = alarm.stepCount,
                                onSuccess = { stepSolved = true }
                            )
                        }
                        ChallengeType.MAZE -> {
                            com.loud.alarm.ui.challenge.MazeChallengeScreen(
                                onSuccess = { mazeSolved = true }
                            )
                        }
                        ChallengeType.MEMORY -> {
                            com.loud.alarm.ui.challenge.MemoryChallengeScreen(
                                onSuccess = { memorySolved = true }
                            )
                        }
                        ChallengeType.SCAN_SINK -> {
                            Log.d("ActiveAlarmScreen", "Rendering ScanChallengeScreen for SINK")
                            ScanChallengeScreen(
                                targetLabel = "Sink",
                                displayTitle = "Scan Your Sink",
                                displaySubtitle = "Point the camera at your sink to dismiss",
                                onSuccess = {
                                    Log.d("ActiveAlarmScreen", "Scan Sink challenge SOLVED!")
                                    scanSinkSolved = true
                                },
                                onFallbackToMath = {
                                    Log.d("ActiveAlarmScreen", "User chose math fallback for scan sink")
                                    scanSinkSolved = true  // fallback skips
                                }
                            )
                        }
                        ChallengeType.SCAN_OBJECT -> {
                            val objectLabel = alarm.scanObjectLabel.ifEmpty { "Object" }
                            Log.d("ActiveAlarmScreen", "Rendering ScanChallengeScreen for OBJECT: $objectLabel")
                            ScanChallengeScreen(
                                targetLabel = objectLabel,
                                displayTitle = "Find the $objectLabel",
                                displaySubtitle = "Point the camera at a $objectLabel to dismiss",
                                onSuccess = {
                                    Log.d("ActiveAlarmScreen", "Scan Object challenge SOLVED! ($objectLabel)")
                                    scanObjectSolved = true
                                },
                                onFallbackToMath = {
                                    Log.d("ActiveAlarmScreen", "User chose math fallback for scan object")
                                    scanObjectSolved = true  // fallback skips
                                }
                            )
                        }
                        else -> {
                            // Should not reach here
                            Log.d("ActiveAlarmScreen", "Unhandled ChallengeType in active block")
                            Text("Loading...")
                        }
                    }
                }
            }
        }
    }
}
private val backgroundImages = listOf(
    R.drawable.bg_mountain_peaks,
    R.drawable.bg_ocean_deep,
    R.drawable.image1,
    R.drawable.image2,
    R.drawable.image3,
    R.drawable.tree
)

@Composable
fun DismissOrSnoozeScreen(
    alarm: Alarm,
    snoozeEnabled: Boolean,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val bgImage = remember { backgroundImages.random() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = bgImage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark scrim for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = formatTime(alarm.hour, alarm.minute),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White
        )
        Text(
            text = getAmPm(alarm.hour),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (alarm.label.isNotEmpty()) {
            Text(
                text = alarm.label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Dismiss
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            )
        ) {
            Text("DISMISS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        if (snoozeEnabled) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Or Snooze for...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SnoozeButton(min = 5, onSnooze = onSnooze, modifier = Modifier.weight(1f))
                SnoozeButton(min = 10, onSnooze = onSnooze, modifier = Modifier.weight(1f))
                SnoozeButton(min = 15, onSnooze = onSnooze, modifier = Modifier.weight(1f))
            }
        }
    }
    } // Close Box
}

@Composable
fun SnoozeButton(min: Int, onSnooze: (Int) -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = { onSnooze(min) },
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
             contentColor = MaterialTheme.colorScheme.secondary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Text("${min}m")
    }
}

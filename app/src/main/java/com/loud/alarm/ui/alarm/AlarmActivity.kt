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
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import com.loud.alarm.data.SquatDetectionMode
import com.loud.alarm.service.AlarmService
import com.loud.alarm.ui.challenge.QrCodeChallengeScreen
import com.loud.alarm.ui.challenge.MathChallengeScreen
import com.loud.alarm.ui.challenge.PuzzleChallengeScreen
import com.loud.alarm.ui.challenge.ScanChallengeScreen
import com.loud.alarm.ui.challenge.ShakeChallengeScreen
import com.loud.alarm.ui.challenge.SpellBeeChallengeScreen
import com.loud.alarm.ui.challenge.SquatChallengeScreen
import com.loud.alarm.ui.challenge.PushUpChallengeScreen
import com.loud.alarm.ui.challenge.ReverseTypingChallengeScreen
import com.loud.alarm.ui.challenge.AudioMemoryChallengeScreen
import com.loud.alarm.ui.challenge.ChargerChallengeScreen
import com.loud.alarm.ui.challenge.RandomObjectPickerScreen
import com.loud.alarm.ui.challenge.TapChallengeScreen
import com.loud.alarm.ui.home.formatTime
import com.loud.alarm.ui.home.getAmPm
import com.loud.alarm.ui.theme.LoudAlarmTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private val viewModel: AlarmActiveViewModel by viewModels()
    private var alarmFlowCompleted: Boolean = false

    @Inject
    lateinit var settingsRepository: com.loud.alarm.data.SettingsRepository

    companion object {
        private const val TAG = "AlarmActivity"
        const val EXTRA_PREVIEW_MODE = "extra_preview_mode"
        const val EXTRA_PREVIEW_CHALLENGE = "extra_preview_challenge"
        @Volatile
        var isAlarmScreenVisible: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isPreview = intent.getBooleanExtra(EXTRA_PREVIEW_MODE, false)
        if (!isPreview) showOnLockScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (isPreview) {
            val startPreviewIntent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_PREVIEW_ALARM
                putExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, intent.getBooleanExtra("isVolumeBoostEnabled", false))
                putExtra("EXTRA_SOUND_URI", intent.getStringExtra("soundUri"))
            }
            startService(startPreviewIntent)

            // Preview mode: build a temporary alarm from current editor state
            val challengeName = intent.getStringExtra(EXTRA_PREVIEW_CHALLENGE) ?: ChallengeType.MATH.name
            val challengeType = try { ChallengeType.valueOf(challengeName) } catch (_: Exception) { ChallengeType.MATH }
            val previewAlarm = Alarm(
                id = -999,
                hour = 0, minute = 0, enabled = false,
                challengeTypes = setOf(challengeType),
                mathDifficulty = MathDifficulty.valueOf(intent.getStringExtra("mathDifficulty") ?: "EASY"),
                mathQuestionCount = intent.getIntExtra("mathQuestionCount", 3),
                mazeDifficulty = MathDifficulty.valueOf(intent.getStringExtra("mazeDifficulty") ?: "EASY"),
                puzzleDifficulty = MathDifficulty.valueOf(intent.getStringExtra("puzzleDifficulty") ?: "EASY"),
                memoryDifficulty = MathDifficulty.valueOf(intent.getStringExtra("memoryDifficulty") ?: "EASY"),
                memoryChallengeCount = intent.getIntExtra("memoryChallengeCount", 1),
                stepCount = intent.getIntExtra("stepCount", 20),
                shakeCount = intent.getIntExtra("shakeCount", 30),
                tapCount = intent.getIntExtra("tapCount", 30),
                squatCount = intent.getIntExtra("squatCount", 10),
                squatDetectionMode = SquatDetectionMode.valueOf(intent.getStringExtra("squatDetectionMode") ?: "CAMERA"),
                pushUpCount = intent.getIntExtra("pushUpCount", 10),
                reverseTypingCount = intent.getIntExtra("reverseTypingCount", 2),
                rewriteText = intent.getStringExtra("rewriteText") ?: "",
                barcodeValue = intent.getStringExtra("barcodeValue"),
                scanObjectLabel = intent.getStringExtra("scanObjectLabel") ?: "RANDOM",
                scanObjectExcluded = intent.getStringArrayListExtra("scanObjectExcluded")?.toSet() ?: emptySet(),
                spellBeeDifficulty = MathDifficulty.valueOf(intent.getStringExtra("spellBeeDifficulty") ?: "EASY"),
                spellBeeCount = intent.getIntExtra("spellBeeCount", 3),
                audioMemoryDifficulty = MathDifficulty.valueOf(intent.getStringExtra("audioMemoryDifficulty") ?: "EASY"),
                audioMemoryChallengeCount = intent.getIntExtra("audioMemoryChallengeCount", 1)
            )
            setContent {
                LoudAlarmTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ActiveAlarmScreen(
                            alarm = previewAlarm,
                            snoozeEnabled = false,
                            viewModel = viewModel,
                            isPreview = true,
                            onStopAlarmSound = {
                                stopAlarmService()
                            },
                            onDismissActivity = { 
                                stopAlarmService()
                                finish() 
                            }
                        )
                    }
                }
            }
            return
        }

        val alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
        Log.d(TAG, "AlarmActivity onCreate, alarmId=$alarmId")
        if (alarmId != -1) {
            viewModel.loadAlarm(alarmId)
        }

        setContent {
            LoudAlarmTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val alarmState by viewModel.alarm.collectAsState()
                    val loadError by viewModel.loadError.collectAsState()
                    val snoozeEnabled by viewModel.snoozeEnabled.collectAsState()
                    
                    if (alarmState != null) {
                        Log.d(TAG, "Alarm loaded: id=${alarmState!!.id}, challengeTypes=${alarmState!!.challengeTypes}")
                        ActiveAlarmScreen(
                            alarm = alarmState!!,
                            snoozeEnabled = snoozeEnabled,
                            viewModel = viewModel,
                            onStopAlarmSound = {
                                stopAlarmService()
                            },
                            onDismissActivity = {
                                alarmFlowCompleted = true
                                viewModel.logAlarmDismissed(alarmState!!)
                                lifecycleScope.launch {
                                    settingsRepository.incrementAlarmDismissCount()
                                }
                                if (alarmState!!.wakeUpCheckMinutes > 0) {
                                    scheduleWakeUpCheck(alarmState!!)
                                }
                                stopAlarmService()
                                finish()
                            }
                        )
                    } else if (loadError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Couldn't load alarm", color = Color.White)
                                Text(
                                    text = loadError ?: "",
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        alarmFlowCompleted = true
                                        stopAlarmService()
                                        finish()
                                    }
                                ) {
                                    Text("Stop Alarm")
                                }
                            }
                        }
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

        hideSystemUI()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
        Log.d(TAG, "AlarmActivity onNewIntent, alarmId=$alarmId")
        if (alarmId != -1) {
            viewModel.loadAlarm(alarmId)
        }
    }

    override fun onStart() {
        super.onStart()
        isAlarmScreenVisible = true
    }

    override fun onStop() {
        super.onStop()
        isAlarmScreenVisible = false
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
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra(AlarmService.EXTRA_IS_VOLUME_BOOST_ENABLED, alarm.isVolumeBoostEnabled)
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
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!alarmFlowCompleted) {
            val isPreview = intent.getBooleanExtra(EXTRA_PREVIEW_MODE, false)
            if (isPreview) {
                stopAlarmService()
                finish()
                return
            }
            val alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
            Log.w(TAG, "User pressed Home on alarm screen; closing and requesting re-show")
            if (alarmId != -1) {
                val reshowIntent = Intent(this, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_RESHOW_ALARM
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                }
                startService(reshowIntent)
            }
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val isPreview = intent.getBooleanExtra(EXTRA_PREVIEW_MODE, false)
        if (isPreview) {
            stopAlarmService()
        }
    }



    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true
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
    isPreview: Boolean = false,
    onStopAlarmSound: () -> Unit = {},
    onDismissActivity: () -> Unit
) {
    Log.d("ActiveAlarmScreen", "=== RENDERING === alarm.id=${alarm.id}, challengeTypes=${alarm.challengeTypes}")

    // Key state to this specific alarm so recomposition doesn't mix up states
    key(alarm.id) {
        var mathSolved by rememberSaveable { mutableStateOf(false) }
        var barcodeScanned by rememberSaveable { mutableStateOf(false) }
        var useScanSinkMathFallback by rememberSaveable { mutableStateOf(false) }
        var useScanObjectMathFallback by rememberSaveable { mutableStateOf(false) }
        var rewriteSolved by remember { mutableStateOf(false) }
        var stepSolved by rememberSaveable { mutableStateOf(false) }
        var mazeSolved by remember { mutableStateOf(false) }
        var memorySolved by remember { mutableStateOf(false) }
        var shakeSolved by rememberSaveable { mutableStateOf(false) }
        var tapSolved by rememberSaveable { mutableStateOf(false) }
        var scanSinkSolved by remember { mutableStateOf(false) }
        var scanObjectSolved by remember { mutableStateOf(false) }
        var spellBeeSolved by rememberSaveable { mutableStateOf(false) }
        var puzzleSolved by rememberSaveable { mutableStateOf(false) }
        var squatSolved by rememberSaveable { mutableStateOf(false) }
        var pushUpSolved by rememberSaveable { mutableStateOf(false) }
        var reverseTypingSolved by rememberSaveable { mutableStateOf(false) }
        var audioMemorySolved by rememberSaveable { mutableStateOf(false) }
        var chargerSolved by rememberSaveable { mutableStateOf(false) }
        
        var isRingingScreenDismissed by rememberSaveable { mutableStateOf(false) }
        var pendingSnoozeMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
        var showCongrats by rememberSaveable { mutableStateOf(false) }

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
                ChallengeType.SHAKE -> shakeSolved
                ChallengeType.TAP_CHALLENGE -> tapSolved
                ChallengeType.SCAN_SINK -> scanSinkSolved
                ChallengeType.SCAN_OBJECT -> scanObjectSolved
                ChallengeType.SPELL_BEE -> spellBeeSolved
                ChallengeType.PUZZLE -> puzzleSolved
                ChallengeType.SQUAT -> squatSolved
                ChallengeType.PUSH_UP -> pushUpSolved
                ChallengeType.REVERSE_TYPING -> reverseTypingSolved
                ChallengeType.AUDIO_MEMORY -> audioMemorySolved
                ChallengeType.CHARGER -> chargerSolved
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
                ChallengeType.SHAKE -> !shakeSolved
                ChallengeType.TAP_CHALLENGE -> !tapSolved
                ChallengeType.SCAN_SINK -> !scanSinkSolved
                ChallengeType.SCAN_OBJECT -> !scanObjectSolved
                ChallengeType.SPELL_BEE -> !spellBeeSolved
                ChallengeType.PUZZLE -> !puzzleSolved
                ChallengeType.SQUAT -> !squatSolved
                ChallengeType.PUSH_UP -> !pushUpSolved
                ChallengeType.REVERSE_TYPING -> !reverseTypingSolved
                ChallengeType.AUDIO_MEMORY -> !audioMemorySolved
                ChallengeType.CHARGER -> !chargerSolved
                ChallengeType.NONE -> false
            }
        }

        Log.d("ActiveAlarmScreen", "mathSolved=$mathSolved, barcodeScanned=$barcodeScanned, shakeSolved=$shakeSolved, isChallengeComplete=$isChallengeComplete")

        LaunchedEffect(isRingingScreenDismissed, isChallengeComplete) {
            if (isRingingScreenDismissed && isChallengeComplete) {
                if (pendingSnoozeMinutes != null) {
                    viewModel.snoozeAlarm(alarm, pendingSnoozeMinutes!!)
                    onDismissActivity()
                } else if (activeChallenges.isNotEmpty()) {
                    onStopAlarmSound()
                    showCongrats = true
                } else {
                    onDismissActivity()
                }
            }
        }

        // Preview mode now acts exactly like the real alarm and shows the Dismiss/Snooze screen first

        if (showCongrats) {
            CongratsScreen(
                onAnimationFinished = {
                    onDismissActivity()
                }
            )
        } else if (!isRingingScreenDismissed) {
            Log.d("ActiveAlarmScreen", "Showing DismissOrSnoozeScreen")
            // Show Dismiss / Snooze Options
            DismissOrSnoozeScreen(
                alarm = alarm,
                snoozeEnabled = snoozeEnabled,
                onDismiss = {
                    Log.d("ActiveAlarmScreen", "User tapped DISMISS, showing challenges")
                    isRingingScreenDismissed = true
                },
                onSnooze = { mins ->
                    if (snoozeEnabled) {
                        Log.d("ActiveAlarmScreen", "User tapped SNOOZE for $mins minutes")
                        pendingSnoozeMinutes = mins
                        isRingingScreenDismissed = true
                    } else {
                        Log.w("ActiveAlarmScreen", "Snooze tapped while snooze is disabled; ignoring")
                    }
                }
            )
        } else if (!isChallengeComplete && currentChallenge != null) {
            Log.d("ActiveAlarmScreen", "Showing Challenge screen for type=$currentChallenge")
            // Show Challenge
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        .padding(horizontal = 16.dp, vertical = if (isPreview) 10.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isPreview) "PREVIEW MODE" else if (pendingSnoozeMinutes != null) "SOLVE TO SNOOZE" else "SOLVE TO DISMISS",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (isPreview) {
                            OutlinedButton(
                                onClick = onDismissActivity,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) { Text("Close") }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentChallenge) {
                        ChallengeType.MATH -> {
                            Log.d("ActiveAlarmScreen", "Rendering MathChallengeScreen")
                            MathChallengeScreen(
                                difficulty = alarm.mathDifficulty,
                                questionCount = alarm.mathQuestionCount,
                                onSuccess = {
                                    Log.d("ActiveAlarmScreen", "Math challenge SOLVED!")
                                    mathSolved = true
                                }
                            )
                        }
                        ChallengeType.QR_CODE -> {
                            Log.d("ActiveAlarmScreen", "Rendering QrCodeChallengeScreen")
                            QrCodeChallengeScreen(
                                targetBarcodeValue = alarm.barcodeValue,
                                onSuccess = {
                                    Log.d("ActiveAlarmScreen", "QR Code challenge SOLVED!")
                                    barcodeScanned = true
                                }
                            )
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
                                difficulty = alarm.mazeDifficulty,
                                onSuccess = { mazeSolved = true }
                            )
                        }
                        ChallengeType.MEMORY -> {
                            com.loud.alarm.ui.challenge.MemoryChallengeScreen(
                                difficulty = alarm.memoryDifficulty,
                                challengeCount = alarm.memoryChallengeCount,
                                onSuccess = { memorySolved = true }
                            )
                        }
                        ChallengeType.SHAKE -> {
                            ShakeChallengeScreen(
                                targetShakes = alarm.shakeCount,
                                onSuccess = { shakeSolved = true }
                            )
                        }
                        ChallengeType.TAP_CHALLENGE -> {
                            TapChallengeScreen(
                                targetTaps = alarm.tapCount,
                                onSuccess = { tapSolved = true }
                            )
                        }
                        ChallengeType.SPELL_BEE -> {
                            SpellBeeChallengeScreen(
                                difficulty = alarm.spellBeeDifficulty,
                                rounds = alarm.spellBeeCount,
                                onSuccess = { spellBeeSolved = true }
                            )
                        }
                        ChallengeType.PUZZLE -> {
                            PuzzleChallengeScreen(
                                difficulty = alarm.puzzleDifficulty,
                                onSuccess = { puzzleSolved = true }
                            )
                        }
                        ChallengeType.SCAN_SINK -> {
                            if (useScanSinkMathFallback) {
                                Log.d("ActiveAlarmScreen", "Rendering MathChallengeScreen (scan sink fallback)")
                                MathChallengeScreen(
                                    difficulty = MathDifficulty.MEDIUM,
                                    onSuccess = {
                                        Log.d("ActiveAlarmScreen", "Scan sink math fallback SOLVED!")
                                        scanSinkSolved = true
                                    }
                                )
                            } else {
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
                                        useScanSinkMathFallback = true
                                    }
                                )
                            }
                        }
                        ChallengeType.SCAN_OBJECT -> {
                            if (useScanObjectMathFallback) {
                                Log.d("ActiveAlarmScreen", "Rendering MathChallengeScreen (scan object fallback)")
                                MathChallengeScreen(
                                    difficulty = MathDifficulty.MEDIUM,
                                    onSuccess = {
                                        Log.d("ActiveAlarmScreen", "Scan object math fallback SOLVED!")
                                        scanObjectSolved = true
                                    }
                                )
                            } else {
                            val isRandomMode = alarm.scanObjectLabel == "RANDOM"
                            if (isRandomMode) {
                                // Random mode: show roulette, then scan
                                var randomPickedLabel by remember { mutableStateOf<String?>(null) }
                                if (randomPickedLabel == null) {
                                    RandomObjectPickerScreen(
                                        excludedLabels = alarm.scanObjectExcluded,
                                        onObjectPicked = { picked ->
                                            Log.d("ActiveAlarmScreen", "Random picker chose: $picked")
                                            randomPickedLabel = picked
                                        }
                                    )
                                } else {
                                    val label = randomPickedLabel!!
                                    ScanChallengeScreen(
                                        targetLabel = label,
                                        displayTitle = "Find the $label",
                                        displaySubtitle = "Point the camera at a $label to dismiss",
                                        onSuccess = {
                                            Log.d("ActiveAlarmScreen", "Scan Object SOLVED! (random: $label)")
                                            scanObjectSolved = true
                                        },
                                        onFallbackToMath = {
                                            Log.d("ActiveAlarmScreen", "Math fallback for random scan object")
                                            useScanObjectMathFallback = true
                                        }
                                    )
                                }
                            } else {
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
                                        useScanObjectMathFallback = true
                                    }
                                )
                            }
                            }
                        }
                        ChallengeType.SQUAT -> {
                            SquatChallengeScreen(
                                targetSquats = alarm.squatCount,
                                detectionMode = alarm.squatDetectionMode,
                                onSuccess = { squatSolved = true }
                            )
                        }
                        ChallengeType.PUSH_UP -> {
                            PushUpChallengeScreen(
                                targetPushUps = alarm.pushUpCount,
                                onSuccess = { pushUpSolved = true }
                            )
                        }
                        ChallengeType.REVERSE_TYPING -> {
                            ReverseTypingChallengeScreen(
                                rounds = alarm.reverseTypingCount,
                                onSuccess = { reverseTypingSolved = true }
                            )
                        }
                        ChallengeType.AUDIO_MEMORY -> {
                            AudioMemoryChallengeScreen(
                                difficulty = alarm.audioMemoryDifficulty,
                                challengeCount = alarm.audioMemoryChallengeCount,
                                onSuccess = { audioMemorySolved = true }
                            )
                        }
                        ChallengeType.CHARGER -> {
                            ChargerChallengeScreen(
                                onSuccess = { chargerSolved = true }
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
            Text("DISMISS ALARM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

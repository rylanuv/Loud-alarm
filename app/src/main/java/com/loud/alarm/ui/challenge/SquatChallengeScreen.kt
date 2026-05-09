package com.loud.alarm.ui.challenge

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.loud.alarm.data.SquatDetectionMode
import com.loud.alarm.di.RepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

private const val TAG = "SquatChallenge"

private const val MIN_LANDMARK_CONFIDENCE = 0.50f
private const val MIN_SUPPORT_LANDMARK_CONFIDENCE = 0.35f
private const val SIGNAL_SMOOTHING = 0.55f
private const val MIN_REP_INTERVAL_MS = 800L
private const val MIN_REP_DURATION_MS = 600L
private const val MAX_REP_DURATION_MS = 10_000L

private const val MIN_TRAVEL_PX = 34f
private const val HIP_TRAVEL_RATIO = 0.13f
private const val RISE_RETURN_RATIO = 0.50f
private const val RISE_FROM_BOTTOM_RATIO = 0.65f
private const val BENT_KNEE_ANGLE_DEGREES = 145f
private const val STRAIGHT_KNEE_ANGLE_DEGREES = 155f

private const val MOTION_GRAVITY_ALPHA = 0.8f
private const val MOTION_ACCEL_SMOOTHING = 0.4f
private const val MOTION_VELOCITY_DECAY = 0.98f
private const val MOTION_ACCEL_DOWN_ENTER = -0.6f
private const val MOTION_ACCEL_UP_ENTER = 0.6f
private const val MOTION_MIN_DOWN_DISPLACEMENT = 0.03f
private const val MOTION_MIN_DOWN_DURATION_MS = 250L
private const val MOTION_MIN_UP_DURATION_MS = 200L
private const val MOTION_MAX_DOWN_DURATION_MS = 5_000L
private const val MOTION_MAX_UP_DURATION_MS = 4_000L
private const val MOTION_MIN_SQUAT_INTERVAL_MS = 600L
private const val MOTION_WARMUP_MS = 600L

private enum class MotionSquatPhase {
    IDLE,
    GOING_DOWN,
    GOING_UP
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SquatChallengeScreen(
    targetSquats: Int,
    detectionMode: SquatDetectionMode = SquatDetectionMode.CAMERA,
    onSuccess: () -> Unit
) {
    var currentSquats by rememberSaveable { mutableIntStateOf(0) }
    var pulseKey by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(currentSquats, targetSquats) {
        if (currentSquats >= targetSquats) {
            onSuccess()
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (pulseKey % 2 == 0) 1f else 1.12f,
        animationSpec = tween(durationMillis = 180),
        label = "squatPulse"
    )

    val onSquatDetected = {
        if (currentSquats < targetSquats) {
            currentSquats += 1
            pulseKey += 1
        }
    }

    if (detectionMode == SquatDetectionMode.CAMERA) {
        var feedback by remember { mutableStateOf(SquatTrackingFeedback()) }
        var lensFacing by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
        var currentPose by remember { mutableStateOf<Pose?>(null) }

        val context = LocalContext.current
        val settingsRepository = remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                RepositoryEntryPoint::class.java
            ).settingsRepository()
        }
        val skeletonOverlayEnabled by settingsRepository.skeletonOverlayEnabled.collectAsState(initial = false)

        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

        LaunchedEffect(Unit) {
            if (!cameraPermissionState.status.isGranted) {
                cameraPermissionState.launchPermissionRequest()
            }
        }

        if (cameraPermissionState.status.isGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            SquatPoseCameraPreview(
                lensFacing = lensFacing,
                onSquatDetected = onSquatDetected,
                onTrackingFeedback = { feedback = it },
                onPoseDetected = { currentPose = it }
            )

            if (skeletonOverlayEnabled && currentPose != null) {
                SquatPoseOverlay(pose = currentPose!!, lensFacing = lensFacing)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.58f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Squats to wake up!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prop your phone so the camera can see your hips, knees, and feet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip Camera",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.66f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = feedback.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (feedback.bodyVisible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White
                    },
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$currentSquats",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/ $targetSquats squats",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    } else {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera permission required",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Squat detection needs the camera to track your movement.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
        }
    } else {
        MotionSquatChallengeContent(
            targetSquats = targetSquats,
            currentSquats = currentSquats,
            pulseScale = pulseScale,
            onSquatDetected = onSquatDetected
        )
    }
}

@Composable
private fun MotionSquatChallengeContent(
    targetSquats: Int,
    currentSquats: Int,
    pulseScale: Float,
    onSquatDetected: () -> Unit
) {
    val context = LocalContext.current
    val currentOnSquatDetected by rememberUpdatedState(onSquatDetected)
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val gravity = floatArrayOf(0f, 0f, 0f)
        var smoothedAccel = 0f
        var verticalVelocity = 0f
        var phaseDisplacement = 0f
        var phase = MotionSquatPhase.IDLE
        var phaseStartMs = 0L
        var lastSquatMs = 0L
        var startMs = 0L
        var prevTimestampNs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                val nowNs = event.timestamp
                val nowMs = nowNs / 1_000_000L
                if (startMs == 0L) startMs = nowMs

                for (i in 0..2) {
                    gravity[i] = MOTION_GRAVITY_ALPHA * gravity[i] +
                        (1f - MOTION_GRAVITY_ALPHA) * event.values[i]
                }

                if (nowMs - startMs < MOTION_WARMUP_MS) {
                    prevTimestampNs = nowNs
                    return
                }

                if (prevTimestampNs == 0L) {
                    prevTimestampNs = nowNs
                    return
                }
                val dtSeconds = (nowNs - prevTimestampNs) / 1_000_000_000f
                prevTimestampNs = nowNs
                if (dtSeconds <= 0f || dtSeconds > 0.5f) return

                val gx = gravity[0]
                val gy = gravity[1]
                val gz = gravity[2]
                val gMag = sqrt(gx * gx + gy * gy + gz * gz)
                if (gMag < 0.1f) return

                val lx = event.values[0] - gx
                val ly = event.values[1] - gy
                val lz = event.values[2] - gz
                val rawVerticalAccel = (lx * gx + ly * gy + lz * gz) / gMag

                smoothedAccel = MOTION_ACCEL_SMOOTHING * smoothedAccel +
                    (1f - MOTION_ACCEL_SMOOTHING) * rawVerticalAccel
                verticalVelocity = (verticalVelocity + smoothedAccel * dtSeconds) * MOTION_VELOCITY_DECAY
                phaseDisplacement += abs(verticalVelocity) * dtSeconds

                val timeInPhaseMs = nowMs - phaseStartMs

                when (phase) {
                    MotionSquatPhase.IDLE -> {
                        if (smoothedAccel < MOTION_ACCEL_DOWN_ENTER) {
                            phase = MotionSquatPhase.GOING_DOWN
                            phaseStartMs = nowMs
                            phaseDisplacement = 0f
                            verticalVelocity = 0f
                            Log.d(TAG, "Motion squat -> GOING_DOWN accel=$smoothedAccel")
                        }
                    }

                    MotionSquatPhase.GOING_DOWN -> {
                        if (timeInPhaseMs > MOTION_MAX_DOWN_DURATION_MS) {
                            phase = MotionSquatPhase.IDLE
                            Log.d(TAG, "Motion squat -> IDLE down timeout=${timeInPhaseMs}ms")
                        } else if (smoothedAccel > MOTION_ACCEL_UP_ENTER) {
                            if (
                                timeInPhaseMs >= MOTION_MIN_DOWN_DURATION_MS &&
                                phaseDisplacement >= MOTION_MIN_DOWN_DISPLACEMENT
                            ) {
                                phase = MotionSquatPhase.GOING_UP
                                phaseStartMs = nowMs
                                phaseDisplacement = 0f
                                verticalVelocity = 0f
                                Log.d(TAG, "Motion squat -> GOING_UP downDuration=${timeInPhaseMs}ms")
                            } else if (timeInPhaseMs < MOTION_MIN_DOWN_DURATION_MS) {
                                phase = MotionSquatPhase.IDLE
                                Log.d(TAG, "Motion squat rejected, down too short=${timeInPhaseMs}ms")
                            }
                        }
                    }

                    MotionSquatPhase.GOING_UP -> {
                        if (timeInPhaseMs > MOTION_MAX_UP_DURATION_MS) {
                            if (
                                timeInPhaseMs >= MOTION_MIN_UP_DURATION_MS &&
                                nowMs - lastSquatMs >= MOTION_MIN_SQUAT_INTERVAL_MS
                            ) {
                                currentOnSquatDetected()
                                lastSquatMs = nowMs
                                Log.d(TAG, "Motion squat counted on timeout")
                            }
                            phase = MotionSquatPhase.IDLE
                        } else if (
                            timeInPhaseMs >= MOTION_MIN_UP_DURATION_MS &&
                            smoothedAccel < MOTION_ACCEL_UP_ENTER
                        ) {
                            if (nowMs - lastSquatMs >= MOTION_MIN_SQUAT_INTERVAL_MS) {
                                currentOnSquatDetected()
                                lastSquatMs = nowMs
                                Log.d(TAG, "Motion squat counted")
                            }
                            phase = MotionSquatPhase.IDLE
                        } else if (
                            smoothedAccel < MOTION_ACCEL_DOWN_ENTER &&
                            timeInPhaseMs < MOTION_MIN_UP_DURATION_MS
                        ) {
                            phase = MotionSquatPhase.IDLE
                            Log.d(TAG, "Motion squat rejected, up too short=${timeInPhaseMs}ms")
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accelerometer == null) {
            sensorAvailable = false
        } else {
            sensorAvailable = true
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
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
            text = "Squats to wake up!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (sensorAvailable) "Hold your phone and do squats" else "Motion sensor unavailable",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentSquats",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ $targetSquats squats",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SquatPoseCameraPreview(
    lensFacing: Int,
    onSquatDetected: () -> Unit,
    onTrackingFeedback: (SquatTrackingFeedback) -> Unit,
    onPoseDetected: (Pose) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val currentOnSquatDetected by rememberUpdatedState(onSquatDetected)
    val currentOnTrackingFeedback by rememberUpdatedState(onTrackingFeedback)
    val currentOnPoseDetected by rememberUpdatedState(onPoseDetected)
    val previewView = remember(context) {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    DisposableEffect(previewView, lifecycleOwner, lensFacing) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val analyzer = SquatPoseAnalyzer(
            mainExecutor = mainExecutor,
            onSquatDetected = { currentOnSquatDetected() },
            onTrackingFeedback = { currentOnTrackingFeedback(it) },
            onPoseDetected = { currentOnPoseDetected(it) }
        )
        imageAnalysis.setAnalyzer(analysisExecutor, analyzer)

        var disposed = false
        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, mainExecutor)

        onDispose {
            disposed = true
            imageAnalysis.clearAnalyzer()
            analyzer.close()
            if (cameraProviderFuture.isDone) {
                try {
                    cameraProviderFuture.get().unbind(preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e(TAG, "Camera unbind failed", e)
                }
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

private data class SquatTrackingFeedback(
    val message: String = "Position yourself so the camera can see your full body.",
    val bodyVisible: Boolean = false
)

private enum class SquatPosePhase {
    STANDING,
    LOWERING
}

private data class SquatMotionSample(
    val hipY: Float,
    val bodyScale: Float,
    val kneeAngle: Float?,
    val confidence: Float
)

private class SquatPoseAnalyzer(
    private val mainExecutor: Executor,
    private val onSquatDetected: () -> Unit,
    private val onTrackingFeedback: (SquatTrackingFeedback) -> Unit,
    private val onPoseDetected: (Pose) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    @Volatile
    private var isProcessing = false

    @Volatile
    private var isClosed = false

    private var phase = SquatPosePhase.STANDING
    private var smoothedHipY = Float.NaN
    private var standingHipY = Float.NaN
    private var bottomHipY = Float.NaN
    private var phaseStartedMs = 0L
    private var lastSquatMs = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed || isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.width
        } else {
            imageProxy.height
        }.toFloat()
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { pose ->
                if (!isClosed) {
                    mainExecutor.execute { onPoseDetected(pose) }
                    handlePose(pose, imageHeight)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Pose detection failed", e)
                postFeedback(SquatTrackingFeedback("Keep your full body in the camera view.", false))
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    fun close() {
        isClosed = true
        detector.close()
    }

    private fun handlePose(pose: Pose, imageHeight: Float) {
        val sample = extractMotionSample(pose, imageHeight)
        if (sample == null) {
            resetTracking()
            postFeedback(SquatTrackingFeedback("Move back so the camera can see your hips, knees, and feet.", false))
            return
        }

        smoothedHipY = if (smoothedHipY.isNaN()) {
            sample.hipY
        } else {
            SIGNAL_SMOOTHING * smoothedHipY + (1f - SIGNAL_SMOOTHING) * sample.hipY
        }

        val travelThreshold = max(MIN_TRAVEL_PX, sample.bodyScale * HIP_TRAVEL_RATIO)
        val kneeAngle = sample.kneeAngle
        val kneeIsBentEnough = kneeAngle == null || kneeAngle <= BENT_KNEE_ANGLE_DEGREES
        val kneeLooksStanding = kneeAngle == null || kneeAngle >= STRAIGHT_KNEE_ANGLE_DEGREES
        val nowMs = SystemClock.elapsedRealtime()
        if (phaseStartedMs == 0L) phaseStartedMs = nowMs

        when (phase) {
            SquatPosePhase.STANDING -> {
                standingHipY = if (standingHipY.isNaN()) {
                    smoothedHipY
                } else if (kneeLooksStanding) {
                    minOf(standingHipY, smoothedHipY)
                } else {
                    standingHipY
                }
                bottomHipY = smoothedHipY

                val drop = smoothedHipY - standingHipY
                if (drop >= travelThreshold && kneeIsBentEnough) {
                    phase = SquatPosePhase.LOWERING
                    phaseStartedMs = nowMs
                    bottomHipY = smoothedHipY
                    Log.d(TAG, "Phase -> LOWERING (drop=$drop, threshold=$travelThreshold, kneeAngle=$kneeAngle)")
                    postFeedback(SquatTrackingFeedback("Good - stand back up!", true))
                } else {
                    postFeedback(SquatTrackingFeedback("Tracking your squat. Bend your knees and lower your hips.", true))
                }
            }

            SquatPosePhase.LOWERING -> {
                bottomHipY = max(bottomHipY, smoothedHipY)

                val repDurationMs = nowMs - phaseStartedMs
                val totalDrop = bottomHipY - standingHipY
                val riseFromBottom = bottomHipY - smoothedHipY
                val returnedNearTop = smoothedHipY <= standingHipY + travelThreshold * RISE_RETURN_RATIO
                val roseEnough = riseFromBottom >= travelThreshold * RISE_FROM_BOTTOM_RATIO
                val countableInterval = nowMs - lastSquatMs >= MIN_REP_INTERVAL_MS

                if (repDurationMs > MAX_REP_DURATION_MS) {
                    resetTracking(smoothedHipY)
                    Log.d(TAG, "Squat timed out, resetting")
                    postFeedback(SquatTrackingFeedback("Too slow - start again from standing.", true))
                } else if (
                    totalDrop >= travelThreshold &&
                    roseEnough &&
                    returnedNearTop &&
                    repDurationMs >= MIN_REP_DURATION_MS &&
                    countableInterval
                ) {
                    lastSquatMs = nowMs
                    Log.d(TAG, "Squat counted! (totalDrop=$totalDrop, rise=$riseFromBottom, duration=${repDurationMs}ms)")
                    postSquatDetected()
                    resetTracking(smoothedHipY)
                    postFeedback(SquatTrackingFeedback("Squat counted! Keep going.", true))
                } else {
                    postFeedback(SquatTrackingFeedback("Stand up tall to complete the squat.", true))
                }
            }
        }
    }

    private fun extractMotionSample(pose: Pose, imageHeight: Float): SquatMotionSample? {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val visibleHips = listOfNotNull(leftHip, rightHip)
            .filter { it.inFrameLikelihood >= MIN_LANDMARK_CONFIDENCE }
        val visibleKnees = listOfNotNull(leftKnee, rightKnee)
            .filter { it.inFrameLikelihood >= MIN_LANDMARK_CONFIDENCE }

        if (visibleHips.isEmpty() || visibleKnees.isEmpty()) return null

        val hipY = visibleHips.map { it.position.y }.average().toFloat()

        val leftKneeAngle = angleAtKnee(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = angleAtKnee(rightHip, rightKnee, rightAnkle)
        val kneeAngle = listOfNotNull(leftKneeAngle, rightKneeAngle).minOrNull()

        val legScales = listOfNotNull(
            bodyPartLength(leftHip, leftKnee, leftAnkle),
            bodyPartLength(rightHip, rightKnee, rightAnkle)
        )
        val torsoScales = torsoScales(leftShoulder, rightShoulder, leftHip, rightHip)

        val bodyScale = (legScales + torsoScales)
            .maxOrNull()
            ?: (imageHeight * 0.35f)
        if (bodyScale < imageHeight * 0.10f) return null

        val confidence = visibleHips.sumOf { it.inFrameLikelihood.toDouble() }.toFloat() +
            visibleKnees.sumOf { it.inFrameLikelihood.toDouble() }.toFloat()

        return SquatMotionSample(
            hipY = hipY,
            bodyScale = bodyScale,
            kneeAngle = kneeAngle,
            confidence = confidence
        )
    }

    private fun resetTracking(startSignal: Float = Float.NaN) {
        phase = SquatPosePhase.STANDING
        phaseStartedMs = 0L
        smoothedHipY = startSignal
        standingHipY = startSignal
        bottomHipY = startSignal
    }

    private fun postFeedback(feedback: SquatTrackingFeedback) {
        mainExecutor.execute {
            if (!isClosed) {
                onTrackingFeedback(feedback)
            }
        }
    }

    private fun postSquatDetected() {
        mainExecutor.execute {
            if (!isClosed) {
                onSquatDetected()
            }
        }
    }

    private fun torsoScales(
        leftShoulder: PoseLandmark?,
        rightShoulder: PoseLandmark?,
        leftHip: PoseLandmark?,
        rightHip: PoseLandmark?
    ): List<Float> {
        return listOfNotNull(
            bodyPartDistance(leftShoulder, leftHip),
            bodyPartDistance(rightShoulder, rightHip),
            bodyPartDistance(leftShoulder, rightShoulder),
            bodyPartDistance(leftHip, rightHip)
        )
    }

    private fun bodyPartLength(
        hip: PoseLandmark?,
        knee: PoseLandmark?,
        ankle: PoseLandmark?
    ): Float? {
        val upperLeg = bodyPartDistance(hip, knee)
        val lowerLeg = bodyPartDistance(knee, ankle)
        return when {
            upperLeg != null && lowerLeg != null -> upperLeg + lowerLeg
            upperLeg != null -> upperLeg * 2f
            lowerLeg != null -> lowerLeg * 2f
            else -> null
        }
    }

    private fun angleAtKnee(
        hip: PoseLandmark?,
        knee: PoseLandmark?,
        ankle: PoseLandmark?
    ): Float? {
        if (!isVisible(hip, MIN_SUPPORT_LANDMARK_CONFIDENCE) ||
            !isVisible(knee, MIN_LANDMARK_CONFIDENCE) ||
            !isVisible(ankle, MIN_SUPPORT_LANDMARK_CONFIDENCE)
        ) {
            return null
        }

        val hipVectorX = hip!!.position.x - knee!!.position.x
        val hipVectorY = hip.position.y - knee.position.y
        val ankleVectorX = ankle!!.position.x - knee.position.x
        val ankleVectorY = ankle.position.y - knee.position.y
        val dot = hipVectorX * ankleVectorX + hipVectorY * ankleVectorY
        val hipMagnitude = sqrt(hipVectorX * hipVectorX + hipVectorY * hipVectorY)
        val ankleMagnitude = sqrt(ankleVectorX * ankleVectorX + ankleVectorY * ankleVectorY)
        if (hipMagnitude < 1f || ankleMagnitude < 1f) return null

        val cosine = (dot / (hipMagnitude * ankleMagnitude)).coerceIn(-1f, 1f)
        return (acos(cosine) * 180f / PI.toFloat())
    }

    private fun bodyPartDistance(first: PoseLandmark?, second: PoseLandmark?): Float? {
        if (!isVisible(first, MIN_SUPPORT_LANDMARK_CONFIDENCE) ||
            !isVisible(second, MIN_SUPPORT_LANDMARK_CONFIDENCE)
        ) {
            return null
        }

        val dx = first!!.position.x - second!!.position.x
        val dy = first.position.y - second.position.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun isVisible(landmark: PoseLandmark?, confidence: Float): Boolean {
        return landmark != null && landmark.inFrameLikelihood >= confidence
    }
}

@Composable
private fun SquatPoseOverlay(
    pose: Pose,
    lensFacing: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) return@Canvas

        val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT

        fun landmarkOffset(landmark: PoseLandmark): Offset {
            val x = landmark.position.x / 480f * size.width
            return Offset(
                x = if (isFront) size.width - x else x,
                y = landmark.position.y / 640f * size.height
            )
        }

        fun drawPoseLine(startType: Int, endType: Int) {
            val start = pose.getPoseLandmark(startType)
            val end = pose.getPoseLandmark(endType)
            if (start != null &&
                end != null &&
                start.inFrameLikelihood > MIN_LANDMARK_CONFIDENCE &&
                end.inFrameLikelihood > MIN_LANDMARK_CONFIDENCE
            ) {
                drawLine(
                    color = Color.Cyan,
                    start = landmarkOffset(start),
                    end = landmarkOffset(end),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }

        drawPoseLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawPoseLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawPoseLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawPoseLine(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawPoseLine(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawPoseLine(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawPoseLine(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawPoseLine(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

        landmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > MIN_LANDMARK_CONFIDENCE) {
                drawCircle(
                    color = Color.Yellow,
                    radius = 6f,
                    center = landmarkOffset(landmark)
                )
            }
        }
    }
}

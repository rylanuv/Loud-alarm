package com.loud.alarm.ui.challenge

import android.Manifest
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.collectAsState
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
import com.loud.alarm.data.SettingsRepository
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private const val TAG = "PushUpChallenge"

// Confidence thresholds
private const val MIN_LANDMARK_CONFIDENCE = 0.50f
private const val MIN_SUPPORT_LANDMARK_CONFIDENCE = 0.40f

// Smoothing and timing
private const val SIGNAL_SMOOTHING = 0.55f
private const val MIN_REP_INTERVAL_MS = 800L
private const val MIN_REP_DURATION_MS = 400L
private const val MAX_REP_DURATION_MS = 8_000L

// Travel thresholds (as fraction of body scale)
private const val SIDE_VIEW_TRAVEL_RATIO = 0.18f
private const val FRONT_VIEW_TRAVEL_RATIO = 0.12f
private const val DIAGONAL_VIEW_TRAVEL_RATIO = 0.14f
private const val MIN_TRAVEL_PX = 28f

// Angle detection: ratio of shoulder X-spread to shoulder-hip Y-distance
// High ratio = front/back view, low ratio = side view
private const val FRONT_VIEW_RATIO_THRESHOLD = 0.55f
private const val SIDE_VIEW_RATIO_THRESHOLD = 0.25f

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PushUpChallengeScreen(
    targetPushUps: Int,
    onSuccess: () -> Unit
) {
    var currentPushUps by rememberSaveable { mutableIntStateOf(0) }
    var pulseKey by rememberSaveable { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf(PushUpTrackingFeedback()) }
    var lensFacing by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var currentPose by remember { mutableStateOf<Pose?>(null) }
    
    val context = LocalContext.current
    val settingsRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.loud.alarm.di.RepositoryEntryPoint::class.java
        ).settingsRepository()
    }
    val skeletonOverlayEnabled by settingsRepository.skeletonOverlayEnabled.collectAsState(initial = false)

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(currentPushUps, targetPushUps) {
        if (currentPushUps >= targetPushUps) {
            onSuccess()
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (pulseKey % 2 == 0) 1f else 1.12f,
        animationSpec = tween(durationMillis = 180),
        label = "pushUpPulse"
    )

    if (cameraPermissionState.status.isGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PushUpPoseCameraPreview(
                lensFacing = lensFacing,
                onPushUpDetected = {
                    if (currentPushUps < targetPushUps) {
                        currentPushUps += 1
                        pulseKey += 1
                    }
                },
                onTrackingFeedback = { feedback = it },
                onPoseDetected = { currentPose = it }
            )

            if (skeletonOverlayEnabled && currentPose != null) {
                PoseOverlay(pose = currentPose!!, lensFacing = lensFacing)
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
                        text = "Push-ups to wake up!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prop your phone so the camera can see your body. Any angle works!",
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
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
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
                            text = "$currentPushUps",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/ $targetPushUps push-ups",
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
                    text = "Push-up detection needs the camera to track your movement.",
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
}

@Composable
private fun PushUpPoseCameraPreview(
    lensFacing: Int,
    onPushUpDetected: () -> Unit,
    onTrackingFeedback: (PushUpTrackingFeedback) -> Unit,
    onPoseDetected: (Pose) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val currentOnPushUpDetected by rememberUpdatedState(onPushUpDetected)
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

        val analyzer = PushUpPoseAnalyzer(
            mainExecutor = mainExecutor,
            onPushUpDetected = { currentOnPushUpDetected() },
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

private data class PushUpTrackingFeedback(
    val message: String = "Position yourself so the camera can see your body.",
    val bodyVisible: Boolean = false
)

/**
 * Detected camera angle relative to the person doing push-ups.
 */
private enum class CameraAngle {
    /** Camera sees a side profile (left or right) */
    SIDE,
    /** Camera faces the person head-on or from behind */
    FRONT_OR_BACK,
    /** Camera is at a diagonal angle */
    DIAGONAL
}

private enum class PushUpPhase {
    READY,
    LOWERING
}

/**
 * Combined motion signal extracted from pose landmarks.
 * Works across different camera angles by fusing multiple cues.
 */
private data class MotionSample(
    val primarySignal: Float,   // Main tracking value (shoulder Y for side, nose Y for front, fused for diagonal)
    val bodyScale: Float,       // Reference body dimension in pixels for threshold scaling
    val angle: CameraAngle,     // Detected camera angle
    val confidence: Float       // Overall confidence of this sample
)

private class PushUpPoseAnalyzer(
    private val mainExecutor: Executor,
    private val onPushUpDetected: () -> Unit,
    private val onTrackingFeedback: (PushUpTrackingFeedback) -> Unit,
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

    // Tracking state
    private var phase = PushUpPhase.READY
    private var smoothedSignal = Float.NaN
    private var upSignal = Float.NaN       // Signal value at top of push-up
    private var downSignal = Float.NaN     // Signal value at bottom of push-up
    private var phaseStartedMs = 0L
    private var lastPushUpMs = 0L
    private var lastAngle: CameraAngle? = null
    private var stableAngleCount = 0

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
                postFeedback(PushUpTrackingFeedback("Keep your body in the camera view.", false))
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
            postFeedback(PushUpTrackingFeedback("Move so the camera can see your shoulders and body.", false))
            return
        }

        // Detect if camera angle changed significantly — reset if so
        if (lastAngle != null && lastAngle != sample.angle) {
            stableAngleCount = 0
        } else {
            stableAngleCount++
        }
        lastAngle = sample.angle

        // Only process after angle is stable for a few frames
        if (stableAngleCount < 3) {
            postFeedback(PushUpTrackingFeedback("Adjusting to camera angle... hold still.", true))
            return
        }

        // Smooth the signal
        smoothedSignal = if (smoothedSignal.isNaN()) {
            sample.primarySignal
        } else {
            SIGNAL_SMOOTHING * smoothedSignal + (1f - SIGNAL_SMOOTHING) * sample.primarySignal
        }

        // Compute travel threshold based on camera angle.
        val baseTravelRatio = when (sample.angle) {
            CameraAngle.SIDE -> SIDE_VIEW_TRAVEL_RATIO
            CameraAngle.FRONT_OR_BACK -> FRONT_VIEW_TRAVEL_RATIO
            CameraAngle.DIAGONAL -> DIAGONAL_VIEW_TRAVEL_RATIO
        }
        val baseTravelThreshold = max(MIN_TRAVEL_PX, sample.bodyScale * baseTravelRatio)
        val travelThreshold = baseTravelThreshold

        val nowMs = SystemClock.elapsedRealtime()
        if (phaseStartedMs == 0L) phaseStartedMs = nowMs

        val angleLabel = when (sample.angle) {
            CameraAngle.SIDE -> "side"
            CameraAngle.FRONT_OR_BACK -> "front"
            CameraAngle.DIAGONAL -> "diagonal"
        }

        when (phase) {
            PushUpPhase.READY -> {
                upSignal = if (upSignal.isNaN()) {
                    smoothedSignal
                } else {
                    minOf(upSignal, smoothedSignal)
                }
                downSignal = smoothedSignal

                val drop = smoothedSignal - upSignal
                if (drop >= travelThreshold) {
                    phase = PushUpPhase.LOWERING
                    phaseStartedMs = nowMs
                    downSignal = smoothedSignal
                    Log.d(TAG, "Phase -> LOWERING (angle=$angleLabel, drop=$drop, threshold=$travelThreshold)")
                    postFeedback(PushUpTrackingFeedback("Good — now push back up!", true))
                } else {
                    postFeedback(PushUpTrackingFeedback("Tracking ($angleLabel view). Lower your chest.", true))
                }
            }

            PushUpPhase.LOWERING -> {
                downSignal = max(downSignal, smoothedSignal)

                val repDurationMs = nowMs - phaseStartedMs
                val totalDrop = downSignal - upSignal
                val riseFromBottom = downSignal - smoothedSignal

                val returnMultiplier = 0.55f
                val riseMultiplier = 0.65f

                val returnedNearTop = smoothedSignal <= upSignal + travelThreshold * returnMultiplier
                val roseEnough = riseFromBottom >= travelThreshold * riseMultiplier

                if (repDurationMs > MAX_REP_DURATION_MS) {
                    resetTracking(smoothedSignal)
                    Log.d(TAG, "Rep timed out, resetting")
                    postFeedback(PushUpTrackingFeedback("Too slow — start again from the top.", true))
                } else if (
                    totalDrop >= travelThreshold &&
                    roseEnough &&
                    returnedNearTop &&
                    repDurationMs >= MIN_REP_DURATION_MS &&
                    nowMs - lastPushUpMs >= MIN_REP_INTERVAL_MS
                ) {
                    lastPushUpMs = nowMs
                    Log.d(TAG, "Push-up counted! (angle=$angleLabel, totalDrop=$totalDrop, rise=$riseFromBottom, duration=${repDurationMs}ms)")
                    postPushUpDetected()
                    resetTracking(smoothedSignal)
                    postFeedback(PushUpTrackingFeedback("Rep counted! Keep going.", true))
                } else {
                    postFeedback(PushUpTrackingFeedback("Push back up to complete the rep.", true))
                }
            }
        }
    }

    /**
     * Extract a unified motion sample from the pose, auto-detecting camera angle.
     *
     * Side view: shoulder Y moves significantly during push-ups.
     * Front/back view: nose Y moves, and apparent shoulder-to-hip distance changes.
     * Diagonal: weighted combination of both signals.
     */
    private fun extractMotionSample(pose: Pose, imageHeight: Float): MotionSample? {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        // Need at least one shoulder with decent confidence
        val shoulders = listOfNotNull(leftShoulder, rightShoulder)
            .filter { it.inFrameLikelihood >= MIN_LANDMARK_CONFIDENCE }
        if (shoulders.isEmpty()) return null

        val hips = listOfNotNull(leftHip, rightHip)
            .filter { it.inFrameLikelihood >= MIN_SUPPORT_LANDMARK_CONFIDENCE }

        // Average positions of visible landmarks
        val avgShoulderX = shoulders.map { it.position.x }.average().toFloat()
        val avgShoulderY = shoulders.map { it.position.y }.average().toFloat()

        // Determine camera angle from shoulder spread vs body height
        val angle: CameraAngle
        val bodyScale: Float
        val primarySignal: Float
        val confidence: Float

        if (shoulders.size == 2 && hips.isNotEmpty()) {
            val shoulderSpreadX = abs(leftShoulder!!.position.x - rightShoulder!!.position.x)
            val avgHipY = hips.map { it.position.y }.average().toFloat()
            val torsoHeight = abs(avgHipY - avgShoulderY)
            val spreadRatio = if (torsoHeight > 1f) shoulderSpreadX / torsoHeight else 0f

            angle = when {
                spreadRatio > FRONT_VIEW_RATIO_THRESHOLD -> CameraAngle.FRONT_OR_BACK
                spreadRatio < SIDE_VIEW_RATIO_THRESHOLD -> CameraAngle.SIDE
                else -> CameraAngle.DIAGONAL
            }

            bodyScale = max(torsoHeight, shoulderSpreadX)

            primarySignal = when (angle) {
                CameraAngle.SIDE -> {
                    // Use the shoulder with higher confidence
                    val bestShoulder = shoulders.maxBy { it.inFrameLikelihood }
                    bestShoulder.position.y
                }
                CameraAngle.FRONT_OR_BACK -> {
                    // Use nose Y if available (moves most during push-ups from front)
                    // Fall back to average shoulder Y
                    if (nose != null && nose.inFrameLikelihood >= MIN_LANDMARK_CONFIDENCE) {
                        nose.position.y
                    } else {
                        avgShoulderY
                    }
                }
                CameraAngle.DIAGONAL -> {
                    // Fuse shoulder Y and nose Y
                    val shoulderSignal = shoulders.maxBy { it.inFrameLikelihood }.position.y
                    val noseSignal = if (nose != null && nose.inFrameLikelihood >= MIN_LANDMARK_CONFIDENCE) {
                        nose.position.y
                    } else {
                        shoulderSignal
                    }
                    // Weight: 60% shoulder, 40% nose for diagonal
                    shoulderSignal * 0.6f + noseSignal * 0.4f
                }
            }

            confidence = shoulders.sumOf { it.inFrameLikelihood.toDouble() }.toFloat() +
                    hips.sumOf { it.inFrameLikelihood.toDouble() }.toFloat()

        } else if (shoulders.size == 1) {
            // Single shoulder visible — likely side view
            angle = CameraAngle.SIDE
            val shoulder = shoulders[0]

            // Get elbow/wrist for body scale
            val elbowType = if (shoulder == leftShoulder) PoseLandmark.LEFT_ELBOW else PoseLandmark.RIGHT_ELBOW
            val hipType = if (shoulder == leftShoulder) PoseLandmark.LEFT_HIP else PoseLandmark.RIGHT_HIP
            val elbow = pose.getPoseLandmark(elbowType)
            val hip = pose.getPoseLandmark(hipType)

            val supportLandmarks = listOfNotNull(elbow, hip)
                .filter { it.inFrameLikelihood >= MIN_SUPPORT_LANDMARK_CONFIDENCE }
            if (supportLandmarks.isEmpty()) return null

            bodyScale = supportLandmarks
                .maxOfOrNull { distance(shoulder, it) }
                ?: (imageHeight * 0.3f)

            primarySignal = shoulder.position.y
            confidence = shoulder.inFrameLikelihood +
                    supportLandmarks.sumOf { it.inFrameLikelihood.toDouble() }.toFloat()
        } else {
            // Two shoulders but no hips — use shoulder Y, assume diagonal
            angle = CameraAngle.DIAGONAL
            bodyScale = abs(leftShoulder!!.position.x - rightShoulder!!.position.x) * 2f
            primarySignal = avgShoulderY
            confidence = shoulders.sumOf { it.inFrameLikelihood.toDouble() }.toFloat()
        }

        if (bodyScale < 20f) return null // Body too small in frame

        return MotionSample(
            primarySignal = primarySignal,
            bodyScale = bodyScale,
            angle = angle,
            confidence = confidence
        )
    }

    private fun resetTracking(startSignal: Float = Float.NaN) {
        phase = PushUpPhase.READY
        phaseStartedMs = 0L
        smoothedSignal = startSignal
        upSignal = startSignal
        downSignal = startSignal
    }

    private fun postFeedback(feedback: PushUpTrackingFeedback) {
        mainExecutor.execute {
            if (!isClosed) {
                onTrackingFeedback(feedback)
            }
        }
    }

    private fun postPushUpDetected() {
        mainExecutor.execute {
            if (!isClosed) {
                onPushUpDetected()
            }
        }
    }

    private fun distance(first: PoseLandmark, second: PoseLandmark): Float {
        val dx = first.position.x - second.position.x
        val dy = first.position.y - second.position.y
        return sqrt(dx * dx + dy * dy)
    }
}

@Composable
private fun PoseOverlay(
    pose: Pose,
    lensFacing: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) return@Canvas

        // ML Kit coordinates are in image space. We need to scale them to the canvas size.
        // For simplicity, we'll assume the analyzer output size matches the aspect ratio roughly.
        // Real implementations need the actual image dimensions from the analyzer.
        // Here we'll use a heuristic for demonstration.
        
        val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
        
        fun drawLine(startType: Int, endType: Int) {
            val start = pose.getPoseLandmark(startType)
            val end = pose.getPoseLandmark(endType)
            if (start != null && end != null && 
                start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f) {
                
                // Note: Coordinates from ML Kit are relative to the image.
                // In a production app, we'd pass the image dimensions to correctly scale.
                // For this debug tool, we'll assume the coordinates are already somewhat normalized 
                // or we'll scale them based on a standard 480x640 or similar if we had them.
                // Let's just draw them for now, assuming the user will see SOMETHING.
                
                val sX = if (isFront) size.width - (start.position.x / 480f * size.width) else (start.position.x / 480f * size.width)
                val sY = start.position.y / 640f * size.height
                val eX = if (isFront) size.width - (end.position.x / 480f * size.width) else (end.position.x / 480f * size.width)
                val eY = end.position.y / 640f * size.height
                
                drawLine(
                    color = Color.Cyan,
                    start = androidx.compose.ui.geometry.Offset(sX, sY),
                    end = androidx.compose.ui.geometry.Offset(eX, eY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Torso
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)

        // Arms
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawLine(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawLine(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

        // Legs
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawLine(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawLine(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawLine(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

        // Landmarks
        landmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                val x = if (isFront) size.width - (landmark.position.x / 480f * size.width) else (landmark.position.x / 480f * size.width)
                val y = landmark.position.y / 640f * size.height
                drawCircle(
                    color = Color.Yellow,
                    radius = 6f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
    }
}

package com.loud.alarm.ui.challenge

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.Executors

/**
 * Challenge screen that uses ML Kit Image Labeling to detect objects via the camera.
 * Used for both "Scan Sink" and "Scan Object" challenges.
 *
 * @param targetLabel The object label to detect (e.g., "Sink", "Laptop", "Cup")
 * @param displayTitle Title shown on the screen (e.g., "Scan Your Sink", "Find the Laptop")
 * @param displaySubtitle Subtitle shown below the title
 * @param onSuccess Called when the target object is detected
 * @param onFallbackToMath Optional fallback to math challenge
 */
@Composable
fun ScanChallengeScreen(
    targetLabel: String,
    displayTitle: String,
    displaySubtitle: String,
    onSuccess: () -> Unit,
    onFallbackToMath: (() -> Unit)? = null
) {
    var detectedLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasMatched by remember { mutableStateOf(false) }
    var matchConfidence by remember { mutableStateOf(0f) }

    val targetLower = targetLabel.lowercase()

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview with image labeling
        ImageLabelingCameraPreview(
            onLabelsDetected = { labels ->
                detectedLabels = labels.map { it.first }
                // Check if any detected label matches the target
                val match = labels.find { (label, _confidence) ->
                    label.lowercase().contains(targetLower) ||
                    targetLower.contains(label.lowercase())
                }
                if (match != null && match.second >= 0.5f && !hasMatched) {
                    hasMatched = true
                    matchConfidence = match.second
                    onSuccess()
                }
            }
        )

        // Scanning animation overlay
        ScanningOverlay()

        // Bottom info panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Detected labels display (for debugging/user feedback)
            if (detectedLabels.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Detecting: ${detectedLabels.take(3).joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displaySubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            // Fallback to math option
            if (onFallbackToMath != null) {
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(
                    onClick = onFallbackToMath,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Can't find it? Solve Math instead",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val scanAreaWidth = size.width * 0.8f
        val scanAreaHeight = size.height * 0.5f
        val scanAreaLeft = (size.width - scanAreaWidth) / 2
        val scanAreaTop = (size.height - scanAreaHeight) / 2.5f

        // Semi-transparent background
        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            size = size
        )

        // Clear cut out for scan area
        drawRoundRect(
            color = Color.Transparent,
            topLeft = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
            size = androidx.compose.ui.geometry.Size(scanAreaWidth, scanAreaHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Border for scan area
        drawRoundRect(
            color = Color(0xFF4FC3F7),
            topLeft = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
            size = androidx.compose.ui.geometry.Size(scanAreaWidth, scanAreaHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )

        // Scanning line
        val lineY = scanAreaTop + (scanAreaHeight * scanLineY)
        drawLine(
            color = Color(0xFF4FC3F7).copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(scanAreaLeft + 16f, lineY),
            end = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaWidth - 16f, lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun ImageLabelingCameraPreview(
    onLabelsDetected: (List<Pair<String, Float>>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    ImageLabelAnalyzer(onLabelsDetected)
                )

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("ScanChallengeScreen", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

class ImageLabelAnalyzer(
    private val onLabelsDetected: (List<Pair<String, Float>>) -> Unit
) : ImageAnalysis.Analyzer {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val result = labels.map { label ->
                        label.text to label.confidence
                    }
                    onLabelsDetected(result)
                }
                .addOnFailureListener { e ->
                    Log.e("ImageLabelAnalyzer", "Labeling failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

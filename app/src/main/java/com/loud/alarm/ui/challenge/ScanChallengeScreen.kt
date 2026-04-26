package com.loud.alarm.ui.challenge

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Arrangement
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/** Phases the scan screen transitions through after a match. */
private enum class ScanPhase {
    SCANNING,       // Actively looking for the object
    CONFIRMING,     // Object found — 1.5s scanning animation
    SUCCESS         // "Scanned Successfully!" — 1.5s then dismiss
}

private const val TAG = "ScanChallengeScreen"

// ── Confidence thresholds (lowered for Play Services thin model) ──────────
private const val DEFAULT_MATCH_CONFIDENCE = 0.18f
private const val KEYBOARD_MATCH_CONFIDENCE = 0.15f
private const val KEYBOARD_MUSICAL_INSTRUMENT_CONFIDENCE = 0.50f
private const val SMALL_OBJECT_MATCH_CONFIDENCE = 0.08f
private const val LARGE_OBJECT_MATCH_CONFIDENCE = 0.12f

// ── Consecutive / sliding-window frame requirements ──────────────────────
private const val CONSECUTIVE_FRAMES_REQUIRED = 2
private const val SLIDING_WINDOW_SIZE = 4
private const val SLIDING_WINDOW_HITS_REQUIRED = 2

/**
 * Map from our app's user-facing target labels to all the ML Kit labels
 * (from Image Labeling) that should be accepted as a valid match.
 * Keys and values are all lowercase.
 *
 * The Play Services (thin) variant of Image Labeling returns a set of
 * labels that sometimes differs from the bundled variant. We add broad
 * coverage to maximise hit rate.
 */
private val targetAliasMap: Map<String, Set<String>> = mapOf(
    "toothbrush" to setOf(
        "toothbrush", "tooth brushing", "brush", "dental",
        "oral hygiene", "bathroom accessory", "personal care"
    ),
    "sink" to setOf(
        "sink", "plumbing fixture", "bathroom sink", "kitchen sink",
        "washbasin", "tap", "faucet", "plumbing", "bathroom",
        "countertop", "basin", "lavatory", "wash", "water"
    ),
    "coffee cup" to setOf(
        "coffee cup", "cup", "mug", "coffee", "drinkware",
        "tableware", "serveware", "teacup", "espresso",
        "ceramic", "drink", "beverage", "porcelain", "pottery"
    ),
    "bowl" to setOf(
        "bowl", "mixing bowl", "tableware", "ceramic",
        "serveware", "dishware", "dish", "plate", "porcelain",
        "pottery", "kitchenware"
    ),
    "shoe" to setOf(
        "shoe", "sneakers", "footwear", "boot", "running shoe",
        "athletic shoe", "walking shoe", "outdoor shoe", "tennis shoe",
        "sandal", "slipper", "high heel", "loafer", "sports shoe",
        "sole", "lace", "ankle"
    ),
    "book" to setOf(
        "book", "publication", "novel", "textbook", "paper",
        "document", "notebook", "paper product", "magazine",
        "reading", "page", "text", "literature", "hardcover",
        "paperback", "booklet", "diary", "journal"
    ),
    "plant" to setOf(
        "plant", "houseplant", "flower", "flowerpot", "herb",
        "potted plant", "vascular plant", "grass", "leaf",
        "shrub", "tree", "vegetation", "garden", "flora",
        "green", "foliage", "succulent", "cactus", "fern",
        "indoor plant", "nature"
    ),
    "laptop" to setOf(
        "laptop", "notebook", "computer", "personal computer",
        "netbook", "electronic device", "output device",
        "computer hardware", "screen", "display", "keyboard",
        "technology", "portable computer", "computing"
    ),
    "fruit" to setOf(
        "fruit", "apple", "banana", "orange", "food",
        "natural foods", "produce", "citrus", "pineapple",
        "mango", "grapes", "berry", "strawberry", "lemon",
        "watermelon", "pear", "peach", "kiwi", "plum",
        "cherry", "melon", "tropical fruit", "fresh",
        "healthy", "organic"
    ),
    "bottle" to setOf(
        "bottle", "water bottle", "plastic bottle", "glass bottle",
        "wine bottle", "drinkware", "beverage can", "drink",
        "container", "flask", "jar", "liquid", "cap", "lid",
        "beverage", "beer bottle", "soda bottle"
    ),
    "watch" to setOf(
        "watch", "wristwatch", "analog watch", "clock", "wrist",
        "timepiece", "digital watch", "smartwatch", "accessory",
        "strap", "band", "dial", "chronometer"
    ),
    "key" to setOf(
        "key", "keys", "key chain", "keychain", "lock",
        "metal", "brass", "security", "door key", "car key",
        "key ring", "padlock"
    ),
    "backpack" to setOf(
        "backpack", "bag", "luggage and bags", "rucksack",
        "handbag", "shoulder bag", "knapsack", "satchel",
        "travel bag", "school bag", "daypack", "pack",
        "luggage", "baggage", "carry"
    ),
    "chair" to setOf(
        "chair", "office chair", "furniture", "seat", "stool",
        "bench", "armrest", "armchair", "seating", "sofa",
        "couch", "recliner", "desk chair", "folding chair",
        "wood"
    ),
    "door" to setOf(
        "door", "door handle", "home door", "handle", "wood",
        "entrance", "doorway", "gate", "doorknob", "hinge",
        "frame", "threshold", "entry", "exit"
    ),
    "television" to setOf(
        "television", "tv", "monitor", "screen", "display device",
        "flat panel display", "led-backlit lcd display",
        "computer monitor", "television set", "lcd tv",
        "display", "electronic device", "entertainment",
        "plasma", "oled", "smart tv", "flat screen"
    ),
    "monitor" to setOf(
        "monitor", "computer monitor", "screen", "display device",
        "flat panel display", "led-backlit lcd display",
        "output device", "television", "desktop computer",
        "display", "lcd", "electronic device", "technology"
    ),
    "mouse" to setOf(
        "mouse", "computer mouse", "input device",
        "electronic device", "peripheral", "cursor",
        "pointing device", "wireless mouse", "optical mouse",
        "trackpad", "gadget", "technology", "computing"
    ),
    "keyboard" to setOf(
        "keyboard", "computer keyboard", "electronic keyboard",
        "musical keyboard", "music keyboard", "input device",
        "space bar", "numeric keypad", "peripheral",
        "typing", "keys", "qwerty", "key", "technology",
        "computing", "computer hardware",
        // Coarse class that sometimes matches computer keyboards
        "musical instrument"
    ),
    "scissors" to setOf(
        "scissors", "scissor", "pair of scissors", "shears",
        "cutting tool", "stationery", "office supplies", "blade",
        "snips", "clippers", "trimmer", "tool", "craft",
        "cut", "metal"
    ),
    "phone" to setOf(
        "phone", "smartphone", "mobile phone", "cell phone",
        "cellular phone", "telephone", "mobile device",
        "communication device", "portable communications device",
        "iphone", "android", "touchscreen", "electronic device",
        "gadget", "technology", "screen", "display"
    ),
    "umbrella" to setOf(
        "umbrella", "canopy", "shade", "parasol", "rain",
        "shelter", "cover", "awning", "sunshade"
    ),
    "calculator" to setOf(
        "calculator", "office equipment", "office supplies",
        "numeric keypad", "number", "electronics",
        "electronic device", "electronic instrument",
        "electronic engineering", "technology",
        "office instrument", "computing", "math",
        "arithmetic", "display", "button"
    ),
    "wallet" to setOf(
        "wallet", "purse", "billfold", "leather", "pocket",
        "money", "cash", "card", "credit card", "id",
        "accessory", "cardholder", "pouch", "coin purse"
    ),
    "refrigerator" to setOf(
        "refrigerator", "fridge", "major appliance",
        "kitchen appliance", "home appliance", "freezer",
        "appliance", "cooler", "cold", "kitchen", "food storage"
    ),
    "bed" to setOf(
        "bed", "bedroom", "bed frame", "mattress", "bed sheet",
        "pillow", "duvet", "bedding", "sleep", "blanket",
        "comforter", "quilt", "furniture", "cushion",
        "linen", "headboard"
    ),
    "bicycle" to setOf(
        "bicycle", "bike", "bicycle wheel", "cycle", "vehicle",
        "cycling", "bicycle tire", "bicycle frame", "pedal",
        "handlebar", "spoke", "chain", "gear", "wheel",
        "mountain bike", "road bike", "bmx"
    ),
    "toilet" to setOf(
        "toilet", "plumbing fixture", "toilet seat", "bathroom",
        "restroom", "lavatory", "commode", "wc", "flush",
        "ceramic", "porcelain", "plumbing"
    ),
    "clock" to setOf(
        "clock", "wall clock", "alarm clock", "timer", "watch",
        "time", "timepiece", "hour", "minute", "second",
        "dial", "analog", "digital", "chronometer"
    ),
    "headphones" to setOf(
        "headphones", "earphones", "headset", "audio equipment",
        "earbuds", "audio", "gadget", "music", "sound",
        "listening", "electronic device", "wireless",
        "bluetooth", "speaker", "ear"
    )
)

/**
 * Check whether a detected label matches the target using our alias map.
 * Falls back to normalized phrase matching if the target isn't in the map.
 */
private fun isLabelMatch(detectedLabel: String, targetLabel: String): Boolean {
    val detectedLower = normalizeLabel(detectedLabel)
    val targetLower = targetLabel.lowercase().trim()

    val aliases = (targetAliasMap[targetLower] ?: setOf(targetLabel))
        .map { normalizeLabel(it) }
        .filter { it.isNotBlank() }
        .toSet()

    // Exact normalized match first.
    if (detectedLower in aliases) return true

    // Phrase-level word boundary matching catches labels like "pair of scissors".
    if (aliases.any { alias ->
        containsPhrase(detectedLower, alias) || containsPhrase(alias, detectedLower)
    }) return true

    // Token overlap: if ANY single word in the detected label matches any alias word,
    // count it. This helps when ML Kit returns compound labels like
    // "Bathroom sink faucet" for target "sink".
    val detectedTokens = detectedLower.split(" ").filter { it.length > 2 }.toSet()
    val aliasTokens = aliases.flatMap { it.split(" ") }.filter { it.length > 2 }.toSet()
    if (detectedTokens.intersect(aliasTokens).isNotEmpty()) return true

    return false
}

private fun requiredConfidence(targetLabel: String, detectedLabel: String): Float {
    val targetLower = targetLabel.lowercase().trim()
    val detectedLower = normalizeLabel(detectedLabel)

    return when {
        targetLower == "keyboard" && detectedLower == "musical instrument" ->
            KEYBOARD_MUSICAL_INSTRUMENT_CONFIDENCE
        targetLower == "keyboard" -> KEYBOARD_MATCH_CONFIDENCE
        targetLower in setOf("scissors", "key", "toothbrush", "watch", "headphones",
            "mouse", "calculator", "wallet") ->
            SMALL_OBJECT_MATCH_CONFIDENCE
        targetLower in setOf("bed", "refrigerator", "door", "chair", "sink",
            "toilet", "television", "monitor") ->
            LARGE_OBJECT_MATCH_CONFIDENCE
        else -> DEFAULT_MATCH_CONFIDENCE
    }
}

private fun normalizeLabel(value: String): String {
    val cleaned = value
        .lowercase()
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    if (cleaned.isBlank()) return cleaned

    return cleaned
        .split(" ")
        .joinToString(" ") { token ->
            when {
                token.length > 4 && token.endsWith("ies") -> token.dropLast(3) + "y"
                token.length > 3 && token.endsWith("s") && !token.endsWith("ss") -> token.dropLast(1)
                else -> token
            }
        }
}

private fun containsPhrase(text: String, phrase: String): Boolean {
    if (text.isBlank() || phrase.isBlank()) return false
    return Regex("\\b${Regex.escape(phrase)}\\b").containsMatchIn(text)
}

/**
 * Challenge screen that uses ML Kit Image Labeling to detect objects via
 * the camera. Image Labeling identifies what the camera sees, and we match
 * detected labels against our alias table.
 *
 * Used for both "Scan Sink" and "Scan Object" challenges.
 *
 * @param targetLabel The object label to detect (e.g., "Sink", "Laptop", "Phone")
 * @param displayTitle Title shown on the screen
 * @param displaySubtitle Subtitle shown below the title
 * @param onSuccess Called when the target object is detected
 * @param onFallbackToMath Optional fallback to math challenge
 */
@OptIn(ExperimentalPermissionsApi::class)
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
    var consecutiveMatchCount by remember { mutableStateOf(0) }
    var recentFrameResults by remember { mutableStateOf(ArrayDeque<Boolean>()) }
    var scanPhase by remember { mutableStateOf(ScanPhase.SCANNING) }

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Phase transitions: CONFIRMING (1.5s) → SUCCESS (1.5s) → dismiss
    LaunchedEffect(scanPhase) {
        when (scanPhase) {
            ScanPhase.CONFIRMING -> {
                delay(1500L)
                scanPhase = ScanPhase.SUCCESS
            }
            ScanPhase.SUCCESS -> {
                delay(1500L)
                onSuccess()
            }
            else -> { /* SCANNING – nothing to do */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Camera preview with image labeling
            ImageLabelingCameraPreview(
                targetLabel = targetLabel,
                onLabelsDetected = { labels, matched, confidence ->
                    // Stop analyzing once we've entered the confirmation phase
                    if (scanPhase != ScanPhase.SCANNING) return@ImageLabelingCameraPreview

                    detectedLabels = labels
                    if (matched && !hasMatched) {
                        consecutiveMatchCount++

                        recentFrameResults.addLast(true)
                        if (recentFrameResults.size > SLIDING_WINDOW_SIZE) {
                            recentFrameResults.removeFirst()
                        }

                        val consecutiveOk = consecutiveMatchCount >= CONSECUTIVE_FRAMES_REQUIRED
                        val slidingOk = recentFrameResults.count { it } >= SLIDING_WINDOW_HITS_REQUIRED

                        if (consecutiveOk || slidingOk) {
                            Log.d(TAG, "Target '$targetLabel' CONFIRMED: " +
                                    "consecutive=$consecutiveMatchCount, " +
                                    "sliding=${recentFrameResults.count { it }}/$SLIDING_WINDOW_SIZE, " +
                                    "confidence=$confidence")
                            hasMatched = true
                            matchConfidence = confidence
                            // Enter confirming phase instead of instant dismiss
                            scanPhase = ScanPhase.CONFIRMING
                        }
                    } else if (!matched) {
                        consecutiveMatchCount = 0
                        recentFrameResults.addLast(false)
                        if (recentFrameResults.size > SLIDING_WINDOW_SIZE) {
                            recentFrameResults.removeFirst()
                        }
                    }
                }
            )

            // ── Overlay layer depends on phase ──
            when (scanPhase) {
                ScanPhase.SCANNING -> {
                    // Normal scanning overlay
                    ScanningOverlay()

                    // Bottom info panel
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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

                ScanPhase.CONFIRMING -> {
                    // "Scanning…" confirmation overlay
                    ConfirmingScanOverlay(targetLabel = targetLabel)
                }

                ScanPhase.SUCCESS -> {
                    // "Scanned Successfully!" overlay
                    ScanSuccessOverlay(targetLabel = targetLabel)
                }
            }

        } else {
            // Permission not granted UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This challenge needs camera access to detect the target object.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Grant Permission")
                }

                if (onFallbackToMath != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onFallbackToMath) {
                        Text(
                            "Use Math instead",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ── Confirming overlay (object detected, fast scan animation) ─────────────

@Composable
private fun ConfirmingScanOverlay(targetLabel: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "confirmScan")

    // Fast-pulsing scan line
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fastScanLine"
    )

    // Border glow pulse
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    // Animated progress (0→1 over 1.5s)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scan area overlay with green tint
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val scanAreaWidth = size.width * 0.8f
            val scanAreaHeight = size.height * 0.5f
            val scanAreaLeft = (size.width - scanAreaWidth) / 2
            val scanAreaTop = (size.height - scanAreaHeight) / 2.5f

            // Dark overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Clear scan area
            drawRoundRect(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
                size = androidx.compose.ui.geometry.Size(scanAreaWidth, scanAreaHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Green pulsing border
            drawRoundRect(
                color = Color(0xFF4CAF50).copy(alpha = borderAlpha),
                topLeft = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
                size = androidx.compose.ui.geometry.Size(scanAreaWidth, scanAreaHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // Fast green scanning line
            val lineY = scanAreaTop + (scanAreaHeight * scanLineY)
            drawLine(
                color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                start = androidx.compose.ui.geometry.Offset(scanAreaLeft + 16f, lineY),
                end = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaWidth - 16f, lineY),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Center content: spinner + text
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular progress indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(80.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 5.dp,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(progress.value * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Scanning $targetLabel…",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hold steady",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Success overlay (checkmark + "Scanned Successfully!") ─────────────────

@Composable
private fun ScanSuccessOverlay(targetLabel: String) {
    // Bounce-in animation for the check icon
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconBounce"
    )

    // Gentle glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "successGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4CAF50).copy(alpha = 0.25f),
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Glow ring behind icon
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        .border(2.dp, Color(0xFF4CAF50).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Scanned Successfully!",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$targetLabel detected",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF81C784),
                textAlign = TextAlign.Center
            )
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

/**
 * Camera preview that runs ML Kit Image Labeling directly on each frame.
 *
 * Previously we used Object Detection as a gatekeeper before Image Labeling,
 * but the base Object Detection model only returns ~5 coarse categories and
 * frequently fails to detect many everyday objects, causing the challenge to
 * never trigger. Now we run Image Labeling directly — it's fast enough on its
 * own and provides much better coverage.
 */
@Composable
fun ImageLabelingCameraPreview(
    targetLabel: String,
    onLabelsDetected: (labels: List<String>, matched: Boolean, confidence: Float) -> Unit
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
                    DirectImageLabelAnalyzer(targetLabel, onLabelsDetected)
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
                    Log.e(TAG, "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

/**
 * Simplified analyzer that runs ML Kit Image Labeling directly on every frame.
 * No Object Detection gatekeeper — Image Labeling alone is sufficient and
 * provides much better coverage for the variety of objects we support.
 */
class DirectImageLabelAnalyzer(
    private val targetLabel: String,
    private val onResult: (labels: List<String>, matched: Boolean, confidence: Float) -> Unit
) : ImageAnalysis.Analyzer {

    // Image labeler: very low pre-filter so we get ALL candidate labels.
    // Our alias-map matching + multi-frame requirement handles false positives.
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.03f)
            .build()
    )

    @Volatile
    private var isProcessing = false

    // Frame skip counter to avoid overwhelming the labeler while still
    // processing frames frequently enough for responsive detection.
    @Volatile
    private var frameCounter = 0

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        frameCounter++

        // Process every frame when not busy (STRATEGY_KEEP_ONLY_LATEST
        // already drops frames when the analyzer is backed up)
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val labelTexts = labels.map { "${it.text}(${"%.0f".format(it.confidence * 100)}%)" }
                val plainLabels = labels.map { it.text }

                // Log all labels for debugging (throttled to every 10th frame)
                if (labels.isNotEmpty() && frameCounter % 10 == 0) {
                    Log.d(TAG, "Frame #$frameCounter labels for target='$targetLabel': ${labelTexts.take(10)}")
                }

                // Check all matching labels and keep the strongest one.
                val match = labels
                    .filter { label -> isLabelMatch(label.text, targetLabel) }
                    .maxByOrNull { it.confidence }

                // Apply target-specific confidence thresholds.
                val matched = match != null &&
                    match.confidence >= requiredConfidence(targetLabel, match.text)

                if (matched) {
                    Log.d(TAG, "MATCH FOUND: target='$targetLabel', " +
                            "detected='${match!!.text}', confidence=${match.confidence}, " +
                            "all labels=${labelTexts.take(8)}")
                }

                onResult(plainLabels, matched, match?.confidence ?: 0f)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Image labeling failed", e)
                onResult(emptyList(), false, 0f)
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }
}

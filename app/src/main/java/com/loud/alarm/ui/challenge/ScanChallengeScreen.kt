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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.EntryPointAccessors
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

// ── Confidence thresholds ────────────────────────────────────────────────
private const val LABELER_CANDIDATE_CONFIDENCE = 0.05f
private const val DEFAULT_MATCH_CONFIDENCE = 0.15f
private const val SMALL_OBJECT_MATCH_CONFIDENCE = 0.10f
private const val LARGE_OBJECT_MATCH_CONFIDENCE = 0.12f
private const val SECONDARY_MATCH_CONFIDENCE = 0.20f
private const val TERTIARY_MATCH_CONFIDENCE = 0.25f
private const val SHORT_PRIMARY_LABEL_CONFIDENCE = 0.18f

// ── Consecutive / sliding-window frame requirements ──────────────────────
private const val CONSECUTIVE_FRAMES_REQUIRED = 2
private const val SLIDING_WINDOW_SIZE = 6
private const val SLIDING_WINDOW_HITS_REQUIRED = 3

/**
 * Broad alias map used as a tertiary fallback in the matching pipeline.
 * The primary matcher uses [targetLabelSpecs] for target-specific labels,
 * but this map catches additional coarse/generic labels that the model
 * may return when it can't identify the object precisely.
 * Keys and values are all lowercase.
 */
private val targetAliasMap: Map<String, Set<String>> = mapOf(

    "sink" to setOf(
        "sink", "plumbing fixture", "bathroom sink", "kitchen sink",
        "washbasin", "tap", "faucet", "plumbing", "bathroom",
        "basin", "lavatory", "porcelain", "ceramic"
    ),
    "coffee cup" to setOf(
        "coffee cup", "cup", "mug", "coffee", "drinkware",
        "tableware", "serveware", "teacup", "espresso",
        "ceramic", "porcelain", "pottery", "dishware", "beverage"
    ),

    "shoe" to setOf(
        "shoe", "sneakers", "footwear", "boot", "running shoe",
        "athletic shoe", "walking shoe", "outdoor shoe", "tennis shoe",
        "sandal", "slipper", "high heel", "loafer", "sports shoe",
        "leather", "rubber", "textile", "sole"
    ),
    "book" to setOf(
        "book", "publication", "novel", "textbook",
        "notebook", "paper product", "magazine",
        "reading", "literature", "hardcover",
        "paperback", "booklet", "diary", "journal",
        "comic book", "book jacket", "bookcase", "bookshop",
        "text", "paper", "document", "font", "rectangle",
        "page", "cover", "library", "writing", "education"
    ),
    "plant" to setOf(
        "plant", "houseplant", "flower", "flowerpot", "herb",
        "potted plant", "vascular plant", "leaf",
        "shrub", "tree", "vegetation", "flora",
        "foliage", "succulent", "cactus", "fern",
        "indoor plant", "green", "garden", "nature"
    ),
    "laptop" to setOf(
        "laptop", "notebook", "computer", "personal computer",
        "netbook", "output device",
        "computer hardware", "portable computer",
        "technology", "electronic device", "gadget", "screen"
    ),
    "fruit" to setOf(
        "fruit", "apple", "banana", "orange", "food",
        "natural foods", "produce", "citrus", "pineapple",
        "mango", "grapes", "berry", "strawberry", "lemon",
        "watermelon", "pear", "peach", "kiwi", "plum",
        "cherry", "melon", "tropical fruit", "jackfruit",
        "vegetable", "plant", "diet"
    ),

    "watch" to setOf(
        "watch", "wristwatch", "analog watch", "clock",
        "timepiece", "digital watch", "smartwatch",
        "chronometer", "stopwatch", "analog clock",
        "digital clock", "strap", "wrist"
    ),

    "backpack" to setOf(
        "backpack", "bag", "luggage and bags", "rucksack",
        "handbag", "shoulder bag", "knapsack", "satchel",
        "travel bag", "school bag", "daypack",
        "strap", "zipper", "textile", "nylon"
    ),
    "chair" to setOf(
        "chair", "office chair", "furniture", "seat", "stool",
        "bench", "armrest", "armchair", "seating", "sofa",
        "couch", "recliner", "desk chair", "folding chair",
        "rocking chair", "barber chair",
        "room", "interior design", "wood", "desk", "comfort"
    ),

    "television" to setOf(
        "television", "tv", "flat panel display",
        "led-backlit lcd display", "computer monitor",
        "television set", "lcd tv",
        "display device", "smart tv", "flat screen",
        "screen", "monitor", "electronic device",
        "technology", "output device", "multimedia"
    ),
    "monitor" to setOf(
        "monitor", "computer monitor", "display device",
        "flat panel display", "led-backlit lcd display",
        "output device", "television", "desktop computer",
        "lcd", "screen", "tv", "display",
        "electronic device", "technology", "gadget",
        "desk", "tableware", "multimedia"
    ),

    "phone" to setOf(
        "phone", "smartphone", "mobile phone", "cell phone",
        "cellular phone", "telephone", "mobile device",
        "communication device", "portable communications device",
        "iphone", "android", "touchscreen",
        "cellular telephone", "dial telephone", "pay-phone",
        "electronic device", "gadget", "technology", "screen"
    ),
    "umbrella" to setOf(
        "umbrella", "canopy", "shade", "parasol", "rain",
        "shelter", "cover", "awning", "sunshade", "textile"
    ),

    "bed" to setOf(
        "bed", "bedroom", "bed frame", "mattress", "bed sheet",
        "pillow", "duvet", "bedding", "blanket",
        "comforter", "quilt", "cushion",
        "linen", "headboard", "furniture",
        "room", "textile", "comfort"
    ),
    "bicycle" to setOf(
        "bicycle", "bike", "bicycle wheel", "cycle",
        "cycling", "bicycle tire", "bicycle frame", "pedal",
        "handlebar", "spoke", "wheel",
        "mountain bike", "road bike", "bmx",
        "vehicle", "tire", "bicycle-built-for-two"
    ),

    "clock" to setOf(
        "clock", "wall clock", "alarm clock", "timer", "watch",
        "timepiece", "chronometer", "analog clock", "digital clock",
        "time", "dial"
    ),
    "bottle" to setOf(
        "bottle", "water bottle", "plastic bottle", "glass bottle",
        "drink", "beverage", "container", "flask", "jug",
        "liquid", "drinkware", "cylinder"
    ),
    "keys" to setOf(
        "key", "keys", "key chain", "keychain", "lock",
        "door key", "car key", "metal", "brass"
    ),
    "towel" to setOf(
        "towel", "bath towel", "hand towel", "washcloth",
        "textile", "fabric", "linen", "cloth", "terry cloth"
    ),
    "mirror" to setOf(
        "mirror", "looking glass", "reflection", "glass",
        "vanity", "bathroom mirror", "wall mirror"
    ),
    "door" to setOf(
        "door", "doorway", "door handle", "entrance",
        "gate", "door frame", "wood", "room"
    ),
    "window" to setOf(
        "window", "glass", "window frame", "curtain",
        "windowpane", "blinds", "shutter"
    ),
    "fan" to setOf(
        "fan", "ceiling fan", "electric fan", "ventilator",
        "blower", "propeller", "blade"
    ),
    "remote" to setOf(
        "remote control", "remote", "controller", "tv remote",
        "electronic device", "gadget", "button"
    ),
    "toothbrush" to setOf(
        "toothbrush", "tooth brush", "brush", "dental",
        "oral hygiene", "bathroom", "bristle"
    ),
    "soap" to setOf(
        "soap", "bar soap", "hand soap", "soap dispenser",
        "cleanser", "detergent", "bathroom", "hygiene"
    )
)

private data class TargetLabelSpec(
    val primaryLabels: Set<String>,
    val secondaryLabels: Set<String> = emptySet(),
    val minimumConfidence: Float = DEFAULT_MATCH_CONFIDENCE,
    val secondaryMinimumConfidence: Float = SECONDARY_MATCH_CONFIDENCE,
    val tertiaryMinimumConfidence: Float = TERTIARY_MATCH_CONFIDENCE
)

private data class LabelMatchRule(
    val requiredConfidence: Float,
    val reason: String,
    val priority: Int
)

private data class DetectionMatch(
    val detectedText: String,
    val confidence: Float,
    val requiredConfidence: Float,
    val reason: String,
    val priority: Int
)

/**
 * Labels that are too generic to ever reliably identify a specific object.
 * These are explicitly blocked from matching ANY target, regardless of
 * what's in the alias maps. Normalised (lowercase, stemmed) forms.
 */
private val NOISE_LABELS: Set<String> = setOf(
    "pattern", "monochrome", "colorfulness", "art", "design",
    "material property", "circle", "line",
    "photography", "stock photography", "snapshot", "photo",
    "sky", "space", "darkness", "shadow", "reflection",
    "close-up", "macro photography", "still life photography",
    "event", "person", "human", "hand", "finger", "face",
    "number", "symbol", "logo", "brand", "label", "sign", "poster"
)

private val targetLabelSpecs: Map<String, TargetLabelSpec> = mapOf(

    "sink" to TargetLabelSpec(
        primaryLabels = setOf(
            "sink", "bathroom sink", "kitchen sink", "washbasin",
            "plumbing fixture", "basin", "lavatory"
        ),
        secondaryLabels = setOf("faucet", "tap", "plumbing", "bathroom", "kitchen", "porcelain", "ceramic"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),
    "coffee cup" to TargetLabelSpec(
        primaryLabels = setOf(
            "coffee cup", "mug", "cup", "teacup", "espresso cup",
            "coffee", "drinkware", "tableware"
        ),
        secondaryLabels = setOf("serveware", "ceramic", "porcelain", "dishware", "beverage", "liquid")
    ),
    "cup" to TargetLabelSpec(
        primaryLabels = setOf("cup", "coffee", "cappuccino", "juice"),
        secondaryLabels = setOf("tableware", "saucer", "food")
    ),

    "shoe" to TargetLabelSpec(
        primaryLabels = setOf(
            "shoe", "sneaker", "sneakers", "footwear", "boot", "running shoe",
            "athletic shoe", "sandal", "slipper", "loafer",
            "walking shoe", "outdoor shoe", "tennis shoe",
            "high heel", "sports shoe"
        ),
        secondaryLabels = setOf("leather", "textile", "rubber", "sole")
    ),
    "book" to TargetLabelSpec(
        primaryLabels = setOf(
            "book", "publication", "novel", "textbook", "notebook", "magazine",
            "hardcover", "paperback", "booklet", "diary", "journal",
            "paper product", "comic book", "book jacket", "bookcase", "text",
            "document", "paper", "rectangle", "font"
        ),
        secondaryLabels = setOf("reading", "literature", "page", "cover", "library", "writing", "education")
    ),
    "plant" to TargetLabelSpec(
        primaryLabels = setOf(
            "plant", "houseplant", "potted plant", "flower", "flowerpot",
            "succulent", "cactus", "fern", "indoor plant",
            "vascular plant", "leaf", "shrub", "tree",
            "vegetation", "flora", "foliage", "herb"
        ),
        secondaryLabels = setOf("green", "garden", "nature", "grass", "soil")
    ),
    "laptop" to TargetLabelSpec(
        primaryLabels = setOf(
            "laptop", "notebook computer", "netbook", "portable computer",
            "personal computer", "computer", "output device", "computer hardware"
        ),
        secondaryLabels = setOf("technology", "electronic device", "gadget", "screen", "display device", "keyboard")
    ),
    "computer" to TargetLabelSpec(
        primaryLabels = setOf("computer"),
        secondaryLabels = setOf("desk", "screen", "web page", "technology")
    ),
    "fruit" to TargetLabelSpec(
        primaryLabels = setOf(
            "fruit", "apple", "banana", "orange", "pineapple", "mango",
            "grape", "grapes", "berry", "strawberry", "lemon", "watermelon",
            "pear", "peach", "kiwi", "plum", "cherry", "melon",
            "natural foods", "produce", "citrus", "tropical fruit"
        ),
        secondaryLabels = setOf("food", "vegetable", "plant", "diet", "nutrition")
    ),

    "watch" to TargetLabelSpec(
        primaryLabels = setOf(
            "watch", "wristwatch", "analog watch", "digital watch", "smartwatch",
            "timepiece", "chronometer", "stopwatch", "analog clock", "digital clock"
        ),
        secondaryLabels = setOf("clock", "strap", "wrist", "accessory"),
        minimumConfidence = SMALL_OBJECT_MATCH_CONFIDENCE
    ),

    "backpack" to TargetLabelSpec(
        primaryLabels = setOf(
            "backpack", "rucksack", "knapsack", "school bag", "daypack",
            "luggage and bags", "satchel", "travel bag",
            "bag", "handbag", "shoulder bag"
        ),
        secondaryLabels = setOf("strap", "zipper", "textile", "nylon", "fabric")
    ),
    "bag" to TargetLabelSpec(
        primaryLabels = setOf("bag", "handbag"),
        secondaryLabels = setOf("pocket", "strap", "leather", "textile")
    ),
    "handbag" to TargetLabelSpec(
        primaryLabels = setOf("handbag", "bag"),
        secondaryLabels = setOf("pocket", "strap", "leather", "textile")
    ),
    "chair" to TargetLabelSpec(
        primaryLabels = setOf(
            "chair", "office chair", "armchair", "desk chair", "folding chair", "stool",
            "furniture", "seat", "bench", "seating", "rocking chair", "barber chair"
        ),
        secondaryLabels = setOf("room", "interior design", "wood", "table", "desk", "comfort"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),

    "television" to TargetLabelSpec(
        primaryLabels = setOf(
            "television", "tv", "television set", "smart tv", "lcd tv", "flat screen",
            "flat panel display", "led-backlit lcd display",
            "display device", "computer monitor", "screen", "monitor"
        ),
        secondaryLabels = setOf("electronic device", "technology", "output device", "gadget", "multimedia"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),
    "monitor" to TargetLabelSpec(
        primaryLabels = setOf(
            "monitor", "computer monitor", "display device",
            "flat panel display", "led-backlit lcd display",
            "output device", "desktop computer", "screen",
            "television", "tv", "display"
        ),
        secondaryLabels = setOf("electronic device", "technology", "gadget", "lcd",
            "computer", "multimedia", "desk", "tableware"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),

    "phone" to TargetLabelSpec(
        primaryLabels = setOf(
            "phone", "smartphone", "mobile phone", "cell phone",
            "cellular phone", "telephone", "mobile device",
            "communication device", "portable communications device",
            "cellular telephone", "dial telephone", "pay-phone"
        ),
        secondaryLabels = setOf("touchscreen", "electronic device", "gadget", "technology", "screen", "display device")
    ),
    "umbrella" to TargetLabelSpec(
        primaryLabels = setOf(
            "umbrella", "parasol", "canopy"
        ),
        secondaryLabels = setOf("rain", "shade", "shelter", "textile")
    ),
    "glasses" to TargetLabelSpec(
        primaryLabels = setOf("glasses"),
        secondaryLabels = setOf("sunglasses", "goggles")
    ),
    "sunglasses" to TargetLabelSpec(
        primaryLabels = setOf("sunglasses", "glasses", "goggles")
    ),

    "bed" to TargetLabelSpec(
        primaryLabels = setOf(
            "bed", "bed frame", "mattress", "bedding",
            "bed sheet", "pillow", "duvet", "blanket",
            "comforter", "quilt", "headboard"
        ),
        secondaryLabels = setOf("bedroom", "cushion", "linen", "furniture", "room", "textile", "comfort"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),
    "pillow" to TargetLabelSpec(
        primaryLabels = setOf("pillow", "cushion"),
        secondaryLabels = setOf("bedroom", "textile", "bedroom", "sleep")
    ),
    "couch" to TargetLabelSpec(
        primaryLabels = setOf("couch", "loveseat"),
        secondaryLabels = setOf("chair", "armrest", "cushion", "room", "furniture"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),
    "bicycle" to TargetLabelSpec(
        primaryLabels = setOf(
            "bicycle", "bike", "bicycle wheel", "bicycle frame",
            "mountain bike", "road bike", "bmx",
            "cycle", "cycling", "bicycle tire", "bicycle-built-for-two"
        ),
        secondaryLabels = setOf("pedal", "handlebar", "spoke", "wheel", "vehicle", "tire")
    ),

    "clock" to TargetLabelSpec(
        primaryLabels = setOf(
            "clock", "wall clock", "alarm clock", "timer",
            "timepiece", "chronometer", "analog clock", "digital clock"
        ),
        secondaryLabels = setOf("watch", "time", "dial", "number")
    ),
    "flowerpot" to TargetLabelSpec(
        primaryLabels = setOf("flowerpot", "plant", "flower"),
        secondaryLabels = setOf("flora", "garden", "soil")
    ),
    "cutlery" to TargetLabelSpec(
        primaryLabels = setOf("cutlery"),
        secondaryLabels = setOf("tableware", "cuisine", "food")
    ),
    "tableware" to TargetLabelSpec(
        primaryLabels = setOf("tableware", "cutlery", "saucer"),
        secondaryLabels = setOf("food", "cuisine")
    ),
    "cookware" to TargetLabelSpec(
        primaryLabels = setOf("cookware and bakeware"),
        secondaryLabels = setOf("kitchen", "food", "cuisine")
    ),
    "paper" to TargetLabelSpec(
        primaryLabels = setOf("paper", "newspaper", "receipt"),
        secondaryLabels = setOf("document", "web page")
    ),
    "newspaper" to TargetLabelSpec(
        primaryLabels = setOf("newspaper", "paper", "news")
    ),
    "receipt" to TargetLabelSpec(
        primaryLabels = setOf("receipt", "paper"),
        secondaryLabels = setOf("money")
    ),
    "money" to TargetLabelSpec(
        primaryLabels = setOf("money"),
        secondaryLabels = setOf("receipt")
    ),
    "ring" to TargetLabelSpec(
        primaryLabels = setOf("ring"),
        secondaryLabels = setOf("jewellery", "metal")
    ),
    "bracelet" to TargetLabelSpec(
        primaryLabels = setOf("bracelet", "bangle"),
        secondaryLabels = setOf("jewellery", "metal")
    ),
    "necklace" to TargetLabelSpec(
        primaryLabels = setOf("necklace"),
        secondaryLabels = setOf("jewellery", "metal")
    ),
    "helmet" to TargetLabelSpec(
        primaryLabels = setOf("helmet"),
        secondaryLabels = setOf("sports")
    ),
    "shelf" to TargetLabelSpec(
        primaryLabels = setOf("shelf"),
        secondaryLabels = setOf("cabinetry", "drawer", "room")
    ),
    "drawer" to TargetLabelSpec(
        primaryLabels = setOf("drawer"),
        secondaryLabels = setOf("cabinetry", "shelf", "room")
    ),
    "cabinetry" to TargetLabelSpec(
        primaryLabels = setOf("cabinetry", "drawer", "shelf"),
        secondaryLabels = setOf("kitchen", "room")
    ),
    "lampshade" to TargetLabelSpec(
        primaryLabels = setOf("lampshade"),
        secondaryLabels = setOf("room")
    ),
    "bottle" to TargetLabelSpec(
        primaryLabels = setOf(
            "bottle", "water bottle", "plastic bottle", "glass bottle",
            "flask", "jug", "container"
        ),
        secondaryLabels = setOf("drink", "beverage", "liquid", "drinkware", "cylinder")
    ),
    "keys" to TargetLabelSpec(
        primaryLabels = setOf(
            "key", "keys", "key chain", "keychain"
        ),
        secondaryLabels = setOf("lock", "metal", "brass", "door key", "car key"),
        minimumConfidence = SMALL_OBJECT_MATCH_CONFIDENCE
    ),
    "towel" to TargetLabelSpec(
        primaryLabels = setOf(
            "towel", "bath towel", "hand towel", "washcloth", "terry cloth"
        ),
        secondaryLabels = setOf("textile", "fabric", "linen", "cloth", "bathroom")
    ),
    "mirror" to TargetLabelSpec(
        primaryLabels = setOf(
            "mirror", "looking glass", "vanity", "bathroom mirror", "wall mirror"
        ),
        secondaryLabels = setOf("glass", "reflection", "bathroom"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),
    "door" to TargetLabelSpec(
        primaryLabels = setOf(
            "door", "doorway", "gate", "entrance", "door frame"
        ),
        secondaryLabels = setOf("door handle", "wood", "room", "house"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),
    "window" to TargetLabelSpec(
        primaryLabels = setOf(
            "window", "windowpane", "window frame"
        ),
        secondaryLabels = setOf("glass", "curtain", "blinds", "shutter", "daylight"),
        minimumConfidence = LARGE_OBJECT_MATCH_CONFIDENCE
    ),
    "fan" to TargetLabelSpec(
        primaryLabels = setOf(
            "fan", "ceiling fan", "electric fan", "ventilator"
        ),
        secondaryLabels = setOf("blower", "propeller", "blade", "mechanical fan")
    ),
    "remote" to TargetLabelSpec(
        primaryLabels = setOf(
            "remote control", "remote", "controller", "tv remote"
        ),
        secondaryLabels = setOf("electronic device", "gadget", "button", "technology"),
        minimumConfidence = SMALL_OBJECT_MATCH_CONFIDENCE
    ),
    "toothbrush" to TargetLabelSpec(
        primaryLabels = setOf(
            "toothbrush", "tooth brush", "brush"
        ),
        secondaryLabels = setOf("dental", "oral hygiene", "bathroom", "bristle"),
        minimumConfidence = SMALL_OBJECT_MATCH_CONFIDENCE
    ),
    "soap" to TargetLabelSpec(
        primaryLabels = setOf(
            "soap", "bar soap", "hand soap", "soap dispenser"
        ),
        secondaryLabels = setOf("cleanser", "detergent", "bathroom", "hygiene"),
        minimumConfidence = SMALL_OBJECT_MATCH_CONFIDENCE
    )
)

/**
 * Check whether a detected label is a target-specific match. Broad scene labels
 * and one-word token overlap are intentionally rejected.
 */
private fun isLabelMatch(detectedLabel: String, targetLabel: String): Boolean {
    return evaluateLabelMatch(detectedLabel, targetLabel) != null
}

private fun requiredConfidence(targetLabel: String, detectedLabel: String): Float {
    return evaluateLabelMatch(detectedLabel, targetLabel)?.requiredConfidence
        ?: DEFAULT_MATCH_CONFIDENCE
}

private fun evaluateLabelMatch(detectedLabel: String, targetLabel: String): LabelMatchRule? {
    val detectedLower = normalizeLabel(detectedLabel)
    val targetLower = normalizeLabel(targetLabel)
    if (detectedLower.isBlank() || targetLower.isBlank()) return null

    // Reject labels that are too generic to identify any specific object
    if (detectedLower in NOISE_LABELS) return null

    val spec = targetLabelSpecs[targetLower]
        ?: TargetLabelSpec(primaryLabels = setOf(targetLabel), minimumConfidence = DEFAULT_MATCH_CONFIDENCE)

    // Priority 3: Direct/exact primary label match
    findMatchingAlias(detectedLower, spec.primaryLabels)?.let { alias ->
        return LabelMatchRule(
            requiredConfidence = confidenceForAlias(alias, spec.minimumConfidence),
            reason = "primary:$alias",
            priority = 3
        )
    }

    // Priority 2: Secondary label match
    findMatchingAlias(detectedLower, spec.secondaryLabels)?.let { alias ->
        return LabelMatchRule(
            requiredConfidence = spec.secondaryMinimumConfidence,
            reason = "secondary:$alias",
            priority = 2
        )
    }

    // Priority 1: Broad alias map fallback (catches coarse model labels)
    val aliasSet = targetAliasMap[targetLower]
    if (aliasSet != null) {
        findMatchingAlias(detectedLower, aliasSet)?.let { alias ->
            return LabelMatchRule(
                requiredConfidence = spec.tertiaryMinimumConfidence,
                reason = "alias-fallback:$alias",
                priority = 1
            )
        }
    }

    return null
}

private fun findMatchingAlias(detectedLabel: String, aliases: Set<String>): String? {
    return aliases
        .map { normalizeLabel(it) }
        .filter { it.isNotBlank() }
        .firstOrNull { alias ->
            detectedLabel == alias || containsPhrase(detectedLabel, alias)
        }
}

private fun confidenceForAlias(alias: String, baseConfidence: Float): Float {
    val tokenCount = alias.split(" ").count { it.isNotBlank() }
    return if (tokenCount == 1 && alias.length <= 4) {
        maxOf(baseConfidence, SHORT_PRIMARY_LABEL_CONFIDENCE)
    } else {
        baseConfidence
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

internal fun hasTargetLabelMatch(
    labels: Iterable<Pair<String, Float>>,
    targetLabel: String
): Boolean {
    return findBestDetectionMatch(labels, targetLabel) != null
}

private fun findBestDetectionMatch(
    labels: Iterable<Pair<String, Float>>,
    targetLabel: String
): DetectionMatch? {
    return labels
        .mapNotNull { (detectedText, confidence) ->
            val rule = evaluateLabelMatch(detectedText, targetLabel)
                ?: return@mapNotNull null
            DetectionMatch(
                detectedText = detectedText,
                confidence = confidence,
                requiredConfidence = rule.requiredConfidence,
                reason = rule.reason,
                priority = rule.priority
            )
        }
        .filter { candidate -> candidate.confidence >= candidate.requiredConfidence }
        .maxWithOrNull(
            compareBy<DetectionMatch> { it.priority }
                .thenBy { it.confidence }
        )
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

    // Tap-to-scan: scanning is only active while the user holds the scan button
    var isScanActive by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    val context = LocalContext.current
    val settingsRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.loud.alarm.di.RepositoryEntryPoint::class.java
        ).settingsRepository()
    }
    val showLabelsEnabled by settingsRepository.showLabelsEnabled.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Reset consecutive/sliding counters when scan button is released
    LaunchedEffect(isScanActive) {
        if (!isScanActive && scanPhase == ScanPhase.SCANNING) {
            consecutiveMatchCount = 0
            recentFrameResults = ArrayDeque()
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

    // Pulsing animation for the scan button
    val infiniteButtonTransition = rememberInfiniteTransition(label = "scanButton")
    val scanButtonPulse by infiniteButtonTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanButtonPulse"
    )
    val scanButtonGlow by infiniteButtonTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanButtonGlow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Camera preview with image labeling
            ImageLabelingCameraPreview(
                targetLabel = targetLabel,
                isScanActive = isScanActive,
                onLabelsDetected = { labels, matched, confidence ->
                    // Stop analyzing once we've entered the confirmation phase
                    if (scanPhase != ScanPhase.SCANNING) return@ImageLabelingCameraPreview
                    // Only process when scan button is held
                    if (!isScanActive) return@ImageLabelingCameraPreview

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
                    // Show scanning overlay only when actively scanning
                    if (isScanActive) {
                        ScanningOverlay()
                    }

                    // Bottom info panel
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 140.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (showLabelsEnabled && isScanActive && detectedLabels.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Detecting: ${detectedLabels.take(6).joinToString(", ")}",
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
                            text = if (isScanActive) displaySubtitle else "Tap the scan button below to start scanning",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        if (onFallbackToMath != null) {
                            Spacer(modifier = Modifier.height(16.dp))
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

            // ── Floating Tap-to-Scan Button ──
            if (scanPhase == ScanPhase.SCANNING) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scan button with press-and-hold
                    Box(contentAlignment = Alignment.Center) {
                        // Outer glow ring (pulsing when idle, solid when active)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(if (isScanActive) 1.12f else scanButtonPulse)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            if (isScanActive)
                                                Color(0xFF4FC3F7).copy(alpha = 0.5f)
                                            else
                                                Color(0xFF4FC3F7).copy(alpha = scanButtonGlow * 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Main button circle
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    elevation = if (isScanActive) 16.dp else 8.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF4FC3F7),
                                    spotColor = Color(0xFF4FC3F7)
                                )
                                .clip(CircleShape)
                                .background(
                                    if (isScanActive)
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF4FC3F7),
                                                Color(0xFF0288D1)
                                            )
                                        )
                                    else
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF29B6F6).copy(alpha = 0.9f),
                                                Color(0xFF0277BD).copy(alpha = 0.9f)
                                            )
                                        )
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isScanActive)
                                        Color.White.copy(alpha = 0.8f)
                                    else
                                        Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isScanActive = true
                                            tryAwaitRelease()
                                            isScanActive = false
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Tap to scan",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isScanActive) "Scanning..." else "Hold to Scan",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isScanActive)
                            Color(0xFF4FC3F7)
                        else
                            Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
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
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle dark tint over the whole screen
        drawRect(
            color = Color.Black.copy(alpha = 0.2f),
            size = size
        )

        // Full width scanning line
        val lineY = size.height * scanLineY
        
        // Glow effect
        drawLine(
            color = Color(0xFF4FC3F7).copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(0f, lineY - 4f),
            end = androidx.compose.ui.geometry.Offset(size.width, lineY - 4f),
            strokeWidth = 8.dp.toPx()
        )
        
        // Core line
        drawLine(
            color = Color(0xFF4FC3F7).copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(0f, lineY),
            end = androidx.compose.ui.geometry.Offset(size.width, lineY),
            strokeWidth = 2.dp.toPx()
        )
        
        // Glow effect below
        drawLine(
            color = Color(0xFF4FC3F7).copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(0f, lineY + 4f),
            end = androidx.compose.ui.geometry.Offset(size.width, lineY + 4f),
            strokeWidth = 8.dp.toPx()
        )
    }
}

/**
 * Camera preview that runs ML Kit Image Labeling (bundled model) directly
 * on each frame. The bundled model has a rich vocabulary of 400+ labels,
 * providing reliable coverage for all the everyday objects we support.
 *
 * @param isScanActive When false, frame analysis is skipped to save resources.
 */
@Composable
fun ImageLabelingCameraPreview(
    targetLabel: String,
    isScanActive: Boolean = true,
    onLabelsDetected: (labels: List<String>, matched: Boolean, confidence: Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val currentOnLabelsDetected by rememberUpdatedState(onLabelsDetected)
    val currentIsScanActive by rememberUpdatedState(isScanActive)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
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

    DisposableEffect(previewView, lifecycleOwner, targetLabel) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val analyzer = DirectImageLabelAnalyzer(
            targetLabel = targetLabel,
            isScanActiveProvider = { currentIsScanActive }
        ) { labels, matched, confidence ->
            currentOnLabelsDetected(labels, matched, confidence)
        }

        imageAnalysis.setAnalyzer(analysisExecutor, analyzer)

        var disposed = false
        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
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

/**
 * Analyzer that runs ML Kit Image Labeling (bundled model) directly on every frame.
 * The bundled model provides specific, accurate labels for 400+ object categories.
 *
 * @param isScanActiveProvider Returns true when scanning should be active. Frames are
 *        skipped (closed immediately) when this returns false.
 */
class DirectImageLabelAnalyzer(
    private val targetLabel: String,
    private val isScanActiveProvider: () -> Boolean = { true },
    private val onResult: (labels: List<String>, matched: Boolean, confidence: Float) -> Unit
) : ImageAnalysis.Analyzer {

    // Image labeler: very low pre-filter so we get ALL candidate labels.
    // Our alias-map matching + multi-frame requirement handles false positives.
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(LABELER_CANDIDATE_CONFIDENCE)
            .build()
    )

    @Volatile
    private var isProcessing = false

    @Volatile
    private var isClosed = false

    // Frame skip counter to avoid overwhelming the labeler while still
    // processing frames frequently enough for responsive detection.
    @Volatile
    private var frameCounter = 0

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed) {
            imageProxy.close()
            return
        }

        // Skip processing when scan is not active (tap-to-scan not held)
        if (!isScanActiveProvider()) {
            imageProxy.close()
            return
        }

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
                if (isClosed) return@addOnSuccessListener

                val labelTexts = labels.map { "${it.text}(${"%.0f".format(it.confidence * 100)}%)" }
                val plainLabels = labels.map { it.text }

                // Log ALL labels every 5th frame for debugging
                if (labels.isNotEmpty() && frameCounter % 5 == 0) {
                    Log.d(TAG, "Frame #$frameCounter ALL labels for target='$targetLabel': $labelTexts")
                }

                // Check all target-specific labels that also clear their required threshold.
                val match = findBestDetectionMatch(
                    labels.map { it.text to it.confidence },
                    targetLabel
                )

                if (match != null) {
                    Log.d(TAG, "MATCH FOUND: target='$targetLabel', " +
                            "detected='${match.detectedText}', " +
                            "confidence=${match.confidence}, " +
                            "required=${match.requiredConfidence}, " +
                            "reason=${match.reason}, all labels=${labelTexts.take(8)}")
                }

                onResult(plainLabels, match != null, match?.confidence ?: 0f)
            }
            .addOnFailureListener { e ->
                if (isClosed) return@addOnFailureListener

                Log.e(TAG, "Image labeling failed", e)
                onResult(emptyList(), false, 0f)
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    fun close() {
        isClosed = true
        labeler.close()
    }
}

package com.loud.alarm.ui.challenge

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.loud.alarm.service.AlarmService
import java.util.Locale

private const val AUDIO_MEMORY_ROUNDS = 2

private val WORD_POOL = listOf(
    "apple", "river", "cloud", "chair", "light",
    "ocean", "piano", "tiger", "bread", "flame",
    "grass", "stone", "music", "dream", "smile",
    "night", "water", "house", "peace", "bloom",
    "frame", "dance", "heart", "frost", "crown",
    "storm", "plant", "tower", "jewel", "flock",
    "grain", "prism", "coral", "cedar", "pearl",
    "vapor", "crest", "forge", "lunar", "ember"
)

private val accentBlue = Color(0xFF4FC3F7)
private val accentPurple = Color(0xFFB388FF)
private val cardBg = Color(0xFF1E1E2E)
private val errorRed = Color(0xFFEF5350)
private val successGreen = Color(0xFF66BB6A)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioMemoryChallengeScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // TTS engine
    var ttsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var currentSpeakingIndex by remember { mutableIntStateOf(-1) }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                engine?.setSpeechRate(0.85f) // Clear, normal rate
                engine?.setPitch(0.85f) // Deepen slightly but keep it natural

                // Try to find and set a male US voice
                try {
                    val voices = engine?.voices
                    if (voices != null) {
                        // Look specifically for US English male voices
                        val maleVoice = voices.firstOrNull { voice ->
                            val isUS = voice.locale.language.startsWith("en", ignoreCase = true) && 
                                       voice.locale.country.equals("US", ignoreCase = true)
                            val isMaleFeature = voice.features?.any { it.contains("male", ignoreCase = true) && !it.contains("female", ignoreCase = true) } == true
                            val isMaleName = voice.name.contains("male", ignoreCase = true) && !voice.name.contains("female", ignoreCase = true)
                            val isGoogleMale = voice.name.contains("en-US", ignoreCase = true) && 
                                (voice.name.endsWith("-b", ignoreCase = true) || 
                                 voice.name.endsWith("-d", ignoreCase = true) || 
                                 voice.name.endsWith("-i", ignoreCase = true) || 
                                 voice.name.endsWith("-j", ignoreCase = true))
                            
                            isUS && (isMaleFeature || isMaleName || isGoogleMale)
                        }

                        if (maleVoice != null) {
                            engine?.voice = maleVoice
                        }
                    }
                } catch (e: Exception) {
                    // Ignore voice selection errors
                }

                mainHandler.post { ttsReady = true }
            }
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
            AlarmService.restoreAlarmVolume()
        }
    }

    // Game state
    var currentRound by rememberSaveable { mutableIntStateOf(0) }

    // Generate word sequences for all rounds up front
    val allRoundWords = remember {
        val rounds = mutableListOf<List<String>>()
        val pool = WORD_POOL.shuffled().toMutableList()
        for (r in 0 until AUDIO_MEMORY_ROUNDS) {
            val count = 3 + r // 3 words, then 4 words
            val words = pool.take(count)
            pool.removeAll(words.toSet())
            rounds.add(words)
        }
        rounds
    }

    val currentWords = allRoundWords[currentRound]

    // Shuffled options: correct words + distractors
    val wordOptions = remember(currentRound) {
        val pool = WORD_POOL.toMutableList()
        pool.removeAll(currentWords.toSet())
        val distractors = pool.shuffled().take(currentWords.size + 1) // extra choices
        (currentWords + distractors).shuffled()
    }

    // Phase: "listening", "choosing", "wrong"
    var phase by rememberSaveable { mutableStateOf("listening") }
    var selectedWords by rememberSaveable { mutableStateOf(listOf<String>()) }
    var hasPlayedOnce by rememberSaveable { mutableStateOf(false) }
    var showWrongFlash by remember { mutableStateOf(false) }
    var replayCount by rememberSaveable { mutableIntStateOf(0) }

    // Wrong flash effect
    LaunchedEffect(showWrongFlash) {
        if (showWrongFlash) {
            delay(800)
            showWrongFlash = false
        }
    }

    // Speak the words sequence — pauses alarm completely during playback
    fun speakWords(words: List<String>) {
        if (tts == null || !ttsReady) return
        isSpeaking = true
        currentSpeakingIndex = 0
        AlarmService.dimAlarmVolume()

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val index = utteranceId?.removePrefix("word_")?.toIntOrNull() ?: return
                mainHandler.post { currentSpeakingIndex = index }
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.removePrefix("word_")?.toIntOrNull() ?: return
                if (index == words.lastIndex) {
                    mainHandler.post {
                        isSpeaking = false
                        currentSpeakingIndex = -1
                        AlarmService.restoreAlarmVolume()
                        // Auto-transition to choosing phase after first play
                        if (!hasPlayedOnce) {
                            hasPlayedOnce = true
                        }
                        if (phase == "listening") {
                            phase = "choosing"
                        }
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    isSpeaking = false
                    currentSpeakingIndex = -1
                    AlarmService.restoreAlarmVolume()
                }
            }
        })

        tts.stop()
        for ((i, word) in words.withIndex()) {
            val params = android.os.Bundle()
            // Longer pauses between words for clarity
            tts.playSilentUtterance(if (i == 0) 600 else 1000, TextToSpeech.QUEUE_ADD, "silence_$i")
            tts.speak(word, TextToSpeech.QUEUE_ADD, params, "word_$i")
        }
    }

    // Reset state AND auto-play when round changes
    // MUST be a single effect so reset runs before the auto-play check
    LaunchedEffect(currentRound, ttsReady) {
        // Reset state for new round
        selectedWords = emptyList()
        phase = "listening"
        hasPlayedOnce = false
        replayCount = 0
        showWrongFlash = false

        // Auto-play after a short delay
        if (ttsReady) {
            delay(600)
            speakWords(currentWords)
        }
    }

    val progress by animateFloatAsState(
        targetValue = currentRound.toFloat() / AUDIO_MEMORY_ROUNDS,
        animationSpec = tween(500),
        label = "progress"
    )

    fun checkAnswers() {
        val isCorrect = selectedWords.size == currentWords.size &&
                selectedWords.indices.all { i ->
                    selectedWords[i].equals(currentWords[i], ignoreCase = true)
                }
        if (isCorrect) {
            if (currentRound == allRoundWords.lastIndex) {
                onSuccess()
            } else {
                currentRound += 1
            }
        } else {
            showWrongFlash = true
            phase = "wrong"
        }
    }

    // Pulsing animation for speaker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Wrong-answer flash overlay
        if (showWrongFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(errorRed.copy(alpha = 0.15f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Text(
                text = "🎧 Audio Memory",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Round & progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Round ${currentRound + 1}/$AUDIO_MEMORY_ROUNDS",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentBlue,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentBlue,
                    trackColor = Color.White.copy(alpha = 0.08f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Speaker visualization card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isSpeaking) listOf(
                                accentBlue.copy(alpha = 0.15f),
                                accentPurple.copy(alpha = 0.1f)
                            ) else listOf(
                                cardBg,
                                cardBg
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSpeaking) accentBlue.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated speaker icon
                    Box(contentAlignment = Alignment.Center) {
                        // Outer ring (pulse)
                        if (isSpeaking) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale * 1.15f)
                                    .clip(CircleShape)
                                    .background(accentBlue.copy(alpha = ringAlpha * 0.3f))
                            )
                        }
                        // Inner circle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(if (isSpeaking) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (isSpeaking) accentBlue.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.06f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speaker",
                                tint = if (isSpeaking) accentBlue else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Word dots indicator
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in currentWords.indices) {
                            val isActive = isSpeaking && i == currentSpeakingIndex
                            val isPast = isSpeaking && i < currentSpeakingIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .size(if (isActive) 14.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> accentBlue
                                            isPast -> accentBlue.copy(alpha = 0.5f)
                                            else -> Color.White.copy(alpha = 0.15f)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status text
                    Text(
                        text = when {
                            !ttsReady -> "Initializing speech engine..."
                            isSpeaking -> "🔊 Listen carefully..."
                            phase == "listening" && !hasPlayedOnce -> "Preparing words..."
                            phase == "choosing" || phase == "wrong" -> "Tap the words in order"
                            else -> "Listen to ${currentWords.size} words"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isSpeaking -> accentBlue
                            else -> Color.White.copy(alpha = 0.6f)
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Replay button — always available after first play
            AnimatedVisibility(
                visible = hasPlayedOnce && !isSpeaking,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                OutlinedButton(
                    onClick = {
                        speakWords(currentWords)
                        replayCount += 1
                        if (phase == "wrong") {
                            phase = "listening"
                            selectedWords = emptyList()
                            showWrongFlash = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Replay Words", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected answers display
            AnimatedVisibility(
                visible = (phase == "choosing" || phase == "wrong") && selectedWords.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Your answer (${selectedWords.size}/${currentWords.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedWords.forEachIndexed { index, word ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentBlue.copy(alpha = 0.15f))
                                    .border(1.dp, accentBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        // Tap to remove last word (only if it's this one)
                                        if (index == selectedWords.lastIndex) {
                                            selectedWords = selectedWords.dropLast(1)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentBlue.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = word,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (index == selectedWords.lastIndex) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Word choice buttons
            AnimatedVisibility(
                visible = (phase == "choosing" || phase == "wrong") && !isSpeaking,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically { it / 3 }
            ) {
                Column {
                    if (phase == "wrong") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(errorRed.copy(alpha = 0.12f))
                                .border(1.dp, errorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "❌ Wrong order — try again!",
                                color = errorRed,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        wordOptions.forEach { word ->
                            val isSelected = word in selectedWords
                            val isDisabled = isSelected || selectedWords.size >= currentWords.size

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isSelected -> accentBlue.copy(alpha = 0.08f)
                                            else -> Color.White.copy(alpha = 0.05f)
                                        }
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            isSelected -> accentBlue.copy(alpha = 0.2f)
                                            else -> Color.White.copy(alpha = 0.12f)
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable(enabled = !isDisabled && phase != "wrong") {
                                        selectedWords = selectedWords + word
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = accentBlue.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = word,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = when {
                                            isSelected -> Color.White.copy(alpha = 0.35f)
                                            else -> Color.White.copy(alpha = 0.9f)
                                        },
                                        fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit button
                    Button(
                        onClick = { checkAnswers() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = selectedWords.size == currentWords.size && phase != "wrong",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentBlue,
                            disabledContainerColor = Color.White.copy(alpha = 0.06f),
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (selectedWords.size < currentWords.size)
                                "Select ${currentWords.size - selectedWords.size} more"
                            else "Submit Answer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Reset selection button when wrong
                    if (phase == "wrong") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                selectedWords = emptyList()
                                phase = "choosing"
                                showWrongFlash = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                        ) {
                            Text("Clear & Try Again")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

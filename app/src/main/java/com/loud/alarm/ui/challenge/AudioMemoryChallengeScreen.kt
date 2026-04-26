package com.loud.alarm.ui.challenge

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
                engine?.setSpeechRate(0.85f)
                // Post to main thread so Compose observes the state change
                mainHandler.post { ttsReady = true }
            }
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
            // Safety net: restore alarm volume if screen exits while speaking
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

    var phase by rememberSaveable { mutableStateOf("listen") } // "listen", "input", "wrong"
    var inputFields by rememberSaveable { mutableStateOf(List(currentWords.size) { "" }) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var hasPlayedOnce by rememberSaveable { mutableStateOf(false) }
    var replayCount by rememberSaveable { mutableIntStateOf(0) }

    // Speak the words sequence
    fun speakWords(words: List<String>) {
        if (tts == null || !ttsReady) return
        isSpeaking = true
        currentSpeakingIndex = 0
        // Dim alarm ringtone so TTS can be heard
        AlarmService.dimAlarmVolume()

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val index = utteranceId?.removePrefix("word_")?.toIntOrNull() ?: return
                // Post to main thread so Compose observes the change
                mainHandler.post { currentSpeakingIndex = index }
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.removePrefix("word_")?.toIntOrNull() ?: return
                if (index == words.lastIndex) {
                    // Post to main thread so Compose observes the change
                    mainHandler.post {
                        isSpeaking = false
                        currentSpeakingIndex = -1
                        // Restore alarm volume after TTS finishes
                        AlarmService.restoreAlarmVolume()
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
            tts.playSilentUtterance(if (i == 0) 500 else 700, TextToSpeech.QUEUE_ADD, "silence_$i")
            tts.speak(word, TextToSpeech.QUEUE_ADD, params, "word_$i")
        }
    }

    // Auto-play on first visit per round
    LaunchedEffect(currentRound, ttsReady) {
        if (ttsReady && !hasPlayedOnce) {
            delay(800)
            speakWords(currentWords)
            hasPlayedOnce = true
        }
    }

    // Reset state when round changes
    LaunchedEffect(currentRound) {
        inputFields = List(allRoundWords[currentRound].size) { "" }
        phase = "listen"
        errorText = null
        hasPlayedOnce = false
        replayCount = 0
    }

    val progress by animateFloatAsState(
        targetValue = currentRound.toFloat() / AUDIO_MEMORY_ROUNDS,
        animationSpec = tween(500),
        label = "progress"
    )

    fun checkAnswers() {
        val isCorrect = inputFields.indices.all { i ->
            inputFields[i].trim().equals(currentWords[i], ignoreCase = true)
        }
        if (isCorrect) {
            if (currentRound == allRoundWords.lastIndex) {
                onSuccess()
            } else {
                currentRound += 1
            }
        } else {
            errorText = "Wrong order! Listen again carefully."
            phase = "wrong"
            replayCount = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Audio Memory",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Round ${currentRound + 1} / $AUDIO_MEMORY_ROUNDS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.1f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Listen to ${currentWords.size} words, then enter them in order",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Speaker visualization
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    if (isSpeaking) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Speaker",
                tint = if (isSpeaking) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
        }

        // Word dots indicator
        if (isSpeaking) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in currentWords.indices) {
                    val isActive = i == currentSpeakingIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play / Replay button
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    speakWords(currentWords)
                    replayCount += 1
                    hasPlayedOnce = true
                },
                modifier = Modifier.weight(1f),
                enabled = !isSpeaking
            ) {
                Icon(
                    imageVector = if (hasPlayedOnce) Icons.Default.Replay else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasPlayedOnce) "Replay" else "Play Words")
            }

            if (phase == "listen" && hasPlayedOnce && !isSpeaking) {
                Button(
                    onClick = { phase = "input" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ready to Answer")
                }
            }
        }

        // Input phase
        if (phase == "input" || phase == "wrong") {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Enter the ${currentWords.size} words in order:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            for (i in currentWords.indices) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${i + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = inputFields[i],
                        onValueChange = { newValue ->
                            inputFields = inputFields.toMutableList().apply { set(i, newValue) }
                            errorText = null
                        },
                        label = { Text("Word ${i + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorText ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { checkAnswers() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Submit", fontWeight = FontWeight.Bold)
            }

            if (phase == "wrong") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        speakWords(currentWords)
                        phase = "listen"
                        inputFields = List(currentWords.size) { "" }
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Listen Again")
                }
            }
        }

        if (!ttsReady) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Initializing speech engine...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

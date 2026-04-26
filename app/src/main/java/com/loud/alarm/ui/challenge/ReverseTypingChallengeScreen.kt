package com.loud.alarm.ui.challenge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val REVERSE_TYPING_ROUNDS = 3

private val SENTENCES = listOf(
    "Good morning sunshine",
    "Wake up and smile",
    "Rise and shine today",
    "Time to get up now",
    "Start a brand new day",
    "Hello beautiful world",
    "Today will be great",
    "Grab your coffee cup",
    "Open your eyes wide",
    "Stretch and breathe deep",
    "Make today count friend",
    "Seize the morning light",
    "Every day is a gift",
    "Chase your dreams today",
    "Be kind to yourself"
)

@Composable
fun ReverseTypingChallengeScreen(rounds: Int = REVERSE_TYPING_ROUNDS, onSuccess: () -> Unit) {
    val sessionSentences = remember {
        SENTENCES.shuffled().take(rounds)
    }
    var currentRound by rememberSaveable { mutableIntStateOf(0) }
    var inputText by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showHint by remember { mutableStateOf(false) }

    val currentSentence = sessionSentences[currentRound]
    val reversedSentence = remember(currentRound) { currentSentence.reversed() }

    val progress by animateFloatAsState(
        targetValue = currentRound.toFloat() / rounds,
        animationSpec = tween(500),
        label = "progress"
    )

    fun checkAnswer() {
        if (inputText.trim().equals(reversedSentence.trim(), ignoreCase = true)) {
            if (currentRound == sessionSentences.lastIndex) {
                onSuccess()
            } else {
                currentRound += 1
                inputText = ""
                errorText = null
                showHint = false
            }
        } else {
            errorText = "Not quite right. Type the sentence backwards!"
            showHint = true
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
            text = "Reverse Typing",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Round ${currentRound + 1} / $rounds",
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
            text = "Type this sentence backwards",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Display the sentence to type backwards
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentSentence,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Arrow indicator
        Text(
            text = "⬇ type it backwards ⬇",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show hint if they got it wrong
        AnimatedVisibility(
            visible = showHint,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hint: The first few characters are:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\"${reversedSentence.take(5)}...\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                errorText = null
            },
            label = { Text("Type backwards") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { checkAnswer() }
            )
        )

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
            onClick = { checkAnswer() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Check", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Letter count comparison
        val targetLen = reversedSentence.length
        val currentLen = inputText.trim().length
        Text(
            text = "$currentLen / $targetLen characters",
            style = MaterialTheme.typography.bodySmall,
            color = if (currentLen == targetLen)
                MaterialTheme.colorScheme.primary
            else
                Color.White.copy(alpha = 0.4f)
        )
    }
}

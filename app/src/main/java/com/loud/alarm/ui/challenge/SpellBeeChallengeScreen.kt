package com.loud.alarm.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val SPELL_BEE_ROUNDS = 3

private data class SpellBeeWord(
    val word: String,
    val hint: String
)

private val SPELL_BEE_WORDS = listOf(
    SpellBeeWord("sunrise", "First light of the morning"),
    SpellBeeWord("journey", "A trip from one place to another"),
    SpellBeeWord("thunder", "Loud sound after lightning"),
    SpellBeeWord("blanket", "Keeps you warm while sleeping"),
    SpellBeeWord("harvest", "Collect ripe crops"),
    SpellBeeWord("whisper", "Speak very softly"),
    SpellBeeWord("gallery", "A place to display art"),
    SpellBeeWord("lantern", "Portable light source"),
    SpellBeeWord("battery", "Stores electrical energy"),
    SpellBeeWord("gravity", "Force pulling things down"),
    SpellBeeWord("morning", "Time just after waking"),
    SpellBeeWord("balance", "Steady and even position")
)

private fun scrambleWord(word: String): String {
    if (word.length < 2) return word.uppercase()
    var scrambled = word
    repeat(10) {
        scrambled = word.toList().shuffled().joinToString("")
        if (!scrambled.equals(word, ignoreCase = true)) {
            return scrambled.uppercase()
        }
    }
    return scrambled.uppercase()
}

@Composable
fun SpellBeeChallengeScreen(onSuccess: () -> Unit) {
    val sessionWords = remember {
        SPELL_BEE_WORDS.shuffled().take(SPELL_BEE_ROUNDS)
    }
    var currentRound by rememberSaveable { mutableStateOf(0) }
    var answer by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val currentWord = sessionWords[currentRound]
    val scrambledWord = remember(currentRound) { scrambleWord(currentWord.word) }

    fun submitAnswer() {
        if (answer.trim().equals(currentWord.word, ignoreCase = true)) {
            if (currentRound == sessionWords.lastIndex) {
                onSuccess()
            } else {
                currentRound += 1
                answer = ""
                errorText = null
            }
        } else {
            errorText = "Not quite. Try again."
            answer = ""
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
            text = "Spell Bee",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Round ${currentRound + 1} / $SPELL_BEE_ROUNDS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Unscramble the word",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = scrambledWord.toCharArray().joinToString(" "),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hint: ${currentWord.hint}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                errorText = null
            },
            singleLine = true,
            label = { Text("Your answer") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { submitAnswer() }
            ),
            modifier = Modifier.fillMaxWidth()
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
            onClick = { submitAnswer() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check")
        }
    }
}

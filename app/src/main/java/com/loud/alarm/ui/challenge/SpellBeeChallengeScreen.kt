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
import com.loud.alarm.data.MathDifficulty

private data class SpellBeeWord(
    val word: String,
    val hint: String
)

// Easy: 5-6 letter words
private val EASY_WORDS = listOf(
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

// Medium: 6-8 letter words
private val MEDIUM_WORDS = listOf(
    SpellBeeWord("balanced", "Equal on all sides"),
    SpellBeeWord("calendar", "Tracks days and months"),
    SpellBeeWord("daughter", "A female child"),
    SpellBeeWord("elephant", "Largest land animal"),
    SpellBeeWord("friendly", "Kind and pleasant"),
    SpellBeeWord("gathered", "Brought together"),
    SpellBeeWord("harmless", "Not causing any damage"),
    SpellBeeWord("industry", "Manufacturing sector"),
    SpellBeeWord("judgment", "Ability to decide wisely"),
    SpellBeeWord("keyboard", "Used for typing"),
    SpellBeeWord("language", "System of communication"),
    SpellBeeWord("midnight", "12 o'clock at night"),
    SpellBeeWord("neighbor", "Person living next door"),
    SpellBeeWord("obstacle", "Something in the way"),
    SpellBeeWord("pleasant", "Giving a sense of comfort")
)

// Hard: 8-10 letter words
private val HARD_WORDS = listOf(
    SpellBeeWord("adventure", "An exciting experience"),
    SpellBeeWord("beautiful", "Pleasing to the eye"),
    SpellBeeWord("chocolate", "Popular sweet treat"),
    SpellBeeWord("dangerous", "Likely to cause harm"),
    SpellBeeWord("education", "Process of learning"),
    SpellBeeWord("furniture", "Tables, chairs, and beds"),
    SpellBeeWord("guarantee", "A promise of quality"),
    SpellBeeWord("happiness", "State of being happy"),
    SpellBeeWord("immediate", "Happening right now"),
    SpellBeeWord("knowledge", "Facts and information"),
    SpellBeeWord("landscape", "View of the scenery"),
    SpellBeeWord("necessary", "Absolutely required"),
    SpellBeeWord("operation", "A planned activity"),
    SpellBeeWord("passenger", "Someone riding in a vehicle"),
    SpellBeeWord("recognize", "Identify from before"),
    SpellBeeWord("electrical", "Related to electricity")
)

// Extreme: 10+ letter words
private val EXTREME_WORDS = listOf(
    SpellBeeWord("acknowledge", "Accept or admit"),
    SpellBeeWord("anniversary", "Yearly celebration"),
    SpellBeeWord("catastrophe", "A sudden disaster"),
    SpellBeeWord("distinguish", "Tell apart from others"),
    SpellBeeWord("embarrassed", "Feeling self-conscious"),
    SpellBeeWord("environment", "Surrounding conditions"),
    SpellBeeWord("furthermore", "In addition to that"),
    SpellBeeWord("handkerchief", "Cloth for wiping your nose"),
    SpellBeeWord("independence", "Freedom from control"),
    SpellBeeWord("nevertheless", "In spite of that"),
    SpellBeeWord("particularly", "Especially or notably"),
    SpellBeeWord("questionnaire", "A set of survey questions"),
    SpellBeeWord("refrigerator", "Keeps food cold"),
    SpellBeeWord("sophisticated", "Complex and refined"),
    SpellBeeWord("extraordinary", "Beyond what is usual"),
    SpellBeeWord("uncomfortable", "Not at ease physically")
)

private fun getWordsForDifficulty(difficulty: MathDifficulty): List<SpellBeeWord> {
    return when (difficulty) {
        MathDifficulty.EASY -> EASY_WORDS
        MathDifficulty.MEDIUM -> MEDIUM_WORDS
        MathDifficulty.HARD -> HARD_WORDS
        MathDifficulty.EXTREME -> EXTREME_WORDS
    }
}



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
fun SpellBeeChallengeScreen(
    difficulty: MathDifficulty = MathDifficulty.EASY,
    rounds: Int = 3,
    onSuccess: () -> Unit
) {
    val sessionWords = remember {
        getWordsForDifficulty(difficulty).shuffled().take(rounds)
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
            text = "Round ${currentRound + 1} / $rounds",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f)
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

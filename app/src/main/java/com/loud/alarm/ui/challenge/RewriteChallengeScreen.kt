package com.loud.alarm.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val RANDOM_PHRASES = listOf(
    "I am awake and ready to start my day",
    "Rise and shine",
    "Today is going to be a great day",
    "I will conquer my goals today",
    "Time to get up and get moving",
    "Waking up early gives me more time",
    "I feel refreshed and energized",
    "Seize the day",
    "The morning sun is beautiful"
)

@Composable
fun RewriteChallengeScreen(
    customText: String,
    onSuccess: () -> Unit
) {
    val targetText = remember {
        if (customText.isNotBlank()) customText else RANDOM_PHRASES.random()
    }
    
    var inputText by remember { mutableStateOf("") }
    
    LaunchedEffect(inputText) {
        if (inputText.trim().equals(targetText.trim(), ignoreCase = true)) {
            onSuccess()
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
            text = "Rewrite the phrase below:",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = targetText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Type here") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 5,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Match the text exactly to dismiss the alarm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

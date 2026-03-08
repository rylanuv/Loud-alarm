package com.loud.alarm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val snoozeEnabled by viewModel.snoozeEnabled.collectAsState()
    val fadeInEnabled by viewModel.fadeInEnabled.collectAsState()
    val fadeInDuration by viewModel.fadeInDuration.collectAsState()
    
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFadeInWarningDialog by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableStateOf(0) }
    var showDevOptions by remember { mutableStateOf(false) }
    var showTroubleshootDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Alarm Settings Section
            item {
                SectionHeader(title = "Alarm Settings")
            }
            
            item {
                SettingsCard {
                    Column {
                        // Vibration Toggle
                        SettingToggleItem(
                            icon = Icons.Default.Notifications,
                            title = "Vibration",
                            subtitle = "Vibrate when alarm rings",
                            checked = vibrationEnabled,
                            onCheckedChange = { viewModel.setVibrationEnabled(it) }
                        )
                        
                        SettingDivider()
                        
                        // Snooze Toggle
                        SettingToggleItem(
                            icon = Icons.Default.Bedtime,
                            title = "Enable Snooze",
                            subtitle = "Allow snoozing alarms",
                            checked = snoozeEnabled,
                            onCheckedChange = { viewModel.setSnoozeEnabled(it) }
                        )
                        
                    }
                }
            }

            // Sound Settings Section
            item {
                SectionHeader(title = "Sound Settings")
            }
            
            item {
                SettingsCard {
                    Column {
                        // Fade In Toggle
                        SettingToggleItem(
                            icon = Icons.Default.BrightnessHigh,
                            title = "Fade In Duration",
                            subtitle = if (fadeInEnabled) "Alarm volume gradually increases" else "Alarm starts at full volume",
                            checked = fadeInEnabled,
                            onCheckedChange = { newValue ->
                                if (!newValue) {
                                    // User is trying to turn off — show warning
                                    showFadeInWarningDialog = true
                                } else {
                                    viewModel.setFadeInEnabled(true)
                                }
                            }
                        )

                        // Fade In Duration Slider (only show if fade in is enabled)
                        if (fadeInEnabled) {
                            SettingDivider()
                            SettingSliderItem(
                                icon = Icons.Default.BrightnessHigh,
                                title = "Fade In Time",
                                subtitle = "$fadeInDuration seconds",
                                value = fadeInDuration.toFloat(),
                                valueRange = 5f..60f,
                                onValueChange = { viewModel.setFadeInDuration((kotlin.math.round(it / 5f) * 5f).toInt()) }
                            )
                        }
                    }
                }
            }

            // Troubleshooting Section
            item {
                SectionHeader(title = "Troubleshooting")
            }
            
            item {
                SettingsCard {
                    SettingClickableItem(
                        icon = Icons.Default.Warning,
                        title = "Alarms not ringing?",
                        subtitle = "Help for Xiaomi, Huawei, Oppo, Vivo users",
                        onClick = { showTroubleshootDialog = true }
                    )
                }
            }

            // About Section
            item {
                SectionHeader(title = "About")
            }
            
            item {
                SettingsCard {
                    SettingClickableItem(
                        icon = Icons.Default.Info,
                        title = "About Loud Alarm",
                        subtitle = if (showDevOptions) "Developer mode enabled" else "Version 1.0.0",
                        onClick = {
                            if (!showDevOptions) {
                                devTapCount++
                                if (devTapCount >= 5) {
                                    showDevOptions = true
                                }
                            } else {
                                showAboutDialog = true
                            }
                        }
                    )
                }
            }
            
            if (showDevOptions) {
                item {
                    SectionHeader(title = "Developer Options")
                }
                
                item {
                    val isPremiumPurchased by viewModel.isPremiumPurchased.collectAsState()
                    val nextAlarm by viewModel.nextAlarm.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    SettingsCard {
                        Column {
                            SettingToggleItem(
                                icon = Icons.Default.Warning,
                                title = "Mock Premium Purchase",
                                subtitle = "Enable premium mode for testing",
                                checked = isPremiumPurchased,
                                onCheckedChange = { viewModel.setDebugPremium(it) }
                            )
                            SettingDivider()
                            SettingClickableItem(
                                icon = Icons.Default.Notifications,
                                title = "Test Upcoming Alarm",
                                subtitle = if (nextAlarm != null) "Trigger next scheduled alarm immediately" else "No upcoming alarm",
                                onClick = {
                                    nextAlarm?.let { alarm ->
                                        val intent = android.content.Intent(context, com.loud.alarm.service.AlarmReceiver::class.java).apply {
                                            putExtra("ALARM_ID", alarm.id)
                                        }
                                        context.sendBroadcast(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Fade In Warning Dialog
    if (showFadeInWarningDialog) {
        AlertDialog(
            onDismissRequest = { showFadeInWarningDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    "⚠\uFE0F Are you sure?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Disabling fade-in means your alarm will blast at full volume instantly. This can seriously harm your health:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("💓 Cardiovascular Stress: ")
                            }
                            append("A sudden loud alarm triggers a sharp spike in heart rate and blood pressure, putting extra strain on your heart — especially dangerous over time.")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("😰 Cortisol Spike: ")
                            }
                            append("Being jolted awake activates your fight-or-flight response, flooding your body with cortisol. Chronic elevated cortisol leads to anxiety, weight gain, and weakened immunity.")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("👂 Hearing Damage: ")
                            }
                            append("Repeated exposure to sudden loud sounds can damage the delicate hair cells in your inner ear, potentially causing permanent hearing loss or tinnitus.")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("🧠 Sleep Inertia: ")
                            }
                            append("Abrupt awakening worsens grogginess and cognitive impairment, making you feel more tired and less alert throughout the day.")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setFadeInEnabled(false)
                        showFadeInWarningDialog = false
                    }
                ) {
                    Text(
                        "Disable Anyway",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showFadeInWarningDialog = false }) {
                    Text("Keep It On")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            },
            title = { Text("About Loud Alarm") },
            text = {
                Column {
                    Text("Version: 1.0.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loud Alarm - Solve2Wake is designed to help you wake up with challenging tasks.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Features:", fontWeight = FontWeight.Bold)
                    Text("• Math challenges")
                    Text("• QR code scanning")
                    Text("• Customizable alarms")
                    Text("• Beautiful UI")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Troubleshoot Dialog
    if (showTroubleshootDialog) {
        AlertDialog(
            onDismissRequest = { showTroubleshootDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Device Specific Settings") },
            text = {
                Column {
                    Text("Some phone manufacturers (Xiaomi, Huawei, Oppo, Vivo) aggressively close background apps to save battery.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("If your alarm isn't ringing on time:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("1. Go to your phone's Settings", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Find 'Apps' or 'Battery'", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Find 'Loud Alarm'", style = MaterialTheme.typography.bodyMedium)
                    Text("4. Enable 'Auto-start' or set battery optimization to 'Unrestricted' / 'No Restrictions'", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTroubleshootDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingSliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

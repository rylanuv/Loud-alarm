package com.loud.alarm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.loud.alarm.data.VibrationPattern
import com.loud.alarm.ui.components.PremiumSubscriptionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToAlarmReliability: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val snoozeEnabled by viewModel.snoozeEnabled.collectAsState()
    val fadeInEnabled by viewModel.fadeInEnabled.collectAsState()
    val fadeInDuration by viewModel.fadeInDuration.collectAsState()
    val autoSilenceDuration by viewModel.autoSilenceDuration.collectAsState()
    val upcomingAlarmNotification by viewModel.upcomingAlarmNotificationEnabled.collectAsState()
    val preventUninstallEnabled by viewModel.preventUninstallEnabled.collectAsState()
    
    var showFadeInWarningDialog by remember { mutableStateOf(false) }
    val vibrationPatternName by viewModel.vibrationPattern.collectAsState()
    val selectedVibrationPattern = VibrationPattern.fromName(vibrationPatternName)
    var showVibrationPatternDialog by remember { mutableStateOf(false) }
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current

    // Device Admin setup for Prevent Uninstall
    val devicePolicyManager = remember {
        context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    val adminComponent = remember {
        ComponentName(context, com.loud.alarm.service.LoudAlarmDeviceAdminReceiver::class.java)
    }
    val isDeviceAdmin = remember(preventUninstallEnabled) {
        devicePolicyManager.isAdminActive(adminComponent)
    }

    // Sync preference with actual device admin status on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentlyAdmin = devicePolicyManager.isAdminActive(adminComponent)
                if (preventUninstallEnabled != currentlyAdmin) {
                    viewModel.setPreventUninstallEnabled(currentlyAdmin)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launcher for Device Admin activation
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.setPreventUninstallEnabled(true)
        }
        // If user cancelled, preference stays false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.loud.alarm.R.drawable.newsettings),
            contentDescription = "Settings Background",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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
                        
                        // Custom Vibration Pattern
                        SettingClickableItem(
                            icon = selectedVibrationPattern.icon,
                            title = "Custom Vibration",
                            subtitle = selectedVibrationPattern.displayName,
                            onClick = { showVibrationPatternDialog = true }
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
                        
                        SettingDivider()
                        
                        // Auto Silence Slider
                        SettingSliderItem(
                            icon = Icons.Default.Notifications,
                            title = "Auto Silence",
                            subtitle = "Stop ringing after ${autoSilenceDuration} minutes",
                            value = autoSilenceDuration.toFloat(),
                            valueRange = 5f..60f,
                            onValueChange = { viewModel.setAutoSilenceDuration((kotlin.math.round(it / 5f) * 5f).toInt()) }
                        )

                        SettingDivider()

                        // Upcoming Alarm Notification
                        SettingToggleItem(
                            icon = Icons.Default.Notifications,
                            title = "Upcoming Alarm Alert",
                            subtitle = "Show notification 30 min before alarm",
                            checked = upcomingAlarmNotification,
                            onCheckedChange = { viewModel.setUpcomingAlarmNotificationEnabled(it) }
                        )

                        SettingDivider()

                        // Prevent Uninstall Toggle
                        SettingToggleItem(
                            icon = Icons.Default.Lock,
                            title = "Prevent Uninstall",
                            subtitle = if (preventUninstallEnabled) "App cannot be uninstalled" else "Allow app uninstallation",
                            checked = preventUninstallEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // Request Device Admin activation
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                        putExtra(
                                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                            "Loud Alarm needs Device Administrator access to prevent uninstallation while alarms are active."
                                        )
                                    }
                                    deviceAdminLauncher.launch(intent)
                                } else {
                                    // Remove Device Admin and update preference
                                    devicePolicyManager.removeActiveAdmin(adminComponent)
                                    viewModel.setPreventUninstallEnabled(false)
                                }
                            }
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

            item {
                PremiumSubscriptionCard(
                    isPremium = isPremium,
                    onClick = onNavigateToSubscription,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Troubleshooting Section
            item {
                SectionHeader(title = "Troubleshooting")
            }
            
            item {
                SettingsCard {
                    Column {
                        SettingClickableItem(
                            icon = Icons.Default.Warning,
                            title = "Alarm reliability",
                            onClick = onNavigateToAlarmReliability
                        )
                    }
                }
            }

            // Support Section
            item {
                SectionHeader(title = "Support")
            }
            
            item {
                SettingsCard {
                    Column {
                        SettingClickableItem(
                            icon = Icons.Default.Star,
                            title = "Rate our app",
                            subtitle = "Love the app? Leave a review!",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                                }
                            }
                        )
                        SettingDivider()
                        SettingClickableItem(
                            icon = Icons.Default.Email,
                            title = "Send Feedback",
                            subtitle = "Report issues or suggest features",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("rylpix.app@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "Loud Alarm Feedback")
                                }
                                context.startActivity(Intent.createChooser(intent, "Send email"))
                            }
                        )
                        SettingDivider()
                        SettingClickableItem(
                            icon = Icons.Default.Share,
                            title = "Share with Friends",
                            subtitle = "Recommend this app to others",
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Check out Loud Alarm!")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Hey! I use Loud Alarm to make sure I never oversleep. It has crazy challenges that force you to wake up! Try it out: https://play.google.com/store/apps/details?id=${context.packageName}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                            }
                        )
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
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Disable fade-in?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "This will start the alarm at full volume immediately. This may cause harm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
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


    // Vibration Pattern Selection Dialog
    if (showVibrationPatternDialog) {
        AlertDialog(
            onDismissRequest = { showVibrationPatternDialog = false },
            containerColor = Color(0xFF1A181C),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            icon = {
                Icon(
                    Icons.Default.Vibration,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Custom Vibration",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Choose how your alarm vibrates",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    VibrationPattern.entries.forEach { pattern ->
                        val isSelected = pattern.name == vibrationPatternName
                        val isLocked = pattern.isPremium && !isPremium
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    if (isLocked) {
                                        // Cannot select — premium is required
                                        // The user sees the lock icon; they'd need to subscribe
                                    } else {
                                        viewModel.setVibrationPattern(pattern.name)
                                        showVibrationPatternDialog = false
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = pattern.icon,
                                contentDescription = pattern.displayName,
                                tint = if (isLocked) Color.White.copy(alpha = 0.45f) else if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.width(32.dp).padding(end = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pattern.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isLocked) Color.White.copy(alpha = 0.45f) else Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isLocked) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFFFD700).copy(alpha = 0.25f),
                                                    Color(0xFFFFA500).copy(alpha = 0.15f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Premium",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    
                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700).copy(alpha = 0.12f),
                                            Color(0xFFFFA500).copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upgrade to Premium to unlock all vibration patterns",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD700).copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVibrationPatternDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

}
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    val cardShape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xC61A181C),
                        Color(0xB5100F12)
                    )
                ),
                shape = cardShape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = cardShape
            )
            .clip(cardShape)
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
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingLeadingIcon(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF151312),
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                checkedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.9f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.28f)
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
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingLeadingIcon(icon = icon)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingLeadingIcon(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = "›",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun SettingDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 18.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun SettingLeadingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
        modifier = Modifier.size(20.dp)
        )
    }
}


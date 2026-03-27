package com.loud.alarm.ui.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.loud.alarm.R

data class RequiredPermissionItem(
    val type: RequiredPermissionType,
    val title: String,
    val description: String,
    val granted: Boolean,
    val icon: ImageVector
)

data class RequiredPermissionsStatus(
    val items: List<RequiredPermissionItem> = emptyList()
) {
    val allGranted: Boolean
        get() = items.all(RequiredPermissionItem::granted)

    val missingItems: List<RequiredPermissionItem>
        get() = items.filterNot(RequiredPermissionItem::granted)
}

enum class RequiredPermissionType {
    EXACT_ALARM,
    BATTERY_OPTIMIZATION,
    NOTIFICATIONS
}

@Composable
fun rememberRequiredPermissionsStatus(): RequiredPermissionsStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(context) { mutableStateOf(context.readRequiredPermissionsStatus()) }

    fun refresh() {
        status = context.readRequiredPermissionsStatus()
    }

    LaunchedEffect(context) {
        refresh()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.troubleshoot),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66000000),
                            Color(0xCC0D0B0A),
                            Color(0xF20C0907)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            PermissionSetupPage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                title = "Turn on the permissions that keep alarms working",
                description = "We will ask for the important permissions right away. If any are skipped, alarms and reminders may not work reliably."
            )
        }
    }
}

@Composable
fun PermissionSetupPage(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    isVisible: Boolean = true,
    autoPromptOnVisible: Boolean = true,
    onStatusChanged: (RequiredPermissionsStatus) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(context) { mutableStateOf(context.readRequiredPermissionsStatus()) }
    var queuedPrompts by remember { mutableStateOf<List<RequiredPermissionType>>(emptyList()) }
    var promptIndex by remember { mutableIntStateOf(0) }
    var activePrompt by remember { mutableStateOf<RequiredPermissionType?>(null) }
    var showDeniedWarning by rememberSaveable { mutableStateOf(false) }
    var hasAutoPrompted by rememberSaveable { mutableStateOf(false) }

    fun refreshStatus() {
        status = context.readRequiredPermissionsStatus()
    }

    fun startPromptFlow() {
        refreshStatus()
        showDeniedWarning = false
        queuedPrompts = status.missingItems.map(RequiredPermissionItem::type)
        promptIndex = 0
        activePrompt = null
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val completedPrompt = activePrompt
        activePrompt = null
        refreshStatus()
        if (completedPrompt != null && !status.isGranted(completedPrompt)) {
            showDeniedWarning = true
        }
        promptIndex += 1
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        activePrompt = null
        refreshStatus()
        if (!granted) {
            showDeniedWarning = true
        }
        promptIndex += 1
    }

    LaunchedEffect(status) {
        onStatusChanged(status)
    }

    LaunchedEffect(isVisible, autoPromptOnVisible) {
        if (!isVisible) {
            return@LaunchedEffect
        }
        refreshStatus()
        if (autoPromptOnVisible && !hasAutoPrompted) {
            hasAutoPrompted = true
            startPromptFlow()
        }
    }

    LaunchedEffect(queuedPrompts, promptIndex, activePrompt, isVisible) {
        if (!isVisible || activePrompt != null) {
            return@LaunchedEffect
        }

        val nextPrompt = queuedPrompts.getOrNull(promptIndex) ?: return@LaunchedEffect
        activePrompt = nextPrompt

        when (nextPrompt) {
            RequiredPermissionType.NOTIFICATIONS -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            RequiredPermissionType.EXACT_ALARM,
            RequiredPermissionType.BATTERY_OPTIMIZATION -> {
                settingsLauncher.launch(nextPrompt.buildIntent(context))
            }
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PermissionHero(
            title = title,
            description = description
        )

        status.items.forEach { item ->
            PermissionStatusCard(
                item = item,
                onClick = {
                    showDeniedWarning = false
                    queuedPrompts = listOf(item.type)
                    promptIndex = 0
                    activePrompt = null
                }
            )
        }

        if (showDeniedWarning && !status.allGranted) {
            PermissionMessageCard(
                icon = Icons.Default.Warning,
                title = "Permissions still missing",
                body = "Loud Alarm may not function correctly without these permissions. Please turn them on before relying on the app.",
                accent = Color(0xFFE88C7B)
            )
        } else if (status.allGranted) {
            PermissionMessageCard(
                icon = Icons.Default.CheckCircle,
                title = "Everything is ready",
                body = "All required permissions are enabled, so the app can ring and notify you reliably.",
                accent = Color(0xFF7DD7A6)
            )
        }

        if (!status.allGranted) {
            OutlinedButton(
                onClick = { startPromptFlow() },
                enabled = activePrompt == null,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (activePrompt == null) "Turn on permissions again" else "Opening settings...",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "If you continue without these, a warning will stay on the home screen until everything is enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PermissionHero(
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF0C876),
                            Color(0xFF91612A)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFF1B1208),
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.82f)
        )
    }
}

@Composable
private fun PermissionStatusCard(
    item: RequiredPermissionItem,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (item.granted) Color(0xFF8CE3AE) else Color(0xFFF0C671)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            StatusPill(
                label = if (item.granted) "On" else "Off",
                tint = if (item.granted) Color(0xFF8CE3AE) else Color(0xFFE7B15B)
            )
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PermissionMessageCard(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color
) {
    GlassCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

private fun Context.readRequiredPermissionsStatus(): RequiredPermissionsStatus {
    val items = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                RequiredPermissionItem(
                    type = RequiredPermissionType.EXACT_ALARM,
                    title = "Exact alarms",
                    description = "Lets alarms ring at the exact minute you set.",
                    granted = canScheduleExactAlarms(),
                    icon = Icons.Default.Alarm
                )
            )
        }

        add(
            RequiredPermissionItem(
                type = RequiredPermissionType.BATTERY_OPTIMIZATION,
                title = "Battery access",
                description = "Stops the system from putting the app to sleep before your alarm fires.",
                granted = isIgnoringBatteryOptimizations(),
                icon = Icons.Default.BatterySaver
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                RequiredPermissionItem(
                    type = RequiredPermissionType.NOTIFICATIONS,
                    title = "Notifications",
                    description = "Allows alarm and wake-up check alerts to appear on time.",
                    granted = hasNotificationPermission(),
                    icon = Icons.Default.Notifications
                )
            )
        }
    }

    return RequiredPermissionsStatus(items = items)
}

private fun Context.canScheduleExactAlarms(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return true
    }
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(packageName)
}

private fun Context.hasNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun RequiredPermissionsStatus.isGranted(type: RequiredPermissionType): Boolean {
    return items.firstOrNull { it.type == type }?.granted ?: true
}

private fun RequiredPermissionType.buildIntent(context: Context): Intent {
    val packageUri = Uri.parse("package:${context.packageName}")
    val candidates = when (this) {
        RequiredPermissionType.EXACT_ALARM -> listOf(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = packageUri
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        )

        RequiredPermissionType.BATTERY_OPTIMIZATION -> listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = packageUri
            },
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        )

        RequiredPermissionType.NOTIFICATIONS -> listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        )
    }

    return candidates.firstOrNull { intent ->
        intent.resolveActivity(context.packageManager) != null
    } ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = packageUri
    }
}

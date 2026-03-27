package com.loud.alarm.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loud.alarm.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmReliabilityScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.troubleshoot),
            contentDescription = "Alarm reliability background",
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
                                Icons.AutoMirrored.Filled.ArrowBack,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HeroSection()

                StepCard(
                    step = "1",
                    title = "Battery",
                    body = "Check for Unrestricted or Don't optimize.",
                    actionLabel = "Open battery settings",
                    onActionClick = { context.openBatteryReliabilitySettings() }
                )

                StepCard(
                    step = "2",
                    title = "Auto start",
                    body = "Look for Auto start or Background launch.",
                    actionLabel = "Open app settings",
                    onActionClick = { context.openAutoStartSettings() }
                )

                StepCard(
                    step = "3",
                    title = "Notifications",
                    body = "Make sure they are allowed and visible.",
                    actionLabel = "Open notification settings",
                    onActionClick = { context.openNotificationSettings() }
                )

                BrandGrid()

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
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
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFF1B1208),
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "Alarm reliability",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Quick settings check for phones with aggressive battery controls.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.76f)
        )
    }
}

@Composable
private fun StepCard(
    step: String,
    title: String,
    body: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF1C46C), Color(0xFF8E5F23))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step,
                        color = Color(0xFF1B1208),
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

            StepActionButton(
                label = actionLabel,
                onClick = onActionClick
            )
        }
    }
}

@Composable
private fun StepActionButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFF0C671), Color(0xFFC98A32))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF1A1107),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BrandGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Where users usually find these settings",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

        val brands = listOf(
            Triple("Xiaomi", "Battery saver, Auto start", Icons.Default.BatterySaver),
            Triple("Huawei", "App launch, Battery", Icons.Default.PhoneAndroid),
            Triple("Oppo", "Auto launch, Battery usage", Icons.Default.Security),
            Triple("Vivo", "Background power, Auto start", Icons.Default.CheckCircle)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            brands.forEach { (brand, hint, icon) ->
                BrandRow(brand = brand, hint = hint, icon = icon)
            }
        }
    }
}

@Composable
private fun BrandRow(
    brand: String,
    hint: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFEEC67B)
                )
            }

            Column {
                Text(
                    text = brand,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD91B1714),
                            Color(0xB3141110)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

private fun Context.openBatteryReliabilitySettings() {
    val packageUri = Uri.parse("package:$packageName")
    val intents = buildList {
        add(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = packageUri
            }
        )
        add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        )
    }
    launchFirstAvailable(intents)
}

private fun Context.openAutoStartSettings() {
    val packageUri = Uri.parse("package:$packageName")
    val manufacturer = Build.MANUFACTURER.lowercase()

    val intents = mutableListOf<Intent>()

    when {
        manufacturer.contains("xiaomi") -> {
            intents += componentIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            intents += componentIntent("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity")
        }
        manufacturer.contains("oppo") -> {
            intents += componentIntent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
            intents += componentIntent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
        }
        manufacturer.contains("vivo") -> {
            intents += componentIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            intents += componentIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
        }
        manufacturer.contains("huawei") -> {
            intents += componentIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            intents += componentIntent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        }
    }

    intents += Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = packageUri
    }

    launchFirstAvailable(intents)
}

private fun Context.openNotificationSettings() {
    val packageUri = Uri.parse("package:$packageName")
    val intents = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            )
        }
        add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        )
    }
    launchFirstAvailable(intents)
}

private fun Context.launchFirstAvailable(intents: List<Intent>) {
    intents.firstOrNull { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.resolveActivity(packageManager) != null
    }?.let(::startActivity)
}

private fun componentIntent(packageName: String, className: String): Intent {
    return Intent().apply {
        component = ComponentName(packageName, className)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

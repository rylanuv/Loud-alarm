package com.loud.alarm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.loud.alarm.ui.editor.AlarmEditorScreen
import com.loud.alarm.ui.home.HomeScreen
import com.loud.alarm.ui.settings.SettingsScreen
import com.loud.alarm.ui.subscription.SubscriptionScreen
import com.loud.alarm.ui.theme.LoudAlarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request battery optimization exemption on first launch
        // This is CRITICAL for alarm reliability on all phones
        requestBatteryOptimizationExemption()

        // Request exact alarm permission on Android 12+
        requestExactAlarmPermission()

        setContent {
            LoudAlarmTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.menu),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        AlarmNavigation()
                    }
                }
            }
        }
    }

    /**
     * Requests the user to disable battery optimization for this app.
     * Without this, manufacturers like Samsung, Xiaomi, Huawei, OnePlus, Oppo, Vivo
     * will aggressively kill the app in the background, preventing alarms from firing.
     *
     * We use REQUEST_IGNORE_BATTERY_OPTIMIZATIONS which shows a system dialog.
     */
    private fun requestBatteryOptimizationExemption() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "Requesting battery optimization exemption")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Log.d(TAG, "Battery optimization already disabled for this app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request battery optimization exemption", e)
        }
    }

    /**
     * On Android 12+ (API 31+), the SCHEDULE_EXACT_ALARM permission requires
     * explicit user approval. If not granted, redirect to system settings.
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "Requesting exact alarm permission")
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open exact alarm settings", e)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AlarmNavigation() {
    val navController = rememberNavController()
    
    // Request Notification Permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            permission = android.Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToEditor = { alarmId ->
                    if (alarmId != null) {
                        navController.navigate("editor?alarmId=$alarmId")
                    } else {
                        navController.navigate("editor")
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = "editor?alarmId={alarmId}",
            arguments = listOf(navArgument("alarmId") { 
                type = NavType.IntType 
                defaultValue = -1 
            })
        ) {
            AlarmEditorScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSubscription = { navController.navigate("subscription") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("subscription") {
            SubscriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

package com.loud.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.loud.alarm.analytics.AnalyticsLogger
import com.loud.alarm.ui.editor.AlarmEditorScreen
import com.loud.alarm.ui.home.HomeScreen
import com.loud.alarm.ui.onboarding.OnboardingScreen
import com.loud.alarm.ui.onboarding.OnboardingViewModel
import com.loud.alarm.ui.permissions.PermissionSetupScreen
import com.loud.alarm.ui.settings.AlarmReliabilityScreen
import com.loud.alarm.ui.settings.SettingsScreen
import com.loud.alarm.ui.subscription.SubscriptionScreen
import com.loud.alarm.ui.theme.LoudAlarmTheme
import com.loud.alarm.billing.BillingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LoudAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val openSubscription = intent.getBooleanExtra("OPEN_SUBSCRIPTION", false)
                    AlarmNavigation(analyticsLogger, openSubscription)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh purchases and subscription status whenever the app enters the foreground
        billingManager.restorePurchases()
    }
}

@Composable
fun AlarmNavigation(analyticsLogger: AnalyticsLogger, openSubscription: Boolean = false) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsState(initial = null)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    val bootstrapRoute = "bootstrap"
    val onboardingRoute = "onboarding"
    val homeRoute = "home"
    val permissionSetupRoute = "permission_setup"

    fun navigateBackOrFallback() {
        if (!navController.popBackStack()) {
            val fallbackRoute = if (onboardingCompleted == false) onboardingRoute else homeRoute
            navController.navigate(fallbackRoute) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(backStackEntry) {
        val route = backStackEntry?.destination?.route?.substringBefore("?")
        if (!route.isNullOrBlank() && route != bootstrapRoute) {
            analyticsLogger.logScreen(route)
        }
    }

    LaunchedEffect(openSubscription) {
        if (openSubscription) {
            navController.navigate("subscription")
        }
    }

    NavHost(navController = navController, startDestination = bootstrapRoute) {
        composable(bootstrapRoute) {
            LaunchedEffect(onboardingCompleted) {
                when (onboardingCompleted) {
                    true -> navController.navigate(homeRoute) {
                        popUpTo(bootstrapRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                    false -> navController.navigate(onboardingRoute) {
                        popUpTo(bootstrapRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                    null -> Unit
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = Color.White)
            }
        }

        composable(onboardingRoute) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(homeRoute) {
                        popUpTo(onboardingRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(homeRoute) {
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
                },
                onNavigateToPermissionSetup = {
                    navController.navigate(permissionSetupRoute) {
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
                onBack = { navigateBackOrFallback() },
                onNavigateToSubscription = { navController.navigate("subscription") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navigateBackOrFallback() },
                onNavigateToSubscription = { navController.navigate("subscription") },
                onNavigateToAlarmReliability = { navController.navigate("alarm_reliability") }
            )
        }
        composable("alarm_reliability") {
            AlarmReliabilityScreen(
                onBack = { navigateBackOrFallback() }
            )
        }
        composable(permissionSetupRoute) {
            PermissionSetupScreen(
                onBack = { navigateBackOrFallback() }
            )
        }
        composable("subscription") {
            SubscriptionScreen(
                onBack = { navigateBackOrFallback() }
            )
        }
    }
}

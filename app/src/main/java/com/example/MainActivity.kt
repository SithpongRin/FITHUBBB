package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notifications.NotificationHelper
import com.example.ui.components.FloatingBottomNav
import com.example.ui.components.InAppUpdateDialogs
import com.example.ui.components.SleepLogDialog
import com.example.ui.screens.*
import com.example.ui.theme.FithubTheme
import com.example.ui.viewmodel.FithubViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    private val viewModel: FithubViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        // Handle notification deep links
        intent?.getStringExtra("navigate_to")?.let { destination ->
            if (destination == "workout") {
                viewModel.currentTab.value = ScreenTab.WORKOUT
            }
        }

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            val isAuthenticated by viewModel.isAuthenticated.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            var showSleepDialog by remember { mutableStateOf(false) }

            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { _ -> }
            )

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    neededPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                neededPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

                val notGranted = neededPermissions.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (notGranted.isNotEmpty()) {
                    permissionsLauncher.launch(notGranted.toTypedArray())
                }

                // Auto-check for OTA features and APK updates on launch
                viewModel.checkForFeatureUpdates(forceSimulate = false)
                viewModel.checkForUpdates(forceSimulate = false)

                // Ensure all active schedules are registered with AlarmManager
                viewModel.allSchedules.value.filter { it.enabled }.forEach { sch ->
                    com.example.notifications.NotificationHelper.scheduleReminder(this@MainActivity, sch, notifyConfirmation = false)
                }
            }

            FithubTheme(darkTheme = isDark) {
                if (!isAuthenticated) {
                    AuthGateScreen(viewModel = viewModel)
                } else {
                    // BackHandler returns to Home if on a secondary tab
                    BackHandler(enabled = currentTab != ScreenTab.HOME) {
                        viewModel.currentTab.value = ScreenTab.HOME
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentWindowInsets = WindowInsets.systemBars
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                                scaleOut(targetScale = 1.02f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                                        )
                                },
                                label = "tab_screen_transition",
                                modifier = Modifier.fillMaxSize()
                            ) { targetTab ->
                                when (targetTab) {
                                    ScreenTab.HOME -> HomeScreen(
                                        viewModel = viewModel,
                                        onOpenSleepDialog = { showSleepDialog = true },
                                        onOpenProfile = { viewModel.currentTab.value = ScreenTab.PROFILE }
                                    )
                                    ScreenTab.WORKOUT -> WorkoutScreen(viewModel = viewModel)
                                    ScreenTab.PLANS -> PlansScreen(viewModel = viewModel)
                                    ScreenTab.PROGRESS -> ProgressScreen(viewModel = viewModel)
                                    ScreenTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                                    ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                }
                            }

                            // Floating Island Nav pinned cleanly above bottom
                            FloatingBottomNav(
                                selectedTab = currentTab,
                                onTabSelected = { viewModel.currentTab.value = it },
                                isKm = viewModel.appLanguage.collectAsState().value.code == "km",
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 10.dp)
                            )
                        }

                        if (showSleepDialog) {
                            SleepLogDialog(
                                viewModel = viewModel,
                                onDismiss = { showSleepDialog = false }
                            )
                        }
                    }
                }

                // Global In-App Update Dialogs (OTA features and native APK updates)
                InAppUpdateDialogs(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkForUpdates(forceSimulate = false)
    }
}

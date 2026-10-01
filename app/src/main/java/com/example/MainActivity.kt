package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            val currentTab by viewModel.currentTab.collectAsState()
            var showSleepDialog by remember { mutableStateOf(false) }

            FithubTheme(darkTheme = isDark) {
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
                                val forward = targetState.ordinal > initialState.ordinal
                                if (forward) {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> (fullWidth * 0.22f).toInt() }
                                    ) + fadeIn(
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    ) + scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(200, easing = FastOutLinearInEasing),
                                            targetOffsetX = { fullWidth -> -(fullWidth * 0.22f).toInt() }
                                        ) + fadeOut(
                                            animationSpec = tween(180)
                                        ) + scaleOut(
                                            targetScale = 1.04f,
                                            animationSpec = tween(180)
                                        )
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> -(fullWidth * 0.22f).toInt() }
                                    ) + fadeIn(
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    ) + scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(200, easing = FastOutLinearInEasing),
                                            targetOffsetX = { fullWidth -> (fullWidth * 0.22f).toInt() }
                                        ) + fadeOut(
                                            animationSpec = tween(180)
                                        ) + scaleOut(
                                            targetScale = 1.04f,
                                            animationSpec = tween(180)
                                        )
                                    )
                                }
                            },
                            label = "tab_animated_content"
                        ) { tab ->
                            when (tab) {
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
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 14.dp)
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
        }
    }
}

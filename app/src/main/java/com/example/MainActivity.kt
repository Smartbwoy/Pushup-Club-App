package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WorkoutViewModel
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextGray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: WorkoutViewModel = viewModel()
                var currentTab by remember { mutableStateOf(0) }
                val workoutState by viewModel.workoutState.collectAsState()
                val preferences by viewModel.preferences.collectAsState()

                val showOnboarding = preferences != null &&
                    preferences?.hasCompletedOnboarding == false &&
                    workoutState == "IDLE"

                if (showOnboarding) {
                    OnboardingScreen(
                        onCompleteSetup = { level, weeklyTarget, enabledDays, startImmediately ->
                            viewModel.completeOnboarding(
                                experienceLevel = level,
                                weeklyTarget = weeklyTarget,
                                enabledDays = enabledDays,
                                startWorkoutImmediately = startImmediately
                            )
                        },
                        onSkipAndStartWorkout = {
                            viewModel.skipOnboardingAndStartWorkout()
                        }
                    )
                } else {
                    // BackHandler behavior: when not on workouts tab and workout is idle, return to tab 0
                    BackHandler(enabled = currentTab != 0 && workoutState == "IDLE") {
                        currentTab = 0
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                containerColor = DarkSurface,
                                tonalElevation = 6.dp,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                val navItemColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF003818),
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )

                                NavigationBarItem(
                                    selected = (currentTab == 0),
                                    onClick = { currentTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = "Workouts",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Workouts", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium) },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("nav_workouts_tab")
                                )

                                NavigationBarItem(
                                    selected = (currentTab == 1),
                                    onClick = { currentTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "History",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("History", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium) },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("nav_history_tab")
                                )

                                NavigationBarItem(
                                    selected = (currentTab == 2),
                                    onClick = { currentTab = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.EventNote,
                                            contentDescription = "Plan",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Plan", fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Medium) },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("nav_schedule_tab")
                                )

                                NavigationBarItem(
                                    selected = (currentTab == 3),
                                    onClick = { currentTab = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Trophies",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Trophies", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium) },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("nav_trophies_tab")
                                )
                            }
                        }
                    ) { innerPadding ->
                        when (currentTab) {
                            0 -> WorkoutScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            1 -> HistoryScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            2 -> ScheduleScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            3 -> AchievementsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

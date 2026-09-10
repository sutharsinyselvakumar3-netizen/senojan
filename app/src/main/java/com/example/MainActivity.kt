package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RobotMode
import com.example.ui.components.ConnectionTestDialog
import com.example.ui.components.StartAutoDialog
import com.example.ui.components.StopAutoDialog
import com.example.ui.screens.AiVisionScreen
import com.example.ui.screens.ArmDrillScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RobotScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.RobotCompanionTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.RobotViewModel

enum class AppDestination(val label: String, val icon: ImageVector, val tag: String) {
    HOME("HOME", Icons.Default.Home, "nav_home"),
    ROBOT("ROBOT", Icons.Default.PrecisionManufacturing, "nav_robot"),
    AI_VISION("AI VISION", Icons.Default.Videocam, "nav_ai_vision"),
    ARM("ARM", Icons.Default.Build, "nav_arm"),
    SETTINGS("SETTINGS", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    private val viewModel: RobotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val robotMode by viewModel.robotMode.collectAsState()
            val showStartAutoDialog by viewModel.showStartAutoDialog.collectAsState()
            val showStopAutoDialog by viewModel.showStopAutoDialog.collectAsState()
            val autoCheckError by viewModel.autoHardwareCheckError.collectAsState()
            val testReport by viewModel.testReport.collectAsState()
            val isTestingConnections by viewModel.isTestingConnections.collectAsState()

            var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

            RobotCompanionTheme(mode = robotMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = TextPrimary,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            AppDestination.values().forEach { destination ->
                                val selected = currentDestination == destination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = destination },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.label,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag(destination.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentDestination,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { destination ->
                            when (destination) {
                                AppDestination.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentDestination = AppDestination.SETTINGS }
                                )
                                AppDestination.ROBOT -> RobotScreen(
                                    viewModel = viewModel
                                )
                                AppDestination.AI_VISION -> AiVisionScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentDestination = AppDestination.SETTINGS }
                                )
                                AppDestination.ARM -> ArmDrillScreen(
                                    viewModel = viewModel
                                )
                                AppDestination.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                // Modal confirmation dialogs
                StartAutoDialog(
                    show = showStartAutoDialog,
                    errorMessage = autoCheckError,
                    onConfirm = { viewModel.confirmStartAutoMode() },
                    onDismiss = { viewModel.dismissStartAutoDialog() }
                )

                StopAutoDialog(
                    show = showStopAutoDialog,
                    onConfirm = { viewModel.confirmStopAutoMode() },
                    onDismiss = { viewModel.dismissStopAutoDialog() }
                )

                ConnectionTestDialog(
                    report = testReport,
                    isLoading = isTestingConnections,
                    onDismiss = { viewModel.dismissTestReport() }
                )
            }
        }
    }
}

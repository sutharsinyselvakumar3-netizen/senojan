package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RobotMode
import com.example.ui.components.EmergencyStopBar
import com.example.ui.components.HardwareStatusRow
import com.example.ui.theme.DarkBlueAccent
import com.example.ui.theme.DarkGreenAccent
import com.example.ui.theme.OnionProtectPurple
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WeedDetectOrange
import com.example.viewmodel.RobotViewModel

@Composable
fun HomeScreen(
    viewModel: RobotViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val robotMode by viewModel.robotMode.collectAsState()
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()
    val camStatus by viewModel.cameraTelemetry.collectAsState()
    val picoStatus by viewModel.picoTelemetry.collectAsState()
    val isEStopActive by viewModel.isEmergencyStopActive.collectAsState()
    val autoStepMessage by viewModel.autoStepMessage.collectAsState()
    val drillCountdown by viewModel.drillCountdown.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PrecisionManufacturing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AI COMPANION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextPrimary
                )
            }
            Text(
                text = "SMART ONION GARDEN ROBOT",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "“See the Weed. Protect the Onion. Work Smart.”",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        // Emergency Stop Bar (Top level safety)
        EmergencyStopBar(
            isEStopActive = isEStopActive,
            onTriggerEStop = { viewModel.triggerEmergencyStop() },
            onResetEStop = { viewModel.resetEmergencyStop() }
        )

        // Real-Time Connection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hardware_status_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REAL HARDWARE STATUS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (settings.isDevDemoMode) "LAB DEMO MODE" else "REAL MODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.isDevDemoMode) StatusWarningYellow else StatusOnlineGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (settings.isDevDemoMode) StatusWarningYellow.copy(alpha = 0.2f)
                                else StatusOnlineGreen.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                HardwareStatusRow(
                    title = "NETWORK",
                    isOnline = isNetworkConnected,
                    statusText = if (isNetworkConnected) "● CONNECTED" else "● OFFLINE"
                )
                HardwareStatusRow(
                    title = "ESP32-CAM",
                    isOnline = camStatus.online,
                    statusText = if (camStatus.online) "● ONLINE" else "✕ OFFLINE"
                )
                HardwareStatusRow(
                    title = "STREAM",
                    isOnline = camStatus.streamReady,
                    statusText = if (camStatus.streamReady) "● READY" else "✕ OFFLINE"
                )
                HardwareStatusRow(
                    title = "PICO W",
                    isOnline = picoStatus.online,
                    statusText = if (picoStatus.online) "● ONLINE" else "✕ OFFLINE"
                )
                HardwareStatusRow(
                    title = "PICO API",
                    isOnline = picoStatus.online,
                    statusText = if (picoStatus.online) "● READY" else "✕ OFFLINE"
                )
                HardwareStatusRow(
                    title = "AI ENGINE",
                    isOnline = true,
                    statusText = "● READY"
                )
            }
        }

        // Mode Switcher: MANUAL ⇄ AUTO
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mode_selector_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (robotMode == RobotMode.AUTO) Color(0xFF0F2B4C) else Color(0xFF0F3621)
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (robotMode == RobotMode.AUTO) DarkBlueAccent else DarkGreenAccent
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPERATION MODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (robotMode == RobotMode.AUTO) DarkBlueAccent else DarkGreenAccent
                    )
                    Text(
                        text = if (robotMode == RobotMode.AUTO) "DARK BLUE THEME" else "DARK GREEN THEME",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Manual Button
                    Button(
                        onClick = {
                            if (robotMode == RobotMode.AUTO) {
                                viewModel.requestStopAutoMode()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("switch_manual_mode_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (robotMode == RobotMode.MANUAL) DarkGreenAccent else Color(0xFF1B3D2A),
                            contentColor = if (robotMode == RobotMode.MANUAL) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "MANUAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    // Auto Button
                    Button(
                        onClick = {
                            if (robotMode == RobotMode.MANUAL) {
                                viewModel.requestStartAutoMode()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("switch_auto_mode_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (robotMode == RobotMode.AUTO) DarkBlueAccent else Color(0xFF183B63),
                            contentColor = if (robotMode == RobotMode.AUTO) Color.White else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = "AUTO",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Status message inside card
                if (autoStepMessage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = autoStepMessage,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (drillCountdown > 0) WeedDetectOrange else TextPrimary
                        )
                    }
                }
            }
        }

        // Telemetry Glance Cards (No Fake Data rule: display real or NOT AVAILABLE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Battery Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if ((picoStatus.batteryVoltage ?: 0.0) < settings.minAutoVoltage)
                                Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = if ((picoStatus.batteryVoltage ?: 0.0) < settings.minAutoVoltage)
                                StatusOfflineRed else StatusOnlineGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "BATTERY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = if (picoStatus.online && picoStatus.batteryVoltage != null)
                            "${String.format("%.1f", picoStatus.batteryVoltage)} V"
                        else "OFFLINE",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Min AUTO: ${settings.minAutoVoltage}V",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // MPU6050 Tilt Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (picoStatus.mpuStable == false) StatusOfflineRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MPU6050",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = if (picoStatus.online && picoStatus.pitchDeg != null)
                            "${String.format("%.1f", picoStatus.pitchDeg)}°"
                        else "OFFLINE",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (picoStatus.mpuStable == true) "STABLE" else "TILT CHECK",
                        fontSize = 10.sp,
                        color = if (picoStatus.mpuStable == true) StatusOnlineGreen else TextSecondary
                    )
                }
            }
        }

        // Relays Overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HARDWARE ACTUATORS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RelayChip(
                        name = "RELAY 1 (DRILL)",
                        active = picoStatus.relay1Drill,
                        extra = if (drillCountdown > 0) "${drillCountdown}s" else "AUTO ONLY"
                    )
                    RelayChip(
                        name = "RELAY 2 (SOIL)",
                        active = picoStatus.relay2Soil,
                        extra = if (picoStatus.relay2Soil) "ON" else "OFF"
                    )
                    RelayChip(
                        name = "RELAY 3 (WATER)",
                        active = picoStatus.relay3Water,
                        extra = if (picoStatus.relay3Water) "ON" else "OFF"
                    )
                }
            }
        }
    }
}

@Composable
fun RelayChip(name: String, active: Boolean, extra: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(name, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) StatusOnlineGreen.copy(alpha = 0.25f) else Color(0xFF1E2822))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = extra,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (active) StatusOnlineGreen else TextSecondary
            )
        }
    }
}

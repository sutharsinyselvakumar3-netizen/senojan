package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
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
import com.example.ui.components.EmergencyStopBar
import com.example.ui.theme.DarkBlueAccent
import com.example.ui.theme.OnionProtectPurple
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WeedDetectOrange
import com.example.viewmodel.RobotViewModel

@Composable
fun ArmDrillScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val picoStatus by viewModel.picoTelemetry.collectAsState()
    val latestDetection by viewModel.latestDetection.collectAsState()
    val drillCountdown by viewModel.drillCountdown.collectAsState()
    val isEStopActive by viewModel.isEmergencyStopActive.collectAsState()
    val autoStepMessage by viewModel.autoStepMessage.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // E-Stop Bar
        EmergencyStopBar(
            isEStopActive = isEStopActive,
            onTriggerEStop = { viewModel.triggerEmergencyStop() },
            onResetEStop = { viewModel.resetEmergencyStop() }
        )

        // Section: RELAY 1 REAL AUTO DRILL MONITOR (Rule 25)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auto_drill_monitor_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (drillCountdown > 0 || picoStatus.relay1Drill)
                    Color(0xFF2C1304) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (drillCountdown > 0 || picoStatus.relay1Drill) WeedDetectOrange else MaterialTheme.colorScheme.outline
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RELAY 1 — AUTO DRILL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (drillCountdown > 0) WeedDetectOrange else MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Text(
                            text = "AUTOMATIC ONLY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                // 7-SECOND COUNTDOWN DISPLAY (Rule 25)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (drillCountdown > 0) "AUTO DRILL ACTIVE" else "DRILL OFF",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (drillCountdown > 0) WeedDetectOrange else TextPrimary
                        )
                        Text(
                            text = "Protected max duration: 7000 ms",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Huge Countdown Number (07..00)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (drillCountdown > 0) WeedDetectOrange.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f))
                            .border(
                                width = 2.dp,
                                color = if (drillCountdown > 0) WeedDetectOrange else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = String.format("%02d", drillCountdown),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = if (drillCountdown > 0) WeedDetectOrange else TextSecondary
                        )
                    }
                }

                // Safety Interlocks Checklist
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "SAFETY INTERLOCKS STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary
                    )
                    InterlockItem(
                        name = "Physical E-Stop Released",
                        passed = !picoStatus.eStopActive && !isEStopActive
                    )
                    InterlockItem(
                        name = "Onion Safety Margin Clear",
                        passed = latestDetection?.onionSafe != false
                    )
                    InterlockItem(
                        name = "MPU Stability Guard",
                        passed = picoStatus.mpuStable != false
                    )
                    InterlockItem(
                        name = "Battery Voltage Safe",
                        passed = (picoStatus.batteryVoltage ?: 12.0) >= settings.minAutoVoltage
                    )
                }
            }
        }

        // Section: ARM SERVOS & KINEMATICS (Rule 23: Camera X/Y -> Ground X/Y -> Servos 1, 2, 3)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("arm_kinematics_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ARM KINEMATICS & SERVOS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Current Target Coordinates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KinematicBox(
                        label = "GROUND X",
                        value = "${String.format("%.1f", latestDetection?.groundX ?: 0f)} mm",
                        modifier = Modifier.weight(1f)
                    )
                    KinematicBox(
                        label = "GROUND Y",
                        value = "${String.format("%.1f", latestDetection?.groundY ?: 0f)} mm",
                        modifier = Modifier.weight(1f)
                    )
                    KinematicBox(
                        label = "CALIBRATION",
                        value = "× ${settings.cameraCalibrationFactor}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Servos Table
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ServoRow(
                        servoName = "SERVO 1 (BASE / AZIMUTH)",
                        controlMode = "AUTO ONLY",
                        currentAngle = picoStatus.servo1Angle,
                        homeAngle = settings.servo1Home
                    )
                    ServoRow(
                        servoName = "SERVO 2 (SHOULDER / REACH)",
                        controlMode = "AUTO ONLY",
                        currentAngle = picoStatus.servo2Angle,
                        homeAngle = settings.servo2Home
                    )
                    ServoRow(
                        servoName = "SERVO 3 (ELBOW / DRILL TILT)",
                        controlMode = "AUTO ONLY",
                        currentAngle = picoStatus.servo3Angle,
                        homeAngle = settings.servo3Home
                    )
                    ServoRow(
                        servoName = "SERVO 4 (AUXILIARY TOOL)",
                        controlMode = "MANUAL ONLY",
                        currentAngle = picoStatus.servo4Angle,
                        homeAngle = settings.servo4Home
                    )
                }
            }
        }

        // Section: AUTO WEED REMOVAL SEQUENCE TRACKER (Rule 24)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AUTO WEED REMOVAL WORKFLOW (RULE 24)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "1. AUTO SCANNING\n" +
                            "2. REAL CAMERA FRAME\n" +
                            "3. REAL AI DETECTION (WEED vs ONION)\n" +
                            "4. CONFIRM MULTIPLE FRAMES\n" +
                            "5. CALCULATE X/Y & CENTER\n" +
                            "6. GROUND TARGET & ONION SAFETY CHECK\n" +
                            "7. ROBOT STOP & PICO SAFETY CHECK\n" +
                            "8. ARM POSITION (SERVOS 1-3)\n" +
                            "9. TARGET VERIFY\n" +
                            "10. RELAY 1 ON (7-SECOND TIMER)\n" +
                            "11. RELAY 1 OFF (MAX 7 SEC ENFORCED)\n" +
                            "12. ARM HOME & RESUME SCANNING",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun InterlockItem(name: String, passed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 11.sp, color = TextPrimary)
        Text(
            text = if (passed) "✓ PASS" else "✕ LOCK",
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = if (passed) StatusOnlineGreen else StatusOfflineRed
        )
    }
}

@Composable
fun KinematicBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(8.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextPrimary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ServoRow(servoName: String, controlMode: String, currentAngle: Int, homeAngle: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(servoName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(controlMode, fontSize = 9.sp, color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "HOME: ${homeAngle}°",
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${currentAngle}°",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

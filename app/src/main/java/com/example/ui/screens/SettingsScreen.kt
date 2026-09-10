package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.ui.components.EmergencyStopBar
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.RobotViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val currentSettings by viewModel.settings.collectAsState()
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()
    val isEStopActive by viewModel.isEmergencyStopActive.collectAsState()
    val scope = rememberCoroutineScope()

    // Form local states
    var hotspotName by remember(currentSettings.hotspotName) { mutableStateOf(currentSettings.hotspotName) }
    var hotspotPassword by remember(currentSettings.hotspotPassword) { mutableStateOf(currentSettings.hotspotPassword) }

    var esp32CamIp by remember(currentSettings.esp32CamIp) { mutableStateOf(currentSettings.esp32CamIp) }
    var streamPort by remember(currentSettings.streamPort) { mutableStateOf(currentSettings.streamPort.toString()) }
    var capturePort by remember(currentSettings.capturePort) { mutableStateOf(currentSettings.capturePort.toString()) }

    var picoIp by remember(currentSettings.picoIp) { mutableStateOf(currentSettings.picoIp) }
    var picoPort by remember(currentSettings.picoPort) { mutableStateOf(currentSettings.picoPort.toString()) }
    var timeoutMs by remember(currentSettings.timeoutMs) { mutableStateOf(currentSettings.timeoutMs.toString()) }
    var reconnectMs by remember(currentSettings.reconnectIntervalMs) { mutableStateOf(currentSettings.reconnectIntervalMs.toString()) }

    var weedConfidence by remember(currentSettings.weedConfidenceThreshold) { mutableStateOf(currentSettings.weedConfidenceThreshold.toString()) }
    var confirmFrames by remember(currentSettings.confirmationFrames) { mutableStateOf(currentSettings.confirmationFrames.toString()) }
    var onionSafetyDist by remember(currentSettings.onionSafetyDistancePx) { mutableStateOf(currentSettings.onionSafetyDistancePx.toString()) }

    var camCalibration by remember(currentSettings.cameraCalibrationFactor) { mutableStateOf(currentSettings.cameraCalibrationFactor.toString()) }
    var armReach by remember(currentSettings.armReachMm) { mutableStateOf(currentSettings.armReachMm.toString()) }

    var minAutoV by remember(currentSettings.minAutoVoltage) { mutableStateOf(currentSettings.minAutoVoltage.toString()) }
    var maxTilt by remember(currentSettings.maxTiltDeg) { mutableStateOf(currentSettings.maxTiltDeg.toString()) }

    var isDevDemoMode by remember(currentSettings.isDevDemoMode) { mutableStateOf(currentSettings.isDevDemoMode) }

    var cameraTestFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingCamera by remember { mutableStateOf(false) }

    var picoTestFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingPico by remember { mutableStateOf(false) }

    var saveFeedback by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun buildUpdatedSettings(): AppSettings {
        return currentSettings.copy(
            hotspotName = hotspotName.trim(),
            hotspotPassword = hotspotPassword,
            esp32CamIp = esp32CamIp.trim(),
            streamPort = streamPort.toIntOrNull() ?: 81,
            capturePort = capturePort.toIntOrNull() ?: 80,
            picoIp = picoIp.trim(),
            picoPort = picoPort.toIntOrNull() ?: 80,
            timeoutMs = timeoutMs.toLongOrNull() ?: 3000L,
            reconnectIntervalMs = reconnectMs.toLongOrNull() ?: 2000L,
            weedConfidenceThreshold = weedConfidence.toFloatOrNull() ?: 0.75f,
            confirmationFrames = confirmFrames.toIntOrNull() ?: 2,
            onionSafetyDistancePx = onionSafetyDist.toFloatOrNull() ?: 60f,
            cameraCalibrationFactor = camCalibration.toFloatOrNull() ?: 0.85f,
            armReachMm = armReach.toFloatOrNull() ?: 250f,
            minAutoVoltage = minAutoV.toDoubleOrNull() ?: 11.1,
            maxTiltDeg = maxTilt.toDoubleOrNull() ?: 20.0,
            isDevDemoMode = isDevDemoMode
        )
    }

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

        // Save Button & Test All Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val updated = buildUpdatedSettings()
                    viewModel.updateSettings(updated)
                    saveFeedback = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(if (saveFeedback) "SAVED!" else "SAVE SETTINGS", fontWeight = FontWeight.Black)
                }
            }

            Button(
                onClick = { viewModel.testAllConnections() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("test_all_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF19406B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                    Text("TEST ALL", fontWeight = FontWeight.Black)
                }
            }
        }

        // Section 1: MOBILE HOTSPOT / ROBOT NETWORK (Rule 10)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hotspot_settings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "MOBILE HOTSPOT / ROBOT NETWORK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Phone Network Status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ROBOT NETWORK STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = if (isNetworkConnected) "● PHONE NETWORK CONNECTED" else "● NETWORK OFFLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isNetworkConnected) StatusOnlineGreen else StatusOfflineRed,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedTextField(
                    value = hotspotName,
                    onValueChange = { hotspotName = it },
                    label = { Text("HOTSPOT NAME") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hotspot_name_field")
                )

                OutlinedTextField(
                    value = hotspotPassword,
                    onValueChange = { hotspotPassword = it },
                    label = { Text("HOTSPOT PASSWORD") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hotspot_password_field")
                )

                // Android Hotspot OS notice
                Text(
                    text = "Note: The Android OS requires manual mobile hotspot toggling in System Settings. This profile is stored locally as the robot network configuration reference.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // Section 2: ESP32-CAM SETTINGS (Rule 2 & 6)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("esp32_cam_settings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "ESP32-CAM REAL CAMERA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = esp32CamIp,
                    onValueChange = { esp32CamIp = it },
                    label = { Text("IP ADDRESS") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("esp32_ip_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = streamPort,
                        onValueChange = { streamPort = it },
                        label = { Text("STREAM PORT") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stream_port_field")
                    )

                    OutlinedTextField(
                        value = capturePort,
                        onValueChange = { capturePort = it },
                        label = { Text("CAPTURE PORT") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("capture_port_field")
                    )
                }

                // Automatically generated real URLs preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("AUTOMATICALLY GENERATED URLS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text("STREAM: http://${esp32CamIp.trim()}:${streamPort.trim()}/stream", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    Text("CAPTURE: http://${esp32CamIp.trim()}:${capturePort.trim()}/capture", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                }

                // Test Camera Button (Rule 6)
                Button(
                    onClick = {
                        isTestingCamera = true
                        cameraTestFeedback = null
                        scope.launch {
                            val tempSettings = buildUpdatedSettings()
                            val res = viewModel.esp32CamClient.testCamera(tempSettings)
                            val status = if (res.cameraOnline) "✓ ONLINE" else "✕ OFFLINE"
                            val stream = if (res.streamReady) "✓ READY" else "✕ FAILED"
                            val capture = if (res.captureReady) "✓ READY" else "✕ FAILED"
                            cameraTestFeedback = "ESP32-CAM CONNECTION TEST\nCamera: $status\nStream: $stream\nCapture: $capture\n${res.details}"
                            isTestingCamera = false
                        }
                    },
                    enabled = !isTestingCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("test_camera_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTestingCamera) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                    } else {
                        Text("TEST CAMERA", fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }

                if (cameraTestFeedback != null) {
                    Text(
                        text = cameraTestFeedback!!,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (cameraTestFeedback!!.contains("✓ ONLINE")) StatusOnlineGreen else StatusOfflineRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(8.dp)
                    )
                }
            }
        }

        // Section 3: RASPBERRY PI PICO W SETTINGS (Rule 8 & 9)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pico_settings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "RASPBERRY PI PICO W CONTROLLER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = picoIp,
                        onValueChange = { picoIp = it },
                        label = { Text("IP ADDRESS") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("pico_ip_field")
                    )

                    OutlinedTextField(
                        value = picoPort,
                        onValueChange = { picoPort = it },
                        label = { Text("PORT") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pico_port_field")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = timeoutMs,
                        onValueChange = { timeoutMs = it },
                        label = { Text("TIMEOUT (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = reconnectMs,
                        onValueChange = { reconnectMs = it },
                        label = { Text("RECONNECT (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Test Pico Button (Rule 9)
                Button(
                    onClick = {
                        isTestingPico = true
                        picoTestFeedback = null
                        scope.launch {
                            val tempSettings = buildUpdatedSettings()
                            val res = viewModel.picoWClient.testPico(tempSettings)
                            val status = if (res.picoOnline) "✓ ONLINE" else "✕ OFFLINE"
                            val api = if (res.apiReady) "✓ READY" else "✕ FAILED"
                            picoTestFeedback = "PICO W CONNECTION TEST\nPico W: $status\nAPI: $api\n${res.details}"
                            isTestingPico = false
                        }
                    },
                    enabled = !isTestingPico,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("test_pico_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTestingPico) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                    } else {
                        Text("TEST PICO", fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }

                if (picoTestFeedback != null) {
                    Text(
                        text = picoTestFeedback!!,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (picoTestFeedback!!.contains("✓ ONLINE")) StatusOnlineGreen else StatusOfflineRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(8.dp)
                    )
                }
            }
        }

        // Section 4: AI & ARM CALIBRATION (Rule 29)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "AI & ARM CALIBRATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weedConfidence,
                        onValueChange = { weedConfidence = it },
                        label = { Text("WEED CONFIDENCE (0.0-1.0)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = confirmFrames,
                        onValueChange = { confirmFrames = it },
                        label = { Text("CONFIRM FRAMES") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = onionSafetyDist,
                        onValueChange = { onionSafetyDist = it },
                        label = { Text("ONION SAFETY (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = camCalibration,
                        onValueChange = { camCalibration = it },
                        label = { Text("CAM CALIBRATION") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 5: BATTERY & MPU THRESHOLDS (Rule 29)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "SAFETY THRESHOLDS (BATTERY & MPU)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAutoV,
                        onValueChange = { minAutoV = it },
                        label = { Text("MIN AUTO VOLTS (V)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxTilt,
                        onValueChange = { maxTilt = it },
                        label = { Text("MAX TILT DEG (°)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Optional Bench / Lab Demo Mode (Separated from Real Mode)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LAB DEMO / BENCH SIMULATION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDevDemoMode) StatusWarningYellow else TextPrimary
                    )
                    Text(
                        text = "Optional mode for UI bench tests when physical robot hardware is turned off. Real Mode is the default.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isDevDemoMode,
                    onCheckedChange = { isDevDemoMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StatusWarningYellow,
                        checkedTrackColor = StatusWarningYellow.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("dev_demo_mode_switch")
                )
            }
        }
    }
}

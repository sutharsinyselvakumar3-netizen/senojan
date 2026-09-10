package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WeedDetectOrange
import com.example.viewmodel.RobotViewModel
import kotlinx.coroutines.launch

@Composable
fun AiVisionScreen(
    viewModel: RobotViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val camStatus by viewModel.cameraTelemetry.collectAsState()
    val latestFrame by viewModel.esp32CamClient.latestFrame.collectAsState()
    val latestDetection by viewModel.latestDetection.collectAsState()
    val isEStopActive by viewModel.isEmergencyStopActive.collectAsState()
    val autoStepMessage by viewModel.autoStepMessage.collectAsState()

    val scope = rememberCoroutineScope()
    var isManualAnalyzing by remember { mutableStateOf(false) }
    var manualAnalysisError by remember { mutableStateOf<String?>(null) }

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

        // Section: LIVE STREAM DISPLAY (Rule 7, 4 & 5)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("camera_stream_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070F16)),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (camStatus.streamReady) StatusOnlineGreen else MaterialTheme.colorScheme.outline
                )
            )
        ) {
            Column {
                // Stream Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE CAMERA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (camStatus.streamReady) StatusOnlineGreen
                                    else if (camStatus.streamConnecting) StatusWarningYellow
                                    else StatusOfflineRed
                                )
                        )
                        Text(
                            text = if (camStatus.streamReady) "● STREAM ONLINE"
                            else if (camStatus.streamConnecting) "CONNECTING..."
                            else "● OFFLINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (camStatus.streamReady) StatusOnlineGreen else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Stream Viewport with Double-Tap Support
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color(0xFF040A0F))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    viewModel.onStreamDoubleTap()
                                }
                            )
                        }
                        .testTag("stream_viewport"),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // 1. Connecting State
                        camStatus.streamConnecting -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "CONNECTING STREAM...",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = settings.streamUrl,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // 2. Real Frame Available
                        latestFrame != null && camStatus.online -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = latestFrame!!.asImageBitmap(),
                                    contentDescription = "ESP32-CAM real video stream",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // AI Detection Bounding Box Overlay
                                if (latestDetection != null && latestDetection!!.box != null) {
                                    val box = latestDetection!!.box!!
                                    val isWeed = latestDetection!!.label == "WEED"
                                    val boxBorderColor = if (isWeed) WeedDetectOrange else OnionProtectPurple

                                    // Floating indicator badge over box
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(boxBorderColor.copy(alpha = 0.85f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${latestDetection!!.label} (${(latestDetection!!.confidence * 100).toInt()}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Stream IP Failure / Offline (Strictly adheres to Rule 5: never fake camera!)
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "STREAM IP FAILED",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = StatusOfflineRed,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "ESP32-CAM STREAM IS NOT AVAILABLE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Check:\n" +
                                            "• ESP32-CAM power\n" +
                                            "• Wi-Fi connection\n" +
                                            "• IP address (${settings.esp32CamIp})\n" +
                                            "• Stream port (${settings.streamPort})\n" +
                                            "• Phone network\n" +
                                            "• Mobile hotspot/network",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.onStreamDoubleTap() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("retry_stream_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("RETRY STREAM", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onNavigateToSettings,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("open_camera_settings_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("SETTINGS", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "DOUBLE TAP = START / RESTART STREAM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // Section: REAL-TIME AI TELEMETRY READOUT (Rule 22: Display CenterX, CenterY, Bounding Box)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_detection_card"),
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
                    Text(
                        text = "AI VISION ANALYSIS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "GEMINI INTELLIGENCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                if (latestDetection != null) {
                    val det = latestDetection!!
                    val isWeed = det.label == "WEED"
                    val isProtectedOnion = det.label == "ONION"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = det.label,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = if (isWeed) WeedDetectOrange else if (isProtectedOnion) OnionProtectPurple else TextSecondary
                        )
                        Text(
                            text = "CONFIDENCE: ${(det.confidence * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (det.box != null) {
                        val b = det.box
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "X: ${b.x.toInt()}    Y: ${b.y.toInt()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "W: ${b.width.toInt()}    H: ${b.height.toInt()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "CENTER X: ${String.format("%.1f", b.centerX)}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "CENTER Y: ${String.format("%.1f", b.centerY)}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Onion Safety Notice
                    if (!det.onionSafe) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusOfflineRed.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = StatusOfflineRed)
                            Text(
                                text = "ONION IN SAFE RANGE — AUTO DRILL PREVENTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = StatusOfflineRed
                            )
                        }
                    } else if (isWeed) {
                        Text(
                            text = "✓ ONION SAFETY VERIFIED: Safe to clear weed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusOnlineGreen
                        )
                    }

                    if (det.message.isNotBlank()) {
                        Text(
                            text = det.message,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No detection frame analyzed yet.\nTap 'CAPTURE & ANALYZE FRAME' or switch to AUTO mode.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Manual Trigger Button for Bench Testing
                Button(
                    onClick = {
                        isManualAnalyzing = true
                        manualAnalysisError = null
                        scope.launch {
                            val bytes = viewModel.esp32CamClient.captureFrame(settings)
                            if (bytes != null) {
                                val res = viewModel.geminiVisionClient.analyzeFrame(bytes, settings)
                                if (res.isFailure) {
                                    manualAnalysisError = res.exceptionOrNull()?.message ?: "Analysis failed"
                                }
                            } else {
                                manualAnalysisError = "Cannot capture frame from ${settings.captureUrl}"
                            }
                            isManualAnalyzing = false
                        }
                    },
                    enabled = !isManualAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("manual_capture_analyze_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isManualAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ANALYZING REAL FRAME...", fontWeight = FontWeight.Bold)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text("CAPTURE & ANALYZE FRAME", fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (manualAnalysisError != null) {
                    Text(
                        text = manualAnalysisError!!,
                        color = StatusOfflineRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

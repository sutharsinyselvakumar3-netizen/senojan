package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectionTestReport
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.StatusWarningYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusIndicatorDot(
    isOnline: Boolean,
    isConnecting: Boolean = false,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        val dotColor = when {
            isConnecting -> StatusWarningYellow
            isOnline -> StatusOnlineGreen
            else -> StatusOfflineRed
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOnline) StatusOnlineGreen else if (isConnecting) StatusWarningYellow else StatusOfflineRed,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun HardwareStatusRow(
    title: String,
    isOnline: Boolean,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        StatusIndicatorDot(
            isOnline = isOnline,
            label = statusText
        )
    }
}

@Composable
fun EmergencyStopBar(
    isEStopActive: Boolean,
    onTriggerEStop: () -> Unit,
    onResetEStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("emergency_stop_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isEStopActive) EmergencyRed.copy(alpha = 0.95f) else Color(0xFF240A0A)
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isEStopActive) Color.White else EmergencyRed)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Dangerous,
                    contentDescription = "E-Stop icon",
                    tint = if (isEStopActive) Color.White else EmergencyRed,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = if (isEStopActive) "EMERGENCY STOP ACTIVE" else "EMERGENCY STOP",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (isEStopActive) "All actuators disabled. Reset required." else "Instant hardware power cut",
                        fontSize = 11.sp,
                        color = if (isEStopActive) Color.White.copy(alpha = 0.85f) else TextSecondary
                    )
                }
            }

            if (isEStopActive) {
                Button(
                    onClick = onResetEStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = EmergencyRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("reset_estop_button")
                ) {
                    Text("RESET", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onTriggerEStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("trigger_estop_button")
                ) {
                    Text("E-STOP", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun StartAutoDialog(
    show: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "START AUTO MODE?",
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Automatic robot operation will start.",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "• Manual movement will be disabled.\n" +
                            "• Servo 4 will be disabled.\n" +
                            "• SOIL will be OFF.\n" +
                            "• WATER will be OFF.\n" +
                            "• AI detection will start.\n" +
                            "• Automatic arm operation will start.\n" +
                            "• AUTO DRILL may operate.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusOfflineRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = StatusOfflineRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("confirm_start_auto_button")
            ) {
                Text("CONFIRM & START AUTO", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_start_auto_button")
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun StopAutoDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "STOP AUTO MODE?",
                fontWeight = FontWeight.Black,
                color = StatusOfflineRed
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Automatic operation will stop safely.",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "• AI AUTO OFF\n" +
                            "• ROBOT AUTO OFF\n" +
                            "• AUTO DRILL OFF\n" +
                            "• SOIL OFF\n" +
                            "• WATER OFF\n" +
                            "• ARM SAFE/HOME",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusOfflineRed,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_stop_auto_button")
            ) {
                Text("CONFIRM & SWITCH TO MANUAL", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_stop_auto_button")
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun ConnectionTestDialog(
    report: ConnectionTestReport?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    if (report == null && !isLoading) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "CONNECTION TEST",
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text("Testing all hardware and network endpoints...", color = TextPrimary)
                }
            } else if (report != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TestResultItem(name = "NETWORK", ok = report.networkConnected, okText = "CONNECTED", failText = "OFFLINE")
                    TestResultItem(name = "ESP32-CAM", ok = report.esp32CamOnline, okText = "ONLINE", failText = "OFFLINE")
                    TestResultItem(name = "STREAM", ok = report.streamReady, okText = "READY", failText = "FAILED")
                    TestResultItem(name = "CAPTURE", ok = report.captureReady, okText = "READY", failText = "FAILED")
                    TestResultItem(name = "PICO W", ok = report.picoOnline, okText = "ONLINE", failText = "OFFLINE")
                    TestResultItem(name = "PICO API", ok = report.picoApiReady, okText = "READY", failText = "FAILED")
                    TestResultItem(name = "AI ENGINE", ok = report.aiEngineReady, okText = "READY", failText = "FAILED")

                    if (report.details.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Diagnostic Log:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        report.details.forEach { (k, v) ->
                            Text(
                                text = "• $k: $v",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_connection_test_button")
            ) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
fun TestResultItem(name: String, ok: Boolean, okText: String, failText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
        Text(
            text = if (ok) "✓ $okText" else "✕ $failText",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            color = if (ok) StatusOnlineGreen else StatusOfflineRed,
            fontFamily = FontFamily.Monospace
        )
    }
}

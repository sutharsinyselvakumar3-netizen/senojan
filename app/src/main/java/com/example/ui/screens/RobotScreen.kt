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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RobotMode
import com.example.ui.components.EmergencyStopBar
import com.example.ui.theme.DarkGreenAccent
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.RobotViewModel

@Composable
fun RobotScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val robotMode by viewModel.robotMode.collectAsState()
    val picoStatus by viewModel.picoTelemetry.collectAsState()
    val isEStopActive by viewModel.isEmergencyStopActive.collectAsState()
    val isManual = robotMode == RobotMode.MANUAL && !isEStopActive

    var servo4LocalAngle by remember(picoStatus.servo4Angle) {
        mutableFloatStateOf(picoStatus.servo4Angle.toFloat())
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // E-Stop Top Bar
        EmergencyStopBar(
            isEStopActive = isEStopActive,
            onTriggerEStop = { viewModel.triggerEmergencyStop() },
            onResetEStop = { viewModel.resetEmergencyStop() }
        )

        // Offline / Mode Warning Banner
        if (!picoStatus.online) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StatusOfflineRed.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = StatusOfflineRed)
                    Column {
                        Text("PICO W OFFLINE", fontWeight = FontWeight.Bold, color = StatusOfflineRed)
                        Text("ROBOT CONTROLS DISABLED. Connect to Pico W.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        } else if (!isManual) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102847)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("AUTO MODE ACTIVE", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Manual movement & actuators locked during autonomous onion weeding.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Section: MANUAL MOVEMENT (Strictly 4 push/tap buttons: FORWARD, LEFT, RIGHT, REVERSE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_movement_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "MANUAL MOVEMENT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // FORWARD BUTTON (Push/tap button)
                Button(
                    onClick = { viewModel.sendMovement("forward") },
                    enabled = isManual,
                    modifier = Modifier
                        .size(width = 130.dp, height = 56.dp)
                        .testTag("forward_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Forward")
                        Text("FORWARD", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                // LEFT & RIGHT BUTTONS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT BUTTON
                    Button(
                        onClick = { viewModel.sendMovement("left") },
                        enabled = isManual,
                        modifier = Modifier
                            .size(width = 120.dp, height = 56.dp)
                            .testTag("left_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Left")
                            Text("LEFT", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Status Indicator between buttons
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = picoStatus.currentDirection.take(1),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // RIGHT BUTTON
                    Button(
                        onClick = { viewModel.sendMovement("right") },
                        enabled = isManual,
                        modifier = Modifier
                            .size(width = 120.dp, height = 56.dp)
                            .testTag("right_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("RIGHT", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Icon(Icons.Default.ArrowForward, contentDescription = "Right")
                        }
                    }
                }

                // REVERSE BUTTON
                Button(
                    onClick = { viewModel.sendMovement("reverse") },
                    enabled = isManual,
                    modifier = Modifier
                        .size(width = 130.dp, height = 56.dp)
                        .testTag("reverse_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REVERSE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Reverse")
                    }
                }
            }
        }

        // Section: SPEED SELECTOR (SLOW / MEDIUM / FAST)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DRIVE SPEED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SLOW", "MEDIUM", "FAST").forEach { speed ->
                        val isSelected = picoStatus.speedLevel.equals(speed, ignoreCase = true)
                        Button(
                            onClick = { viewModel.setSpeed(speed.lowercase()) },
                            enabled = isManual,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("speed_${speed.lowercase()}_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f),
                                contentColor = if (isSelected) Color.Black else TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(speed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: ACTUATOR CONTROLS (Servo 4, Soil, Water)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "AUXILIARY ACTUATORS (MANUAL ONLY)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Servo 4 Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SERVO 4 (AUX TOOL)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${servo4LocalAngle.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = servo4LocalAngle,
                        onValueChange = {
                            servo4LocalAngle = it
                            if (isManual) viewModel.setServo4Angle(it.toInt())
                        },
                        valueRange = 10f..170f,
                        enabled = isManual,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("servo4_slider")
                    )
                }

                // Relays 2 & 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Soil Relay (Relay 2)
                    Button(
                        onClick = { viewModel.toggleSoilRelay(picoStatus.relay2Soil) },
                        enabled = isManual,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("toggle_soil_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (picoStatus.relay2Soil) StatusOnlineGreen else Color.Black.copy(alpha = 0.3f),
                            contentColor = if (picoStatus.relay2Soil) Color.Black else TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Grass, contentDescription = null)
                            Column {
                                Text("SOIL SENSOR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(if (picoStatus.relay2Soil) "ACTIVE" else "OFF", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Water Pump Relay (Relay 3)
                    Button(
                        onClick = { viewModel.toggleWaterRelay(picoStatus.relay3Water) },
                        enabled = isManual,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("toggle_water_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (picoStatus.relay3Water) Color(0xFF00B0FF) else Color.Black.copy(alpha = 0.3f),
                            contentColor = if (picoStatus.relay3Water) Color.Black else TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null)
                            Column {
                                Text("WATER PUMP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(if (picoStatus.relay3Water) "PUMPING" else "OFF", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Relay 1 Banner (Rule 18: Relay 1 must remain unavailable manually)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF261D0F))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AUTO DRILL (RELAY 1)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFB300)
                            )
                            Text(
                                text = "AUTOMATIC ONLY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

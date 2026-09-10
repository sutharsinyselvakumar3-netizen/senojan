package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiDetectionResult
import com.example.data.AppSettings
import com.example.data.CameraTelemetry
import com.example.data.ConnectionStatus
import com.example.data.ConnectionTestReport
import com.example.data.HardwareTelemetry
import com.example.data.RobotMode
import com.example.data.SettingsRepository
import com.example.network.Esp32CamClient
import com.example.network.GeminiVisionClient
import com.example.network.NetworkMonitor
import com.example.network.PicoWClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RobotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val networkMonitor = NetworkMonitor(application)
    val esp32CamClient = Esp32CamClient()
    val picoWClient = PicoWClient()
    val geminiVisionClient = GeminiVisionClient()

    // Settings
    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Active Mode (MANUAL default - never auto-start AUTO)
    private val _robotMode = MutableStateFlow(RobotMode.MANUAL)
    val robotMode: StateFlow<RobotMode> = _robotMode.asStateFlow()

    // Connection Indicators
    val isNetworkConnected = networkMonitor.isConnected
    val cameraTelemetry = esp32CamClient.cameraStatus
    val picoTelemetry = picoWClient.telemetry

    // AI Status & Detection
    private val _latestDetection = MutableStateFlow<AiDetectionResult?>(null)
    val latestDetection: StateFlow<AiDetectionResult?> = _latestDetection.asStateFlow()

    private val _aiEngineStatus = MutableStateFlow(ConnectionStatus.ONLINE)
    val aiEngineStatus: StateFlow<ConnectionStatus> = _aiEngineStatus.asStateFlow()

    // Auto Mode Workflow State
    private val _autoStepMessage = MutableStateFlow<String>("")
    val autoStepMessage: StateFlow<String> = _autoStepMessage.asStateFlow()

    private val _drillCountdown = MutableStateFlow<Int>(0)
    val drillCountdown: StateFlow<Int> = _drillCountdown.asStateFlow()

    // Modal Confirmation Dialogs
    private val _showStartAutoDialog = MutableStateFlow(false)
    val showStartAutoDialog: StateFlow<Boolean> = _showStartAutoDialog.asStateFlow()

    private val _showStopAutoDialog = MutableStateFlow(false)
    val showStopAutoDialog: StateFlow<Boolean> = _showStopAutoDialog.asStateFlow()

    private val _autoHardwareCheckError = MutableStateFlow<String?>(null)
    val autoHardwareCheckError: StateFlow<String?> = _autoHardwareCheckError.asStateFlow()

    // Emergency Stop
    private val _isEmergencyStopActive = MutableStateFlow(false)
    val isEmergencyStopActive: StateFlow<Boolean> = _isEmergencyStopActive.asStateFlow()

    // Connection Test Report
    private val _testReport = MutableStateFlow<ConnectionTestReport?>(null)
    val testReport: StateFlow<ConnectionTestReport?> = _testReport.asStateFlow()

    private val _isTestingConnections = MutableStateFlow(false)
    val isTestingConnections: StateFlow<Boolean> = _isTestingConnections.asStateFlow()

    // Background Jobs
    private var telemetryPollingJob: Job? = null
    private var autoSequenceJob: Job? = null

    init {
        // Startup sequence:
        // 1. Load settings
        // 2. Initial ping to verify status
        // 3. Start telemetry polling
        startTelemetryPolling()
    }

    fun startTelemetryPolling() {
        telemetryPollingJob?.cancel()
        telemetryPollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentSettings = _settings.value
                try {
                    // Poll Pico status
                    picoWClient.fetchStatus(currentSettings)
                } catch (_: Exception) {}

                delay(currentSettings.reconnectIntervalMs.coerceAtLeast(1000L))
            }
        }
    }

    // --- MANUAL CONTROLS ---

    fun sendMovement(direction: String) {
        if (_robotMode.value != RobotMode.MANUAL || _isEmergencyStopActive.value) return
        viewModelScope.launch {
            picoWClient.sendRobotDirection(direction, _settings.value.picoIp, _settings.value)
        }
    }

    fun setSpeed(speed: String) {
        if (_robotMode.value != RobotMode.MANUAL || _isEmergencyStopActive.value) return
        viewModelScope.launch {
            picoWClient.sendRobotDirection("stop", speed, _settings.value)
        }
    }

    fun setServo4Angle(angle: Int) {
        if (_robotMode.value != RobotMode.MANUAL || _isEmergencyStopActive.value) return
        val clamped = angle.coerceIn(_settings.value.servo4Min, _settings.value.servo4Max)
        viewModelScope.launch {
            picoWClient.setServo4Angle(clamped, _settings.value)
        }
    }

    fun toggleSoilRelay(currentState: Boolean) {
        if (_robotMode.value != RobotMode.MANUAL || _isEmergencyStopActive.value) return
        viewModelScope.launch {
            picoWClient.setRelay(2, !currentState, _settings.value)
        }
    }

    fun toggleWaterRelay(currentState: Boolean) {
        if (_robotMode.value != RobotMode.MANUAL || _isEmergencyStopActive.value) return
        viewModelScope.launch {
            picoWClient.setRelay(3, !currentState, _settings.value)
        }
    }

    // --- MODE SWITCHING: MANUAL -> AUTO ---

    fun requestStartAutoMode() {
        if (_isEmergencyStopActive.value) return
        _autoHardwareCheckError.value = null
        _showStartAutoDialog.value = true
    }

    fun dismissStartAutoDialog() {
        _showStartAutoDialog.value = false
        _autoHardwareCheckError.value = null
    }

    fun confirmStartAutoMode() {
        _showStartAutoDialog.value = false
        viewModelScope.launch {
            val currentSettings = _settings.value
            _autoStepMessage.value = "DISABLING MANUAL CONTROLS..."

            // 1. Send stop and turn off all manual relays
            picoWClient.sendRobotDirection("stop", "slow", currentSettings)
            picoWClient.setRelay(2, false, currentSettings)
            picoWClient.setRelay(3, false, currentSettings)
            picoWClient.setServo4Angle(currentSettings.servo4Home, currentSettings)

            _autoStepMessage.value = "CHECKING REAL HARDWARE..."
            delay(300)

            // 2. Check REAL Hardware
            val camTest = esp32CamClient.testCamera(currentSettings)
            val picoTest = picoWClient.testPico(currentSettings)

            val errors = mutableListOf<String>()
            if (!camTest.cameraOnline) errors.add("REAL ESP32-CAM OFFLINE")
            if (!camTest.streamReady) errors.add("REAL STREAM NOT READY")
            if (!picoTest.picoOnline) errors.add("REAL PICO W OFFLINE")
            if (!picoTest.apiReady) errors.add("REAL PICO API NOT READY")

            // Battery check
            val v = picoTest.batteryVoltage ?: picoWClient.telemetry.value.batteryVoltage
            if (v != null && v < currentSettings.minAutoVoltage) {
                errors.add("BATTERY TOO LOW (${String.format("%.1f", v)}V < ${currentSettings.minAutoVoltage}V)")
            }

            // MPU tilt check
            val pitch = picoWClient.telemetry.value.pitchDeg
            val roll = picoWClient.telemetry.value.rollDeg
            if (pitch != null && Math.abs(pitch) > currentSettings.maxTiltDeg) {
                errors.add("MPU TILT DANGEROUS (${String.format("%.1f", pitch)}°)")
            }
            if (roll != null && Math.abs(roll) > currentSettings.maxTiltDeg) {
                errors.add("MPU ROLL DANGEROUS (${String.format("%.1f", roll)}°)")
            }

            // E-Stop check
            if (picoTest.emergencyStopActive || _isEmergencyStopActive.value) {
                errors.add("EMERGENCY STOP IS ACTIVE")
            }

            if (errors.isNotEmpty()) {
                _autoHardwareCheckError.value = errors.joinToString("\n• ", prefix = "Cannot start AUTO:\n• ")
                _autoStepMessage.value = "AUTO START ABORTED: Hardware checks failed"
                return@launch
            }

            // 3. Switch Pico to Auto mode
            picoWClient.setMode("auto", currentSettings)

            // 4. Activate AUTO mode (dark blue theme switches here!)
            _robotMode.value = RobotMode.AUTO
            _autoStepMessage.value = "AUTO MODE ACTIVE: Scanning garden..."

            // Start Auto Weed Removal Loop
            startAutoWeedRemovalLoop()
        }
    }

    // --- MODE SWITCHING: AUTO -> MANUAL ---

    fun requestStopAutoMode() {
        _showStopAutoDialog.value = true
    }

    fun dismissStopAutoDialog() {
        _showStopAutoDialog.value = false
    }

    fun confirmStopAutoMode() {
        _showStopAutoDialog.value = false
        viewModelScope.launch {
            val currentSettings = _settings.value
            _autoStepMessage.value = "STOPPING AUTO SAFELY..."

            // Cancel running auto sequence
            autoSequenceJob?.cancel()
            autoSequenceJob = null

            // Force Drill OFF (Relay 1), Soil OFF (Relay 2), Water OFF (Relay 3)
            picoWClient.setRelay(1, false, currentSettings)
            picoWClient.setRelay(2, false, currentSettings)
            picoWClient.setRelay(3, false, currentSettings)
            _drillCountdown.value = 0

            // Stop movement
            picoWClient.sendRobotDirection("stop", "slow", currentSettings)

            // Safe arm home
            picoWClient.sendDetectionData("home", 160f, 120f, 1.0f, 0f, 0f, currentSettings)
            picoWClient.setMode("manual", currentSettings)

            delay(400)

            // Re-enable manual mode (dark green theme switches here!)
            _robotMode.value = RobotMode.MANUAL
            _autoStepMessage.value = "MANUAL ACTIVE"
        }
    }

    // --- AUTO WEED REMOVAL SEQUENCE (Rule 24 & 37) ---

    private fun startAutoWeedRemovalLoop() {
        autoSequenceJob?.cancel()
        autoSequenceJob = viewModelScope.launch(Dispatchers.IO) {
            var consecutiveWeedFrames = 0
            var lastDetectedWeed: AiDetectionResult? = null

            while (isActive && _robotMode.value == RobotMode.AUTO && !_isEmergencyStopActive.value) {
                val currentSettings = _settings.value
                _autoStepMessage.value = "AUTO SCANNING..."

                // 1. Capture real frame
                val frameBytes = esp32CamClient.captureFrame(currentSettings)
                if (frameBytes == null) {
                    _autoStepMessage.value = "CAMERA FRAME FAILED: Retrying..."
                    delay(1500)
                    continue
                }

                // 2. Real AI Analysis
                _autoStepMessage.value = "AI ANALYZING FRAME..."
                val aiResult = geminiVisionClient.analyzeFrame(frameBytes, currentSettings)

                if (aiResult.isFailure) {
                    _autoStepMessage.value = "AI ANALYSIS FAILED: Safe hold..."
                    delay(2000)
                    continue
                }

                val detection = aiResult.getOrNull()
                _latestDetection.value = detection

                if (detection == null) {
                    delay(1000)
                    continue
                }

                // Rule: ONION = NEVER DRILL, UNKNOWN = NEVER DRILL, LOW CONFIDENCE = NEVER DRILL
                if (detection.label == "ONION") {
                    _autoStepMessage.value = "ONION DETECTED: Protecting plant, skipping drill."
                    consecutiveWeedFrames = 0
                    delay(1500)
                    continue
                }

                if (detection.label == "UNKNOWN" || detection.confidence < currentSettings.weedConfidenceThreshold) {
                    _autoStepMessage.value = "UNKNOWN/LOW CONFIDENCE (${(detection.confidence * 100).toInt()}%): Skipping."
                    consecutiveWeedFrames = 0
                    delay(1500)
                    continue
                }

                if (detection.label == "WEED") {
                    consecutiveWeedFrames++
                    lastDetectedWeed = detection

                    if (consecutiveWeedFrames < currentSettings.confirmationFrames) {
                        _autoStepMessage.value = "WEED DETECTED: Confirming frame $consecutiveWeedFrames/${currentSettings.confirmationFrames}..."
                        delay(600)
                        continue
                    }

                    // Verified Weed confirmed over multiple frames!
                    _autoStepMessage.value = "WEED CONFIRMED: Center (${detection.box?.centerX?.toInt()}, ${detection.box?.centerY?.toInt()})"

                    // Onion safety check before drilling
                    if (!detection.onionSafe) {
                        _autoStepMessage.value = "ONION SAFETY ALERT: Onion too close to weed! Drilling ABORTED."
                        consecutiveWeedFrames = 0
                        delay(3000)
                        continue
                    }

                    // Check Pico status before drilling
                    val currentTelemetry = picoWClient.telemetry.value
                    if (!currentTelemetry.online || currentTelemetry.eStopActive) {
                        _autoStepMessage.value = "PICO OFFLINE / E-STOP: Drilling aborted."
                        consecutiveWeedFrames = 0
                        delay(2000)
                        continue
                    }

                    // ROBOT STOP
                    _autoStepMessage.value = "ROBOT STOPPING..."
                    picoWClient.sendRobotDirection("stop", "slow", currentSettings)
                    delay(400)

                    // ARM POSITION: send detection & target angles to Pico
                    _autoStepMessage.value = "ARM POSITIONING TO TARGET..."
                    picoWClient.sendDetectionData(
                        "weed",
                        detection.box?.centerX ?: 160f,
                        detection.box?.centerY ?: 120f,
                        detection.confidence,
                        detection.groundX,
                        detection.groundY,
                        currentSettings
                    )
                    delay(1000)

                    // TARGET VERIFY
                    _autoStepMessage.value = "TARGET VERIFIED. STARTING AUTO DRILL..."
                    delay(300)

                    // RELAY 1 ON
                    picoWClient.setRelay(1, true, currentSettings)

                    // 7 SECOND COUNTDOWN (07..00)
                    for (sec in 7 downTo 0) {
                        if (!isActive || _robotMode.value != RobotMode.AUTO || _isEmergencyStopActive.value) {
                            picoWClient.setRelay(1, false, currentSettings)
                            break
                        }
                        _drillCountdown.value = sec
                        _autoStepMessage.value = "AUTO DRILL ACTIVE: ${String.format("%02d", sec)}s"
                        delay(1000)
                    }

                    // RELAY 1 OFF (Strictly after 7 seconds)
                    picoWClient.setRelay(1, false, currentSettings)
                    _drillCountdown.value = 0
                    _autoStepMessage.value = "DRILL OFF. ARM RETRACTING..."

                    // ARM HOME
                    picoWClient.sendDetectionData("home", 160f, 120f, 1.0f, 0f, 0f, currentSettings)
                    delay(currentSettings.drillCooldownMs)

                    consecutiveWeedFrames = 0
                    _autoStepMessage.value = "WEED CLEARED. RESUMING SCANNING..."
                }

                delay(800)
            }
        }
    }

    // --- EMERGENCY STOP ---

    fun triggerEmergencyStop() {
        _isEmergencyStopActive.value = true
        _robotMode.value = RobotMode.MANUAL
        autoSequenceJob?.cancel()
        autoSequenceJob = null
        _drillCountdown.value = 0

        viewModelScope.launch {
            val currentSettings = _settings.value
            picoWClient.triggerEmergencyStop(currentSettings)
            picoWClient.setRelay(1, false, currentSettings)
            picoWClient.setRelay(2, false, currentSettings)
            picoWClient.setRelay(3, false, currentSettings)
            picoWClient.sendRobotDirection("stop", "slow", currentSettings)
            _autoStepMessage.value = "EMERGENCY STOP ACTIVE"
        }
    }

    fun resetEmergencyStop() {
        _isEmergencyStopActive.value = false
        _autoStepMessage.value = "EMERGENCY STOP RESET. MANUAL READY."
        viewModelScope.launch {
            picoWClient.setMode("manual", _settings.value)
        }
    }

    // --- STREAM DOUBLE TAP ---

    fun onStreamDoubleTap() {
        esp32CamClient.startOrRestartStream(_settings.value)
    }

    // --- DIAGNOSTICS: TEST ALL CONNECTIONS ---

    fun testAllConnections() {
        if (_isTestingConnections.value) return
        _isTestingConnections.value = true

        viewModelScope.launch {
            val currentSettings = _settings.value
            val details = mutableMapOf<String, String>()

            // 1. Network
            val netConnected = networkMonitor.checkCurrentConnectivity()
            details["Network"] = if (netConnected) "Connected via ${networkMonitor.networkType.value}" else "Offline"

            // 2. ESP32-CAM (Ping, Stream, Capture)
            val camTest = esp32CamClient.testCamera(currentSettings)
            details["Camera Socket"] = if (camTest.cameraOnline) "OK" else "Unreachable"
            details["Stream Port"] = if (camTest.streamReady) "Ready (:81)" else "Failed"
            details["Capture Endpoint"] = if (camTest.captureReady) "Ready" else "Failed"

            // 3. Pico W (Socket & API)
            val picoTest = picoWClient.testPico(currentSettings)
            details["Pico Socket"] = if (picoTest.picoOnline) "OK" else "Unreachable"
            details["Pico API"] = if (picoTest.apiReady) "Ready (/api/status OK)" else "Failed"

            // 4. AI Engine
            val aiReady = geminiVisionClient.testAiEngine()
            details["Gemini AI"] = if (aiReady) "API Key verified & responsive" else "Key missing or failed"

            _testReport.value = ConnectionTestReport(
                networkConnected = netConnected,
                esp32CamOnline = camTest.cameraOnline,
                streamReady = camTest.streamReady,
                captureReady = camTest.captureReady,
                picoOnline = picoTest.picoOnline,
                picoApiReady = picoTest.apiReady,
                aiEngineReady = aiReady,
                details = details,
                timestampMs = System.currentTimeMillis()
            )

            _isTestingConnections.value = false
        }
    }

    fun dismissTestReport() {
        _testReport.value = null
    }

    // --- SETTINGS PERSISTENCE ---

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        repository.saveSettings(newSettings)
        startTelemetryPolling()
    }
}

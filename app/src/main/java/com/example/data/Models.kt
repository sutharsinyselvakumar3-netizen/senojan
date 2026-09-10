package com.example.data

data class AppSettings(
    // Mobile Hotspot
    val hotspotName: String = "Robot_Hotspot",
    val hotspotPassword: String = "",

    // ESP32-CAM
    val esp32CamIp: String = "192.168.43.50",
    val streamPort: Int = 81,
    val capturePort: Int = 80,

    // Raspberry Pi Pico W
    val picoIp: String = "192.168.43.51",
    val picoPort: Int = 80,
    val timeoutMs: Long = 3000L,
    val reconnectIntervalMs: Long = 2000L,

    // AI Configuration
    val weedConfidenceThreshold: Float = 0.75f,
    val confirmationFrames: Int = 2,
    val onionSafetyDistancePx: Float = 60f,
    val detectionTimeoutMs: Long = 5000L,

    // Arm Calibration
    val servo1Home: Int = 90,
    val servo2Home: Int = 45,
    val servo3Home: Int = 45,
    val servo4Home: Int = 90,
    val servo4Min: Int = 10,
    val servo4Max: Int = 170,
    val cameraCalibrationFactor: Float = 0.85f,
    val armReachMm: Float = 250f,

    // Drill Configuration (Strictly 7000 ms)
    val drillDurationMs: Long = 7000L,
    val drillSafetyDelayMs: Long = 500L,
    val drillCooldownMs: Long = 3000L,

    // Battery Configuration
    val minAutoVoltage: Double = 11.1,
    val criticalVoltage: Double = 10.2,
    val fullVoltage: Double = 12.6,
    val emptyVoltage: Double = 9.6,
    val voltageCalibration: Double = 1.0,

    // MPU6050
    val maxTiltDeg: Double = 20.0,

    // Dev / Demo Mode (separated and explicitly indicated)
    val isDevDemoMode: Boolean = false
) {
    val streamUrl: String
        get() = "http://${esp32CamIp.trim()}:$streamPort/stream"

    val captureUrl: String
        get() = if (capturePort == 80) "http://${esp32CamIp.trim()}/capture"
                else "http://${esp32CamIp.trim()}:$capturePort/capture"

    val picoBaseUrl: String
        get() = if (picoPort == 80) "http://${picoIp.trim()}"
                else "http://${picoIp.trim()}:$picoPort"
}

enum class RobotMode {
    MANUAL,
    AUTO
}

enum class ConnectionStatus {
    ONLINE,
    CONNECTING,
    OFFLINE,
    ERROR
}

data class HardwareTelemetry(
    val online: Boolean = false,
    val batteryVoltage: Double? = null,
    val soilMoisture: Int? = null,
    val pitchDeg: Double? = null,
    val rollDeg: Double? = null,
    val mpuStable: Boolean? = null,
    val eStopActive: Boolean = false,
    val motorsActive: Boolean = false,
    val currentDirection: String = "IDLE",
    val speedLevel: String = "MEDIUM",
    val servo1Angle: Int = 90,
    val servo2Angle: Int = 45,
    val servo3Angle: Int = 45,
    val servo4Angle: Int = 90,
    val relay1Drill: Boolean = false,
    val relay2Soil: Boolean = false,
    val relay3Water: Boolean = false,
    val drillCountdownSec: Int = 0,
    val activeMode: String = "MANUAL",
    val lastUpdateMs: Long = 0L,
    val lastError: String? = null
)

data class CameraTelemetry(
    val online: Boolean = false,
    val streamReady: Boolean = false,
    val captureReady: Boolean = false,
    val streamConnecting: Boolean = false,
    val lastError: String? = null,
    val lastSuccessMs: Long = 0L
)

data class WeedBoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    val centerX: Float = x + width / 2f
    val centerY: Float = y + height / 2f
}

data class AiDetectionResult(
    val label: String, // "WEED", "ONION", "UNKNOWN"
    val confidence: Float,
    val box: WeedBoundingBox?,
    val groundX: Float = 0f,
    val groundY: Float = 0f,
    val servo1Target: Int = 90,
    val servo2Target: Int = 45,
    val servo3Target: Int = 45,
    val onionSafe: Boolean = true,
    val message: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

data class ConnectionTestReport(
    val networkConnected: Boolean = false,
    val esp32CamOnline: Boolean = false,
    val streamReady: Boolean = false,
    val captureReady: Boolean = false,
    val picoOnline: Boolean = false,
    val picoApiReady: Boolean = false,
    val aiEngineReady: Boolean = false,
    val details: Map<String, String> = emptyMap(),
    val timestampMs: Long = System.currentTimeMillis()
)

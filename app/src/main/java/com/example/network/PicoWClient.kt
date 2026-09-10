package com.example.network

import com.example.data.AppSettings
import com.example.data.HardwareTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class PicoWClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _telemetry = MutableStateFlow(HardwareTelemetry())
    val telemetry: StateFlow<HardwareTelemetry> = _telemetry.asStateFlow()

    private fun createHttpClient(timeoutMs: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    data class PicoTestResult(
        val picoOnline: Boolean,
        val apiReady: Boolean,
        val details: String,
        val batteryVoltage: Double? = null,
        val emergencyStopActive: Boolean = false
    )

    /**
     * TEST PICO:
     * 1. Socket connection to IP:Port
     * 2. GET /api/status
     * 3. Validate JSON response
     */
    suspend fun testPico(settings: AppSettings): PicoTestResult = withContext(Dispatchers.IO) {
        val ip = settings.picoIp.trim()
        val port = settings.picoPort
        var picoOnline = false
        var apiReady = false
        var details = ""
        var batteryV: Double? = null
        var eStop = false

        // 1. Socket check
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), settings.timeoutMs.toInt().coerceAtMost(2500))
                picoOnline = true
                details = "Socket connected to $ip:$port. "
            }
        } catch (e: Exception) {
            details = "Cannot connect to $ip:$port (${e.message}). "
        }

        // 2. HTTP GET /api/status
        if (picoOnline) {
            val client = createHttpClient(settings.timeoutMs)
            val url = "${settings.picoBaseUrl}/api/status"
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotBlank()) {
                            try {
                                val json = JSONObject(bodyStr)
                                apiReady = true
                                batteryV = json.optDouble("battery_voltage", Double.NaN).takeIf { !it.isNaN() }
                                eStop = json.optBoolean("emergency_stop", false)
                                details += "API /api/status returned 200 OK."
                                parseTelemetryJson(json, true)
                            } catch (e: Exception) {
                                details += "API responded with non-JSON format: ${e.message}"
                            }
                        } else {
                            details += "API responded with empty body."
                        }
                    } else {
                        details += "API HTTP error: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                details += "API request failed: ${e.message}"
            }
        }

        if (!picoOnline || !apiReady) {
            _telemetry.value = _telemetry.value.copy(
                online = false,
                lastError = details
            )
        }

        PicoTestResult(
            picoOnline = picoOnline,
            apiReady = apiReady,
            details = details,
            batteryVoltage = batteryV,
            emergencyStopActive = eStop
        )
    }

    /**
     * Real Poll: GET /api/status
     */
    suspend fun fetchStatus(settings: AppSettings): HardwareTelemetry = withContext(Dispatchers.IO) {
        val client = createHttpClient(settings.timeoutMs)
        val url = "${settings.picoBaseUrl}/api/status"
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "{}"
                    val json = JSONObject(bodyStr)
                    val updated = parseTelemetryJson(json, true)
                    _telemetry.value = updated
                    return@withContext updated
                } else {
                    val err = "HTTP ${response.code} from Pico"
                    val offline = _telemetry.value.copy(online = false, lastError = err)
                    _telemetry.value = offline
                    return@withContext offline
                }
            }
        } catch (e: Exception) {
            val offline = _telemetry.value.copy(
                online = false,
                lastError = e.message ?: "Pico unreachable"
            )
            _telemetry.value = offline
            return@withContext offline
        }
    }

    /**
     * POST /api/mode
     * e.g. {"mode": "manual" | "auto"}
     */
    suspend fun setMode(mode: String, settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("mode", mode.lowercase())
        }
        postJson("/api/mode", payload.toString(), settings)
    }

    /**
     * POST /api/robot
     * e.g. {"direction": "forward" | "reverse" | "left" | "right" | "stop", "speed": "slow" | "medium" | "fast"}
     */
    suspend fun sendRobotDirection(
        direction: String,
        speed: String,
        settings: AppSettings
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("direction", direction.lowercase())
            put("speed", speed.lowercase())
        }
        postJson("/api/robot", payload.toString(), settings)
    }

    /**
     * POST /api/servo4
     * e.g. {"angle": 90}
     */
    suspend fun setServo4Angle(angle: Int, settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("angle", angle)
        }
        postJson("/api/servo4", payload.toString(), settings)
    }

    /**
     * POST /api/relay
     * e.g. {"relay": 1 | 2 | 3, "state": true | false}
     */
    suspend fun setRelay(relayNumber: Int, state: Boolean, settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("relay", relayNumber)
            put("state", state)
        }
        postJson("/api/relay", payload.toString(), settings)
    }

    /**
     * POST /api/emergency_stop
     */
    suspend fun triggerEmergencyStop(settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("emergency_stop", true)
            put("timestamp", System.currentTimeMillis())
        }
        postJson("/api/emergency_stop", payload.toString(), settings)
    }

    /**
     * POST /api/detection
     * Sends detected weed coordinates to Pico W for arm aiming and safety interlock.
     */
    suspend fun sendDetectionData(
        target: String,
        centerX: Float,
        centerY: Float,
        confidence: Float,
        groundX: Float,
        groundY: Float,
        settings: AppSettings
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("target", target.lowercase())
            put("center_x", centerX)
            put("center_y", centerY)
            put("confidence", confidence)
            put("ground_x", groundX)
            put("ground_y", groundY)
        }
        postJson("/api/detection", payload.toString(), settings)
    }

    private fun postJson(endpoint: String, jsonBody: String, settings: AppSettings): Boolean {
        val client = createHttpClient(settings.timeoutMs)
        val url = "${settings.picoBaseUrl}$endpoint"
        return try {
            val body = jsonBody.toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            _telemetry.value = _telemetry.value.copy(
                online = false,
                lastError = "Failed to POST $endpoint: ${e.message}"
            )
            false
        }
    }

    private fun parseTelemetryJson(json: JSONObject, isOnline: Boolean): HardwareTelemetry {
        val v = json.optDouble("battery_voltage", Double.NaN).takeIf { !it.isNaN() }
        val soil = if (json.has("soil_moisture")) json.optInt("soil_moisture") else null
        val pitch = if (json.has("mpu_pitch")) json.optDouble("mpu_pitch") else null
        val roll = if (json.has("mpu_roll")) json.optDouble("mpu_roll") else null
        val mpuStable = if (json.has("mpu_stable")) json.optBoolean("mpu_stable") else null
        val eStop = json.optBoolean("emergency_stop", false)
        val motors = json.optBoolean("motors_active", false)
        val dir = json.optString("direction", "IDLE")
        val speed = json.optString("speed", "MEDIUM")
        val s1 = json.optInt("servo1", 90)
        val s2 = json.optInt("servo2", 45)
        val s3 = json.optInt("servo3", 45)
        val s4 = json.optInt("servo4", 90)
        val r1 = json.optBoolean("relay1", false)
        val r2 = json.optBoolean("relay2", false)
        val r3 = json.optBoolean("relay3", false)
        val countdown = json.optInt("drill_countdown", 0)
        val modeStr = json.optString("mode", "MANUAL")

        return HardwareTelemetry(
            online = isOnline,
            batteryVoltage = v,
            soilMoisture = soil,
            pitchDeg = pitch,
            rollDeg = roll,
            mpuStable = mpuStable,
            eStopActive = eStop,
            motorsActive = motors,
            currentDirection = dir,
            speedLevel = speed,
            servo1Angle = s1,
            servo2Angle = s2,
            servo3Angle = s3,
            servo4Angle = s4,
            relay1Drill = r1,
            relay2Soil = r2,
            relay3Water = r3,
            drillCountdownSec = countdown,
            activeMode = modeStr,
            lastUpdateMs = System.currentTimeMillis(),
            lastError = null
        )
    }
}

package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_companion_robot_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        return AppSettings(
            hotspotName = prefs.getString("hotspot_name", "Robot_Hotspot") ?: "Robot_Hotspot",
            hotspotPassword = prefs.getString("hotspot_password", "") ?: "",
            esp32CamIp = prefs.getString("esp32_cam_ip", "192.168.43.50") ?: "192.168.43.50",
            streamPort = prefs.getInt("stream_port", 81),
            capturePort = prefs.getInt("capture_port", 80),
            picoIp = prefs.getString("pico_ip", "192.168.43.51") ?: "192.168.43.51",
            picoPort = prefs.getInt("pico_port", 80),
            timeoutMs = prefs.getLong("timeout_ms", 3000L),
            reconnectIntervalMs = prefs.getLong("reconnect_ms", 2000L),
            weedConfidenceThreshold = prefs.getFloat("weed_confidence", 0.75f),
            confirmationFrames = prefs.getInt("confirmation_frames", 2),
            onionSafetyDistancePx = prefs.getFloat("onion_safety_dist", 60f),
            detectionTimeoutMs = prefs.getLong("detection_timeout_ms", 5000L),
            servo1Home = prefs.getInt("servo1_home", 90),
            servo2Home = prefs.getInt("servo2_home", 45),
            servo3Home = prefs.getInt("servo3_home", 45),
            servo4Home = prefs.getInt("servo4_home", 90),
            servo4Min = prefs.getInt("servo4_min", 10),
            servo4Max = prefs.getInt("servo4_max", 170),
            cameraCalibrationFactor = prefs.getFloat("cam_calib", 0.85f),
            armReachMm = prefs.getFloat("arm_reach", 250f),
            drillDurationMs = prefs.getLong("drill_duration_ms", 7000L),
            drillSafetyDelayMs = prefs.getLong("drill_safety_delay_ms", 500L),
            drillCooldownMs = prefs.getLong("drill_cooldown_ms", 3000L),
            minAutoVoltage = prefs.getFloat("min_auto_v", 11.1f).toDouble(),
            criticalVoltage = prefs.getFloat("crit_v", 10.2f).toDouble(),
            fullVoltage = prefs.getFloat("full_v", 12.6f).toDouble(),
            emptyVoltage = prefs.getFloat("empty_v", 9.6f).toDouble(),
            voltageCalibration = prefs.getFloat("v_calib", 1.0f).toDouble(),
            maxTiltDeg = prefs.getFloat("max_tilt", 20.0f).toDouble(),
            isDevDemoMode = prefs.getBoolean("dev_demo_mode", false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("hotspot_name", settings.hotspotName)
            .putString("hotspot_password", settings.hotspotPassword)
            .putString("esp32_cam_ip", settings.esp32CamIp)
            .putInt("stream_port", settings.streamPort)
            .putInt("capture_port", settings.capturePort)
            .putString("pico_ip", settings.picoIp)
            .putInt("pico_port", settings.picoPort)
            .putLong("timeout_ms", settings.timeoutMs)
            .putLong("reconnect_ms", settings.reconnectIntervalMs)
            .putFloat("weed_confidence", settings.weedConfidenceThreshold)
            .putInt("confirmation_frames", settings.confirmationFrames)
            .putFloat("onion_safety_dist", settings.onionSafetyDistancePx)
            .putLong("detection_timeout_ms", settings.detectionTimeoutMs)
            .putInt("servo1_home", settings.servo1Home)
            .putInt("servo2_home", settings.servo2Home)
            .putInt("servo3_home", settings.servo3Home)
            .putInt("servo4_home", settings.servo4Home)
            .putInt("servo4_min", settings.servo4Min)
            .putInt("servo4_max", settings.servo4Max)
            .putFloat("cam_calib", settings.cameraCalibrationFactor)
            .putFloat("arm_reach", settings.armReachMm)
            .putLong("drill_duration_ms", settings.drillDurationMs)
            .putLong("drill_safety_delay_ms", settings.drillSafetyDelayMs)
            .putLong("drill_cooldown_ms", settings.drillCooldownMs)
            .putFloat("min_auto_v", settings.minAutoVoltage.toFloat())
            .putFloat("crit_v", settings.criticalVoltage.toFloat())
            .putFloat("full_v", settings.fullVoltage.toFloat())
            .putFloat("empty_v", settings.emptyVoltage.toFloat())
            .putFloat("v_calib", settings.voltageCalibration.toFloat())
            .putFloat("max_tilt", settings.maxTiltDeg.toFloat())
            .putBoolean("dev_demo_mode", settings.isDevDemoMode)
            .apply()
    }
}

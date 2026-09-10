package com.example.network

import android.util.Base64
import com.example.BuildConfig
import com.example.data.AiDetectionResult
import com.example.data.AppSettings
import com.example.data.WeedBoundingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2

class GeminiVisionClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeFrame(
        jpegBytes: ByteArray,
        settings: AppSettings
    ): Result<AiDetectionResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured in Secrets panel")
            )
        }

        try {
            val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val modelName = "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val promptText = """
                You are the precision vision AI for the AI Companion Smart Onion Garden Robot.
                Tagline: "See the Weed. Protect the Onion. Work Smart."
                
                Examine this camera image taken above the onion garden bed.
                Detect all plant objects. Classify targets strictly as:
                - "WEED" (wild unwanted weed that needs removal)
                - "ONION" (cultivated onion sprout or plant to protect)
                - "UNKNOWN" (soil, debris, rock, or ambiguous plant)
                
                Output strictly in JSON matching this schema:
                {
                  "primary_detection": "WEED" | "ONION" | "UNKNOWN",
                  "confidence": 0.0 to 1.0,
                  "box_x": number (pixel coordinate x, or 0-320),
                  "box_y": number (pixel coordinate y, or 0-240),
                  "box_width": number,
                  "box_height": number,
                  "onion_nearby": boolean,
                  "onion_distance_px": number,
                  "safety_verdict": "SAFE_TO_DRILL" | "ABORT_ONION_RISK" | "NO_WEED",
                  "notes": "brief summary"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return@withContext Result.failure(
                        Exception("Gemini API error ${response.code}: $errBody")
                    )
                }

                val responseBody = response.body?.string() ?: ""
                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No AI candidates returned"))
                }

                val textContent = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val parsedAi = JSONObject(textContent)
                val label = parsedAi.optString("primary_detection", "UNKNOWN").uppercase()
                val confidence = parsedAi.optDouble("confidence", 0.0).toFloat()
                val x = parsedAi.optDouble("box_x", 0.0).toFloat()
                val y = parsedAi.optDouble("box_y", 0.0).toFloat()
                val w = parsedAi.optDouble("box_width", 0.0).toFloat()
                val h = parsedAi.optDouble("box_height", 0.0).toFloat()
                val onionNearby = parsedAi.optBoolean("onion_nearby", false)
                val onionDist = parsedAi.optDouble("onion_distance_px", 1000.0).toFloat()
                val safetyVerdict = parsedAi.optString("safety_verdict", "NO_WEED")
                val notes = parsedAi.optString("notes", "")

                val box = if (w > 0 && h > 0) WeedBoundingBox(x, y, w, h) else null

                // Calculations
                val centerX = box?.centerX ?: 160f
                val centerY = box?.centerY ?: 120f

                // Arm kinematics target calculation
                // Camera X/Y -> Ground X/Y (mm) relative to robot center
                val imgCenterX = 160f
                val groundX = (centerX - imgCenterX) * settings.cameraCalibrationFactor
                val groundY = centerY * settings.cameraCalibrationFactor + 100f // forward offset

                // Calculate servo angles
                // Servo 1: Base azimuth angle (90 is forward)
                val azimuthRad = atan2(groundX.toDouble(), groundY.toDouble())
                val azimuthDeg = Math.toDegrees(azimuthRad).toInt()
                val servo1 = (90 + azimuthDeg).coerceIn(40, 140)

                // Servo 2 & 3: Reach and height
                val distance = Math.hypot(groundX.toDouble(), groundY.toDouble())
                val reachFactor = (distance / settings.armReachMm).coerceIn(0.2, 1.0)
                val servo2 = (30 + (reachFactor * 50)).toInt().coerceIn(30, 90)
                val servo3 = (40 + (reachFactor * 40)).toInt().coerceIn(30, 90)

                // Safety rule check:
                // Onion safety verification before drilling!
                val onionSafe = !onionNearby &&
                        (onionDist >= settings.onionSafetyDistancePx) &&
                        (safetyVerdict != "ABORT_ONION_RISK") &&
                        (label != "ONION")

                val result = AiDetectionResult(
                    label = label,
                    confidence = confidence,
                    box = box,
                    groundX = groundX,
                    groundY = groundY,
                    servo1Target = servo1,
                    servo2Target = servo2,
                    servo3Target = servo3,
                    onionSafe = onionSafe,
                    message = if (!onionSafe) "ONION IN RANGE - DRILL ABORTED FOR SAFETY" else notes,
                    timestampMs = System.currentTimeMillis()
                )

                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testAiEngine(): Boolean = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext false
        }
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val testPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "PING") })
                        })
                    })
                })
            }
            val body = testPayload.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url(url).post(body).build()
            httpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}

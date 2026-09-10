package com.example.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.AppSettings
import com.example.data.CameraTelemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class Esp32CamClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _cameraStatus = MutableStateFlow(CameraTelemetry())
    val cameraStatus: StateFlow<CameraTelemetry> = _cameraStatus.asStateFlow()

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private var streamJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    data class CameraTestResult(
        val cameraOnline: Boolean,
        val streamReady: Boolean,
        val captureReady: Boolean,
        val details: String
    )

    /**
     * Performs a real connection test against ESP32-CAM:
     * 1. Socket ping/HTTP check
     * 2. Stream endpoint check
     * 3. Capture endpoint check
     */
    suspend fun testCamera(settings: AppSettings): CameraTestResult = withContext(Dispatchers.IO) {
        val ip = settings.esp32CamIp.trim()
        val streamPort = settings.streamPort
        val capturePort = settings.capturePort

        var cameraOnline = false
        var streamReady = false
        var captureReady = false
        val log = StringBuilder()

        // 1. Socket reachability check to device
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, capturePort), 2500)
                cameraOnline = true
                log.append("Device reachable on port $capturePort. ")
            }
        } catch (e: Exception) {
            // Try stream port if capture port failed
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, streamPort), 2500)
                    cameraOnline = true
                    log.append("Device reachable on port $streamPort. ")
                }
            } catch (e2: Exception) {
                log.append("Cannot reach $ip: ${e2.message}. ")
            }
        }

        // 2. Test Stream Endpoint
        if (cameraOnline) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, streamPort), 2500)
                    streamReady = true
                    log.append("Stream socket OK. ")
                }
            } catch (e: Exception) {
                log.append("Stream port $streamPort unreachable: ${e.message}. ")
            }
        }

        // 3. Test Capture Endpoint
        if (cameraOnline) {
            try {
                val req = Request.Builder()
                    .url(settings.captureUrl)
                    .build()
                httpClient.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        captureReady = true
                        log.append("Capture HTTP 200 OK. ")
                    } else {
                        log.append("Capture HTTP error: ${response.code}. ")
                    }
                }
            } catch (e: Exception) {
                log.append("Capture failed: ${e.message}. ")
            }
        }

        _cameraStatus.value = _cameraStatus.value.copy(
            online = cameraOnline,
            streamReady = streamReady,
            captureReady = captureReady,
            lastError = if (cameraOnline) null else "Cannot connect to $ip",
            lastSuccessMs = if (cameraOnline) System.currentTimeMillis() else _cameraStatus.value.lastSuccessMs
        )

        CameraTestResult(
            cameraOnline = cameraOnline,
            streamReady = streamReady,
            captureReady = captureReady,
            details = log.toString().trim()
        )
    }

    /**
     * Captures a single JPEG from ESP32-CAM /capture endpoint.
     */
    suspend fun captureFrame(settings: AppSettings): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(settings.captureUrl)
                .build()
            httpClient.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            _latestFrame.value = bitmap
                            _cameraStatus.value = _cameraStatus.value.copy(
                                online = true,
                                captureReady = true,
                                lastSuccessMs = System.currentTimeMillis()
                            )
                            return@withContext bytes
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            _cameraStatus.value = _cameraStatus.value.copy(
                captureReady = false,
                lastError = "Capture failed: ${e.message}"
            )
            null
        }
    }

    /**
     * Starts or restarts the real live stream via double-tap.
     */
    fun startOrRestartStream(settings: AppSettings) {
        stopStream()
        _cameraStatus.value = _cameraStatus.value.copy(
            streamConnecting = true,
            lastError = null
        )
        // Clear previous frame to adhere strictly to: "Do not silently show an old/fake frame."
        _latestFrame.value = null

        streamJob = scope.launch {
            try {
                // First test reachability of stream port
                val reachable = withContext(Dispatchers.IO) {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(settings.esp32CamIp.trim(), settings.streamPort), 2500)
                            true
                        }
                    } catch (_: Exception) {
                        false
                    }
                }

                if (!reachable) {
                    _cameraStatus.value = _cameraStatus.value.copy(
                        online = false,
                        streamReady = false,
                        streamConnecting = false,
                        lastError = "STREAM IP FAILED: Cannot connect to ${settings.esp32CamIp}:${settings.streamPort}"
                    )
                    return@launch
                }

                // Connect to real MJPEG stream
                val request = Request.Builder()
                    .url(settings.streamUrl)
                    .build()

                val call = httpClient.newCall(request)
                val response: Response = call.execute()

                if (!response.isSuccessful) {
                    _cameraStatus.value = _cameraStatus.value.copy(
                        online = false,
                        streamReady = false,
                        streamConnecting = false,
                        lastError = "STREAM IP FAILED: HTTP ${response.code}"
                    )
                    response.close()
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _cameraStatus.value = _cameraStatus.value.copy(
                        online = false,
                        streamReady = false,
                        streamConnecting = false,
                        lastError = "STREAM IP FAILED: Empty response body"
                    )
                    return@launch
                }

                _cameraStatus.value = _cameraStatus.value.copy(
                    online = true,
                    streamReady = true,
                    streamConnecting = false,
                    lastError = null,
                    lastSuccessMs = System.currentTimeMillis()
                )

                // Read multipart MJPEG stream frames
                readMjpegStream(body.byteStream())

            } catch (e: CancellationException) {
                // Stopped intentionally
            } catch (e: Exception) {
                _cameraStatus.value = _cameraStatus.value.copy(
                    online = false,
                    streamReady = false,
                    streamConnecting = false,
                    lastError = "STREAM IP FAILED: ${e.message ?: "Connection reset"}"
                )
                _latestFrame.value = null
            }
        }
    }

    fun stopStream() {
        streamJob?.cancel()
        streamJob = null
        _cameraStatus.value = _cameraStatus.value.copy(
            streamConnecting = false,
            streamReady = false
        )
    }

    /**
     * Parses MJPEG stream byte-by-byte looking for JPEG SOI (0xFF, 0xD8) and EOI (0xFF, 0xD9) markers.
     */
    private suspend fun readMjpegStream(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val buffer = ByteArrayOutputStream()
        var prevByte = -1

        try {
            while (isActive) {
                val curByte = inputStream.read()
                if (curByte == -1) break

                if (prevByte == 0xFF && curByte == 0xD8) {
                    // Start of JPEG
                    buffer.reset()
                    buffer.write(0xFF)
                    buffer.write(0xD8)
                } else if (prevByte == 0xFF && curByte == 0xD9) {
                    // End of JPEG
                    buffer.write(0xD9)
                    val frameBytes = buffer.toByteArray()
                    if (frameBytes.size > 100) {
                        val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                        if (bitmap != null) {
                            _latestFrame.value = bitmap
                            _cameraStatus.value = _cameraStatus.value.copy(
                                online = true,
                                streamReady = true,
                                lastSuccessMs = System.currentTimeMillis()
                            )
                        }
                    }
                    buffer.reset()
                } else {
                    buffer.write(curByte)
                }

                prevByte = curByte
            }
        } finally {
            try {
                inputStream.close()
            } catch (_: Exception) {}
        }
    }
}

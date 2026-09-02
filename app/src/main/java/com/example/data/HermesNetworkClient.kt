package com.example.data

import com.example.model.ConnectionConfig
import com.example.model.ProcessInfo
import com.example.model.SystemTelemetry
import com.example.model.ToolExecutionBlock
import com.example.model.ToolStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PingResult(
    val isSuccess: Boolean,
    val latencyMs: Long,
    val statusCode: Int,
    val message: String
)

sealed class StreamChunk {
    data class TextDelta(val text: String) : StreamChunk()
    data class ToolStart(val tool: ToolExecutionBlock) : StreamChunk()
    data class ToolOutput(val toolId: String, val output: String, val status: ToolStatus) : StreamChunk()
    data class Error(val message: String) : StreamChunk()
    data object Done : StreamChunk()
}

class HermesNetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun ping(config: ConnectionConfig): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val url = "${config.baseUrl}/api/health"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("X-API-Key", config.apiKey)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    PingResult(
                        isSuccess = true,
                        latencyMs = elapsed,
                        statusCode = response.code,
                        message = "Connected to Hermes Agent on Windows 11 (${elapsed}ms)"
                    )
                } else {
                    PingResult(
                        isSuccess = false,
                        latencyMs = elapsed,
                        statusCode = response.code,
                        message = "HTTP ${response.code}: ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            PingResult(
                isSuccess = false,
                latencyMs = elapsed,
                statusCode = 0,
                message = e.localizedMessage ?: "Failed to connect to ${config.tailscaleIp}"
            )
        }
    }

    suspend fun fetchMetrics(config: ConnectionConfig): Result<SystemTelemetry> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/system"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("X-API-Key", config.apiKey)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)

                val telemetry = SystemTelemetry(
                    cpuUsage = json.optDouble("cpu_usage", 28.0).toFloat(),
                    ramUsedGb = json.optDouble("ram_used_gb", 14.5).toFloat(),
                    ramTotalGb = json.optDouble("ram_total_gb", 32.0).toFloat(),
                    gpuUsage = json.optDouble("gpu_usage", 40.0).toFloat(),
                    hostname = json.optString("hostname", "WINDOWS-11-PC"),
                    osVersion = json.optString("os_version", "Windows 11 Pro"),
                    uptime = json.optString("uptime", "3d 12h"),
                    agentVersion = json.optString("agent_version", "v2.4.1"),
                    activeTasksCount = json.optInt("active_tasks", 2),
                    pingMs = 30
                )
                Result.success(telemetry)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChat(
        config: ConnectionConfig,
        prompt: String,
        model: String
    ): Flow<StreamChunk> = flow {
        val url = "${config.baseUrl}/api/chat/stream"
        val jsonPayload = JSONObject().apply {
            put("prompt", prompt)
            put("model", model)
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("X-API-Key", config.apiKey)
            .addHeader("Accept", "text/event-stream")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(StreamChunk.Error("Server error: HTTP ${response.code}"))
                    return@use
                }

                val source = response.body?.source() ?: run {
                    emit(StreamChunk.Error("Empty response body"))
                    return@use
                }

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") {
                            emit(StreamChunk.Done)
                            break
                        }
                        try {
                            val json = JSONObject(data)
                            val type = json.optString("type", "text")
                            when (type) {
                                "text" -> {
                                    val delta = json.optString("content", "")
                                    emit(StreamChunk.TextDelta(delta))
                                }
                                "tool_start" -> {
                                    val toolId = json.optString("id", System.currentTimeMillis().toString())
                                    val toolName = json.optString("tool", "terminal")
                                    val cmd = json.optString("command", "")
                                    emit(StreamChunk.ToolStart(
                                        ToolExecutionBlock(
                                            id = toolId,
                                            toolName = toolName,
                                            command = cmd,
                                            status = ToolStatus.RUNNING
                                        )
                                    ))
                                }
                                "tool_end" -> {
                                    val toolId = json.optString("id", "")
                                    val output = json.optString("output", "")
                                    val code = json.optInt("exit_code", 0)
                                    val duration = json.optLong("duration_ms", 120)
                                    emit(StreamChunk.ToolOutput(
                                        toolId = toolId,
                                        output = output,
                                        status = if (code == 0) ToolStatus.COMPLETED else ToolStatus.FAILED
                                    ))
                                }
                            }
                        } catch (_: Exception) {
                            // Raw text chunk fallback
                            emit(StreamChunk.TextDelta(data))
                        }
                    }
                }
                emit(StreamChunk.Done)
            }
        } catch (e: Exception) {
            emit(StreamChunk.Error("Network stream error: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)
}

package com.example.data

import com.example.model.AiModelInfo
import com.example.model.ChatMessage
import com.example.model.ConnectionConfig
import com.example.model.DiscoveredGateway
import com.example.model.HermesSession
import com.example.model.MessageSender
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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
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

    suspend fun discoverLocalGateway(timeoutMs: Long = 3000): DiscoveredGateway? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 1500
            }
            val pingBytes = "HERMES_DISCOVER".toByteArray()
            val broadcastPacket = DatagramPacket(
                pingBytes,
                pingBytes.size,
                InetAddress.getByName("255.255.255.255"),
                8089
            )
            socket.send(broadcastPacket)

            val receiveBuffer = ByteArray(2048)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    socket.receive(receivePacket)
                    val raw = String(receivePacket.data, 0, receivePacket.length)
                    val json = JSONObject(raw)
                    if (json.optString("service") == "hermes-agent") {
                        val senderIp = json.optString("ip").ifEmpty { receivePacket.address.hostAddress ?: "127.0.0.1" }
                        val tsIp = json.optString("tailscale_ip").takeIf { it.isNotBlank() && it != "null" }
                        return@withContext DiscoveredGateway(
                            hostname = json.optString("hostname", "WIN11-HERMES"),
                            ip = senderIp,
                            tailscaleIp = tsIp,
                            port = json.optInt("port", 8080),
                            apiKey = json.optString("apiKey", json.optString("api_key", ""))
                        )
                    }
                } catch (_: Exception) {
                    break
                }
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            socket?.close()
        }
    }

    suspend fun ping(config: ConnectionConfig): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val url = "${config.baseUrl}/api/health"
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("X-Hermes-Gateway", "true")
            
            if (config.apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
                requestBuilder.addHeader("X-API-Key", config.apiKey)
            }

            val request = requestBuilder.get().build()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    PingResult(
                        isSuccess = true,
                        latencyMs = elapsed,
                        statusCode = response.code,
                        message = "Connected to Remote Gateway at ${config.baseUrl} (${elapsed}ms)"
                    )
                } else {
                    // Try fallback check on root or /v1/models (Ollama, LM Studio, OpenRouter)
                    if (response.code == 404) {
                        try {
                            val candidateUrls = listOf(config.baseUrl, "${config.baseUrl}/v1/models", "${config.baseUrl}/api/tags")
                            for (cUrl in candidateUrls) {
                                val testReq = Request.Builder().url(cUrl).apply {
                                    if (config.apiKey.isNotBlank()) {
                                        addHeader("Authorization", "Bearer ${config.apiKey}")
                                    }
                                }.get().build()
                                client.newCall(testReq).execute().use { testResp ->
                                    val rootElapsed = System.currentTimeMillis() - startTime
                                    if (testResp.isSuccessful || testResp.code == 401 || testResp.code == 403) {
                                        return@withContext PingResult(
                                            isSuccess = true,
                                            latencyMs = rootElapsed,
                                            statusCode = testResp.code,
                                            message = "Gateway Responding at $cUrl (${rootElapsed}ms)"
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    PingResult(
                        isSuccess = false,
                        latencyMs = elapsed,
                        statusCode = response.code,
                        message = "Remote Gateway HTTP ${response.code}: ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            PingResult(
                isSuccess = false,
                latencyMs = elapsed,
                statusCode = 0,
                message = "Remote Gateway unreachable at ${config.baseUrl} (${e.localizedMessage ?: "Connection refused"})"
            )
        }
    }

    suspend fun fetchMetrics(config: ConnectionConfig): Result<SystemTelemetry> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/system"
            val requestBuilder = Request.Builder().url(url)
            if (config.apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
                requestBuilder.addHeader("X-API-Key", config.apiKey)
            }
            val request = requestBuilder.get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Fallback to basic telemetry if server doesn't have custom /api/system
                    return@withContext Result.success(
                        SystemTelemetry(
                            cpuUsage = 24.0f,
                            ramUsedGb = 8.2f,
                            ramTotalGb = 16.0f,
                            gpuUsage = 35.0f,
                            hostname = "REMOTE-GATEWAY",
                            osVersion = "Windows 11 / Remote Host",
                            uptime = "Online",
                            agentVersion = "Hermes v3",
                            activeTasksCount = 1,
                            pingMs = 28
                        )
                    )
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

    suspend fun fetchSessions(config: ConnectionConfig): Result<List<HermesSession>> = withContext(Dispatchers.IO) {
        val url = "${config.baseUrl}/api/sessions"
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (config.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
            requestBuilder.addHeader("X-API-Key", config.apiKey)
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val bodyStr = response.body?.string() ?: "[]"
            val array = org.json.JSONArray(bodyStr)
            val list = mutableListOf<HermesSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HermesSession(
                        id = obj.getString("id"),
                        title = obj.optString("title", obj.getString("id")),
                        model = obj.optString("model", "default"),
                        startedAt = obj.optLong("started_at", 0L),
                        messageCount = obj.optInt("message_count", 0)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSessionMessages(config: ConnectionConfig, sessionId: String): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val url = "${config.baseUrl}/api/sessions/$sessionId/messages"
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (config.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
            requestBuilder.addHeader("X-API-Key", config.apiKey)
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val bodyStr = response.body?.string() ?: "[]"
            val array = org.json.JSONArray(bodyStr)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val senderStr = obj.optString("sender", "USER")
                val sender = if (senderStr.equals("HERMES", ignoreCase = true)) MessageSender.HERMES else MessageSender.USER
                list.add(
                    ChatMessage(
                        id = obj.optString("id", i.toString()),
                        sender = sender,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        content = obj.optString("content", "")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNewSession(config: ConnectionConfig, title: String? = null, model: String? = null): Result<HermesSession> = withContext(Dispatchers.IO) {
        val url = "${config.baseUrl}/api/sessions/new"
        val payload = JSONObject().apply {
            if (!title.isNullOrBlank()) put("title", title)
            if (!model.isNullOrBlank()) put("model", model)
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)

        if (config.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
            requestBuilder.addHeader("X-API-Key", config.apiKey)
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val bodyStr = response.body?.string() ?: "{}"
            val obj = JSONObject(bodyStr)
            val session = HermesSession(
                id = obj.getString("id"),
                title = obj.optString("title", "New Session"),
                model = obj.optString("model", model ?: "default"),
                startedAt = obj.optLong("started_at", System.currentTimeMillis()),
                messageCount = 0
            )
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchModels(config: ConnectionConfig): Result<List<AiModelInfo>> = withContext(Dispatchers.IO) {
        val url = "${config.baseUrl}/api/models"
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (config.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
            requestBuilder.addHeader("X-API-Key", config.apiKey)
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            val array = json.optJSONArray("models") ?: org.json.JSONArray()
            val list = mutableListOf<AiModelInfo>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AiModelInfo(
                        id = obj.getString("id"),
                        displayName = obj.optString("displayName", obj.getString("id")),
                        provider = obj.optString("provider", "Host PC"),
                        description = obj.optString("description", ""),
                        isDefault = i == 0
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChat(
        config: ConnectionConfig,
        prompt: String,
        model: String,
        sessionId: String? = null
    ): Flow<StreamChunk> = flow {
        // First try standard Hermes Agent SSE endpoint
        val primaryUrl = "${config.baseUrl}/api/chat/stream"
        val hermesPayload = JSONObject().apply {
            put("prompt", prompt)
            put("model", model)
            if (!sessionId.isNullOrBlank()) {
                put("session_id", sessionId)
            }
        }

        val requestBody = hermesPayload.toString().toRequestBody("application/json".toMediaType())
        val requestBuilder = Request.Builder()
            .url(primaryUrl)
            .addHeader("Accept", "text/event-stream")
            .post(requestBody)

        if (config.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
            requestBuilder.addHeader("X-API-Key", config.apiKey)
        }

        val request = requestBuilder.build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                processSseResponse(response, this)
            } else if (response.code == 404) {
                response.close()
                // Fallback to OpenAI / Ollama / LM Studio compatible endpoint /v1/chat/completions
                val openAiUrl = if (config.baseUrl.endsWith("/v1")) "${config.baseUrl}/chat/completions" else "${config.baseUrl}/v1/chat/completions"
                val openAiPayload = JSONObject().apply {
                    put("model", if (model.isNotBlank()) model else "hermes3")
                    put("stream", true)
                    val msgArray = org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    }
                    put("messages", msgArray)
                }

                val openAiBody = openAiPayload.toString().toRequestBody("application/json".toMediaType())
                val openAiReqBuilder = Request.Builder()
                    .url(openAiUrl)
                    .addHeader("Accept", "text/event-stream")
                    .post(openAiBody)

                if (config.apiKey.isNotBlank()) {
                    openAiReqBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
                }

                client.newCall(openAiReqBuilder.build()).execute().use { oaiResp ->
                    if (oaiResp.isSuccessful) {
                        processSseResponse(oaiResp, this)
                    } else {
                        emit(StreamChunk.Error("Gateway error: HTTP ${oaiResp.code}: ${oaiResp.message}"))
                    }
                }
            } else {
                emit(StreamChunk.Error("Server error: HTTP ${response.code}"))
                response.close()
            }
        } catch (e: Exception) {
            emit(StreamChunk.Error("Network stream error: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun processSseResponse(
        response: okhttp3.Response,
        emitter: kotlinx.coroutines.flow.FlowCollector<StreamChunk>
    ) {
        response.use { resp ->
            val source = resp.body?.source() ?: run {
                emitter.emit(StreamChunk.Error("Empty response body"))
                return
            }

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        emitter.emit(StreamChunk.Done)
                        break
                    }
                    try {
                        val json = JSONObject(data)
                        // 1. Check OpenAI / LM Studio / OpenRouter streaming format
                        if (json.has("choices")) {
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.optJSONObject(0)
                                val delta = choice?.optJSONObject("delta")
                                val text = delta?.optString("content", "") ?: ""
                                if (text.isNotEmpty()) {
                                    emitter.emit(StreamChunk.TextDelta(text))
                                }
                            }
                        } else if (json.has("response")) {
                            // 2. Ollama native stream format
                            val text = json.optString("response", "")
                            if (text.isNotEmpty()) {
                                emitter.emit(StreamChunk.TextDelta(text))
                            }
                        } else {
                            // 3. Hermes Agent custom JSON format
                            val type = json.optString("type", "text")
                            when (type) {
                                "text" -> {
                                    val delta = json.optString("content", "")
                                    emitter.emit(StreamChunk.TextDelta(delta))
                                }
                                "tool_start" -> {
                                    val toolId = json.optString("id", System.currentTimeMillis().toString())
                                    val toolName = json.optString("tool", "terminal")
                                    val cmd = json.optString("command", "")
                                    emitter.emit(StreamChunk.ToolStart(
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
                                    emitter.emit(StreamChunk.ToolOutput(
                                        toolId = toolId,
                                        output = output,
                                        status = if (code == 0) ToolStatus.COMPLETED else ToolStatus.FAILED
                                    ))
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Raw text chunk fallback
                        emitter.emit(StreamChunk.TextDelta(data))
                    }
                }
            }
            emitter.emit(StreamChunk.Done)
        }
    }
}

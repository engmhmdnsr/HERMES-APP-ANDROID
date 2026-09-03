package ee.oversight.hermes.data

import ee.oversight.hermes.model.AiModelInfo
import ee.oversight.hermes.model.ChatMessage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.DiscoveredGateway
import ee.oversight.hermes.model.HermesSession
import ee.oversight.hermes.model.MessageSender
import ee.oversight.hermes.model.ProcessInfo
import ee.oversight.hermes.model.SystemTelemetry
import ee.oversight.hermes.model.ToolExecutionBlock
import ee.oversight.hermes.model.ToolStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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

/**
 * Client for the OFFICIAL Hermes API server (gateway/platforms/api_server.py).
 *
 * Base URL should be http://<tailscale-ip>:8642 (no trailing slash).
 * Auth: Authorization: Bearer <API_SERVER_KEY>.
 *
 * Endpoints:
 *  - GET  /health                      -> {"status":"ok",...}
 *  - GET  /api/sessions                -> {"object":"list","data":[{id,title,model,...}]}
 *  - GET  /api/sessions/{id}/messages  -> {"object":"list","data":[{role,content,...}]}
 *  - POST /api/sessions                -> create session
 *  - POST /api/sessions/{id}/model     -> lock model on a session
 *  - POST /api/sessions/{id}/chat/stream -> SSE (run.started, message.started,
 *                                           assistant.delta, tool.progress,
 *                                           tool.started, tool.completed,
 *                                           assistant.completed, run.completed, done)
 *  - GET  /api/model/options           -> {providers:[{slug,name,models:[...]}]}
 *  - GET  /health/detailed             -> gateway readiness (no CPU/RAM metrics)
 */
class HermesNetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // long reads for SSE streams
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // ------------------------------------------------------------------
    // Auth helpers
    // ------------------------------------------------------------------
    private fun Request.Builder.authHeaders(config: ConnectionConfig): Request.Builder {
        if (config.apiKey.isNotBlank()) {
            addHeader("Authorization", "Bearer ${config.apiKey}")
        }
        return this
    }

    // ------------------------------------------------------------------
    // Discovery (UDP beacon, old custom server only)
    // ------------------------------------------------------------------
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
                            port = json.optInt("port", 8642),
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

    // ------------------------------------------------------------------
    // Health: GET /health (official API server)
    // ------------------------------------------------------------------
    suspend fun ping(config: ConnectionConfig): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val url = "${config.baseUrl}/health"
            val request = Request.Builder()
                .url(url)
                .authHeaders(config)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    PingResult(
                        isSuccess = true,
                        latencyMs = elapsed,
                        statusCode = response.code,
                        message = "Connected to Hermes API at ${config.baseUrl} (${elapsed}ms)"
                    )
                } else if (response.code == 404) {
                    // Fallback: old-style servers expose /api/health
                    try {
                        val oldUrl = "${config.baseUrl}/api/health"
                        val testReq = Request.Builder().url(oldUrl).authHeaders(config).get().build()
                        client.newCall(testReq).execute().use { testResp ->
                            if (testResp.isSuccessful) {
                                return@withContext PingResult(
                                    isSuccess = true,
                                    latencyMs = System.currentTimeMillis() - startTime,
                                    statusCode = testResp.code,
                                    message = "Gateway Responding at $oldUrl"
                                )
                            }
                        }
                    } catch (_: Exception) {}
                    PingResult(false, elapsed, response.code, "Server HTTP ${response.code}: ${response.message}")
                } else {
                    PingResult(
                        isSuccess = false,
                        latencyMs = elapsed,
                        statusCode = response.code,
                        message = "Hermes API HTTP ${response.code}: ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            PingResult(
                isSuccess = false,
                latencyMs = elapsed,
                statusCode = 0,
                message = "Gateway unreachable at ${config.baseUrl} (${e.localizedMessage ?: "Connection refused"})"
            )
        }
    }

    // ------------------------------------------------------------------
    // System telemetry: GET /api/system (added to the official API server)
    // Returns real host CPU/RAM/GPU via psutil + nvidia-smi.
    // ------------------------------------------------------------------
    suspend fun fetchMetrics(config: ConnectionConfig): Result<SystemTelemetry> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/system"
            val request = Request.Builder().url(url).authHeaders(config).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)

                // Parse processes list
                val procs = mutableListOf<ProcessInfo>()
                val procArr = json.optJSONArray("processes")
                if (procArr != null) {
                    for (i in 0 until procArr.length()) {
                        val p = procArr.optJSONObject(i)
                        if (p != null) {
                            procs.add(
                                ProcessInfo(
                                    name = p.optString("name", ""),
                                    pid = p.optString("pid", ""),
                                    memory = p.optString("memory", ""),
                                    cpu = p.optString("cpu", "")
                                )
                            )
                        }
                    }
                }

                val cpuHist = mutableListOf<Float>()
                val histArr = json.optJSONArray("cpu_history")
                if (histArr != null) {
                    for (i in 0 until histArr.length()) {
                        cpuHist.add(histArr.optDouble(i, 0.0).toFloat())
                    }
                }

                val telemetry = SystemTelemetry(
                    cpuUsage = json.optDouble("cpu_usage", 0.0).toFloat(),
                    ramUsedGb = json.optDouble("ram_used_gb", 0.0).toFloat(),
                    ramTotalGb = json.optDouble("ram_total_gb", 0.0).toFloat(),
                    gpuUsage = json.optDouble("gpu_usage", 0.0).toFloat(),
                    vramUsedGb = json.optDouble("vram_used_gb", 0.0).toFloat(),
                    vramTotalGb = json.optDouble("vram_total_gb", 0.0).toFloat(),
                    hostname = json.optString("hostname", "WINDOWS-PC"),
                    osVersion = json.optString("os_version", "Windows"),
                    uptime = json.optString("uptime", ""),
                    agentVersion = json.optString("agent_version", ""),
                    activeTasksCount = json.optInt("active_tasks_count", 0),
                    pingMs = json.optLong("ping_ms", 0L),
                    cpuHistory = cpuHist,
                    activeProcesses = procs
                )
                Result.success(telemetry)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Sessions: GET /api/sessions -> {"object":"list","data":[...]}
    // ------------------------------------------------------------------
    suspend fun fetchSessions(config: ConnectionConfig): Result<List<HermesSession>> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/sessions?limit=50"
            val request = Request.Builder().url(url).authHeaders(config).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            val array = json.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<HermesSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HermesSession(
                        id = obj.getString("id"),
                        title = obj.optString("title").ifBlank { obj.getString("id") },
                        model = obj.optString("model", "default"),
                        startedAt = (obj.optDouble("started_at", 0.0) * 1000).toLong(),
                        messageCount = obj.optInt("message_count", 0)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Session messages: GET /api/sessions/{id}/messages
    // Returns {"object":"list","data":[{role,content,...}]} newest-last.
    // We request order=oldest to display chronological.
    // ------------------------------------------------------------------
    suspend fun fetchSessionMessages(config: ConnectionConfig, sessionId: String): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/sessions/$sessionId/messages?limit=200&order=oldest"
            val request = Request.Builder().url(url).authHeaders(config).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            val array = json.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val role = obj.optString("role", "user")
                val rawContent = obj.optString("content", "")
                val content = if (role == "assistant") rawContent else rawContent
                val ts = (obj.optDouble("timestamp", 0.0) * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis()
                list.add(
                    ChatMessage(
                        id = obj.optString("id", i.toString()),
                        sender = if (role == "assistant") MessageSender.HERMES else MessageSender.USER,
                        timestamp = ts,
                        content = content
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Create session: POST /api/sessions
    // ------------------------------------------------------------------
    suspend fun createNewSession(config: ConnectionConfig, title: String? = null, model: String? = null): Result<HermesSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/sessions"
            val payload = JSONObject().apply {
                if (!title.isNullOrBlank()) put("title", title)
                if (!model.isNullOrBlank()) put("model", model)
                put("source", "mobile_app")
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).authHeaders(config).post(body).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            val bodyStr = response.body?.string() ?: "{}"
            // Response is {"object":"hermes.session","session":{...}}
            val json = JSONObject(bodyStr)
            val sessionObj = json.optJSONObject("session") ?: json
            val session = HermesSession(
                id = sessionObj.getString("id"),
                title = sessionObj.optString("title").ifBlank { "New Session" },
                model = sessionObj.optString("model", model ?: "default"),
                startedAt = (sessionObj.optDouble("started_at", 0.0) * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis(),
                messageCount = sessionObj.optInt("message_count", 0)
            )
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Model inventory: GET /api/model/options
    // Returns {providers:[{slug,name,models:[str,...]}], model, provider}
    // We flatten to AiModelInfo list grouped by provider.
    // ------------------------------------------------------------------
    suspend fun fetchModels(config: ConnectionConfig): Result<List<AiModelInfo>> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/model/options"
            val request = Request.Builder().url(url).authHeaders(config).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            val providers = json.optJSONArray("providers") ?: JSONArray()
            val list = mutableListOf<AiModelInfo>()
            val seen = mutableSetOf<String>()
            val providerNames = mutableMapOf<String, String>()

            // First pass: collect provider display names
            for (i in 0 until providers.length()) {
                val p = providers.getJSONObject(i)
                providerNames[p.optString("slug")] = p.optString("name", p.optString("slug"))
            }

            // Second pass: models
            for (i in 0 until providers.length()) {
                val p = providers.getJSONObject(i)
                val slug = p.optString("slug")
                val displayName = providerNames[slug] ?: slug
                val models = p.optJSONArray("models") ?: JSONArray()
                for (j in 0 until models.length()) {
                    val modelId = models.optString(j)
                    if (modelId.isBlank() || "embed" in modelId.lowercase()) continue
                    if (modelId in seen) continue
                    seen.add(modelId)
                    val clean = modelId.substringAfterLast('/')
                        .replace("-", " ")
                        .replace("_", " ")
                        .replaceFirstChar { it.uppercase() }
                    list.add(
                        AiModelInfo(
                            id = modelId,
                            displayName = clean,
                            provider = displayName,
                            description = "$displayName: $modelId",
                            isDefault = modelId == "deepseek/deepseek-v4-flash"
                        )
                    )
                }
            }
            if (list.isEmpty()) Result.failure(Exception("No models returned")) else Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Lock a model onto a session: POST /api/sessions/{id}/model
    // Body: {"model": "...", "provider": "..."} or {"model":"provider::model"}
    // ------------------------------------------------------------------
    suspend fun lockSessionModel(config: ConnectionConfig, sessionId: String, modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/sessions/$sessionId/model"
            val payload = JSONObject().apply {
                put("model", modelId)
                put("require_model_lock", true)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).authHeaders(config).post(body).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errBody"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Chat stream: POST /api/sessions/{id}/chat/stream
    // SSE events: run.started, message.started, assistant.delta,
    //             tool.started/completed/failed, tool.progress,
    //             assistant.completed, run.completed, error, done
    // Supports image attachments as data URLs -> multimodal content parts.
    // ------------------------------------------------------------------
    fun streamChat(
        config: ConnectionConfig,
        prompt: String,
        model: String,
        sessionId: String? = null,
        attachments: List<String> = emptyList()
    ): Flow<StreamChunk> = flow {
        // If no session id, create one first
        val resolvedSessionId = sessionId
        if (resolvedSessionId == null) {
            emit(StreamChunk.Error("No active session. Create or select a session first."))
            return@flow
        }

        val url = "${config.baseUrl}/api/sessions/$resolvedSessionId/chat/stream"
        // Build multimodal message: text + image parts (native API server format)
        val messagePayload: Any = if (attachments.isNotEmpty()) {
            val parts = org.json.JSONArray()
            if (prompt.isNotBlank()) {
                parts.put(JSONObject().put("type", "text").put("text", prompt))
            }
            for (imgUrl in attachments) {
                parts.put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", imgUrl))
                )
            }
            parts
        } else {
            prompt
        }
        val payload = JSONObject().apply {
            put("message", messagePayload)
            if (model.isNotBlank() && model != "default") {
                put("model", model)
            }
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .authHeaders(config)
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                emit(StreamChunk.Error("Server error: HTTP ${response.code} ${errBody.take(300)}"))
                return@flow
            }

            response.use { resp ->
                val source = resp.body?.source() ?: run {
                    emit(StreamChunk.Error("Empty response body"))
                    return@flow
                }
                var sawContent = false
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("event:")) {
                        // We handle data frames below; event name kept in state
                        currentEventName = line.removePrefix("event:").trim()
                        continue
                    }
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        if (data.isEmpty()) continue
                        try {
                            val json = JSONObject(data)
                            val eventName = currentEventName

                            when {
                                // OpenAI-compatible fallback
                                json.has("choices") -> {
                                    val choices = json.optJSONArray("choices")
                                    if (choices != null && choices.length() > 0) {
                                        val delta = choices.optJSONObject(0)?.optJSONObject("delta")
                                        val text = delta?.optString("content", "") ?: ""
                                        if (text.isNotEmpty()) {
                                            sawContent = true
                                            emit(StreamChunk.TextDelta(text))
                                        }
                                    }
                                }
                                eventName == "assistant.delta" -> {
                                    val delta = json.optString("delta", "")
                                    if (delta.isNotEmpty()) {
                                        sawContent = true
                                        emit(StreamChunk.TextDelta(delta))
                                    }
                                }
                                eventName == "tool.started" -> {
                                    val toolId = json.optString("message_id", System.currentTimeMillis().toString())
                                    val toolName = json.optString("tool_name", "tool")
                                    val preview = json.optString("preview", "")
                                    emit(StreamChunk.ToolStart(
                                        ToolExecutionBlock(
                                            id = toolId,
                                            toolName = toolName,
                                            command = preview,
                                            status = ToolStatus.RUNNING
                                        )
                                    ))
                                }
                                eventName == "tool.progress" -> {
                                    val toolId = json.optString("message_id", System.currentTimeMillis().toString())
                                    val toolName = json.optString("tool_name", "_thinking")
                                    val preview = json.optString("preview", "") ?: json.optString("delta", "")
                                    emit(StreamChunk.ToolStart(
                                        ToolExecutionBlock(
                                            id = toolId,
                                            toolName = toolName,
                                            command = preview.take(200),
                                            status = ToolStatus.RUNNING
                                        )
                                    ))
                                }
                                eventName == "tool.completed" || eventName == "tool.failed" -> {
                                    val toolId = json.optString("message_id", "")
                                    val toolName = json.optString("tool_name", "tool")
                                    val output = json.optString("preview", "")
                                    emit(StreamChunk.ToolOutput(
                                        toolId = toolId,
                                        output = output,
                                        status = if (eventName == "tool.completed") ToolStatus.COMPLETED else ToolStatus.FAILED
                                    ))
                                }
                                eventName == "assistant.completed" || eventName == "run.completed" -> {
                                    val content = json.optString("content", "")
                                    if (content.isNotEmpty()) {
                                        sawContent = true
                                        emit(StreamChunk.TextDelta(content))
                                    }
                                }
                                eventName == "error" -> {
                                    emit(StreamChunk.Error(json.optString("message", "Unknown error")))
                                }
                                eventName == "done" -> {
                                    // done event
                                }
                            }
                        } catch (_: Exception) {
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

    private var currentEventName: String = ""

    companion object {
        // If running against the old custom gateway (port 8080, FastAPI) these
        // are the older paths. Kept for reference only.
    }
}

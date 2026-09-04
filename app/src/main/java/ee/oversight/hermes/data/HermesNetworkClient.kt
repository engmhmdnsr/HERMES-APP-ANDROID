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
    data class ThinkingDelta(val text: String) : StreamChunk()
    data object ThinkingDone : StreamChunk()
    data class ToolStart(val tool: ToolExecutionBlock) : StreamChunk()
    data class ToolOutput(val toolId: String, val output: String, val status: ToolStatus) : StreamChunk()
    data class ApprovalNeeded(val request: ee.oversight.hermes.model.ApprovalRequest) : StreamChunk()
    data class Usage(val inputTokens: Long, val outputTokens: Long, val totalTokens: Long) : StreamChunk()
    data class Error(val message: String) : StreamChunk()
    data object Done : StreamChunk()
}

/**
 * Client for the OFFICIAL Hermes API server (gateway/platforms/api_server.py).
 *
 * Base URL should be http://<tailscale-ip>:8080 (no trailing slash).
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
                    cpuUsage = json.optJSONObject("cpu")?.optDouble("percent", 0.0)?.toFloat() ?: json.optDouble("cpu_usage", 0.0).toFloat(),
                    ramUsedGb = json.optJSONObject("memory")?.optDouble("used_gb", 0.0)?.toFloat() ?: json.optDouble("ram_used_gb", 0.0).toFloat(),
                    ramTotalGb = json.optJSONObject("memory")?.optDouble("total_gb", 0.0)?.toFloat() ?: json.optDouble("ram_total_gb", 0.0).toFloat(),
                    gpuUsage = json.optJSONObject("gpu")?.optDouble("utilization", 0.0)?.toFloat() ?: json.optDouble("gpu_usage", 0.0).toFloat(),
                    vramUsedGb = json.optJSONObject("gpu")?.optDouble("memory_used_gb", 0.0)?.toFloat() ?: json.optDouble("vram_used_gb", 0.0).toFloat(),
                    vramTotalGb = json.optJSONObject("gpu")?.optDouble("memory_total_gb", 0.0)?.toFloat() ?: json.optDouble("vram_total_gb", 0.0).toFloat(),
                    gpuName = json.optJSONObject("gpu")?.optString("name", "") ?: "",
                    diskUsedGb = json.optJSONObject("disk")?.optDouble("used_gb", 0.0)?.toFloat() ?: 0f,
                    diskTotalGb = json.optJSONObject("disk")?.optDouble("total_gb", 0.0)?.toFloat() ?: 0f,
                    hostname = json.optJSONObject("host")?.optString("hostname", "WINDOWS-PC") ?: json.optString("hostname", "WINDOWS-PC"),
                    osVersion = json.optJSONObject("host")?.optString("os", "Windows") ?: json.optString("os_version", "Windows"),
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
            val url = "${config.baseUrl}/api/sessions?limit=100"
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
                        title = cleanSessionTitle(obj.optString("title")),
                        model = obj.optString("model", "default"),
                        startedAt = (obj.optDouble("started_at", 0.0) * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis(),
                        messageCount = obj.optInt("message_count", 0),
                        inputTokens = obj.optLong("input_tokens", 0L),
                        outputTokens = obj.optLong("output_tokens", 0L),
                        reasoningTokens = obj.optLong("reasoning_tokens", 0L),
                        isPinned = obj.optBoolean("pinned", false) || obj.optBoolean("is_pinned", false),
                        isThread = obj.optBoolean("is_thread", false) || obj.optString("type") == "thread",
                        isArchived = obj.optBoolean("archived", false) || obj.optBoolean("is_archived", false),
                        source = obj.optString("source", ""),
                        lastActiveAt = (obj.optDouble("started_at", 0.0) * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            }
            // Sort newest first
            list.sortByDescending { it.startedAt }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Session messages: GET /api/sessions/{id}/messages
    // Paginates up to 10,000 messages and filters out internal tool JSON.
    // ------------------------------------------------------------------
    suspend fun fetchSessionMessages(config: ConnectionConfig, sessionId: String): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<ChatMessage>()
            var offset = 0
            val limit = 500
            val maxMessages = 10000

            while (list.size < maxMessages) {
                val url = "${config.baseUrl}/api/sessions/$sessionId/messages?limit=$limit&offset=$offset"
                val request = Request.Builder().url(url).authHeaders(config).get().build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    if (list.isNotEmpty()) break
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val array = json.optJSONArray("data") ?: JSONArray()
                if (array.length() == 0) break

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val role = obj.optString("role", "user")
                    // Filter out tool output and internal system messages
                    if (role == "tool" || role == "system") continue

                    val rawContent = obj.optString("content", "")
                    val trimmed = rawContent.trim()
                    // Filter out raw JSON tool execution payloads
                    if (trimmed.startsWith("{\"output\":") || 
                        trimmed.startsWith("{\"total_count\":") || 
                        (trimmed.startsWith("{\"success\":") && trimmed.contains("\"exit_code\""))) {
                        continue
                    }
                    if (trimmed.isEmpty()) continue

                    val ts = (obj.optDouble("timestamp", 0.0) * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis()
                    list.add(
                        ChatMessage(
                            id = obj.optString("id", "${sessionId}_${offset + i}"),
                            sender = if (role == "assistant") MessageSender.HERMES else MessageSender.USER,
                            timestamp = ts,
                            content = rawContent
                        )
                    )
                }

                if (array.length() < limit) break
                offset += array.length()
            }

            // Ensure chronological order
            list.sortBy { it.timestamp }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Delete session: DELETE /api/sessions/{id}
    // ------------------------------------------------------------------
    suspend fun deleteSession(config: ConnectionConfig, sessionId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.baseUrl}/api/sessions/$sessionId"
            val request = Request.Builder().url(url).authHeaders(config).delete().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
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
                title = cleanSessionTitle(sessionObj.optString("title")),
                model = sessionObj.optString("model", model ?: "default"),
                startedAt = (sessionObj.optDouble("started_at", 0.0) * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis(),
                messageCount = sessionObj.optInt("message_count", 0),
                source = sessionObj.optString("source", "mobile_app"),
                lastActiveAt = (sessionObj.optDouble("started_at", 0.0) * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis()
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
        attachments: List<String> = emptyList(),
        reasoningEffort: String = ""
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
            // Reasoning effort (low/medium/high/none) — server maps it onto the
            // agent's reasoning_effort when the model supports it.
            if (reasoningEffort.isNotBlank() && reasoningEffort != "none") {
                put("model_options", JSONObject().put("reasoning_effort", reasoningEffort))
            }
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .authHeaders(config)
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()

        val call = client.newCall(request)
        try {
            val response = call.execute()
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
                var currentEventName = ""
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
                                    val toolName = json.optString("tool_name", "_thinking")
                                    if (toolName == "_thinking" || toolName == "thinking") {
                                        // Hidden reasoning stream → ThinkingDelta so the
                                        // UI can render it dimmed + collapsible.
                                        val delta = json.optString("delta", json.optString("preview", ""))
                                        if (delta.isNotEmpty()) {
                                            emit(StreamChunk.ThinkingDelta(delta))
                                        }
                                    } else {
                                        val toolId = json.optString("message_id", System.currentTimeMillis().toString())
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
                                    val usageObj = json.optJSONObject("usage")
                                    if (usageObj != null) {
                                        val inTok = usageObj.optLong("input_tokens", 0L)
                                        val outTok = usageObj.optLong("output_tokens", 0L)
                                        val totTok = usageObj.optLong("total_tokens", inTok + outTok)
                                        if (totTok > 0) {
                                            emit(StreamChunk.Usage(inTok, outTok, totTok))
                                        }
                                    }
                                    val content = json.optString("content", "")
                                    // Only emit if no deltas were streamed to prevent text duplication
                                    if (!sawContent && content.isNotEmpty()) {
                                        sawContent = true
                                        emit(StreamChunk.TextDelta(content))
                                    }
                                    // Reply phase is over → thinking is final, collapse it.
                                    emit(StreamChunk.ThinkingDone)
                                }
                                eventName == "approval.request" || eventName == "approval_required" || json.optString("type") == "approval.request" -> {
                                    val runId = json.optString("run_id", json.optString("id", System.currentTimeMillis().toString()))
                                    val callId = if (json.has("call_id")) json.getString("call_id") else null
                                    val tool = json.optString("tool", json.optString("tool_name", "terminal"))
                                    val command = json.optString("command", json.optString("payload", json.optString("preview", "")))
                                    val reason = json.optString("reason", "")
                                    val msg = json.optString("message", "Hermes requires approval to execute this action.")
                                    emit(StreamChunk.ApprovalNeeded(
                                        ee.oversight.hermes.model.ApprovalRequest(
                                            runId = runId,
                                            callId = callId,
                                            sessionId = resolvedSessionId,
                                            toolName = tool,
                                            command = command,
                                            reason = reason,
                                            message = msg
                                        )
                                    ))
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
        } finally {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    // ------------------------------------------------------------------
    // File upload: POST /api/files (using shared OkHttpClient)
    // ------------------------------------------------------------------
    suspend fun uploadFile(
        config: ConnectionConfig,
        displayName: String,
        bytes: ByteArray
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            if (bytes.size > 9 * 1024 * 1024) {
                return@withContext Result.failure(Exception("File size exceeds 9MB limit"))
            }
            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val payload = JSONObject().apply {
                put("filename", displayName)
                put("content_b64", b64)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${config.baseUrl}/api/files")
                .authHeaders(config)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val path = json.optString("path", "")
                    if (path.isNotBlank()) {
                        return@withContext Result.success(Pair(path, displayName))
                    }
                }
                Result.failure(Exception("Upload failed (HTTP ${response.code}: ${response.message})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitApproval(
        config: ConnectionConfig,
        runId: String,
        approved: Boolean,
        sessionId: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("approved", approved)
                if (sessionId != null) put("session_id", sessionId)
                put("run_id", runId)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)

            // Try /v1/runs/{runId}/approval first
            val urlPrimary = "${config.baseUrl}/v1/runs/$runId/approval"
            try {
                val reqPrimary = Request.Builder()
                    .url(urlPrimary)
                    .authHeaders(config)
                    .post(body)
                    .build()
                client.newCall(reqPrimary).execute().use { resp ->
                    if (resp.isSuccessful) return@withContext Result.success(true)
                }
            } catch (_: Exception) {}

            // Fallback to /api/sessions/{sessionId}/approval if sessionId available
            if (sessionId != null) {
                val urlFallback = "${config.baseUrl}/api/sessions/$sessionId/approval"
                val reqFallback = Request.Builder()
                    .url(urlFallback)
                    .authHeaders(config)
                    .post(body)
                    .build()
                client.newCall(reqFallback).execute().use { resp ->
                    if (resp.isSuccessful) return@withContext Result.success(true)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        // If running against the old custom gateway (port 8080, FastAPI) these
        // are the older paths. Kept for reference only.

        /**
         * The API returns JSON `null` for sessions without an explicit title.
         * org.json's optString() then yields the literal string "null", which
         * would otherwise be shown as the session name. Normalize it here.
         */
        fun cleanSessionTitle(raw: String): String {
            val t = raw.trim()
            if (t.isEmpty() || t.equals("null", ignoreCase = true)) return "New Session"
            return t
        }
    }
}

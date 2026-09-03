package ee.oversight.hermes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ee.oversight.hermes.data.HermesDemoSimulator
import ee.oversight.hermes.data.HermesNetworkClient
import ee.oversight.hermes.data.HermesPreferencesRepository
import ee.oversight.hermes.data.PingResult
import ee.oversight.hermes.data.StreamChunk
import ee.oversight.hermes.model.AiModelInfo
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.AvailableAiModels
import ee.oversight.hermes.model.ChatMessage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.ConnectionStatus
import ee.oversight.hermes.model.DiscoveredGateway
import ee.oversight.hermes.model.HermesSession
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.model.MessageSender
import ee.oversight.hermes.model.SystemTelemetry
import ee.oversight.hermes.model.ToolStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab {
    TERMINAL,
    TELEMETRY,
    GATEWAY
}

class HermesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = HermesPreferencesRepository(application)
    private val networkClient = HermesNetworkClient()

    private val _config = MutableStateFlow(prefsRepo.getConnectionConfig())
    val config: StateFlow<ConnectionConfig> = _config.asStateFlow()

    private val _connectionStatus = MutableStateFlow(
        if (_config.value.isDemoMode) ConnectionStatus.DEMO_MODE else ConnectionStatus.DISCONNECTED
    )
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        AvailableAiModels.find { it.id == prefsRepo.getSelectedModelId() } ?: AvailableAiModels.first()
    )
    val selectedModel: StateFlow<AiModelInfo> = _selectedModel.asStateFlow()

    private val _activeTab = MutableStateFlow(AppTab.TERMINAL)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _pingResult = MutableStateFlow<PingResult?>(null)
    val pingResult: StateFlow<PingResult?> = _pingResult.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefsRepo.getAppLanguage())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _discoveredGateway = MutableStateFlow<DiscoveredGateway?>(null)
    val discoveredGateway: StateFlow<DiscoveredGateway?> = _discoveredGateway.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _sessions = MutableStateFlow<List<HermesSession>>(emptyList())
    val sessions: StateFlow<List<HermesSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AiModelInfo>>(AvailableAiModels)
    val availableModels: StateFlow<List<AiModelInfo>> = _availableModels.asStateFlow()

    private val _isLoadingSessions = MutableStateFlow(false)
    val isLoadingSessions: StateFlow<Boolean> = _isLoadingSessions.asStateFlow()

    private var telemetryPollingJob: Job? = null
    private var streamingJob: Job? = null

    init {
        // Initialize with helpful welcome message from Hermes Agent
        seedInitialWelcomeMessage()
        startTelemetryPolling()
        if (!_config.value.isDemoMode) {
            testPing() // this loads sessions + models on success
        }
    }

    fun startAutoDiscovery() {
        if (_isDiscovering.value) return
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveredGateway.value = null
            val result = networkClient.discoverLocalGateway(2500)
            _discoveredGateway.value = result
            _isDiscovering.value = false
        }
    }

    fun connectDiscovered(discovered: DiscoveredGateway, useTailscale: Boolean = false) {
        val targetIp = if (useTailscale && !discovered.tailscaleIp.isNullOrBlank()) {
            discovered.tailscaleIp
        } else {
            discovered.ip
        }
        val updated = _config.value.copy(
            tailscaleIp = targetIp,
            port = discovered.port,
            apiKey = discovered.apiKey.ifEmpty { _config.value.apiKey },
            isDemoMode = false,
            useCustomGatewayUrl = false
        )
        updateConnectionConfig(updated)
    }

    fun importFromQr(raw: String): Boolean {
        try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("hermes://connect")) {
                val uri = android.net.Uri.parse(trimmed)
                val ip = uri.getQueryParameter("ip") ?: uri.getQueryParameter("tailscale_ip") ?: "127.0.0.1"
                val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 8642
                val key = uri.getQueryParameter("key") ?: ""
                val updated = _config.value.copy(
                    tailscaleIp = ip,
                    port = port,
                    apiKey = key.ifEmpty { _config.value.apiKey },
                    isDemoMode = false,
                    useCustomGatewayUrl = false
                )
                updateConnectionConfig(updated)
                return true
            } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val json = org.json.JSONObject(trimmed)
                val ip = json.optString("ip", json.optString("tailscale_ip", "127.0.0.1"))
                val port = json.optInt("port", 8642)
                val key = json.optString("key", json.optString("apiKey", ""))
                val updated = _config.value.copy(
                    tailscaleIp = ip,
                    port = port,
                    apiKey = key.ifEmpty { _config.value.apiKey },
                    isDemoMode = false,
                    useCustomGatewayUrl = false
                )
                updateConnectionConfig(updated)
                return true
            } else if (trimmed.contains(":")) {
                val parts = trimmed.split(":")
                val ip = parts[0].trim()
                val port = parts[1].toIntOrNull() ?: 8642
                val updated = _config.value.copy(
                    tailscaleIp = ip,
                    port = port,
                    isDemoMode = false,
                    useCustomGatewayUrl = false
                )
                updateConnectionConfig(updated)
                return true
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefsRepo.saveAppLanguage(language)
        // If current chat is only the welcome message, refresh it with new language
        if (_chatMessages.value.size <= 1) {
            seedInitialWelcomeMessage()
        }
    }

    private fun seedInitialWelcomeMessage() {
        val welcome = ChatMessage(
            id = "welcome_msg",
            sender = MessageSender.HERMES,
            timestamp = System.currentTimeMillis(),
            modelName = _selectedModel.value.displayName,
            content = HermesStrings.welcomeMessage(_appLanguage.value),
            isStreaming = false
        )
        _chatMessages.value = listOf(welcome)
    }

    private fun startTelemetryPolling() {
        telemetryPollingJob?.cancel()
        telemetryPollingJob = viewModelScope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                if (_config.value.isDemoMode) {
                    _telemetry.update { current ->
                        HermesDemoSimulator.generateSimulatedTelemetry(current)
                    }
                    _connectionStatus.value = ConnectionStatus.DEMO_MODE
                    consecutiveFailures = 0
                } else {
                    val result = networkClient.fetchMetrics(_config.value)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { newMetrics ->
                            _telemetry.value = newMetrics
                        }
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        consecutiveFailures = 0
                    } else {
                        consecutiveFailures++
                        // 3 consecutive failures (~15s) = host unreachable
                        if (consecutiveFailures >= 3) {
                            _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        }
                    }
                }
                delay(5000)
            }
        }
    }

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun updateConnectionConfig(newConfig: ConnectionConfig) {
        _config.value = newConfig
        prefsRepo.saveConnectionConfig(newConfig)
        if (newConfig.isDemoMode) {
            _connectionStatus.value = ConnectionStatus.DEMO_MODE
        } else {
            testPing()
        }
        startTelemetryPolling()
    }

    fun toggleDemoMode(enabled: Boolean) {
        val updated = _config.value.copy(isDemoMode = enabled)
        _config.value = updated
        prefsRepo.saveConnectionConfig(updated)
        _connectionStatus.value = if (enabled) ConnectionStatus.DEMO_MODE else ConnectionStatus.CONNECTING
        if (enabled) {
            _pingResult.value = HermesDemoSimulator.simulatePing(updated.tailscaleIp, updated.port)
        } else {
            testPing()
        }
        startTelemetryPolling()
    }

    fun testPing() {
        viewModelScope.launch {
            _isPinging.value = true
            val currentConfig = _config.value
            val result = if (currentConfig.isDemoMode) {
                delay(400)
                HermesDemoSimulator.simulatePing(currentConfig.tailscaleIp, currentConfig.port)
            } else {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                val res = networkClient.ping(currentConfig)
                if (res.isSuccess) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    loadSessions()
                    refreshModels()
                } else {
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
                res
            }
            _pingResult.value = result
            _isPinging.value = false
        }
    }

    fun loadSessions() {
        if (_config.value.isDemoMode) return
        viewModelScope.launch {
            _isLoadingSessions.value = true
            val result = networkClient.fetchSessions(_config.value)
            result.onSuccess { list ->
                _sessions.value = list
                if (_currentSessionId.value == null && list.isNotEmpty()) {
                    // Prefer a session that actually has messages
                    val withMsgs = list.firstOrNull { it.messageCount > 0 } ?: list.first()
                    selectSession(withMsgs.id)
                }
            }
            _isLoadingSessions.value = false
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        if (_config.value.isDemoMode) return
        // Show session's model in the picker if known
        val s = _sessions.value.find { it.id == sessionId }
        if (s != null && s.model.isNotBlank() && s.model != "default") {
            val known = _availableModels.value.find { it.id == s.model }
            if (known != null) _selectedModel.value = known
            else {
                // Session model may not be in our live list yet; add it
                val placeholder = AiModelInfo(
                    id = s.model,
                    displayName = s.model.substringAfterLast('/').replace("-", " ").replaceFirstChar { it.uppercase() },
                    provider = s.model.substringBefore('/', "hermes"),
                    description = s.model,
                    isDefault = false
                )
                if (_availableModels.value.none { it.id == s.model }) {
                    _availableModels.value = _availableModels.value + placeholder
                }
                _selectedModel.value = placeholder
            }
        }
        viewModelScope.launch {
            val result = networkClient.fetchSessionMessages(_config.value, sessionId)
            result.onSuccess { msgs ->
                if (msgs.isNotEmpty()) {
                    _chatMessages.value = msgs
                } else {
                    _chatMessages.value = listOf(
                        ChatMessage(
                            id = "empty_$sessionId",
                            sender = MessageSender.HERMES,
                            content = "This session has no messages yet. Send a prompt to start.",
                            isStreaming = false
                        )
                    )
                }
            }
        }
    }

    fun createNewSession(title: String? = null) {
        viewModelScope.launch {
            if (_config.value.isDemoMode) {
                _chatMessages.value = emptyList()
                seedInitialWelcomeMessage()
                return@launch
            }
            val result = networkClient.createNewSession(_config.value, title, _selectedModel.value.id)
            result.onSuccess { newSession ->
                _sessions.update { listOf(newSession) + it }
                _currentSessionId.value = newSession.id
                // Lock the currently selected model on the new session
                networkClient.lockSessionModel(_config.value, newSession.id, _selectedModel.value.id)
                _chatMessages.value = listOf(
                    ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        sender = MessageSender.HERMES,
                        content = "Connected to new session: ${newSession.title}\nHost PC is ready for commands & chat.",
                        isStreaming = false
                    )
                )
            }.onFailure { e ->
                _chatMessages.update { list ->
                    list + ChatMessage(
                        id = "err_${System.currentTimeMillis()}",
                        sender = MessageSender.HERMES,
                        content = "⚠️ Failed to create session: ${e.message}",
                        isStreaming = false
                    )
                }
            }
        }
    }

    fun refreshModels() {
        if (_config.value.isDemoMode) return
        viewModelScope.launch {
            val result = networkClient.fetchModels(_config.value)
            result.onSuccess { models ->
                if (models.isNotEmpty()) {
                    _availableModels.value = models
                    val savedId = prefsRepo.getSelectedModelId()
                    val current = _selectedModel.value
                    val found = models.find { it.id == savedId } ?: models.find { it.id == current.id }
                    if (found != null) {
                        _selectedModel.value = found
                    } else {
                        _selectedModel.value = models.first()
                    }
                }
            }.onFailure { e ->
                // Keep the placeholders if the server is unreachable
                android.util.Log.w("HermesVM", "refreshModels failed: ${e.message}")
            }
        }
    }

    fun selectModel(model: AiModelInfo) {
        _selectedModel.value = model
        prefsRepo.saveSelectedModelId(model.id)
        // Lock model on the current session so the API server routes this
        // session to the chosen provider/model going forward.
        val sid = _currentSessionId.value
        if (sid != null && !_config.value.isDemoMode) {
            viewModelScope.launch {
                networkClient.lockSessionModel(_config.value, sid, model.id)
                    .onSuccess { }
                    .onFailure { e ->
                        android.util.Log.w("HermesVM", "lockSessionModel failed: ${e.message}")
                    }
            }
        }
    }

    fun sendMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isStreaming.value) return

        // Ensure we have a session to talk to
        if (!_config.value.isDemoMode && _currentSessionId.value == null) {
            createNewSession()
            // After creating, the actual message send happens below with the new session
            // but the session id state may not be updated yet, so queue via state update.
            viewModelScope.launch {
                // Small delay to allow session creation to land, then send.
                kotlinx.coroutines.delay(400)
                if (_currentSessionId.value != null) {
                    doSendMessage(trimmed)
                } else {
                    // surface error
                    _chatMessages.update { list ->
                        list + ChatMessage(
                            id = "err_${System.currentTimeMillis()}",
                            sender = MessageSender.HERMES,
                            content = "⚠️ Could not create a session. Check the gateway connection.",
                            isStreaming = false
                        )
                    }
                }
            }
            return
        }

        doSendMessage(trimmed)
    }

    private fun doSendMessage(trimmed: String) {
        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            sender = MessageSender.USER,
            content = trimmed,
            timestamp = System.currentTimeMillis()
        )

        val agentMessageId = "hermes_${System.currentTimeMillis()}"
        val agentInitialMessage = ChatMessage(
            id = agentMessageId,
            sender = MessageSender.HERMES,
            timestamp = System.currentTimeMillis(),
            modelName = _selectedModel.value.displayName,
            content = "",
            isStreaming = true,
            toolExecutions = emptyList()
        )

        _chatMessages.update { it + userMessage + agentInitialMessage }
        _isStreaming.value = true

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            val streamFlow = if (_config.value.isDemoMode) {
                HermesDemoSimulator.simulateChatStream(
                    prompt = trimmed,
                    modelName = _selectedModel.value.displayName,
                    lang = _appLanguage.value
                )
            } else {
                networkClient.streamChat(_config.value, trimmed, _selectedModel.value.id, _currentSessionId.value)
            }

            streamFlow.collect { chunk ->
                when (chunk) {
                    is StreamChunk.TextDelta -> {
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == agentMessageId) {
                                    msg.copy(content = msg.content + chunk.text)
                                } else msg
                            }
                        }
                    }
                    is StreamChunk.ToolStart -> {
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == agentMessageId) {
                                    msg.copy(toolExecutions = msg.toolExecutions + chunk.tool)
                                } else msg
                            }
                        }
                    }
                    is StreamChunk.ToolOutput -> {
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == agentMessageId) {
                                    val updatedTools = msg.toolExecutions.map { tool ->
                                        if (tool.id == chunk.toolId) {
                                            tool.copy(output = chunk.output, status = chunk.status)
                                        } else tool
                                    }
                                    msg.copy(toolExecutions = updatedTools)
                                } else msg
                            }
                        }
                    }
                    is StreamChunk.Error -> {
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == agentMessageId) {
                                    msg.copy(
                                        content = msg.content + "\n⚠️ [Error]: ${chunk.message}",
                                        isStreaming = false
                                    )
                                } else msg
                            }
                        }
                    }
                    StreamChunk.Done -> {
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == agentMessageId) {
                                    msg.copy(isStreaming = false)
                                } else msg
                            }
                        }
                    }
                }
            }
            _isStreaming.value = false
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _isStreaming.value = false
        _chatMessages.update { list ->
            list.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }
        }
    }

    fun clearChat() {
        seedInitialWelcomeMessage()
    }
}

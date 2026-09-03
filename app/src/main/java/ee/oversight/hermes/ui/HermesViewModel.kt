package ee.oversight.hermes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
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
    private var pendingSend: String? = null
    private var pendingAttachments: List<String> = emptyList()

    init {
        // Start with empty chat (no fake welcome) until a session loads.
        _chatMessages.value = emptyList()
        startTelemetryPolling()
        // If the user already has a saved config, try connecting automatically.
        if (_config.value.tailscaleIp.isNotBlank()) {
            testPing()
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
            useCustomGatewayUrl = false
        )
        updateConnectionConfig(updated)
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefsRepo.saveAppLanguage(language)
    }

    private fun startTelemetryPolling() {
        telemetryPollingJob?.cancel()
        telemetryPollingJob = viewModelScope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                // Only poll when we have a target configured
                if (_config.value.tailscaleIp.isBlank()) {
                    delay(3000)
                    continue
                }
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
        testPing()
        startTelemetryPolling()
    }

    fun testPing() {
        viewModelScope.launch {
            _isPinging.value = true
            _connectionStatus.value = ConnectionStatus.CONNECTING
            val currentConfig = _config.value
            if (currentConfig.tailscaleIp.isBlank()) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _isPinging.value = false
                return@launch
            }
            val res = networkClient.ping(currentConfig)
            if (res.isSuccess) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                loadSessions()
                refreshModels()
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
            }
            _pingResult.value = res
            _isPinging.value = false
        }
    }

    fun loadSessions() {
        if (_config.value.tailscaleIp.isBlank()) return
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

    fun createNewSession(title: String? = null, thenSend: String? = null) {
        viewModelScope.launch {
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
                // If a message was queued behind session creation, send it now.
                val queued = thenSend ?: pendingSend
                val queuedAttachments = pendingAttachments
                pendingSend = null
                pendingAttachments = emptyList()
                if (queued != null || queuedAttachments.isNotEmpty()) {
                    doSendMessage(queued ?: "", queuedAttachments)
                }
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
        if (_config.value.tailscaleIp.isBlank()) return
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
        if (sid != null) {
            viewModelScope.launch {
                networkClient.lockSessionModel(_config.value, sid, model.id)
                    .onSuccess { }
                    .onFailure { e ->
                        android.util.Log.w("HermesVM", "lockSessionModel failed: ${e.message}")
                    }
            }
        }
    }

    fun sendMessage(prompt: String, attachments: List<String> = emptyList()) {
        val trimmed = prompt.trim()
        if ((trimmed.isEmpty() && attachments.isEmpty()) || _isStreaming.value) return

        // If no session yet, create one and queue the message to send after.
        if (_currentSessionId.value == null) {
            pendingSend = trimmed
            pendingAttachments = attachments
            createNewSession()
            return
        }

        doSendMessage(trimmed, attachments)
    }

    private fun doSendMessage(trimmed: String, attachments: List<String> = emptyList()) {
        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            sender = MessageSender.USER,
            content = trimmed,
            timestamp = System.currentTimeMillis(),
            attachments = attachments
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
            val streamFlow = networkClient.streamChat(
                _config.value,
                trimmed,
                _selectedModel.value.id,
                _currentSessionId.value,
                attachments
            )

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
}

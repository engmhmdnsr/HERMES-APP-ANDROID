package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HermesDemoSimulator
import com.example.data.HermesNetworkClient
import com.example.data.HermesPreferencesRepository
import com.example.data.PingResult
import com.example.data.StreamChunk
import com.example.model.AiModelInfo
import com.example.model.AppLanguage
import com.example.model.AvailableAiModels
import com.example.model.ChatMessage
import com.example.model.ConnectionConfig
import com.example.model.ConnectionStatus
import com.example.model.HermesStrings
import com.example.model.MessageSender
import com.example.model.SystemTelemetry
import com.example.model.ToolStatus
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

    private var telemetryPollingJob: Job? = null
    private var streamingJob: Job? = null

    init {
        // Initialize with helpful welcome message from Hermes Agent
        seedInitialWelcomeMessage()
        startTelemetryPolling()
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
            while (isActive) {
                if (_config.value.isDemoMode) {
                    _telemetry.update { current ->
                        HermesDemoSimulator.generateSimulatedTelemetry(current)
                    }
                    _connectionStatus.value = ConnectionStatus.DEMO_MODE
                } else {
                    // Try real HTTP metrics fetch
                    val result = networkClient.fetchMetrics(_config.value)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { newMetrics ->
                            val currentHist = _telemetry.value.cpuHistory
                            _telemetry.value = newMetrics.copy(
                                cpuHistory = (currentHist + newMetrics.cpuUsage).takeLast(16)
                            )
                        }
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                    } else {
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    }
                }
                delay(2000)
            }
        }
    }

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun selectModel(model: AiModelInfo) {
        _selectedModel.value = model
        prefsRepo.saveSelectedModelId(model.id)
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
                } else {
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
                res
            }
            _pingResult.value = result
            _isPinging.value = false
        }
    }

    fun sendMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isStreaming.value) return

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
                networkClient.streamChat(_config.value, trimmed, _selectedModel.value.id)
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
                                        content = msg.content + "\n⚠️ [خطأ بالاتصال]: ${chunk.message}",
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

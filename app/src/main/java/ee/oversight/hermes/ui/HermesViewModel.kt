package ee.oversight.hermes.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ee.oversight.hermes.data.HermesAppLog
import ee.oversight.hermes.data.HermesNetworkClient
import ee.oversight.hermes.data.HermesPreferencesRepository
import ee.oversight.hermes.data.PingResult
import ee.oversight.hermes.data.StreamChunk
import ee.oversight.hermes.model.AiModelInfo
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ApprovalMode
import ee.oversight.hermes.model.ApprovalRequest
import ee.oversight.hermes.model.AvailableAiModels
import ee.oversight.hermes.model.ChatMessage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.ConnectionStatus
import ee.oversight.hermes.model.DiscoveredGateway
import ee.oversight.hermes.model.HermesSession
import ee.oversight.hermes.model.MessageSender
import ee.oversight.hermes.model.SystemTelemetry
import ee.oversight.hermes.model.TokenUsage
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
    CHAT,
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

    /** Messages queued while a run is in progress — auto-send when it finishes. */
    data class QueuedMessage(
        val prompt: String,
        val attachments: List<String> = emptyList(),
        val sessionId: String? = null,
        val queuedAt: Long = System.currentTimeMillis()
    )

    /** Transient: input captured while streaming, waiting for the user to pick Send-now vs Queue. */
    data class QueuedInput(
        val prompt: String,
        val attachments: List<String> = emptyList()
    )

    private val _queuedMessages = MutableStateFlow<List<QueuedMessage>>(emptyList())
    val queuedMessages: StateFlow<List<QueuedMessage>> = _queuedMessages.asStateFlow()
    val queuedCount: Int get() = _queuedMessages.value.size

    private val _pendingInput = MutableStateFlow<QueuedInput?>(null)
    val pendingInput: StateFlow<QueuedInput?> = _pendingInput.asStateFlow()

    fun clearPendingInput() { _pendingInput.value = null }

    /** User pressed stop — cancel the run and optionally drop queued messages. */
    fun cancelQueued() { _queuedMessages.value = emptyList() }

    /** True when the device screen is on (used to avoid fake disconnects during screen-off). */
    private fun isScreenInteractive(): Boolean {
        return try {
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isInteractive
        } catch (_: Exception) {
            true
        }
    }

    private val _selectedModel = MutableStateFlow(
        AvailableAiModels.find { it.id == prefsRepo.getSelectedModelId() } ?: AvailableAiModels.first()
    )
    val selectedModel: StateFlow<AiModelInfo> = _selectedModel.asStateFlow()

    // Reasoning effort for the current model: none/low/medium/high (server accepts
    // values in {none, minimal, low, medium, high, xhigh, max, ultra}).
    private val _reasoningEffort = MutableStateFlow(prefsRepo.getReasoningEffort())
    val reasoningEffort: StateFlow<String> = _reasoningEffort.asStateFlow()

    fun setReasoningEffort(effort: String) {
        val valid = listOf("none", "low", "medium", "high")
        val normalized = if (effort.lowercase() in valid) effort.lowercase() else "medium"
        _reasoningEffort.value = normalized
        prefsRepo.saveReasoningEffort(normalized)
        HermesAppLog.info("Reasoning effort set to: $normalized")
    }

    private val _activeTab = MutableStateFlow(AppTab.CHAT)
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

    private val _activeTokenUsage = MutableStateFlow(TokenUsage())
    val activeTokenUsage: StateFlow<TokenUsage> = _activeTokenUsage.asStateFlow()

    private val _pinnedSessionIds = MutableStateFlow<Set<String>>(prefsRepo.getPinnedSessionIds())
    val pinnedSessionIds: StateFlow<Set<String>> = _pinnedSessionIds.asStateFlow()

    fun togglePinSession(sessionId: String) {
        _pinnedSessionIds.update { set ->
            val next = if (set.contains(sessionId)) set - sessionId else set + sessionId
            prefsRepo.savePinnedSessionIds(next)
            next
        }
    }

    // Interactive Approval Cards & Control Modes
    private val _activeApprovalRequest = MutableStateFlow<ApprovalRequest?>(null)
    val activeApprovalRequest: StateFlow<ApprovalRequest?> = _activeApprovalRequest.asStateFlow()

    private val _globalAutoApprove = MutableStateFlow(prefsRepo.getGlobalAutoApprove())
    val globalAutoApprove: StateFlow<Boolean> = _globalAutoApprove.asStateFlow()

    private val _sessionAutoApproveIds = MutableStateFlow<Set<String>>(emptySet())
    val sessionAutoApproveIds: StateFlow<Set<String>> = _sessionAutoApproveIds.asStateFlow()

    fun setGlobalAutoApprove(enabled: Boolean) {
        _globalAutoApprove.value = enabled
        prefsRepo.saveGlobalAutoApprove(enabled)
        HermesAppLog.info("Global Auto-Approve set to: $enabled")
    }

    fun isSessionAutoApproved(sessionId: String?): Boolean {
        if (_globalAutoApprove.value) return true
        if (sessionId == null) return false
        return _sessionAutoApproveIds.value.contains(sessionId)
    }

    fun toggleSessionAutoApprove(sessionId: String) {
        _sessionAutoApproveIds.update { set ->
            if (set.contains(sessionId)) set - sessionId else set + sessionId
        }
    }

    fun resolveApproval(
        request: ApprovalRequest,
        approved: Boolean,
        mode: ApprovalMode = ApprovalMode.MANUAL
    ) {
        viewModelScope.launch {
            when (mode) {
                ApprovalMode.ALLOW_ALL -> {
                    _globalAutoApprove.value = true
                    prefsRepo.saveGlobalAutoApprove(true)
                    HermesAppLog.info("Activated ALLOW ALL (Global Autonomous Mode)")
                }
                ApprovalMode.ALLOW_SESSION -> {
                    val sid = request.sessionId ?: _currentSessionId.value
                    if (sid != null) {
                        _sessionAutoApproveIds.update { it + sid }
                        HermesAppLog.info("Activated ALLOW SESSION for: $sid")
                    }
                }
                ApprovalMode.MANUAL -> {
                    // Single run decision
                }
            }

            networkClient.submitApproval(
                config = _config.value,
                runId = request.runId,
                approved = approved,
                sessionId = request.sessionId ?: _currentSessionId.value
            )

            val resolutionBadge = if (approved) {
                when (mode) {
                    ApprovalMode.ALLOW_ALL -> "🛡️ [APPROVED: ALLOW ALL] ${request.command}"
                    ApprovalMode.ALLOW_SESSION -> "🛡️ [APPROVED: SESSION] ${request.command}"
                    ApprovalMode.MANUAL -> "✓ [APPROVED] ${request.command}"
                }
            } else {
                "✗ [DENIED BY USER] ${request.command}"
            }
            HermesAppLog.info("Approval resolved: $resolutionBadge")
            _activeApprovalRequest.value = null
        }
    }

    fun triggerMockApproval(
        command: String = "sudo systemctl restart hermes-agent",
        reason: String = "Reload agent configuration and apply system service updates"
    ) {
        _activeApprovalRequest.value = ApprovalRequest(
            runId = "mock_${System.currentTimeMillis()}",
            sessionId = _currentSessionId.value,
            toolName = "terminal",
            command = command,
            reason = reason,
            message = "Hermes Agent requires security approval to execute this command on host PC."
        )
    }

    // In-app logs (viewable from Gateway -> About)
    private val _appLogs = MutableStateFlow<List<HermesAppLog.LogEntry>>(HermesAppLog.entries.value)
    val appLogs: StateFlow<List<HermesAppLog.LogEntry>> = _appLogs.asStateFlow()

    fun refreshLogs() {
        _appLogs.value = HermesAppLog.all()
    }

    fun clearLogs() {
        HermesAppLog.clear()
        _appLogs.value = emptyList()
    }

    private var telemetryPollingJob: Job? = null
    private var healthPollingJob: Job? = null
    private var streamingJob: Job? = null
    private var chatPollingJob: Job? = null
    private var pendingSend: String? = null
    private var pendingAttachments: List<String> = emptyList()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var hasEverConnected = false
    private var manuallyDisconnected = false

    // Holds a partial WakeLock ONLY while a reply is streaming, so the SSE
    // receive coroutine isn't suspended when the user locks the screen.
    private var streamingWakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireStreamingWakeLock() {
        try {
            if (streamingWakeLock?.isHeld == true) return
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val lock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "hermes:stream")
            lock.setReferenceCounted(false)
            lock.acquire(10 * 60 * 1000L) // safety timeout: 10 min max
            streamingWakeLock = lock
            HermesAppLog.info("Streaming wakelock acquired (screen may be off, reply continues)")
        } catch (e: Exception) {
            HermesAppLog.error("Could not acquire wakelock: ${e.message}")
        }
    }

    private fun releaseStreamingWakeLock() {
        try {
            streamingWakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) { }
        streamingWakeLock = null
    }

    init {
        // Start with empty chat (no fake welcome) until a session loads.
        _chatMessages.value = emptyList()
        startTelemetryPolling()
        startChatPolling()
        startHealthPolling()
        registerNetworkCallback()
        // Mirror the shared logger into UI state live — new log lines appear
        // immediately (Gateway -> APP LOGS) with no manual refresh.
        viewModelScope.launch {
            HermesAppLog.entries.collect { updated ->
                _appLogs.value = updated
            }
        }
        // If the user already has a saved config, try connecting automatically.
        if (_config.value.tailscaleIp.isNotBlank()) {
            testPing()
        }
    }

    private fun registerNetworkCallback() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        viewModelScope.launch {
                            if (manuallyDisconnected) {
                                HermesAppLog.info("Network reconnected, but connection was stopped by user; skipping auto-recovery")
                                return@launch
                            }
                            HermesAppLog.info("Network reconnected. Triggering auto-recovery...")
                            if (_config.value.tailscaleIp.isNotBlank()) {
                                testPing()
                                loadSessions()
                            }
                        }
                    }

                    override fun onLost(network: Network) {
                        viewModelScope.launch {
                            HermesAppLog.info("Network connection lost")
                        }
                    }
                }
                cm.registerDefaultNetworkCallback(callback)
                networkCallback = callback
            }
        } catch (e: Exception) {
            HermesAppLog.warn("Could not register default network callback: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            networkCallback?.let {
                val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(it)
            }
        } catch (_: Exception) {}
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

    /**
     * Periodic /health check that maintains connection state.
     * Uses the user-entered config (IP + PORT from the gateway page) on every
     * request. Once a connection has succeeded (hasEverConnected), transient
     * failures never downgrade status or disconnect; the app silently retries
     * indefinitely until manually disconnected by the user. If connection has
     * never succeeded (!hasEverConnected), repeated failures mark ERROR and then DISCONNECTED.
     */
    private fun startHealthPolling() {
        healthPollingJob?.cancel()
        healthPollingJob = viewModelScope.launch {
            var consecutiveFailures = 0
            var wasScreenOff = false
            while (isActive) {
                if (manuallyDisconnected) {
                    delay(15000)
                    continue
                }
                val cfg = _config.value
                if (cfg.tailscaleIp.isBlank()) {
                    delay(5000)
                    continue
                }
                val screenOn = isScreenInteractive()
                if (screenOn && wasScreenOff) {
                    // Screen just came back — ping immediately so the UI reconnects fast.
                    wasScreenOff = false
                    val r = networkClient.ping(cfg)
                    if (r.isSuccess) {
                        hasEverConnected = true
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        consecutiveFailures = 0
                        if (_pingResult.value?.isSuccess != true) loadSessions()
                    }
                    delay(15000)
                    continue
                }
                if (!screenOn) wasScreenOff = true
                // Only actively probe when we're not already streaming (SSE
                // traffic itself is proof of life) and chat is being watched.
                if (_isStreaming.value) {
                    consecutiveFailures = 0
                    hasEverConnected = true
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    delay(15000)
                    continue
                }
                val result = networkClient.ping(cfg)
                val screenStillOn = isScreenInteractive()
                if (result.isSuccess) {
                    hasEverConnected = true
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    consecutiveFailures = 0
                    // If we were in an error state, also refresh data so the UI
                    // is immediately useful again after a reconnect.
                    if (_pingResult.value?.isSuccess != true) {
                        loadSessions()
                    }
                } else {
                    // While the screen is off or turned off mid-ping, don't count failure.
                    if (!screenOn || !screenStillOn) {
                        wasScreenOff = true
                        HermesAppLog.info("Ping failed but screen is off — not counted as disconnect")
                    } else {
                        consecutiveFailures++
                        if (hasEverConnected) {
                            HermesAppLog.info("Gateway unreachable (will retry)")
                        } else {
                            if (consecutiveFailures >= 3) {
                                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                            } else {
                                _connectionStatus.value = ConnectionStatus.ERROR
                            }
                        }
                    }
                }
                delay(15000)
            }
        }
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
                // Poll less frequently when not viewing the Telemetry tab to save battery and bandwidth
                val isTelemetryTab = _activeTab.value == AppTab.TELEMETRY
                val pollDelay = if (isTelemetryTab) 4000L else 20000L

                val result = networkClient.fetchMetrics(_config.value)
                if (result.isSuccess) {
                    result.getOrNull()?.let { newMetrics ->
                        _telemetry.value = newMetrics
                    }
                    consecutiveFailures = 0
                } else {
                    consecutiveFailures++
                    // Telemetry is best-effort: a failing /api/system (e.g. old
                    // server without the endpoint) must NEVER flip the app to
                    // offline. Connection state is owned by health polling only.
                }
                delay(pollDelay)
            }
        }
    }

    fun setActiveTab(tab: AppTab) {
        val prev = _activeTab.value
        _activeTab.value = tab
        if (tab == AppTab.TELEMETRY && prev != AppTab.TELEMETRY && _config.value.tailscaleIp.isNotBlank()) {
            viewModelScope.launch {
                val result = networkClient.fetchMetrics(_config.value)
                result.getOrNull()?.let { _telemetry.value = it }
            }
        }
    }

    /**
     * Poll the current session for new messages periodically.
     * Keeps the chat live when messages arrive from elsewhere without
     * manual refresh. Skips polling while streaming (SSE already appends
     * live deltas) and while the chat tab is not visible to reduce load.
     * Preserves locally streamed tool executions and attachments so they
     * aren't wiped when server returns plain text turn history.
     */
    private fun startChatPolling() {
        chatPollingJob?.cancel()
        chatPollingJob = viewModelScope.launch {
            while (isActive) {
                val sid = _currentSessionId.value
                val streaming = _isStreaming.value
                val hasConfig = _config.value.tailscaleIp.isNotBlank()
                val chatVisible = _activeTab.value == AppTab.CHAT
                if (sid != null && !streaming && hasConfig && chatVisible) {
                    val result = networkClient.fetchSessionMessages(_config.value, sid)
                    result.onSuccess { msgs ->
                        if (msgs.isNotEmpty()) {
                            val currentMsgs = _chatMessages.value
                            val merged = msgs.map { newMsg ->
                                val existing = currentMsgs.find {
                                    it.id == newMsg.id || (it.sender == newMsg.sender && it.content == newMsg.content)
                                }
                                if (existing != null) {
                                    newMsg.copy(
                                        toolExecutions = if (newMsg.toolExecutions.isEmpty()) existing.toolExecutions else newMsg.toolExecutions,
                                        attachments = if (newMsg.attachments.isEmpty()) existing.attachments else newMsg.attachments,
                                        // Thinking never comes back from the server (SSE-only), so
                                        // keep the locally streamed reasoning, else the 8s poll
                                        // wipes it once the reply finishes.
                                        thinkingContent = if (existing.thinkingContent.isNotBlank()) existing.thinkingContent else newMsg.thinkingContent,
                                        thinkingDone = existing.thinkingDone || newMsg.thinkingDone
                                    )
                                } else {
                                    newMsg
                                }
                            }
                            if (merged != currentMsgs) {
                                _chatMessages.value = merged
                            }
                        }
                    }
                }
                delay(8000)
            }
        }
    }

    fun updateConnectionConfig(newConfig: ConnectionConfig) {
        _config.value = newConfig
        prefsRepo.saveConnectionConfig(newConfig)
        testPing()
        startHealthPolling()
        startTelemetryPolling()
        startChatPolling()
    }

    // ---- Named connection profiles ----

    fun getSavedProfileNames(): List<String> = prefsRepo.getSavedProfileNames()

    fun saveCurrentAsProfile(name: String) {
        if (name.isBlank()) return
        prefsRepo.saveProfile(name.trim(), _config.value)
        prefsRepo.setActiveProfileName(name.trim())
        HermesAppLog.info("Saved connection profile: ${name.trim()}")
    }

    fun loadProfile(name: String) {
        val cfg = prefsRepo.getProfileConfig(name) ?: return
        prefsRepo.setActiveProfileName(name)
        updateConnectionConfig(cfg)
        HermesAppLog.info("Loaded connection profile: $name")
    }

    fun deleteProfile(name: String) {
        prefsRepo.deleteProfile(name)
        if (prefsRepo.getActiveProfileName() == name) {
            prefsRepo.setActiveProfileName("")
        }
        HermesAppLog.info("Deleted connection profile: $name")
    }

    fun getActiveProfileName(): String = prefsRepo.getActiveProfileName()

    fun connectToSaved() {
        if (_config.value.tailscaleIp.isNotBlank()) {
            testPing()
        }
    }

    fun disconnectManual() {
        manuallyDisconnected = true
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        HermesAppLog.info("Connection stopped by user")
    }

    fun forgetDevice() {
        prefsRepo.clearConnectionConfig()
        if (prefsRepo.getActiveProfileName().isNotBlank()) {
            prefsRepo.clearActiveProfile()
        }
        _config.value = prefsRepo.getConnectionConfig()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        hasEverConnected = false
        manuallyDisconnected = false
        HermesAppLog.info("Device removed")
    }

    fun testPing() {
        manuallyDisconnected = false
        viewModelScope.launch {
            _isPinging.value = true
            _connectionStatus.value = ConnectionStatus.CONNECTING
            HermesAppLog.info("Testing connection to ${_config.value.effectiveGatewayUrl}...")
            val currentConfig = _config.value
            if (currentConfig.tailscaleIp.isBlank()) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                HermesAppLog.warn("No gateway IP configured yet")
                _isPinging.value = false
                return@launch
            }
            val res = networkClient.ping(currentConfig)
            if (res.isSuccess) {
                hasEverConnected = true
                _connectionStatus.value = ConnectionStatus.CONNECTED
                HermesAppLog.info("Connected: ${res.message}")
                loadSessions()
                refreshModels()
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
                HermesAppLog.error("Connection failed: ${res.message}")
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
                // Preserve locally-bumped activity times (a session the user just
                // messaged in must stay at the top even when the server list refreshes).
                val prev = _sessions.value
                val merged = list.map { fresh ->
                    val old = prev.find { it.id == fresh.id }
                    when {
                        old == null -> fresh
                        // Server reports more messages than we last saw → external
                        // activity (Telegram reply, another gateway) → treat as new activity.
                        fresh.messageCount > old.messageCount ->
                            fresh.copy(lastActiveAt = System.currentTimeMillis())
                        // Keep our local bump (we messaged in it recently).
                        old.lastActiveAt > fresh.lastActiveAt ->
                            fresh.copy(lastActiveAt = old.lastActiveAt)
                        else -> fresh
                    }
                }
                val sorted = merged.sortedByDescending { it.lastActiveAt.takeIf { t -> t > 0 } ?: it.startedAt }
                _sessions.value = sorted
                val active = sorted.find { it.id == _currentSessionId.value }
                if (active != null) {
                    _activeTokenUsage.value = active.toTokenUsage()
                } else if (sorted.isNotEmpty()) {
                    _activeTokenUsage.value = sorted.first().toTokenUsage()
                }
                if (_currentSessionId.value == null && sorted.isNotEmpty()) {
                    selectSession(sorted.first().id)
                }
            }
            _isLoadingSessions.value = false
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            networkClient.deleteSession(_config.value, sessionId).onSuccess {
                _sessions.update { list -> list.filter { it.id != sessionId } }
                if (_currentSessionId.value == sessionId) {
                    val next = _sessions.value.firstOrNull()
                    if (next != null) {
                        selectSession(next.id)
                    } else {
                        createNewSession()
                    }
                }
            }
        }
    }

    fun exportSessionAsMarkdown(sessionId: String, title: String, context: android.content.Context) {
        viewModelScope.launch {
            val result = networkClient.fetchSessionMessages(_config.value, sessionId)
            val msgs = result.getOrNull() ?: _chatMessages.value
            val sb = StringBuilder("# Hermes Session: $title\n")
            sb.append("ID: $sessionId\n")
            sb.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())}\n\n---\n\n")

            msgs.forEach { m ->
                val senderName = if (m.sender == MessageSender.USER) "### 👤 User" else "### 🤖 Hermes"
                sb.append("$senderName\n")
                sb.append("${m.content}\n\n")
                if (m.toolExecutions.isNotEmpty()) {
                    m.toolExecutions.forEach { t ->
                        sb.append("```bash\n# [TOOL: ${t.toolName}]\n$ ${t.command}\n${t.output}\n```\n\n")
                    }
                }
            }

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Hermes Session: $title")
                putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = android.content.Intent.createChooser(shareIntent, "Share Hermes Session")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        HermesAppLog.info("Opened session: ${sessionId.take(20)}...")
        // Show session's model and tokens in the picker / bar
        val s = _sessions.value.find { it.id == sessionId }
        if (s != null) {
            _activeTokenUsage.value = s.toTokenUsage()
        }
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
                    val currentMsgs = _chatMessages.value
                    val merged = msgs.map { newMsg ->
                        val existing = currentMsgs.find {
                            it.id == newMsg.id || (it.sender == newMsg.sender && it.content == newMsg.content)
                        }
                        if (existing != null) {
                            newMsg.copy(
                                toolExecutions = if (newMsg.toolExecutions.isEmpty()) existing.toolExecutions else newMsg.toolExecutions,
                                attachments = if (newMsg.attachments.isEmpty()) existing.attachments else newMsg.attachments
                            )
                        } else {
                            newMsg
                        }
                    }
                    _chatMessages.value = merged
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

    suspend fun uploadFile(displayName: String, bytes: ByteArray): Result<Pair<String, String>> {
        return networkClient.uploadFile(_config.value, displayName, bytes)
    }

    fun createNewSession(title: String? = null, thenSend: String? = null) {
        viewModelScope.launch {
            val result = networkClient.createNewSession(_config.value, title, _selectedModel.value.id)
            result.onSuccess { newSession ->
                HermesAppLog.info("Created new session: ${newSession.id.take(20)}...")
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

    /**
     * Move a session to the top of the drawer list by bumping its
     * lastActiveAt. Called whenever the user sends a message in it or a
     * reply/tool activity arrives for it.
     */
    private fun bumpSessionActivity(sessionId: String?) {
        val sid = sessionId ?: _currentSessionId.value ?: return
        val now = System.currentTimeMillis()
        _sessions.update { list ->
            list.map { s ->
                if (s.id == sid) s.copy(lastActiveAt = now) else s
            }
        }
    }

    fun sendMessage(prompt: String, attachments: List<String> = emptyList()) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() && attachments.isEmpty()) return

        // If no session yet, create one and queue the message to send after.
        if (_currentSessionId.value == null) {
            pendingSend = trimmed
            pendingAttachments = attachments
            createNewSession()
            return
        }

        if (_isStreaming.value) {
            // User pressed Send while a reply is in progress → stop the current
            // reply and send this message immediately (direct-send behavior).
            HermesAppLog.info("Streaming active — interrupting current run to send (${trimmed.take(40)})")
            sendMessageInterrupt(trimmed, attachments)
            return
        }

        doSendMessage(trimmed, attachments)
    }

    /** While streaming: stop the current run and send this message immediately. */
    fun sendMessageInterrupt(prompt: String, attachments: List<String> = emptyList()) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() && attachments.isEmpty()) return
        stopStreaming(clearQueue = true)
        if (_currentSessionId.value == null) {
            pendingSend = trimmed
            pendingAttachments = attachments
            createNewSession()
            return
        }
        doSendMessage(trimmed, attachments)
    }

    /** While streaming: push this message into the queue; it auto-sends when the current run finishes. */
    fun sendQueuedMessage(prompt: String, attachments: List<String> = emptyList()) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() && attachments.isEmpty()) return
        if (!_isStreaming.value) {
            // Nothing is running — send straight away.
            sendMessage(trimmed, attachments)
            return
        }
        val sid = _currentSessionId.value
        _queuedMessages.update { list ->
            list + QueuedMessage(trimmed, attachments, sid, System.currentTimeMillis())
        }
        HermesAppLog.info("Queued message (${trimmed.take(40)}) — will send when current run finishes")
    }

    private fun doSendMessage(trimmed: String, attachments: List<String> = emptyList()) {
        // This session just became active — bump it to the top of the drawer.
        bumpSessionActivity(_currentSessionId.value)

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
        acquireStreamingWakeLock()
        HermesAppLog.info("Sending to session ${_currentSessionId.value} [${_selectedModel.value.id}]${if (attachments.isNotEmpty()) " + ${attachments.size} attachment(s)" else ""}")

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            var attempt = 0
            var receivedAnyContent = false
            while (attempt < 2) {
                attempt++
                receivedAnyContent = false
                try {
                    val streamFlow = networkClient.streamChat(
                        _config.value,
                        trimmed,
                        _selectedModel.value.id,
                        _currentSessionId.value,
                        attachments,
                        _reasoningEffort.value
                    )

                    streamFlow.collect { chunk ->
                        when (chunk) {
                            is StreamChunk.TextDelta -> {
                                // Live reply arriving — keep this session on top.
                                receivedAnyContent = true
                                bumpSessionActivity(_currentSessionId.value)
                                _chatMessages.update { list ->
                                    list.map { msg ->
                                        if (msg.id == agentMessageId) {
                                            msg.copy(content = msg.content + chunk.text)
                                        } else msg
                                    }
                                }
                            }
                            is StreamChunk.ThinkingDelta -> {
                                // Hidden reasoning — accumulate it on the message so the
                                // UI can show it dimmed while streaming, then collapse.
                                bumpSessionActivity(_currentSessionId.value)
                                _chatMessages.update { list ->
                                    list.map { msg ->
                                        if (msg.id == agentMessageId) {
                                            msg.copy(thinkingContent = msg.thinkingContent + chunk.text)
                                        } else msg
                                    }
                                }
                            }
                            is StreamChunk.ThinkingDone -> {
                                // Real reply started — the thinking phase is over.
                                _chatMessages.update { list ->
                                    list.map { msg ->
                                        if (msg.id == agentMessageId) {
                                            msg.copy(thinkingDone = true)
                                        } else msg
                                    }
                                }
                            }
                            is StreamChunk.ToolStart -> {
                                // Tool activity started (terminal/web/etc) — bump too.
                                receivedAnyContent = true
                                bumpSessionActivity(_currentSessionId.value)
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
                            is StreamChunk.ApprovalNeeded -> {
                                val req = chunk.request
                                val autoApprove = _globalAutoApprove.value || (_currentSessionId.value != null && _sessionAutoApproveIds.value.contains(_currentSessionId.value))
                                if (autoApprove) {
                                    HermesAppLog.info("Auto-approving run ${req.runId} (${req.command})")
                                    viewModelScope.launch {
                                        networkClient.submitApproval(
                                            _config.value,
                                            req.runId,
                                            approved = true,
                                            sessionId = _currentSessionId.value
                                        )
                                    }
                                } else {
                                    HermesAppLog.info("Interactive approval requested: ${req.command}")
                                    _activeApprovalRequest.value = req
                                }
                            }
                            is StreamChunk.Usage -> {
                                _activeTokenUsage.value = TokenUsage(
                                    inputTokens = chunk.inputTokens,
                                    outputTokens = chunk.outputTokens,
                                    totalTokens = chunk.totalTokens
                                )
                                val sid = _currentSessionId.value
                                if (sid != null) {
                                    bumpSessionActivity(sid)
                                    _sessions.update { list ->
                                        list.map { s ->
                                            if (s.id == sid) {
                                                s.copy(
                                                    inputTokens = chunk.inputTokens,
                                                    outputTokens = chunk.outputTokens
                                                )
                                            } else s
                                        }
                                    }
                                }
                            }
                            is StreamChunk.Error -> {
                                HermesAppLog.error("Stream error: ${chunk.message}")
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
                                HermesAppLog.info("Stream completed")
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
                    break // completed without exception
                } catch (e: Exception) {
                    if (attempt >= 2 || receivedAnyContent) {
                        // Give up (or partial content already shown — don't duplicate).
                        HermesAppLog.error("Stream failed after attempt $attempt: ${e.message}")
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == agentMessageId) {
                                    msg.copy(
                                        content = msg.content.ifBlank {
                                            "⚠️ Connection dropped mid-reply. Tap send to retry."
                                        },
                                        isStreaming = false
                                    )
                                } else msg
                            }
                        }
                    } else {
                        // Failed BEFORE any content arrived — safe to auto-retry once.
                        HermesAppLog.warn("Stream attempt $attempt failed before content (${e.message}); retrying…")
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == agentMessageId) msg.copy(content = "") else msg
                            }
                        }
                    }
                }
            }
            _isStreaming.value = false
            releaseStreamingWakeLock()

            // Auto-send anything queued while the previous run was in progress.
            val queued = _queuedMessages.value
            if (queued.isNotEmpty()) {
                _queuedMessages.value = emptyList()
                val next = queued.first()
                if (next.sessionId == null || next.sessionId == _currentSessionId.value) {
                    HermesAppLog.info("Draining queue: sending queued message (${next.prompt.take(40)})")
                    doSendMessage(next.prompt, next.attachments)
                } else {
                    HermesAppLog.info("Queue item targets another session (${next.sessionId}); leaving it.")
                }
            }
        }
    }

    fun stopStreaming(clearQueue: Boolean = false) {
        if (clearQueue) _queuedMessages.value = emptyList()
        streamingJob?.cancel()
        _isStreaming.value = false
        releaseStreamingWakeLock()
        _chatMessages.update { list ->
            list.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }
        }
    }
}

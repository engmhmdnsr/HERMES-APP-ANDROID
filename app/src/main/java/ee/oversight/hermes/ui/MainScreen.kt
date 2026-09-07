package ee.oversight.hermes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import ee.oversight.hermes.ui.components.BiometricLockGate
import ee.oversight.hermes.ui.components.SessionsDrawerContent
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.ui.components.CyberpunkTopBar
import ee.oversight.hermes.ui.screens.ChatTerminalScreen
import ee.oversight.hermes.ui.screens.GatewayConfigScreen
import ee.oversight.hermes.ui.screens.HermesTerminalScreen
import ee.oversight.hermes.ui.screens.JobsScreen
import ee.oversight.hermes.ui.screens.SystemMonitoringScreen
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurface
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary
import ee.oversight.hermes.ui.theme.TextTerminal

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: HermesViewModel? = null
) {
    // Application-scoped VM: survives activity destruction / backgrounding so a
    // streaming reply keeps going when the user leaves the chat.
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ee.oversight.hermes.HermesApp
    val vm = viewModel ?: app.viewModel
    val config by vm.config.collectAsState()
    val status by vm.connectionStatus.collectAsState()
    val telemetry by vm.telemetry.collectAsState()
    val isSystemSupported by vm.telemetrySupported.collectAsState()
    val gatewayHealth by vm.gatewayHealth.collectAsState()
    val chatMessages by vm.chatMessages.collectAsState()
    val isStreaming by vm.isStreaming.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val reasoningEffort by vm.reasoningEffort.collectAsState()
    val activeTab by vm.activeTab.collectAsState()
    val pingResult by vm.pingResult.collectAsState()
    val isPinging by vm.isPinging.collectAsState()
    val language by vm.appLanguage.collectAsState()
    val discoveredGateway by vm.discoveredGateway.collectAsState()
    val isDiscovering by vm.isDiscovering.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val currentSessionId by vm.currentSessionId.collectAsState()
    val availableModels by vm.availableModels.collectAsState()
    val isLoadingSessions by vm.isLoadingSessions.collectAsState()
    val appLogs by vm.appLogs.collectAsState()
    val tokenUsage by vm.activeTokenUsage.collectAsState()
    val pinnedSessionIds by vm.pinnedSessionIds.collectAsState()
    val activeApprovalRequest by vm.activeApprovalRequest.collectAsState()
    val globalAutoApprove by vm.globalAutoApprove.collectAsState()
    val sessionAutoApproveIds by vm.sessionAutoApproveIds.collectAsState()
    val queuedMessages by vm.queuedMessages.collectAsState()
    val needsBiometricUnlock by vm.needsBiometricUnlock.collectAsState()
    val biometricLockEnabled by vm.biometricLockEnabled.collectAsState()
    val isSessionAutoApproved = currentSessionId != null && sessionAutoApproveIds.contains(currentSessionId)
    val currentSessionCost = sessions.find { it.id == currentSessionId }?.costUsd ?: 0.0
    val jobs by vm.jobs.collectAsState()
    val isLoadingJobs by vm.isLoadingJobs.collectAsState()
    val sessionsHasMore by vm.sessionsHasMore.collectAsState()
    val isLoadingMoreSessions by vm.isLoadingMoreSessions.collectAsState()

    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    val context = androidx.compose.ui.platform.LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible

    // A notification tap requested a specific session to open
    // ("open_session" extra set by MainActivity). Consume it once.
    val pendingOpenSession by app.pendingOpenSession.collectAsState()
    LaunchedEffect(pendingOpenSession) {
        val sid = pendingOpenSession
        if (sid != null) {
            vm.selectSession(sid)
            vm.setActiveTab(AppTab.CHAT)
            app.pendingOpenSession.value = null
        }
    }

    // Notification "Approve" tap: after the biometric gate clears, resolve the
    // approval. If app-lock is off the gate is already open, so this runs
    // immediately; if it was on, the unlock flips needsBiometricUnlock and
    // re-triggers this effect.
    val pendingApprovalRunId by app.pendingApprovalRunId.collectAsState()
    LaunchedEffect(pendingApprovalRunId, needsBiometricUnlock) {
        val runId = pendingApprovalRunId
        if (runId != null && !needsBiometricUnlock) {
            vm.resolveApprovalFromNotification(runId, approved = true, sessionId = app.pendingApprovalSessionId.value)
            app.pendingApprovalRunId.value = null
            app.pendingApprovalSessionId.value = null
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0A0D15),
                    drawerContentColor = TextPrimary,
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    SessionsDrawerContent(
                        sessions = sessions,
                        currentSessionId = currentSessionId,
                        isLoading = isLoadingSessions,
                        language = language,
                        pinnedSessionIds = pinnedSessionIds,
                        onTogglePinSession = { id -> vm.togglePinSession(id) },
                        onSelectSession = { id ->
                            vm.selectSession(id)
                            scope.launch { drawerState.close() }
                        },
                        onCreateNewSession = {
                            vm.createNewSession()
                            scope.launch { drawerState.close() }
                        },
                        onDeleteSession = { id ->
                            vm.deleteSession(id)
                        },
                        onExportSession = { id, title ->
                            vm.exportSessionAsMarkdown(id, title, context)
                        },
                        onRenameSession = { id, newTitle ->
                            vm.renameSession(id, newTitle)
                        },
                        onForkSession = { id ->
                            vm.forkSession(id)
                            scope.launch { drawerState.close() }
                        },
                        hasMoreSessions = sessionsHasMore,
                        isLoadingMoreSessions = isLoadingMoreSessions,
                        onLoadMoreSessions = { vm.loadMoreSessions() },
                        onRefreshSessions = {
                            vm.loadSessions()
                        },
                        onClose = {
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = CyberBg,
                topBar = {
                    CyberpunkTopBar(
                        status = status,
                        config = config,
                        pingMs = telemetry.pingMs,
                        tokenUsage = tokenUsage,
                        sessionCostUsd = currentSessionCost,
                        language = language,
                        onOpenDrawer = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        globalAutoApprove = globalAutoApprove,
                        isSessionAutoApproved = isSessionAutoApproved,
                        onToggleGlobalAutoApprove = { enabled ->
                            vm.setGlobalAutoApprove(enabled)
                        },
                        onTriggerTestApproval = {
                            vm.triggerMockApproval()
                        },
                        onToggleConnection = { on ->
                            if (on) {
                                vm.connectToSaved()
                            } else {
                                vm.disconnectManual()
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isImeVisible) {
                        NavigationBar(
                            containerColor = CyberSurface,
                            modifier = Modifier
                                .border(width = 1.dp, color = CyberSurfaceBorder)
                                .testTag("main_navigation_bar")
                        ) {
                    // Tab 1: Chat
                    val isChat = activeTab == AppTab.CHAT
                    NavigationBarItem(
                        selected = isChat,
                        onClick = { vm.setActiveTab(AppTab.CHAT) },
                        icon = {
                            Icon(
                                imageVector = if (isChat) Icons.Filled.Chat else Icons.Outlined.Chat,
                                contentDescription = HermesStrings.tabChat(language),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = HermesStrings.tabChat(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isChat) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonViolet,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NeonViolet.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_chat")
                    )

                    // Tab 2: Terminal (Between Chat and Telemetry)
                    val isTerminal = activeTab == AppTab.TERMINAL
                    NavigationBarItem(
                        selected = isTerminal,
                        onClick = { vm.setActiveTab(AppTab.TERMINAL) },
                        icon = {
                            Icon(
                                imageVector = if (isTerminal) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                                contentDescription = HermesStrings.tabTerminal(language),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = HermesStrings.tabTerminal(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isTerminal) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextTerminal,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = TextTerminal.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_terminal")
                    )

                    // Tab 3: System Telemetry
                    val isTelemetry = activeTab == AppTab.TELEMETRY
                    NavigationBarItem(
                        selected = isTelemetry,
                        onClick = { vm.setActiveTab(AppTab.TELEMETRY) },
                        icon = {
                            Icon(
                                imageVector = if (isTelemetry) Icons.Filled.MonitorHeart else Icons.Outlined.MonitorHeart,
                                contentDescription = HermesStrings.tabTelemetry(language),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = HermesStrings.tabTelemetry(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isTelemetry) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NeonCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_telemetry")
                    )

                    // Tab 3.5: Scheduled Jobs
                    val isJobs = activeTab == AppTab.JOBS
                    NavigationBarItem(
                        selected = isJobs,
                        onClick = { vm.setActiveTab(AppTab.JOBS); vm.loadJobs() },
                        icon = {
                            Icon(
                                imageVector = if (isJobs) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                                contentDescription = HermesStrings.tabJobs(language),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = HermesStrings.tabJobs(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isJobs) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonAmber,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NeonAmber.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_jobs")
                    )

                    // Tab 4: Gateway & Tailscale
                    val isGateway = activeTab == AppTab.GATEWAY
                    NavigationBarItem(
                        selected = isGateway,
                        onClick = { vm.setActiveTab(AppTab.GATEWAY) },
                        icon = {
                            Icon(
                                imageVector = if (isGateway) Icons.Filled.Lan else Icons.Outlined.Lan,
                                contentDescription = HermesStrings.tabGateway(language),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = HermesStrings.tabGateway(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isGateway) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonViolet,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NeonViolet.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_gateway")
                    )
                }
            }
        }
    ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    AppTab.CHAT -> {
                        ChatTerminalScreen(
                            messages = chatMessages,
                            isStreaming = isStreaming,
                            selectedModel = selectedModel,
                            availableModels = availableModels,
                            sessions = sessions,
                            currentSessionId = currentSessionId,
                            isLoadingSessions = isLoadingSessions,
                            config = config,
                            language = language,
                            onSelectModel = { vm.selectModel(it) },
                            onSelectSession = { vm.selectSession(it) },
                            onCreateNewSession = { vm.createNewSession() },
                            onRefreshSessions = { vm.loadSessions() },
                            onSendMessage = { text, attachments -> vm.sendMessage(text, attachments) },
                            onStopStreaming = { vm.stopStreaming() },
                            onQueueMessage = { text, attachments -> vm.sendQueuedMessage(text, attachments) },
                            reasoningEffort = reasoningEffort,
                            onEffortSelected = { vm.setReasoningEffort(it) },
                            activeApprovalRequest = activeApprovalRequest,
                            onResolveApproval = { req, approved, mode ->
                                vm.resolveApproval(req, approved, mode)
                            },
                            queuedMessageCount = queuedMessages.size,
                            onCancelQueued = { vm.cancelQueued() },
                            onGoToSettings = { vm.setActiveTab(AppTab.GATEWAY) }
                        )
                    }
                    AppTab.TERMINAL -> {
                        HermesTerminalScreen(
                            viewModel = vm,
                            config = config,
                            status = status,
                            telemetry = telemetry,
                            language = language,
                            currentSessionId = currentSessionId
                        )
                    }
                    AppTab.TELEMETRY -> {
                        SystemMonitoringScreen(
                            telemetry = telemetry,
                            gatewayHealth = gatewayHealth,
                            config = config,
                            language = language,
                            isSupported = isSystemSupported,
                            onRefresh = { vm.testPing() }
                        )
                    }
                    AppTab.GATEWAY -> {
                        GatewayConfigScreen(
                            config = config,
                            connectionStatus = status,
                            pingResult = pingResult,
                            isPinging = isPinging,
                            language = language,
                            logs = appLogs,
                            onRefreshLogs = { vm.refreshLogs() },
                            onClearLogs = { vm.clearLogs() },
                            savedProfiles = vm.getSavedProfileNames(),
                            activeProfile = vm.getActiveProfileName(),
                            onSaveProfile = { name -> vm.saveCurrentAsProfile(name) },
                            onLoadProfile = { name ->
                                vm.loadProfile(name)
                                vm.connectToSaved()
                            },
                            onDeleteProfile = { name -> vm.deleteProfile(name) },
                            onToggleDeviceConnection = { name, connect ->
                                if (connect) {
                                    vm.loadProfile(name)
                                    vm.connectToSaved()
                                } else {
                                    vm.disconnectManual()
                                }
                            },
                            discoveredGateway = discoveredGateway,
                            isDiscovering = isDiscovering,
                            onLanguageChange = { vm.setAppLanguage(it) },
                            biometricLockEnabled = biometricLockEnabled,
                            onToggleBiometricLock = { vm.setBiometricLockEnabled(it) },
                            encryptionAvailable = vm.encryptionAvailable,
                            onSaveConfig = { vm.updateConnectionConfig(it) },
                            onTestPing = { vm.testPing() },
                            onStartAutoDiscovery = { vm.startAutoDiscovery() },
                            onConnectDiscovered = { discovered, useTailscale -> vm.connectDiscovered(discovered, useTailscale) }
                        )
                    }
                    AppTab.JOBS -> {
                        JobsScreen(
                            jobs = jobs,
                            isLoading = isLoadingJobs,
                            language = language,
                            onRefresh = { vm.loadJobs() },
                            onCreateJob = { name, schedule, prompt -> vm.createJob(name, schedule, prompt) },
                            onJobAction = { jobId, action -> vm.jobAction(jobId, action) },
                            onDeleteJob = { jobId -> vm.deleteJob(jobId) }
                        )
                    }
                }
            }
        }
        // Biometric lock gate overlays everything when app lock is enabled.
        if (needsBiometricUnlock) {
            BiometricLockGate(onUnlocked = { vm.onBiometricUnlocked() })
        }
        } // end Box
        } // end CompositionLocalProvider
    }
}

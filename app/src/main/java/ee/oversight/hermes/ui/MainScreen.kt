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
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.MonitorHeart
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.ui.components.CyberpunkTopBar
import ee.oversight.hermes.ui.screens.ChatTerminalScreen
import ee.oversight.hermes.ui.screens.GatewayConfigScreen
import ee.oversight.hermes.ui.screens.HermesTerminalScreen
import ee.oversight.hermes.ui.screens.SystemMonitoringScreen
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurface
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary
import ee.oversight.hermes.ui.theme.TextTerminal

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: HermesViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val pingResult by viewModel.pingResult.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val discoveredGateway by viewModel.discoveredGateway.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoadingSessions by viewModel.isLoadingSessions.collectAsState()
    val appLogs by viewModel.appLogs.collectAsState()
    val tokenUsage by viewModel.activeTokenUsage.collectAsState()
    val pinnedSessionIds by viewModel.pinnedSessionIds.collectAsState()
    val activeApprovalRequest by viewModel.activeApprovalRequest.collectAsState()
    val globalAutoApprove by viewModel.globalAutoApprove.collectAsState()
    val sessionAutoApproveIds by viewModel.sessionAutoApproveIds.collectAsState()
    val isSessionAutoApproved = currentSessionId != null && sessionAutoApproveIds.contains(currentSessionId)

    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    val context = androidx.compose.ui.platform.LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
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
                        onTogglePinSession = { id -> viewModel.togglePinSession(id) },
                        onSelectSession = { id ->
                            viewModel.selectSession(id)
                            scope.launch { drawerState.close() }
                        },
                        onCreateNewSession = {
                            viewModel.createNewSession()
                            scope.launch { drawerState.close() }
                        },
                        onDeleteSession = { id ->
                            viewModel.deleteSession(id)
                        },
                        onExportSession = { id, title ->
                            viewModel.exportSessionAsMarkdown(id, title, context)
                        },
                        onRefreshSessions = {
                            viewModel.loadSessions()
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
                        language = language,
                        onOpenDrawer = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        globalAutoApprove = globalAutoApprove,
                        isSessionAutoApproved = isSessionAutoApproved,
                        onToggleGlobalAutoApprove = { enabled ->
                            viewModel.setGlobalAutoApprove(enabled)
                        },
                        onTriggerTestApproval = {
                            viewModel.triggerMockApproval()
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
                        onClick = { viewModel.setActiveTab(AppTab.CHAT) },
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
                        onClick = { viewModel.setActiveTab(AppTab.TERMINAL) },
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
                        onClick = { viewModel.setActiveTab(AppTab.TELEMETRY) },
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

                    // Tab 4: Gateway & Tailscale
                    val isGateway = activeTab == AppTab.GATEWAY
                    NavigationBarItem(
                        selected = isGateway,
                        onClick = { viewModel.setActiveTab(AppTab.GATEWAY) },
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
                            onSelectModel = { viewModel.selectModel(it) },
                            onSelectSession = { viewModel.selectSession(it) },
                            onCreateNewSession = { viewModel.createNewSession() },
                            onRefreshSessions = { viewModel.loadSessions() },
                            onSendMessage = { text, attachments -> viewModel.sendMessage(text, attachments) },
                            onStopStreaming = { viewModel.stopStreaming() },
                            activeApprovalRequest = activeApprovalRequest,
                            onResolveApproval = { req, approved, mode ->
                                viewModel.resolveApproval(req, approved, mode)
                            }
                        )
                    }
                    AppTab.TERMINAL -> {
                        HermesTerminalScreen(
                            viewModel = viewModel,
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
                            config = config,
                            language = language,
                            onRefresh = { viewModel.testPing() }
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
                            onRefreshLogs = { viewModel.refreshLogs() },
                            onClearLogs = { viewModel.clearLogs() },
                            savedProfiles = viewModel.getSavedProfileNames(),
                            activeProfile = viewModel.getActiveProfileName(),
                            onSaveProfile = { name -> viewModel.saveCurrentAsProfile(name) },
                            onLoadProfile = { name -> viewModel.loadProfile(name) },
                            onDeleteProfile = { name -> viewModel.deleteProfile(name) },
                            discoveredGateway = discoveredGateway,
                            isDiscovering = isDiscovering,
                            onLanguageChange = { viewModel.setAppLanguage(it) },
                            onSaveConfig = { viewModel.updateConnectionConfig(it) },
                            onTestPing = { viewModel.testPing() },
                            onStartAutoDiscovery = { viewModel.startAutoDiscovery() },
                            onConnectDiscovered = { discovered, useTailscale -> viewModel.connectDiscovered(discovered, useTailscale) }
                        )
                    }
                }
            }
        }
        }
    }
}

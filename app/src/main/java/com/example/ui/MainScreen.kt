package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppLanguage
import com.example.model.HermesStrings
import com.example.ui.components.CyberpunkTopBar
import com.example.ui.screens.ChatTerminalScreen
import com.example.ui.screens.GatewayConfigScreen
import com.example.ui.screens.SystemMonitoringScreen
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.MonospaceStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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

    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            containerColor = CyberBg,
            topBar = {
                CyberpunkTopBar(
                    status = status,
                    config = config,
                    pingMs = telemetry.pingMs,
                    language = language,
                    onToggleDemoMode = { viewModel.toggleDemoMode(it) },
                    onClearChat = { viewModel.clearChat() }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = CyberSurface,
                    modifier = Modifier
                        .border(width = 1.dp, color = CyberSurfaceBorder)
                        .testTag("main_navigation_bar")
                ) {
                    // Tab 1: Terminal & Chat
                    val isTerminal = activeTab == AppTab.TERMINAL
                    NavigationBarItem(
                        selected = isTerminal,
                        onClick = { viewModel.setActiveTab(AppTab.TERMINAL) },
                        icon = {
                            Icon(
                                imageVector = if (isTerminal) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                                contentDescription = HermesStrings.tabChat(language),
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = HermesStrings.tabChat(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isTerminal) FontWeight.Bold else FontWeight.Normal
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
                        modifier = Modifier.testTag("nav_terminal")
                    )

                    // Tab 2: System Telemetry
                    val isTelemetry = activeTab == AppTab.TELEMETRY
                    NavigationBarItem(
                        selected = isTelemetry,
                        onClick = { viewModel.setActiveTab(AppTab.TELEMETRY) },
                        icon = {
                            Icon(
                                imageVector = if (isTelemetry) Icons.Filled.MonitorHeart else Icons.Outlined.MonitorHeart,
                                contentDescription = HermesStrings.tabTelemetry(language),
                                modifier = Modifier.size(22.dp)
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

                    // Tab 3: Gateway & Tailscale
                    val isGateway = activeTab == AppTab.GATEWAY
                    NavigationBarItem(
                        selected = isGateway,
                        onClick = { viewModel.setActiveTab(AppTab.GATEWAY) },
                        icon = {
                            Icon(
                                imageVector = if (isGateway) Icons.Filled.Lan else Icons.Outlined.Lan,
                                contentDescription = HermesStrings.tabGateway(language),
                                modifier = Modifier.size(22.dp)
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
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    AppTab.TERMINAL -> {
                        ChatTerminalScreen(
                            messages = chatMessages,
                            isStreaming = isStreaming,
                            selectedModel = selectedModel,
                            config = config,
                            language = language,
                            onSelectModel = { viewModel.selectModel(it) },
                            onSendMessage = { viewModel.sendMessage(it) },
                            onStopStreaming = { viewModel.stopStreaming() }
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
                            onLanguageChange = { viewModel.setAppLanguage(it) },
                            onSaveConfig = { viewModel.updateConnectionConfig(it) },
                            onTestPing = { viewModel.testPing() },
                            onToggleDemoMode = { viewModel.toggleDemoMode(it) }
                        )
                    }
                }
            }
        }
    }
}

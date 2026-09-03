package ee.oversight.hermes.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.data.StreamChunk
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.ConnectionStatus
import ee.oversight.hermes.model.HermesSession
import ee.oversight.hermes.model.SystemTelemetry
import ee.oversight.hermes.model.TokenUsage
import ee.oversight.hermes.ui.HermesViewModel
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurface
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.components.InteractiveApprovalCard
import ee.oversight.hermes.ui.theme.CyberTerminalBg
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonGreen
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary
import ee.oversight.hermes.ui.theme.TextTerminal
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TerminalLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRunning: Boolean = false,
    val isError: Boolean = false
)

@Composable
fun HermesTerminalScreen(
    viewModel: HermesViewModel,
    config: ConnectionConfig,
    status: ConnectionStatus,
    telemetry: SystemTelemetry,
    language: AppLanguage,
    currentSessionId: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val sessions by viewModel.sessions.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val activeSession = remember(sessions, currentSessionId) {
        sessions.find { it.id == currentSessionId }
    }
    val tokenUsage by viewModel.activeTokenUsage.collectAsState()
    val activeApprovalRequest by viewModel.activeApprovalRequest.collectAsState()
    val globalAutoApprove by viewModel.globalAutoApprove.collectAsState()
    val sessionAutoApproveIds by viewModel.sessionAutoApproveIds.collectAsState()

    val logEntries = remember {
        mutableStateListOf(
            TerminalLogEntry(
                command = "hermes --version",
                output = "Hermes Agent Terminal Shell v2.4.0 [x86_64-pc]\nHost: ${config.effectiveGatewayUrl}\nStatus: ${status.name} • Ping: ${telemetry.pingMs}ms\nType 'help' for built-in commands or run any shell command."
            )
        )
    }

    val commandHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableIntStateOf(-1) }

    var commandInput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var executionJob by remember { mutableStateOf<Job?>(null) }

    val quickCommands = listOf(
        "hermes status",
        "approval status",
        "approval test",
        "git status",
        "hermes doctor",
        "models",
        "sessions",
        "ping",
        "ls -la",
        "systeminfo",
        "clear"
    )

    fun executeTerminalCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        commandHistory.add(trimmed)
        historyIndex = -1
        commandInput = ""

        when (trimmed.lowercase()) {
            "clear", "cls" -> {
                logEntries.clear()
                return
            }
            "help" -> {
                val helpText = """
                    HERMES TERMINAL BUILT-IN COMMANDS:
                    ----------------------------------
                    • status       - Display live gateway, latency, model & token usage
                    • approval     - Manage security approval mode (status, allow-all, manual, test)
                    • models       - List all available AI models and providers
                    • sessions     - Show list of active Hermes sessions
                    • ping         - Test ping latency to server
                    • clear / cls  - Clear terminal screen
                    • history      - List previously executed commands
                    • help         - Display this help message

                    SHELL & AGENT EXECUTION:
                    ----------------------------------
                    Any other command (e.g. 'git status', 'ls', 'dir', 'python --version')
                    will be dispatched to the Hermes Host Shell and streamed live.
                """.trimIndent()
                logEntries.add(TerminalLogEntry(command = trimmed, output = helpText))
                return
            }
            "approval", "approval status" -> {
                val isSess = currentSessionId != null && sessionAutoApproveIds.contains(currentSessionId)
                val statusText = """
                    [SECURITY APPROVAL CONFIGURATION]
                    • Current Mode   : ${if (globalAutoApprove) "ALLOW ALL (AUTONOMOUS)" else if (isSess) "ALLOW SESSION" else "MANUAL (PROMPT PER COMMAND)"}
                    • Global Auto    : $globalAutoApprove
                    • Session Auto   : $isSess
                    • Active Session : ${currentSessionId ?: "None"}
                    • Pending Request: ${if (activeApprovalRequest != null) "${activeApprovalRequest?.toolName}: ${activeApprovalRequest?.command}" else "None"}

                    Usage:
                    - 'approval allow-all' : Auto-approve all commands (Autonomous)
                    - 'approval manual'    : Require prompt for sensitive commands
                    - 'approval test'      : Pop up interactive approval test card
                """.trimIndent()
                logEntries.add(TerminalLogEntry(command = trimmed, output = statusText))
                return
            }
            "approval allow-all", "approval auto" -> {
                viewModel.setGlobalAutoApprove(true)
                logEntries.add(TerminalLogEntry(command = trimmed, output = "✓ Global Auto-Approve (Autonomous Mode) ENABLED. Sensitive commands will proceed without prompting."))
                return
            }
            "approval manual" -> {
                viewModel.setGlobalAutoApprove(false)
                logEntries.add(TerminalLogEntry(command = trimmed, output = "✓ Switched to MANUAL Approvals. Sensitive commands will prompt with interactive card."))
                return
            }
            "approval test" -> {
                viewModel.triggerMockApproval()
                logEntries.add(TerminalLogEntry(command = trimmed, output = "🧪 Triggered interactive security approval card! Check above the input line."))
                return
            }
            "status", "hermes status" -> {
                val statusText = """
                    [GATEWAY STATUS]
                    • Connection : $status
                    • Endpoint   : ${config.effectiveGatewayUrl}
                    • Tailscale  : ${config.tailscaleIp.ifBlank { "Not configured" }}
                    • Latency    : ${telemetry.pingMs}ms
                    • Active Sess: ${activeSession?.title ?: "None"} (${currentSessionId ?: "No session"})
                    • Total Toks : ${TokenUsage.formatTokenCount(tokenUsage.totalTokens)} (In: ${tokenUsage.inputTokens}, Out: ${tokenUsage.outputTokens})
                    • CPU / RAM  : ${telemetry.cpuUsage.toInt()}% / ${String.format(Locale.US, "%.1f", telemetry.ramUsedGb)}GB (${String.format(Locale.US, "%.1f", telemetry.ramTotalGb)}GB total)
                """.trimIndent()
                logEntries.add(TerminalLogEntry(command = trimmed, output = statusText))
                return
            }
            "models", "hermes models" -> {
                val modelsText = StringBuilder("AVAILABLE MODELS:\n-----------------\n")
                availableModels.forEach { m ->
                    modelsText.append("• [${m.provider}] ${m.displayName} (${m.id})\n")
                }
                logEntries.add(TerminalLogEntry(command = trimmed, output = modelsText.toString().trimEnd()))
                return
            }
            "sessions", "hermes sessions" -> {
                val sessText = StringBuilder("HERMES SESSIONS (${sessions.size}):\n-------------------------\n")
                sessions.take(15).forEach { s ->
                    val isCur = s.id == currentSessionId
                    sessText.append("${if (isCur) "▶ " else "  "}${s.title.take(30)} [${s.messageCount} msgs | ${TokenUsage.formatTokenCount(s.totalTokens)}]\n")
                }
                logEntries.add(TerminalLogEntry(command = trimmed, output = sessText.toString().trimEnd()))
                return
            }
            "ping" -> {
                viewModel.testPing()
                logEntries.add(TerminalLogEntry(command = trimmed, output = "Pinging ${config.effectiveGatewayUrl}... Latency: ${telemetry.pingMs}ms"))
                return
            }
            "history" -> {
                val historyText = commandHistory.mapIndexed { idx, c -> "${idx + 1}: $c" }.joinToString("\n")
                logEntries.add(TerminalLogEntry(command = trimmed, output = historyText.ifBlank { "No command history yet." }))
                return
            }
        }

        // Execute remote command on Hermes Host
        val entry = TerminalLogEntry(
            command = trimmed,
            output = "Executing on host...",
            isRunning = true
        )
        val entryIndex = logEntries.size
        logEntries.add(entry)
        isExecuting = true

        executionJob?.cancel()
        executionJob = scope.launch {
            try {
                // Instruction prompt to run shell command directly
                val prompt = "Execute this command in the terminal tool and output raw output: $trimmed"
                val stream = ee.oversight.hermes.data.HermesNetworkClient().streamChat(
                    config = config,
                    prompt = prompt,
                    model = viewModel.selectedModel.value.id,
                    sessionId = currentSessionId
                )

                var accumulatedOutput = ""
                stream.collect { chunk ->
                    when (chunk) {
                        is StreamChunk.TextDelta -> {
                            accumulatedOutput += chunk.text
                            if (entryIndex < logEntries.size) {
                                logEntries[entryIndex] = logEntries[entryIndex].copy(
                                    output = accumulatedOutput.ifBlank { "Executing..." },
                                    isRunning = true
                                )
                            }
                        }
                        is StreamChunk.ToolOutput -> {
                            accumulatedOutput += "\n[Tool ${chunk.toolId}]: ${chunk.output}\n"
                            if (entryIndex < logEntries.size) {
                                logEntries[entryIndex] = logEntries[entryIndex].copy(
                                    output = accumulatedOutput,
                                    isRunning = true
                                )
                            }
                        }
                        is StreamChunk.Error -> {
                            accumulatedOutput += "\n⚠️ Error: ${chunk.message}"
                            if (entryIndex < logEntries.size) {
                                logEntries[entryIndex] = logEntries[entryIndex].copy(
                                    output = accumulatedOutput,
                                    isRunning = false,
                                    isError = true
                                )
                            }
                        }
                        is StreamChunk.Done -> {
                            if (entryIndex < logEntries.size) {
                                logEntries[entryIndex] = logEntries[entryIndex].copy(
                                    output = accumulatedOutput.ifBlank { "Command completed with no output." },
                                    isRunning = false
                                )
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                if (entryIndex < logEntries.size) {
                    logEntries[entryIndex] = logEntries[entryIndex].copy(
                        output = "Failed to execute: ${e.localizedMessage}",
                        isRunning = false,
                        isError = true
                    )
                }
            } finally {
                isExecuting = false
            }
        }
    }

    // Auto-scroll to bottom on new output
    LaunchedEffect(logEntries.size, logEntries.lastOrNull()?.output) {
        if (logEntries.isNotEmpty()) {
            listState.animateScrollToItem(logEntries.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        // Terminal Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B12))
                .border(1.dp, CyberSurfaceBorder)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (status == ConnectionStatus.CONNECTED) NeonGreen else NeonRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = TextTerminal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HERMES TERMINAL",
                    style = MonospaceStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTerminal
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${telemetry.pingMs}ms",
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        color = NeonCyan
                    )
                )
            }

            // Quick actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isExecuting) {
                    IconButton(
                        onClick = {
                            executionJob?.cancel()
                            isExecuting = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val allText = logEntries.joinToString("\n\n") { "hermes:~$ ${it.command}\n${it.output}" }
                        clipboardManager.setText(AnnotatedString(allText))
                        Toast.makeText(
                            context,
                            if (language == AppLanguage.AR) "تم نسخ مخرجات الطرفية" else "Terminal logs copied",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Output",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { logEntries.clear() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Terminal Output Console Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CyberTerminalBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(logEntries, key = { it.id }) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090E18))
                                .border(1.dp, Color(0xFF161F30), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            // Command Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "hermes:~$ ",
                                        style = MonospaceStyle.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextTerminal
                                        )
                                    )
                                    Text(
                                        text = entry.command,
                                        style = MonospaceStyle.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (entry.isRunning) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = NeonCyan,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
                                    Text(
                                        text = timeStr,
                                        style = MonospaceStyle.copy(fontSize = 9.sp, color = TextSecondary)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = Color(0xFF141C2B), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            // Output Text
                            Text(
                                text = entry.output,
                                style = MonospaceStyle.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (entry.isError) NeonRed else Color(0xFFD1D5DB),
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Interactive Approval Card (prominently shown if pending approval)
        if (activeApprovalRequest != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                InteractiveApprovalCard(
                    request = activeApprovalRequest!!,
                    language = language,
                    onResolve = { req, approved, mode ->
                        viewModel.resolveApproval(req, approved, mode)
                    }
                )
            }
        }

        // Quick Command Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0F19))
                .border(1.dp, CyberSurfaceBorder)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickCommands.forEach { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF161E30))
                        .border(1.dp, Color(0xFF23304A), RoundedCornerShape(6.dp))
                        .clickable { executeTerminalCommand(cmd) }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cmd,
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonCyan
                        )
                    )
                }
            }
        }

        // Virtual Developer Key Strip (Ctrl+C, Tab, Esc, |, ~, &&, /, -, $, ;, >)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B13))
                .border(0.8.dp, Color(0xFF1B2436))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ctrl+C Key (Special Interrupt Action)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isExecuting) NeonRed.copy(alpha = 0.25f) else Color(0xFF1E1420))
                    .border(1.dp, if (isExecuting) NeonRed else Color(0xFF4A2535), RoundedCornerShape(5.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isExecuting) {
                            executionJob?.cancel()
                            isExecuting = false
                            logEntries.add(TerminalLogEntry(command = "^C", output = "[Process interrupted by SIGINT / Ctrl+C]", isError = true))
                        } else {
                            commandInput = ""
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Ctrl+C",
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExecuting) NeonRed else Color(0xFFFF7A8A)
                    )
                )
            }

            // Developer Symbol Keys
            val devKeys = listOf("Tab", "Esc", "|", "~", "&&", "/", "\\", "-", "$", ";", ">", "clear")
            devKeys.forEach { key ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF121826))
                        .border(1.dp, Color(0xFF202C42), RoundedCornerShape(5.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (key) {
                                "Tab" -> commandInput += "    "
                                "Esc" -> commandInput = ""
                                "clear" -> logEntries.clear()
                                else -> commandInput += key
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = key,
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (key == "Tab" || key == "Esc") NeonCyan else TextPrimary
                        )
                    )
                }
            }
        }

        // Command Input Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1420))
                .border(1.dp, CyberSurfaceBorder)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prompt Prefix
            Text(
                text = "hermes:~$ ",
                style = MonospaceStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTerminal
                )
            )

            // Input Field
            BasicTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                textStyle = MonospaceStyle.copy(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(TextTerminal),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank() && !isExecuting) {
                            executeTerminalCommand(commandInput)
                        }
                    }
                ),
                decorationBox = { inner ->
                    if (commandInput.isEmpty()) {
                        Text(
                            text = if (language == AppLanguage.AR) "أدخل أمر للتشغيل (e.g. status, git status)..." else "Enter command (e.g. status, git status)...",
                            style = MonospaceStyle.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                    }
                    inner()
                }
            )

            // History Recall Buttons
            if (commandHistory.isNotEmpty()) {
                IconButton(
                    onClick = {
                        if (commandHistory.isNotEmpty()) {
                            if (historyIndex < commandHistory.size - 1) {
                                historyIndex++
                                commandInput = commandHistory[commandHistory.size - 1 - historyIndex]
                            }
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous Command",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (commandHistory.isNotEmpty()) {
                            if (historyIndex > 0) {
                                historyIndex--
                                commandInput = commandHistory[commandHistory.size - 1 - historyIndex]
                            } else if (historyIndex == 0) {
                                historyIndex = -1
                                commandInput = ""
                            }
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next Command",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Run Button
            IconButton(
                onClick = {
                    if (commandInput.isNotBlank() && !isExecuting) {
                        executeTerminalCommand(commandInput)
                    }
                },
                enabled = commandInput.isNotBlank() && !isExecuting,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (commandInput.isNotBlank() && !isExecuting) NeonGreen.copy(alpha = 0.2f) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Run",
                    tint = if (commandInput.isNotBlank() && !isExecuting) NeonGreen else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

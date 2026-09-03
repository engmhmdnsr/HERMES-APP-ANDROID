package ee.oversight.hermes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.model.AiModelInfo
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.AvailableAiModels
import ee.oversight.hermes.model.ChatMessage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.HermesSession
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.model.MessageSender
import ee.oversight.hermes.ui.components.MonospaceToolBlock
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurface
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.CyberSurfaceElevated
import ee.oversight.hermes.ui.theme.CyberTerminalBg
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.NeonVioletLight
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTerminalScreen(
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    selectedModel: AiModelInfo,
    availableModels: List<AiModelInfo>,
    sessions: List<HermesSession>,
    currentSessionId: String?,
    isLoadingSessions: Boolean,
    config: ConnectionConfig,
    language: AppLanguage,
    onSelectModel: (AiModelInfo) -> Unit,
    onSelectSession: (String) -> Unit,
    onCreateNewSession: () -> Unit,
    onRefreshSessions: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    var showSessionsSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Remember which session we already auto-scrolled to bottom for.
    // This prevents re-scrolling to the top every time the tab is reopened.
    var lastScrolledSession by remember { mutableStateOf<String?>(null) }

    // Scroll to bottom when:
    //  1. A new session is selected (first load)
    //  2. New messages arrive while streaming (isStreaming or size grows)
    // NOT on every recomposition / tab re-entry for an already-seen session.
    val lastMsgLen = messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(currentSessionId, messages.size, lastMsgLen, messages.lastOrNull()?.toolExecutions?.size) {
        val sid = currentSessionId
        if (messages.isNotEmpty()) {
            val isNewSession = sid != null && lastScrolledSession != sid
            val isStreamingUpdate = isStreaming || (sid != null && lastScrolledSession == sid && messages.size > 1)
            if (isNewSession) {
                // First open of this session: jump to bottom instantly
                listState.scrollToItem(messages.size - 1)
                lastScrolledSession = sid
            } else if (isStreamingUpdate) {
                // Streaming new content: follow along
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .imePadding()
    ) {
        // Model Selector Bar
        ModelSelectorRow(
            selectedModel = selectedModel,
            availableModels = availableModels,
            language = language,
            onSelectModel = onSelectModel
        )

        // Sessions Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val activeSession = sessions.find { it.id == currentSessionId }
            val activeTitle = activeSession?.title ?: (if (language == AppLanguage.AR) "الجلسات (${sessions.size})" else "Sessions (${sessions.size})")

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { showSessionsSheet = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeTitle,
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // New Session Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonViolet.copy(alpha = 0.2f))
                    .border(1.dp, NeonViolet, RoundedCornerShape(8.dp))
                    .clickable { onCreateNewSession() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = NeonVioletLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.AR) "جديدة" else "New",
                    style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonVioletLight)
                )
            }
        }

        if (showSessionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSessionsSheet = false },
                containerColor = Color(0xFF0C1017),
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.AR) "جلسات هيرمز على الكمبيوتر" else "Hermes Agent Sessions on PC",
                            style = MonospaceStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                        IconButton(onClick = onRefreshSessions) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (sessions.isEmpty()) {
                        Text(
                            text = if (isLoadingSessions) {
                                if (language == AppLanguage.AR) "جاري تحميل الجلسات من السيرفر..." else "Loading sessions..."
                            } else {
                                if (language == AppLanguage.AR) "لا توجد جلسات محفوظة حتى الآن" else "No saved sessions found"
                            },
                            style = MonospaceStyle.copy(color = TextSecondary, fontSize = 12.sp),
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sessions) { s ->
                                val isCurrent = s.id == currentSessionId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) NeonViolet.copy(alpha = 0.25f) else CyberSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isCurrent) NeonViolet else CyberSurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onSelectSession(s.id)
                                            showSessionsSheet = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            style = MonospaceStyle.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isCurrent) NeonCyan else TextPrimary
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Model: ${s.model} • Msgs: ${s.messageCount}",
                                            style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                                        )
                                    }
                                    if (isCurrent) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonVioletLight, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(message = message, language = language)
            }
        }

        // Quick Preset Prompts Row
        QuickPresetPrompts(
            enabled = !isStreaming,
            language = language,
            onPresetClick = { prompt ->
                promptInput = prompt
                onSendMessage(prompt)
                promptInput = ""
            }
        )

        // Bottom Input Bar
        ChatInputBar(
            text = promptInput,
            language = language,
            onTextChange = { promptInput = it },
            isStreaming = isStreaming,
            onSend = {
                if (promptInput.isNotBlank()) {
                    onSendMessage(promptInput)
                    promptInput = ""
                }
            },
            onStop = onStopStreaming
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorRow(
    selectedModel: AiModelInfo,
    availableModels: List<AiModelInfo>,
    language: AppLanguage,
    onSelectModel: (AiModelInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val models = if (availableModels.isNotEmpty()) availableModels else AvailableAiModels
    var showAllModelsSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderFilter by remember { mutableStateOf("ALL") }

    val quickChips = remember(models, selectedModel) {
        val list = mutableListOf<AiModelInfo>()
        if (models.any { it.id == selectedModel.id }) {
            list.add(selectedModel)
        }
        for (m in models) {
            if (list.size >= 7) break
            if (m.id != selectedModel.id) {
                list.add(m)
            }
        }
        list
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0F15))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = HermesStrings.modelLabel(language),
            style = MonospaceStyle.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        )

        // All Models Sheet Trigger Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NeonCyan.copy(alpha = 0.15f))
                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                .clickable { showAllModelsSheet = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (language == AppLanguage.AR) "كل الموديلات (${models.size}) 🔍" else "All Models (${models.size}) 🔍",
                style = MonospaceStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            )
        }

        // Quick Top Chips
        quickChips.forEach { model ->
            val isSelected = model.id == selectedModel.id
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NeonViolet.copy(alpha = 0.25f) else CyberSurfaceElevated)
                    .border(
                        1.dp,
                        if (isSelected) NeonViolet else CyberSurfaceBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectModel(model) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("model_chip_${model.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NeonVioletLight,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = model.displayName,
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                )
            }
        }
    }

    if (showAllModelsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllModelsSheet = false },
            containerColor = CyberSurface
        ) {
            val providers = remember(models) {
                listOf("ALL") + models.map { it.provider }.distinct()
            }
            val filteredModels = remember(models, searchQuery, selectedProviderFilter) {
                models.filter { m ->
                    val matchesSearch = searchQuery.isBlank() ||
                        m.displayName.contains(searchQuery, ignoreCase = true) ||
                        m.id.contains(searchQuery, ignoreCase = true) ||
                        m.provider.contains(searchQuery, ignoreCase = true)
                    val matchesProvider = selectedProviderFilter == "ALL" || m.provider == selectedProviderFilter
                    matchesSearch && matchesProvider
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.AR) "موديلات هيرمز المتاحة" else "Available Hermes Models",
                            style = MonospaceStyle.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                        Text(
                            text = if (language == AppLanguage.AR) "${filteredModels.size} من أصل ${models.size} موديل" else "${filteredModels.size} of ${models.size} models",
                            style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary)
                        )
                    }
                    IconButton(onClick = { showAllModelsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(NeonCyan),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (language == AppLanguage.AR) "ابحث عن أي موديل بالاسم أو المزود..." else "Search models...",
                                    style = MonospaceStyle.copy(color = TextSecondary, fontSize = 12.sp)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Provider Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    providers.forEach { prov ->
                        val isProvSelected = prov == selectedProviderFilter
                        val label = if (prov == "ALL") (if (language == AppLanguage.AR) "الكل" else "ALL") else prov
                        Text(
                            text = label,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isProvSelected) NeonCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                                .border(1.dp, if (isProvSelected) NeonCyan else CyberSurfaceBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedProviderFilter = prov }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MonospaceStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isProvSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isProvSelected) NeonCyan else TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Models LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredModels) { m ->
                        val isSelected = m.id == selectedModel.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonViolet.copy(alpha = 0.25f) else CyberSurfaceElevated)
                                .border(1.dp, if (isSelected) NeonViolet else CyberSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectModel(m)
                                    showAllModelsSheet = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = m.displayName,
                                        style = MonospaceStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) NeonVioletLight else TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = m.provider,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF141E28))
                                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MonospaceStyle.copy(fontSize = 9.sp, color = NeonCyan)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = m.id,
                                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonVioletLight, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage, language: AppLanguage) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    // Blinking cursor for streaming
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_stream")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    if (message.sender == MessageSender.USER) {
        // User Message (Right Aligned)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = HermesStrings.you(language),
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedTime,
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(Color(0xFF23143F))
                    .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    style = MonospaceStyle.copy(
                        fontSize = 13.5.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    } else {
        // Hermes Agent Message (Left Aligned)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            // Header Bar with Agent Name, Model Tag & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = HermesStrings.hermesAgent(language),
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonVioletLight
                    )
                )

                message.modelName?.let { mName ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[$mName]",
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedTime,
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                )

                if (message.isStreaming) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = HermesStrings.streamingBadge(language),
                        style = MonospaceStyle.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Message Bubble Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .padding(14.dp)
            ) {
                // Tool Execution Blocks (Monospace Tool Windows)
                if (message.toolExecutions.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = if (message.content.isNotEmpty()) 12.dp else 0.dp)
                    ) {
                        message.toolExecutions.forEach { tool ->
                            MonospaceToolBlock(tool = tool, language = language)
                        }
                    }
                }

                // AI Conversational Text
                if (message.content.isNotEmpty()) {
                    Row {
                        Text(
                            text = message.content,
                            style = MonospaceStyle.copy(
                                fontSize = 13.5.sp,
                                color = TextPrimary,
                                lineHeight = 21.sp
                            )
                        )
                        if (message.isStreaming) {
                            Text(
                                text = " ▋",
                                style = MonospaceStyle.copy(
                                    fontSize = 14.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.alpha(cursorAlpha)
                            )
                        }
                    }
                } else if (message.isStreaming && message.toolExecutions.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = NeonViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = HermesStrings.receivingStream(language),
                            style = MonospaceStyle.copy(
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPresetPrompts(
    enabled: Boolean,
    language: AppLanguage,
    onPresetClick: (String) -> Unit
) {
    val presets = listOf(
        HermesStrings.presetCpu(language),
        HermesStrings.presetPython(language),
        HermesStrings.presetTailscale(language),
        HermesStrings.presetDeepSeek(language)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { prompt ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131922))
                    .border(1.dp, Color(0xFF263345), RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { onPresetClick(prompt) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prompt,
                    style = MonospaceStyle.copy(
                        fontSize = 11.5.sp,
                        color = if (enabled) TextPrimary else TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    language: AppLanguage,
    onTextChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0E14))
            .border(width = 1.dp, color = CyberSurfaceBorder)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Monospace Terminal Styled Input Field
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberTerminalBg)
                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = HermesStrings.inputPlaceholder(language),
                    style = MonospaceStyle.copy(
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = MonospaceStyle.copy(
                    fontSize = 13.sp,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(NeonCyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_input_field")
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Send or Stop Streaming Button
        if (isStreaming) {
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NeonRed.copy(alpha = 0.2f))
                    .border(1.dp, NeonRed, CircleShape)
                    .testTag("stop_stream_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = HermesStrings.stopStreaming(language),
                    tint = NeonRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (text.isNotBlank()) NeonViolet else Color(0xFF1F2633))
                    .border(
                        1.dp,
                        if (text.isNotBlank()) NeonVioletLight else Color(0xFF2E384D),
                        CircleShape
                    )
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = HermesStrings.sendCommand(language),
                    tint = if (text.isNotBlank()) Color.White else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

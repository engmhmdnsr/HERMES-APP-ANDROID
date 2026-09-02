package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.model.AiModelInfo
import com.example.model.AppLanguage
import com.example.model.AvailableAiModels
import com.example.model.ChatMessage
import com.example.model.ConnectionConfig
import com.example.model.HermesStrings
import com.example.model.MessageSender
import com.example.ui.components.MonospaceToolBlock
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTerminalBg
import com.example.ui.theme.MonospaceStyle
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatTerminalScreen(
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    selectedModel: AiModelInfo,
    config: ConnectionConfig,
    language: AppLanguage,
    onSelectModel: (AiModelInfo) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive or stream updates
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length, messages.lastOrNull()?.toolExecutions?.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
            language = language,
            onSelectModel = onSelectModel
        )

        // Remote Gateway Active Route Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (config.isRemoteGatewayActive) NeonCyan.copy(alpha = 0.08f) else NeonViolet.copy(alpha = 0.08f))
                .border(
                    1.dp,
                    if (config.isRemoteGatewayActive) NeonCyan.copy(alpha = 0.3f) else NeonViolet.copy(alpha = 0.3f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (config.isRemoteGatewayActive) NeonCyan else NeonVioletLight)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (config.isRemoteGatewayActive) {
                        if (language == AppLanguage.AR) "بوابة عن بعد نشطة:" else "REMOTE GATEWAY:"
                    } else {
                        if (language == AppLanguage.AR) "محاكاة محلية:" else "SIMULATOR:"
                    },
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.isRemoteGatewayActive) NeonCyan else NeonVioletLight
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (config.isRemoteGatewayActive) {
                        config.effectiveGatewayUrl
                    } else {
                        "simulated-engine://offline"
                    },
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                )
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

@Composable
fun ModelSelectorRow(
    selectedModel: AiModelInfo,
    language: AppLanguage,
    onSelectModel: (AiModelInfo) -> Unit,
    modifier: Modifier = Modifier
) {
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

        AvailableAiModels.forEach { model ->
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

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import ee.oversight.hermes.data.HermesAppLog
import ee.oversight.hermes.model.AiModelInfo
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ApprovalMode
import ee.oversight.hermes.model.ApprovalRequest
import ee.oversight.hermes.model.AvailableAiModels
import ee.oversight.hermes.model.ChatMessage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.HermesSession
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.model.MessageSender
import ee.oversight.hermes.ui.components.InteractiveApprovalCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.style.TextOverflow
import ee.oversight.hermes.model.ToolExecutionBlock
import ee.oversight.hermes.model.ToolStatus
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
    onSendMessage: (String, List<String>) -> Unit,
    onStopStreaming: () -> Unit,
    onQueueMessage: ((String, List<String>) -> Unit)? = null,
    reasoningEffort: String = "medium",
    onEffortSelected: ((String) -> Unit)? = null,
    activeApprovalRequest: ApprovalRequest? = null,
    onResolveApproval: ((ApprovalRequest, Boolean, ApprovalMode) -> Unit)? = null,
    queuedMessageCount: Int = 0,
    onCancelQueued: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    var showAllModelsSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Image attachments picked for the next message (data URLs)
    var pendingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    // Uploaded file paths (absolute paths on the PC) + display names
    var pendingFiles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    // Show attach options sheet
    var showAttachSheet by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Speech-to-Text Dictation launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                promptInput = if (promptInput.isBlank()) spokenText else "$promptInput $spokenText"
            }
        }
    }

    // Image picker launcher (opens system photo picker)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val newImages = uris.mapNotNull { uri ->
                    uriToCompressedDataUrl(context, uri)
                }
                pendingImages = pendingImages + newImages
            }
        }
    )

    // File picker launcher (opens system file picker for any type)
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                val fileName = queryDisplayName(context, uri) ?: "file"
                // Upload immediately to the PC via /api/files
                scope.launch {
                    isUploading = true
                    val uploaded = uploadFileToGateway(config, context, uri, fileName, language)
                    isUploading = false
                    if (uploaded != null) {
                        HermesAppLog.info("File uploaded: $fileName -> ${uploaded.first}")
                        pendingFiles = pendingFiles + uploaded
                    } else {
                        HermesAppLog.error("File upload failed: $fileName")
                    }
                }
            }
        }
    )
    // Remember which session we already auto-scrolled to bottom for.
    var lastScrolledSession by remember { mutableStateOf<String?>(null) }
    var userFollows by remember { mutableStateOf(true) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var prevMessagesSize by remember { mutableStateOf(0) }

    // Detect user-initiated vs programmatic scrolling
    LaunchedEffect(listState, messages.size) {
        snapshotFlow {
            val atBottom = isAtTrueBottom(listState, messages.size)
            val inProgress = listState.isScrollInProgress
            Pair(atBottom, inProgress)
        }.collect { (atBottom, inProgress) ->
            if (inProgress && !isProgrammaticScroll) {
                userFollows = atBottom
            } else if (!inProgress && atBottom) {
                userFollows = true
            }
        }
    }

    val lastMessage = messages.lastOrNull()
    val lastMsgContentLen = lastMessage?.content?.length ?: 0
    val lastMsgThinkingLen = lastMessage?.thinkingContent?.length ?: 0
    val lastMsgToolsCount = lastMessage?.toolExecutions?.size ?: 0
    val lastMsgToolStatus = lastMessage?.toolExecutions?.lastOrNull()?.status

    LaunchedEffect(
        currentSessionId,
        messages.size,
        lastMsgContentLen,
        lastMsgThinkingLen,
        lastMsgToolsCount,
        lastMsgToolStatus,
        isStreaming
    ) {
        val sid = currentSessionId
        if (messages.isNotEmpty()) {
            val isNewSession = sid != null && lastScrolledSession != sid
            val isNewMessage = messages.size > prevMessagesSize
            prevMessagesSize = messages.size

            if (isNewMessage) {
                userFollows = true
            }

            val lastIndex = messages.size - 1
            if (isNewSession) {
                userFollows = true
                isProgrammaticScroll = true
                try {
                    listState.scrollToItem(lastIndex, scrollOffset = 500_000)
                } finally {
                    isProgrammaticScroll = false
                }
                lastScrolledSession = sid
            } else if (userFollows) {
                isProgrammaticScroll = true
                try {
                    listState.scrollToItem(lastIndex, scrollOffset = 500_000)
                } finally {
                    isProgrammaticScroll = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        // Chat Message Stream with Floating Scroll-To-Bottom Button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(message = message, language = language)
                }
            }

            // Floating Scroll-To-Bottom Button
            val canScrollDown = remember {
                derivedStateOf { listState.canScrollForward }
            }
            if (canScrollDown.value) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceElevated)
                        .border(1.dp, NeonCyan, CircleShape)
                        .clickable {
                            userFollows = true
                            scope.launch {
                                isProgrammaticScroll = true
                                try {
                                    listState.animateScrollToItem(messages.size - 1, scrollOffset = 500_000)
                                } finally {
                                    isProgrammaticScroll = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll down",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Preset Prompts Row
        QuickPresetPrompts(
            enabled = !isStreaming,
            language = language,
            onPresetClick = { prompt ->
                promptInput = prompt
                onSendMessage(prompt, emptyList())
                promptInput = ""
            }
        )

        // Uploading indicator
        if (isUploading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = NeonCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.AR) "جاري رفع الملف..." else "Uploading file...",
                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                )
            }
        }

        // Pending files (uploaded, ready to send)
        if (pendingFiles.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pendingFiles.forEach { (path, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, NeonViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.InsertDriveFile, null, tint = NeonVioletLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MonospaceStyle.copy(fontSize = 11.sp, color = TextPrimary),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { pendingFiles = pendingFiles.filter { it.first != path } },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        // Image attachment previews (picked but not yet sent)
        if (pendingImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pendingImages.forEach { imgUrl ->
                    val bitmap = remember(imgUrl) { dataUrlToBitmap(imgUrl).asImageBitmap() }
                    Box {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap,
                            contentDescription = "Attachment",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        )
                        // Remove (X) button
                        IconButton(
                            onClick = { pendingImages = pendingImages - imgUrl },
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        // Interactive Approval Card (prominently shown above input)
        if (activeApprovalRequest != null && onResolveApproval != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                InteractiveApprovalCard(
                    request = activeApprovalRequest,
                    language = language,
                    onResolve = onResolveApproval
                )
            }
        }

        // Queued-messages indicator (shown while a run is active and messages wait)
        if (queuedMessageCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonAmber.copy(alpha = 0.12f))
                    .border(1.dp, NeonAmber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (language == AppLanguage.AR)
                        "⏳ $queuedMessageCount رسالة في الانتظار... هتتبعت أول ما الرد يخلص"
                    else
                        "⏳ $queuedMessageCount queued · will send when the current reply finishes",
                    style = MonospaceStyle.copy(fontSize = 10.5.sp, color = NeonAmber),
                    maxLines = 2
                )
                if (onCancelQueued != null) {
                    IconButton(onClick = onCancelQueued, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (language == AppLanguage.AR) "إلغاء الانتظار" else "Cancel queued",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Bottom Input Bar matching reference image (pill container with model selector inside)
        ChatInputBar(
            text = promptInput,
            language = language,
            selectedModel = selectedModel,
            onOpenModelSheet = { showAllModelsSheet = true },
            onTextChange = { promptInput = it },
            isStreaming = isStreaming,
            hasAttachments = pendingImages.isNotEmpty() || pendingFiles.isNotEmpty() || isUploading,
            reasoningEffort = reasoningEffort,
            onEffortSelected = onEffortSelected,
            onAttachClick = { showAttachSheet = true },
            onVoiceInput = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == AppLanguage.AR) "ar-SA" else "en-US")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == AppLanguage.AR) "تحدث الآن لتسجيل أمرك..." else "Speak your message...")
                }
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    speechLauncher.launch(intent)
                } catch (_: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        if (language == AppLanguage.AR) "خدمة التعرف الصوتي غير متوفرة" else "Voice recognition not available",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSend = {
                if (promptInput.isNotBlank() || pendingImages.isNotEmpty() || pendingFiles.isNotEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Build the message text: prompt + uploaded file paths
                    var finalPrompt = promptInput
                    if (pendingFiles.isNotEmpty()) {
                        val fileNote = pendingFiles.joinToString("\n") { (path, name) ->
                            "[File: $name] Saved at: $path"
                        }
                        finalPrompt = if (finalPrompt.isNotBlank()) {
                            "$finalPrompt\n\n$fileNote"
                        } else {
                            fileNote
                        }
                    }
                    onSendMessage(finalPrompt, pendingImages)
                    promptInput = ""
                    pendingImages = emptyList()
                    pendingFiles = emptyList()
                }
            },
            onStop = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStopStreaming()
            },
            onQueue = {
                if (promptInput.isNotBlank() || pendingImages.isNotEmpty() || pendingFiles.isNotEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    var finalPrompt = promptInput
                    if (pendingFiles.isNotEmpty()) {
                        val fileNote = pendingFiles.joinToString("\n") { (path, name) ->
                            "[File: $name] Saved at: $path"
                        }
                        finalPrompt = if (finalPrompt.isNotBlank()) {
                            "$finalPrompt\n\n$fileNote"
                        } else {
                            fileNote
                        }
                    }
                    onQueueMessage?.invoke(finalPrompt, pendingImages)
                    promptInput = ""
                    pendingImages = emptyList()
                    pendingFiles = emptyList()
                }
            },
            modifier = Modifier.imePadding()
        )

        // Models Selection Sheet (triggered from bottom Model Pill)
        if (showAllModelsSheet) {
            ModelsSelectionBottomSheet(
                selectedModel = selectedModel,
                availableModels = availableModels,
                language = language,
                onSelectModel = {
                    onSelectModel(it)
                    showAllModelsSheet = false
                },
                onDismiss = { showAllModelsSheet = false }
            )
        }

        // Attach options bottom sheet (photo or file)
        if (showAttachSheet) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showAttachSheet = false },
                containerColor = CyberSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 30.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "إرفاق" else "Attach",
                        style = MonospaceStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    // Photo option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceElevated)
                            .clickable {
                                showAttachSheet = false
                                imagePicker.launch("image/*")
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.AR) "صورة" else "Photo",
                                style = MonospaceStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            )
                            Text(
                                text = if (language == AppLanguage.AR) "اختر صورة من المعرض" else "Pick an image from gallery",
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // File option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceElevated)
                            .clickable {
                                showAttachSheet = false
                                filePicker.launch("*/*")
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.InsertDriveFile, null, tint = NeonVioletLight, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.AR) "ملف" else "File",
                                style = MonospaceStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            )
                            Text(
                                text = if (language == AppLanguage.AR) "PDF, DOCX, ZIP وأي نوع ملفات" else "PDF, DOCX, ZIP and any file",
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsSelectionBottomSheet(
    selectedModel: AiModelInfo,
    availableModels: List<AiModelInfo>,
    language: AppLanguage,
    onSelectModel: (AiModelInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val models = if (availableModels.isNotEmpty()) availableModels else AvailableAiModels
    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderFilter by remember { mutableStateOf("ALL") }

    val providers = remember(models) {
        listOf("ALL") + models.map { it.provider }.distinct().filter { it.isNotBlank() }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C1017),
        contentColor = TextPrimary
    ) {
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
                IconButton(onClick = onDismiss) {
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
                Column {
                    // Attachment images (if any)
                    if (message.attachments.isNotEmpty()) {
                        message.attachments.forEach { imgUrl ->
                            val bitmap = remember(imgUrl) { dataUrlToBitmap(imgUrl).asImageBitmap() }
                            androidx.compose.foundation.Image(
                                bitmap = bitmap,
                                contentDescription = "Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    if (message.content.isNotBlank()) {
                        SelectionContainer {
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
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp)
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

                Spacer(modifier = Modifier.weight(1f))
                val clipboardManager = LocalClipboardManager.current
                val ctx = LocalContext.current
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        Toast.makeText(ctx, if (language == AppLanguage.AR) "تم نسخ الرد بالكامل" else "Response copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
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
                // Thinking / Reasoning section (dimmed, collapses when the real reply starts)
                if (message.thinkingContent.isNotBlank() && message.sender == MessageSender.HERMES) {
                    ThinkingBlock(
                        thinking = message.thinkingContent,
                        thinkingDone = message.thinkingDone,
                        isStreaming = message.isStreaming,
                        language = language
                    )
                }

                // Tool Execution Blocks (Compact One-Line Tool Entries)
                if (message.toolExecutions.isNotEmpty() && message.sender == MessageSender.HERMES) {
                    ToolExecutionsBlock(
                        toolExecutions = message.toolExecutions,
                        thinkingDone = message.thinkingDone,
                        isStreaming = message.isStreaming,
                        language = language
                    )
                }

                // AI Conversational Text
                if (message.content.isNotEmpty()) {
                    SelectionContainer {
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
                    }
                } else if (message.isStreaming && message.toolExecutions.isEmpty() && message.thinkingContent.isEmpty()) {
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
fun ThinkingBlock(
    thinking: String,
    thinkingDone: Boolean,
    isStreaming: Boolean,
    language: AppLanguage
) {
    var expanded by remember { mutableStateOf(false) }
    // Live (still thinking) → show Arabic "جاري التفكير"; done → compact "thinking"
    val liveLabel = if (language == AppLanguage.AR) "جاري التفكير" else "thinking"
    val doneLabel = "thinking"

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        // Dimmed live text while still thinking (font ~50% of normal 13.5sp ≈ 7sp)
        if (!thinkingDone && thinking.isNotBlank()) {
            SelectionContainer {
                Text(
                    text = thinking,
                    style = MonospaceStyle.copy(
                        fontSize = 7.sp,
                        color = TextSecondary.copy(alpha = 0.55f),
                        lineHeight = 10.sp
                    )
                )
            }
        }

        // Collapsible toggle row shown once the reply started (or always after done)
        if (thinkingDone || thinking.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .testTag("thinking_toggle")
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (thinkingDone) doneLabel else liveLabel,
                    style = MonospaceStyle.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (thinkingDone) TextSecondary else NeonVioletLight
                    )
                )
                if (!thinkingDone && isStreaming) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        strokeWidth = 1.5.dp,
                        color = NeonVioletLight,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            if (expanded && thinking.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C1118))
                        .border(1.dp, Color(0xFF1A2130), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = thinking,
                            style = MonospaceStyle.copy(
                                fontSize = 9.5.sp,
                                color = TextSecondary.copy(alpha = 0.8f),
                                lineHeight = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolExecutionsBlock(
    toolExecutions: List<ToolExecutionBlock>,
    thinkingDone: Boolean,
    isStreaming: Boolean,
    language: AppLanguage
) {
    if (toolExecutions.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val isDone = thinkingDone || !isStreaming
    val count = toolExecutions.size
    val toolsLabel = if (language == AppLanguage.AR) {
        "$count أدوات"
    } else {
        if (count == 1) "1 tool" else "$count tools"
    }
    val anyRunning = toolExecutions.any { it.status == ToolStatus.RUNNING }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        // While still streaming, show each tool as one dim live line as it arrives.
        if (!isDone) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                toolExecutions.forEach { tool ->
                    CompactToolLine(tool = tool)
                }
            }
            // Live row: current count + spinner while tools are still running.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = toolsLabel,
                    style = MonospaceStyle.copy(fontSize = 9.sp, color = NeonCyan)
                )
                if (anyRunning) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        strokeWidth = 1.5.dp,
                        color = NeonCyan,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        } else {
            // Collapsible toggle row shown once the reply starts / message is done.
            Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .testTag("tools_toggle")
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = toolsLabel,
                style = MonospaceStyle.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) TextSecondary else NeonCyan
                )
            )
            if (anyRunning && isStreaming) {
                Spacer(modifier = Modifier.width(6.dp))
                CircularProgressIndicator(
                    strokeWidth = 1.5.dp,
                    color = NeonCyan,
                    modifier = Modifier.size(10.dp)
                )
            }
            }
        } // end else (done-state toggle row)

        if (expanded && isDone) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C1118))
                    .border(1.dp, Color(0xFF1A2130), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    toolExecutions.forEach { tool ->
                        CompactToolLine(tool = tool)
                    }
                }
            }
        }
    }
}

@Composable
fun CompactToolLine(
    tool: ToolExecutionBlock,
    modifier: Modifier = Modifier
) {
    val icon = getToolIcon(tool.toolName)
    val preview = tool.command.trim().replace(Regex("\\s+"), " ").take(40)
    val color = when (tool.status) {
        ToolStatus.RUNNING -> NeonCyan.copy(alpha = 0.9f)
        ToolStatus.COMPLETED -> TextSecondary.copy(alpha = 0.55f)
        ToolStatus.FAILED -> NeonRed.copy(alpha = 0.75f)
    }

    val lineText = buildString {
        append(icon)
        append(" ")
        append(tool.toolName)
        if (preview.isNotBlank()) {
            append(" · ")
            append(preview)
        }
    }

    SelectionContainer {
        Text(
            text = lineText,
            style = MonospaceStyle.copy(
                fontSize = 7.sp,
                color = color,
                lineHeight = 10.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.fillMaxWidth()
        )
    }
}

private fun getToolIcon(toolName: String): String {
    val name = toolName.lowercase()
    return when {
        name.contains("search_files") || name == "search" || name.contains("find") -> "🔍"
        name.contains("web") -> "🌐"
        name.contains("read") -> "📄"
        name.contains("write") -> "📝"
        name.contains("patch") || name.contains("edit") -> "🔧"
        name.contains("terminal") || name.contains("shell") || name.contains("bash") || name.contains("cmd") || name.contains("exec") -> "💻"
        name.contains("search") -> "🔍"
        else -> "⚙️"
    }
}

private fun isAtTrueBottom(listState: LazyListState, totalItems: Int): Boolean {
    if (totalItems <= 0) return true
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return true
    val lastVisible = visibleItems.last()
    if (lastVisible.index < totalItems - 1) return false
    val bottomEdge = lastVisible.offset + lastVisible.size
    val viewportBottom = layoutInfo.viewportEndOffset
    return !listState.canScrollForward || bottomEdge <= viewportBottom + 40
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
    selectedModel: AiModelInfo,
    onOpenModelSheet: () -> Unit,
    onTextChange: (String) -> Unit,
    isStreaming: Boolean,
    hasAttachments: Boolean = false,
    onAttachClick: () -> Unit = {},
    onVoiceInput: (() -> Unit)? = null,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onQueue: (() -> Unit)? = null,
    reasoningEffort: String = "medium",
    onEffortSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF131823))
            .border(1.dp, Color(0xFF263345), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Multi-line Text Input Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp, max = 120.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.AR) "اكتب رسالتك أو الأمر..." else "Message...",
                    style = MonospaceStyle.copy(
                        fontSize = 13.5.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = MonospaceStyle.copy(
                    fontSize = 13.5.sp,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(NeonCyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_input_field")
            )
        }

        // Bottom Controls Row: [+] [ model ⌄ ] ... [ Send / Stop ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Action Group: [+] [ Model Pill ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attach (+) button
                if (!isStreaming) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (hasAttachments) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1B2332))
                            .border(1.dp, if (hasAttachments) NeonCyan else Color(0xFF323F54), CircleShape)
                            .clickable { onAttachClick() }
                            .testTag("attach_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = if (hasAttachments) NeonCyan else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Model Selector Pill [ model-name ⌄ ]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1B2332))
                        .border(1.dp, Color(0xFF384961), RoundedCornerShape(16.dp))
                        .clickable { onOpenModelSheet() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("model_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedModel.displayName.take(18),
                        style = MonospaceStyle.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select model",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Reasoning Effort Selector Pill [ effort ▾ ] — next to the model
                if (onEffortSelected != null && !isStreaming) {
                    var effortMenuOpen by remember { mutableStateOf(false) }
                    val effortLabel = when (reasoningEffort) {
                        "low" -> if (language == AppLanguage.AR) "منخفض" else "LOW"
                        "medium" -> if (language == AppLanguage.AR) "متوسط" else "MED"
                        "high" -> if (language == AppLanguage.AR) "مرتفع" else "HIGH"
                        "none" -> if (language == AppLanguage.AR) "بدون" else "OFF"
                        else -> "MED"
                    }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(NeonViolet.copy(alpha = 0.12f))
                                .border(1.dp, NeonViolet.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .clickable { effortMenuOpen = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("effort_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Reasoning effort",
                                tint = NeonVioletLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = effortLabel,
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonVioletLight
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = NeonVioletLight.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = effortMenuOpen,
                            onDismissRequest = { effortMenuOpen = false },
                            containerColor = Color(0xFF131826)
                        ) {
                            listOf("low", "medium", "high", "none").forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (level) {
                                                "low" -> if (language == AppLanguage.AR) "⚡ منخفض (Low)" else "⚡ Low"
                                                "medium" -> if (language == AppLanguage.AR) "🔶 متوسط (Medium)" else "🔶 Medium"
                                                "high" -> if (language == AppLanguage.AR) "🔥 مرتفع (High)" else "🔥 High"
                                                else -> if (language == AppLanguage.AR) "⛔ بدون تفكير (None)" else "⛔ None"
                                            },
                                            style = MonospaceStyle.copy(
                                                fontSize = 13.sp,
                                                color = if (level == reasoningEffort) NeonVioletLight else TextPrimary
                                            )
                                        )
                                    },
                                    onClick = {
                                        effortMenuOpen = false
                                        onEffortSelected(level)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Right Action Group: [Mic] [Send / Stop]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Microphone Voice Dictation Button
                if (!isStreaming && onVoiceInput != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2332))
                            .border(1.dp, Color(0xFF323F54), CircleShape)
                            .clickable { onVoiceInput() }
                            .testTag("voice_input_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (language == AppLanguage.AR) "إملاء صوتي" else "Voice Dictation",
                            tint = NeonCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (isStreaming) {
                    // Streaming: offer Queue (schedule for after this reply),
                    // Send-now (interrupt + send) and Stop (just cancel).
                    val canSend = text.isNotBlank() || hasAttachments
                    if (canSend && onQueue != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.15f))
                                .border(1.dp, NeonAmber.copy(alpha = 0.6f), CircleShape)
                                .clickable { onQueue() }
                                .testTag("queue_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (language == AppLanguage.AR) "أضف للقائمة (يُرسل بعد الرد)" else "Queue (send after reply)",
                                tint = NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (canSend) {
                        // Send now = interrupt current reply and send immediately
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonViolet.copy(alpha = 0.85f))
                                .border(1.dp, NeonVioletLight, CircleShape)
                                .clickable { onSend() }
                                .testTag("send_interrupt_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (language == AppLanguage.AR) "إرسال الآن (يوقف الرد الحالي)" else "Send now (interrupt)",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NeonRed.copy(alpha = 0.2f))
                            .border(1.dp, NeonRed, CircleShape)
                            .clickable { onStop() }
                            .testTag("stop_stream_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = HermesStrings.stopStreaming(language),
                            tint = NeonRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
            } else {
                val canSend = text.isNotBlank() || hasAttachments
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (canSend) NeonViolet else Color(0xFF1C2230))
                        .border(1.dp, if (canSend) NeonVioletLight else Color(0xFF2E384D), CircleShape)
                        .clickable(enabled = canSend) { onSend() }
                        .testTag("send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = HermesStrings.sendCommand(language),
                        tint = if (canSend) Color.White else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
}

// ============ Attachment helpers ============

/**
 * Read a content Uri (image), downscale to max ~1280px, compress to JPEG,
 * and return a base64 data URL suitable for the Hermes API vision input.
 */
private fun uriToCompressedDataUrl(context: Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(input, null, bounds)
        input.close()

        // Downscale if larger than 1280px
        val maxDim = 1280
        var sample = 1
        val w = bounds.outWidth
        val h = bounds.outHeight
        while ((w / sample) > maxDim * 2 || (h / sample) > maxDim * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val input2 = context.contentResolver.openInputStream(uri) ?: return null
        var bitmap = BitmapFactory.decodeStream(input2, null, opts)
        input2.close()

        // Scale down to maxDim if still too big
        if (bitmap != null && (bitmap.width > maxDim || bitmap.height > maxDim)) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val nw = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val nh = (bitmap.height * scale).toInt().coerceAtLeast(1)
            bitmap = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
        }

        val out = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, out)
        val bytes = out.toByteArray()
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        "data:image/jpeg;base64,$b64"
    } catch (_: Exception) {
        null
    }
}

/** Decode a data URL (base64 image) back to a Bitmap for preview rendering. */
private fun dataUrlToBitmap(dataUrl: String): Bitmap {
    return try {
        val comma = dataUrl.indexOf(',')
        if (comma > 0) {
            val b64 = dataUrl.substring(comma + 1)
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    } catch (_: Exception) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}

// ============ File attachment helpers ============

/** Get a content Uri's display name (filename) from the ContentResolver. */
private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        uri.lastPathSegment
    }
}

/**
 * Upload a picked file to the PC gateway (/api/files) as base64 JSON using shared HermesNetworkClient.
 * Returns (absolutePathOnPC, displayName) on success, null on failure.
 */
private suspend fun uploadFileToGateway(
    config: ConnectionConfig,
    context: Context,
    uri: Uri,
    displayName: String,
    language: AppLanguage
): Pair<String, String>? {
    return withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null

            // Cap ~9MB raw so base64 stays under the 10MB server limit
            if (bytes.size > 9 * 1024 * 1024) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        if (language == AppLanguage.AR) "حجم الملف يتجاوز الحد الأقصى (9 ميجابايت)" else "File size exceeds 9MB limit",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext null
            }

            val client = ee.oversight.hermes.data.HermesNetworkClient()
            val result = client.uploadFile(config, displayName, bytes)
            if (result.isSuccess) {
                result.getOrNull()
            } else {
                withContext(Dispatchers.Main) {
                    val msg = result.exceptionOrNull()?.localizedMessage ?: "Upload failed"
                    android.widget.Toast.makeText(
                        context,
                        if (language == AppLanguage.AR) "فشل رفع الملف: $msg" else "File upload failed: $msg",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                null
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    if (language == AppLanguage.AR) "خطأ أثناء قراءة الملف" else "Error reading file: ${e.localizedMessage}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            null
        }
    }
}

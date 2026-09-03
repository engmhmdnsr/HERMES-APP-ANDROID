package ee.oversight.hermes.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.HermesSession
import ee.oversight.hermes.model.TokenUsage
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SessionTabFilter {
    ALL,
    THREADS,
    PINNED,
    ARCHIVED
}

enum class SessionSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    MOST_MESSAGES,
    MOST_TOKENS
}

@Composable
fun SessionsDrawerContent(
    sessions: List<HermesSession>,
    currentSessionId: String?,
    isLoading: Boolean,
    language: AppLanguage,
    onSelectSession: (String) -> Unit,
    onCreateNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onRefreshSessions: () -> Unit,
    onClose: () -> Unit,
    pinnedSessionIds: Set<String> = emptySet(),
    onTogglePinSession: ((String) -> Unit)? = null,
    onExportSession: ((sessionId: String, title: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedFilter by remember { mutableStateOf(SessionTabFilter.ALL) }
    var sortOrder by remember { mutableStateOf(SessionSortOrder.NEWEST_FIRST) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    var showCustomizeDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<HermesSession?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.ENGLISH) }

    // Filter & sort logic
    val displaySessions = remember(sessions, selectedFilter, sortOrder, searchQuery, pinnedSessionIds) {
        var list = when (selectedFilter) {
            SessionTabFilter.ALL -> sessions.filter { !it.isArchived }
            SessionTabFilter.THREADS -> sessions.filter { it.isThread }
            SessionTabFilter.PINNED -> sessions.filter { pinnedSessionIds.contains(it.id) || it.isPinned }
            SessionTabFilter.ARCHIVED -> sessions.filter { it.isArchived }
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.model.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortOrder) {
            SessionSortOrder.NEWEST_FIRST -> list.sortedWith(
                compareByDescending<HermesSession> { pinnedSessionIds.contains(it.id) || it.isPinned }
                    .thenByDescending { it.startedAt }
            )
            SessionSortOrder.OLDEST_FIRST -> list.sortedWith(
                compareByDescending<HermesSession> { pinnedSessionIds.contains(it.id) || it.isPinned }
                    .thenBy { it.startedAt }
            )
            SessionSortOrder.MOST_MESSAGES -> list.sortedWith(
                compareByDescending<HermesSession> { pinnedSessionIds.contains(it.id) || it.isPinned }
                    .thenByDescending { it.messageCount }
            )
            SessionSortOrder.MOST_TOKENS -> list.sortedWith(
                compareByDescending<HermesSession> { pinnedSessionIds.contains(it.id) || it.isPinned }
                    .thenByDescending { it.totalTokens }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(Color(0xFF0A0D15))
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hermes",
                    style = MonospaceStyle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = if (language == AppLanguage.AR) "جلسات الخادم الافتراضية" else "Server default sessions",
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF8896A6)
                    )
                )
            }

            // Right actions: Refresh & Search
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefreshSessions,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "Refresh / Branches",
                            tint = Color(0xFFBAC7D5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) searchQuery = ""
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchVisible) NeonCyan else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141926))
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF8A96A6),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(NeonCyan),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (language == AppLanguage.AR) "بحث في الجلسات..." else "Search sessions...",
                                style = MonospaceStyle.copy(color = TextSecondary, fontSize = 13.sp)
                            )
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF8A96A6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // "+ New Chat" Prominent Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFA6B4FE)) // Soft lavender/periwinkle from image
                .clickable { onCreateNewSession() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF0F1527),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.AR) "محادثة جديدة" else "New Chat",
                style = MonospaceStyle.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F1527)
                )
            )
        }

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" Chip
            item {
                val isSelected = selectedFilter == SessionTabFilter.ALL
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF22293F) else Color(0xFF10131E))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38446A) else Color(0xFF1B2234),
                            CircleShape
                        )
                        .clickable { selectedFilter = SessionTabFilter.ALL }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "الكل" else "All",
                        style = MonospaceStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF8E9AA8)
                        )
                    )
                }
            }

            // "Threads" Chip with "Beta" Badge
            item {
                val isSelected = selectedFilter == SessionTabFilter.THREADS
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF22293F) else Color(0xFF10131E))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38446A) else Color(0xFF1B2234),
                            CircleShape
                        )
                        .clickable { selectedFilter = SessionTabFilter.THREADS }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "الثريدات" else "Threads",
                        style = MonospaceStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF8E9AA8)
                        )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF532E0A))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Beta",
                            style = MonospaceStyle.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        )
                    }
                }
            }

            // "Pinned" Chip
            item {
                val isSelected = selectedFilter == SessionTabFilter.PINNED
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF22293F) else Color(0xFF10131E))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38446A) else Color(0xFF1B2234),
                            CircleShape
                        )
                        .clickable { selectedFilter = SessionTabFilter.PINNED }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "المثبتة" else "Pinned",
                        style = MonospaceStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF8E9AA8)
                        )
                    )
                }
            }

            // "Archived" Chip
            item {
                val isSelected = selectedFilter == SessionTabFilter.ARCHIVED
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF22293F) else Color(0xFF10131E))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38446A) else Color(0xFF1B2234),
                            CircleShape
                        )
                        .clickable { selectedFilter = SessionTabFilter.ARCHIVED }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "المؤرشفة" else "Archived",
                        style = MonospaceStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF8E9AA8)
                        )
                    )
                }
            }
        }

        // "Customize sessions" Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCustomizeDialog = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = Color(0xFF7E8EA4),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.AR) "تخصيص الجلسات" else "Customize sessions",
                style = MonospaceStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF7E8EA4)
                )
            )
        }

        HorizontalDivider(
            color = Color(0xFF161C2C),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Sessions List
        if (displaySessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank())
                        (if (language == AppLanguage.AR) "لا توجد جلسات تطابق البحث" else "No matching sessions")
                    else
                        (if (language == AppLanguage.AR) "لا توجد جلسات هنا" else "No sessions here"),
                    style = MonospaceStyle.copy(fontSize = 13.sp, color = Color(0xFF6B788A))
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displaySessions, key = { it.id }) { s ->
                    val isCurrent = s.id == currentSessionId
                    val isPinned = pinnedSessionIds.contains(s.id) || s.isPinned
                    val dateFormatted = remember(s.startedAt) {
                        if (s.startedAt > 0) dateFormat.format(Date(s.startedAt)) else ""
                    }

                    var showMenu by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) Color(0xFF1A2238) else Color.Transparent)
                            .clickable { onSelectSession(s.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Title
                            Text(
                                text = s.title,
                                style = MonospaceStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isCurrent) Color.White else Color(0xFFE2E8F0)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            // Subtitle Row (Starred / Started Date / msgs / tokens)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPinned) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Pinned",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                if (s.isThread) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF162137))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ヒ Thread",
                                            style = MonospaceStyle.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF60A5FA)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                if (dateFormatted.isNotBlank()) {
                                    Text(
                                        text = if (language == AppLanguage.AR) "بدأت $dateFormatted" else "Started $dateFormatted",
                                        style = MonospaceStyle.copy(
                                            fontSize = 11.sp,
                                            color = Color(0xFF7E8EA4)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Text(
                                    text = "${s.messageCount}msgs",
                                    style = MonospaceStyle.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFF7E8EA4)
                                    )
                                )

                                if (s.totalTokens > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ⚡${TokenUsage.formatTokenCount(s.totalTokens)}",
                                        style = MonospaceStyle.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeonAmber
                                        )
                                    )
                                }
                            }
                        }

                        // Three vertical dots menu button
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = if (isCurrent) Color(0xFFBAC7D5) else Color(0xFF6B788A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = Color(0xFF131826)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isPinned)
                                                (if (language == AppLanguage.AR) "إلغاء التثبيت" else "Unpin session")
                                            else
                                                (if (language == AppLanguage.AR) "تثبيت في الأعلى" else "Pin to top"),
                                            style = MonospaceStyle.copy(fontSize = 13.sp, color = TextPrimary)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = if (isPinned) Color(0xFFF59E0B) else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onTogglePinSession?.invoke(s.id)
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (language == AppLanguage.AR) "نسخ معرف الجلسة" else "Copy Session ID",
                                            style = MonospaceStyle.copy(fontSize = 13.sp, color = TextPrimary)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString(s.id))
                                        Toast.makeText(
                                            context,
                                            if (language == AppLanguage.AR) "تم نسخ معرف الجلسة" else "Session ID copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (language == AppLanguage.AR) "مشاركة / تصدير الجلسة" else "Share / Export Session",
                                            style = MonospaceStyle.copy(fontSize = 13.sp, color = TextPrimary)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onExportSession?.invoke(s.id, s.title)
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (language == AppLanguage.AR) "حذف الجلسة" else "Delete session",
                                            style = MonospaceStyle.copy(fontSize = 13.sp, color = NeonRed)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = NeonRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        sessionToDelete = s
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (sessionToDelete != null) {
            val s = sessionToDelete!!
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                containerColor = Color(0xFF0F1420),
                titleContentColor = NeonRed,
                textContentColor = TextPrimary,
                title = {
                    Text(
                        text = if (language == AppLanguage.AR) "حذف الجلسة" else "Delete Session",
                        style = MonospaceStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = if (language == AppLanguage.AR)
                            "هل أنت متأكد من حذف محادثة \"${s.title}\" نهائياً؟"
                        else
                            "Are you sure you want to permanently delete \"${s.title}\"?",
                        style = MonospaceStyle.copy(fontSize = 13.sp, color = TextSecondary)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val id = s.id
                        sessionToDelete = null
                        onDeleteSession(id)
                    }) {
                        Text(
                            text = if (language == AppLanguage.AR) "حذف" else "Delete",
                            style = MonospaceStyle.copy(color = NeonRed, fontWeight = FontWeight.Bold)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text(
                            text = if (language == AppLanguage.AR) "إلغاء" else "Cancel",
                            style = MonospaceStyle.copy(color = TextSecondary)
                        )
                    }
                }
            )
        }

        // Customize Sessions Dialog (Sorting & Display)
        if (showCustomizeDialog) {
            AlertDialog(
                onDismissRequest = { showCustomizeDialog = false },
                containerColor = Color(0xFF0F1420),
                titleContentColor = Color.White,
                textContentColor = TextPrimary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.AR) "تخصيص وترتيب الجلسات" else "Customize Sessions",
                            style = MonospaceStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (language == AppLanguage.AR) "طريقة الترتيب:" else "Sort sessions by:",
                            style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        listOf(
                            SessionSortOrder.NEWEST_FIRST to (if (language == AppLanguage.AR) "الأحدث أولاً (Newest)" else "Newest first"),
                            SessionSortOrder.OLDEST_FIRST to (if (language == AppLanguage.AR) "الأقدم أولاً (Oldest)" else "Oldest first"),
                            SessionSortOrder.MOST_MESSAGES to (if (language == AppLanguage.AR) "الأكثر رسائل (Most Messages)" else "Most messages"),
                            SessionSortOrder.MOST_TOKENS to (if (language == AppLanguage.AR) "الأكثر استهلاكاً للتوكنز (Most Tokens)" else "Most tokens")
                        ).forEach { (order, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { sortOrder = order }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = sortOrder == order,
                                    onClick = { sortOrder = order },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeonCyan,
                                        unselectedColor = Color(0xFF4A5568)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MonospaceStyle.copy(
                                        fontSize = 13.sp,
                                        color = if (sortOrder == order) Color.White else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomizeDialog = false }) {
                        Text(
                            text = if (language == AppLanguage.AR) "تم" else "Done",
                            style = MonospaceStyle.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )
        }
    }
}
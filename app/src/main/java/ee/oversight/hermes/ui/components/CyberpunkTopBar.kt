package ee.oversight.hermes.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.R
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.ConnectionStatus
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonGreen
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ee.oversight.hermes.model.TokenUsage
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonVioletLight

@Composable
fun CyberpunkTopBar(
    status: ConnectionStatus,
    config: ConnectionConfig,
    pingMs: Long,
    language: AppLanguage,
    tokenUsage: TokenUsage? = null,
    onOpenDrawer: (() -> Unit)? = null,
    globalAutoApprove: Boolean = false,
    isSessionAutoApproved: Boolean = false,
    onToggleGlobalAutoApprove: ((Boolean) -> Unit)? = null,
    onTriggerTestApproval: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_topbar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (statusColor, statusLabel) = when (status) {
        ConnectionStatus.CONNECTED -> NeonGreen to HermesStrings.statusConnected(language)
        ConnectionStatus.CONNECTING -> NeonGreen.copy(alpha = 0.7f) to HermesStrings.statusConnecting(language)
        ConnectionStatus.DISCONNECTED -> NeonRed to HermesStrings.statusDisconnected(language)
        ConnectionStatus.ERROR -> NeonRed to HermesStrings.statusError(language)
    }

    var showTokenDetail by remember { mutableStateOf(false) }
    var showApprovalDialog by remember { mutableStateOf(false) }
    val totalTok = tokenUsage?.totalTokens ?: 0L
    val inTok = tokenUsage?.inputTokens ?: 0L
    val outTok = tokenUsage?.outputTokens ?: 0L

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenDrawer != null) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Sessions Menu",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16192E))
                        .border(1.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hermes_logo),
                        contentDescription = "Oversight Logo",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = HermesStrings.appTitle(language),
                            style = MonospaceStyle.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = " // CTRL",
                            style = MonospaceStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet
                            )
                        )
                    }
                    Text(
                        text = HermesStrings.appSubtitle(language),
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    )
                }
            }

            // Right side: token consumption (compact, next to the logo/title)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (totalTok > 0) NeonAmber.copy(alpha = 0.10f) else Color(0xFF0F141C))
                    .border(
                        1.dp,
                        if (totalTok > 0) NeonAmber.copy(alpha = 0.35f) else Color(0xFF1A2130),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { showTokenDetail = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Token Usage",
                    tint = if (totalTok > 0) NeonAmber else TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "⬆${TokenUsage.formatTokenCount(inTok)}",
                    style = MonospaceStyle.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "⬇${TokenUsage.formatTokenCount(outTok)}",
                    style = MonospaceStyle.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = NeonVioletLight)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = TokenUsage.formatTokenCount(totalTok),
                    style = MonospaceStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (totalTok > 0) NeonAmber else TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F141C))
                .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection target
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .alpha(if (status == ConnectionStatus.CONNECTED) pulseAlpha else 1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (config.tailscaleIp.isNotBlank()) {
                    Text(
                        text = config.effectiveGatewayUrl.removePrefix("http://").removePrefix("https://"),
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            color = TextSecondary
                        ),
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Approval Mode Badge
                val approvalBadgeText = when {
                    globalAutoApprove -> "⚡ AUTO-ALL"
                    isSessionAutoApproved -> "⚡ SESS-AUTO"
                    else -> "🛡️ MANUAL"
                }
                val approvalBadgeBg = when {
                    globalAutoApprove -> Color(0xFF2E1B4E)
                    isSessionAutoApproved -> Color(0xFF2B200E)
                    else -> Color(0xFF101926)
                }
                val approvalBadgeColor = when {
                    globalAutoApprove -> Color(0xFFA6B4FE)
                    isSessionAutoApproved -> NeonAmber
                    else -> NeonCyan.copy(alpha = 0.8f)
                }
                val approvalBadgeBorder = when {
                    globalAutoApprove -> NeonViolet
                    isSessionAutoApproved -> NeonAmber.copy(alpha = 0.5f)
                    else -> CyberSurfaceBorder
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(approvalBadgeBg)
                        .border(1.dp, approvalBadgeBorder, RoundedCornerShape(6.dp))
                        .clickable { showApprovalDialog = true }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = approvalBadgeText,
                        style = MonospaceStyle.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = approvalBadgeColor
                        )
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Latency / Ping
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pingMs}ms",
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                    )
                }
            }
        }

        // Token Detail Breakdown Dialog
        if (showTokenDetail) {
            AlertDialog(
                onDismissRequest = { showTokenDetail = false },
                containerColor = Color(0xFF0C1017),
                titleContentColor = NeonAmber,
                textContentColor = TextPrimary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.AR) "استهلاك التوكنز (Tokens)" else "Token Usage Stats",
                            style = MonospaceStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (language == AppLanguage.AR)
                                "تفاصيل استهلاك التوكنز في الجلسة الحالية:"
                            else
                                "Current session token consumption breakdown:",
                            style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141923))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.AR) "📥 المدخلات (Input):" else "📥 Input (Prompt):",
                                style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary)
                            )
                            Text(
                                text = "${TokenUsage.formatTokenCount(inTok)} ($inTok)",
                                style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141923))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.AR) "📤 المخرجات (Output):" else "📤 Output (Response):",
                                style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary)
                            )
                            Text(
                                text = "${TokenUsage.formatTokenCount(outTok)} ($outTok)",
                                style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonVioletLight)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonAmber.copy(alpha = 0.15f))
                                .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.AR) "⚡ الإجمالي (Total):" else "⚡ Total Tokens:",
                                style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                            )
                            Text(
                                text = "${TokenUsage.formatTokenCount(totalTok)} ($totalTok)",
                                style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = NeonAmber)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTokenDetail = false }) {
                        Text(
                            text = if (language == AppLanguage.AR) "إغلاق" else "Close",
                            style = MonospaceStyle.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )
        }

        // Security Approval Settings Dialog
        if (showApprovalDialog) {
            val isArabic = language == AppLanguage.AR
            AlertDialog(
                onDismissRequest = { showApprovalDialog = false },
                containerColor = Color(0xFF0C1017),
                titleContentColor = NeonAmber,
                textContentColor = TextPrimary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "إعدادات الموافقة الأمنية" else "Security Approvals",
                            style = MonospaceStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isArabic)
                                "يحدد هذا الخيار كيف يتعامل هيرمز مع الأوامر الحساسة (تشغيل الأوامر بالطرفية، حذف وتعديل الملفات):"
                            else
                                "Choose how Hermes handles sensitive executions (terminal shell, file modifications):",
                            style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary)
                        )

                        // Current Mode Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF131824))
                                .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "الوضع الحالي:" else "CURRENT MODE:",
                                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = when {
                                        globalAutoApprove -> if (isArabic) "⚡ موافقة تلقائية على كافة الأوامر (Allow All)" else "⚡ Autonomous Mode (Allow All)"
                                        isSessionAutoApproved -> if (isArabic) "⚡ موافقة تلقائية لهذه الجلسة (Session Auto)" else "⚡ Session Auto-Approved"
                                        else -> if (isArabic) "🛡️ موافقة يدوية عند كل أمر حرج (Manual)" else "🛡️ Manual Approvals (Prompt per command)"
                                    },
                                    style = MonospaceStyle.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (globalAutoApprove) Color(0xFFA6B4FE) else if (isSessionAutoApproved) NeonAmber else NeonCyan
                                    )
                                )
                            }
                        }

                        // Toggle Button: Switch between Allow All and Manual
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (globalAutoApprove) NeonRed.copy(alpha = 0.15f) else NeonViolet.copy(alpha = 0.2f))
                                .border(1.dp, if (globalAutoApprove) NeonRed else NeonViolet, RoundedCornerShape(8.dp))
                                .clickable {
                                    onToggleGlobalAutoApprove?.invoke(!globalAutoApprove)
                                    showApprovalDialog = false
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (globalAutoApprove)
                                    (if (isArabic) "الرجوع للوضع اليدوي (طلب موافقة دائماً)" else "Switch to Manual Mode")
                                else
                                    (if (isArabic) "تفعيل الوضع التلقائي (Allow All)" else "Enable Allow All (Autonomous)"),
                                style = MonospaceStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (globalAutoApprove) NeonRed else Color(0xFFA6B4FE)
                                )
                            )
                        }

                        // Test Approval Card Trigger
                        if (onTriggerTestApproval != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1F2C))
                                    .border(1.dp, Color(0xFF2B354C), RoundedCornerShape(8.dp))
                                    .clickable {
                                        onTriggerTestApproval()
                                        showApprovalDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) "🧪 تجربة بطاقة الموافقة الآن" else "🧪 Test Interactive Approval Card",
                                    style = MonospaceStyle.copy(fontSize = 11.sp, color = NeonCyan)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showApprovalDialog = false }) {
                        Text(
                            text = if (isArabic) "إغلاق" else "Close",
                            style = MonospaceStyle.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )
        }
    }
}

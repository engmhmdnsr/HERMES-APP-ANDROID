package ee.oversight.hermes.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ApprovalMode
import ee.oversight.hermes.model.ApprovalRequest
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonGreen
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary
import ee.oversight.hermes.ui.theme.TextTerminal

@Composable
fun InteractiveApprovalCard(
    request: ApprovalRequest,
    language: AppLanguage,
    onResolve: (request: ApprovalRequest, approved: Boolean, mode: ApprovalMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val isArabic = language == AppLanguage.AR

    val warningBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1C1309),
            Color(0xFF120C07)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = NeonAmber)
            .clip(RoundedCornerShape(16.dp))
            .background(warningBg)
            .border(1.5.dp, NeonAmber.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Security Icon + Title + Tool Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.2f))
                            .border(1.dp, NeonAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Alert",
                            tint = NeonAmber,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isArabic) "مطلوب موافقة أمنية" else "SECURITY APPROVAL REQUIRED",
                            style = MonospaceStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonAmber
                            )
                        )
                        Text(
                            text = if (isArabic) "يرغب الوكيل في تنفيذ إجراء حساس" else "Hermes Agent requests permission",
                            style = MonospaceStyle.copy(
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }

                // Tool Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F1B2B))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "[${request.toolName.uppercase()}]",
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reason / Message if available
            if (request.reason.isNotBlank()) {
                Text(
                    text = "${if (isArabic) "السبب: " else "Reason: "}${request.reason}",
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = Color(0xFFFFD580),
                        lineHeight = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
            } else if (request.message.isNotBlank()) {
                Text(
                    text = request.message,
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = Color(0xFFFFD580),
                        lineHeight = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Monospace Command Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF07090F))
                    .border(1.dp, Color(0xFF2E2413), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$ ",
                                style = MonospaceStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTerminal
                                )
                            )
                            Text(
                                text = request.command.ifBlank { "Execute sensitive operation" },
                                style = MonospaceStyle.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    if (request.command.isNotBlank()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                clipboardManager.setText(AnnotatedString(request.command))
                                Toast.makeText(
                                    context,
                                    if (isArabic) "تم نسخ الأمر" else "Command copied",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy command",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF2E2413), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Primary Action Row: [ DENY ] and [ ALLOW ONCE ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Deny Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonRed.copy(alpha = 0.15f))
                        .border(1.dp, NeonRed.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onResolve(request, false, ApprovalMode.MANUAL)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Deny",
                            tint = NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "رفض" else "DENY",
                            style = MonospaceStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonRed
                            )
                        )
                    }
                }

                // Allow Once Button
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .border(1.dp, NeonGreen, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onResolve(request, true, ApprovalMode.MANUAL)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Allow",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "سماح (مرة واحدة)" else "ALLOW ONCE",
                            style = MonospaceStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Scope Row: [ ALLOW SESSION ] and [ ALLOW ALL ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Allow Session Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF261D12))
                        .border(1.dp, NeonAmber.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onResolve(request, true, ApprovalMode.ALLOW_SESSION)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Allow Session",
                            tint = NeonAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isArabic) "سماح للجلسة" else "ALLOW SESSION",
                            style = MonospaceStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber
                            )
                        )
                    }
                }

                // Allow All Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E152E))
                        .border(1.dp, NeonViolet.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onResolve(request, true, ApprovalMode.ALLOW_ALL)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Allow All",
                            tint = Color(0xFFA6B4FE),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isArabic) "سماح لجميع الأوامر" else "ALLOW ALL (AUTO)",
                            style = MonospaceStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA6B4FE)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle helper
            Text(
                text = if (isArabic)
                    "• سماح للجلسة أو لجميع الأوامر يتيح لهيرمز العمل ذاتياً دون تكرار طلب الموافقة."
                else
                    "• Allow Session or Allow All enables autonomous mode without repeated prompts.",
                style = MonospaceStyle.copy(
                    fontSize = 9.sp,
                    color = TextSecondary.copy(alpha = 0.8f),
                    lineHeight = 12.sp
                )
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionConfig
import com.example.model.ConnectionStatus
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.MonospaceStyle
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CyberpunkTopBar(
    status: ConnectionStatus,
    config: ConnectionConfig,
    pingMs: Long,
    onToggleDemoMode: (Boolean) -> Unit,
    onClearChat: () -> Unit,
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
        ConnectionStatus.CONNECTED -> NeonGreen to "TAILSCALE LIVE"
        ConnectionStatus.CONNECTING -> NeonAmber to "CONNECTING..."
        ConnectionStatus.DEMO_MODE -> NeonVioletLight to "DEMO MODE"
        ConnectionStatus.DISCONNECTED -> NeonRed to "OFFLINE"
        ConnectionStatus.ERROR -> NeonRed to "CONN ERROR"
    }

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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16192E))
                        .border(1.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Hermes Logo",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HERMES",
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
                        text = "Windows 11 Agent Gateway",
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    )
                }
            }

            // Top Actions: Demo Mode Toggle & Clear
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Demo Mode Switch Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (config.isDemoMode) NeonViolet.copy(alpha = 0.2f) else CyberSurface)
                        .border(
                            1.dp,
                            if (config.isDemoMode) NeonViolet else CyberSurfaceBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onToggleDemoMode(!config.isDemoMode) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("toggle_demo_mode"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (config.isDemoMode) "DEMO ON" else "DEMO OFF",
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (config.isDemoMode) NeonVioletLight else TextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
            // Live status & IP
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .alpha(if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.DEMO_MODE) pulseAlpha else 1f)
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
                Text(
                    text = if (config.isDemoMode) "SIMULATED" else "${config.tailscaleIp}:${config.port}",
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                )
            }

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
}

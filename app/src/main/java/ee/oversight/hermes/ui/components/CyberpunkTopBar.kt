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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource
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
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.ConnectionStatus
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurface
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonGreen
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.NeonVioletLight
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary

@Composable
fun CyberpunkTopBar(
    status: ConnectionStatus,
    config: ConnectionConfig,
    pingMs: Long,
    language: AppLanguage,
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
        ConnectionStatus.CONNECTED -> NeonGreen to (if (config.isRemoteGatewayActive) {
            if (language == AppLanguage.AR) "بوابة عن بعد متصلة" else "REMOTE GW ONLINE"
        } else HermesStrings.statusConnected(language))
        ConnectionStatus.CONNECTING -> NeonAmber to HermesStrings.statusConnecting(language)
        ConnectionStatus.DEMO_MODE -> NeonVioletLight to HermesStrings.statusDemoMode(language)
        ConnectionStatus.DISCONNECTED -> NeonRed to (if (config.isRemoteGatewayActive) {
            if (language == AppLanguage.AR) "البوابة غير متصلة" else "GATEWAY OFFLINE"
        } else HermesStrings.statusDisconnected(language))
        ConnectionStatus.ERROR -> NeonRed to HermesStrings.statusError(language)
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
                        .border(1.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = ee.oversight.hermes.R.drawable.ic_hermes_logo),
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

            // Top Actions: Demo Mode Toggle & Clear
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Remote Gateway / Demo Mode Switch Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (config.isRemoteGatewayActive) NeonCyan.copy(alpha = 0.15f) else NeonViolet.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (config.isRemoteGatewayActive) NeonCyan else NeonViolet,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onToggleDemoMode(!config.isDemoMode) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("toggle_demo_mode"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (config.isRemoteGatewayActive) NeonCyan else NeonVioletLight)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (config.isRemoteGatewayActive) {
                            if (language == AppLanguage.AR) "بوابة عن بعد" else "REMOTE GW"
                        } else {
                            if (language == AppLanguage.AR) "محاكاة" else "SIMULATOR"
                        },
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (config.isRemoteGatewayActive) NeonCyan else NeonVioletLight
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
                        contentDescription = HermesStrings.clearChat(language),
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
                    text = if (config.isDemoMode) {
                        HermesStrings.simulated(language)
                    } else {
                        config.effectiveGatewayUrl.removePrefix("http://").removePrefix("https://")
                    },
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

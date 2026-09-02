package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import com.example.model.DiscoveredGateway
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PingResult
import com.example.model.AppLanguage
import com.example.model.ConnectionConfig
import com.example.model.ConnectionStatus
import com.example.model.HermesStrings
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.MonospaceStyle
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTerminal

@Composable
fun GatewayConfigScreen(
    config: ConnectionConfig,
    connectionStatus: ConnectionStatus,
    pingResult: PingResult?,
    isPinging: Boolean,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onSaveConfig: (ConnectionConfig) -> Unit,
    onTestPing: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    discoveredGateway: DiscoveredGateway? = null,
    isDiscovering: Boolean = false,
    onStartAutoDiscovery: () -> Unit = {},
    onConnectDiscovered: (DiscoveredGateway, Boolean) -> Unit = { _, _ -> },
    onImportFromQr: (String) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var qrInput by remember { mutableStateOf("") }
    var qrFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showQrManualInput by remember { mutableStateOf(false) }

    var ipInput by remember(config.tailscaleIp) { mutableStateOf(config.tailscaleIp) }
    var portInput by remember(config.port) { mutableStateOf(config.port.toString()) }
    var remoteGatewayUrlInput by remember(config.remoteGatewayUrl) { mutableStateOf(config.remoteGatewayUrl) }
    var useCustomGatewayUrl by remember(config.useCustomGatewayUrl) { mutableStateOf(config.useCustomGatewayUrl) }
    var useHttps by remember(config.useHttps) { mutableStateOf(config.useHttps) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showApiKeyGuide by remember { mutableStateOf(true) }

    val calculatedTargetUrl = remember(useCustomGatewayUrl, remoteGatewayUrlInput, ipInput, portInput, useHttps) {
        if (useCustomGatewayUrl && remoteGatewayUrlInput.isNotBlank()) {
            remoteGatewayUrlInput.trim().trimEnd('/')
        } else {
            val scheme = if (useHttps) "https" else "http"
            "$scheme://${ipInput.trim()}:${portInput.trim()}"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = HermesStrings.gatewayTitle(language),
                    style = MonospaceStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = HermesStrings.gatewaySubtitle(language),
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        // Language Selection Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .testTag("language_settings_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = HermesStrings.languageSectionTitle(language),
                        style = MonospaceStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = HermesStrings.languageSectionDesc(language),
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // English Option Button
                    val isEn = language == AppLanguage.EN
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isEn) NeonViolet.copy(alpha = 0.25f) else CyberSurfaceElevated)
                            .border(
                                1.dp,
                                if (isEn) NeonViolet else CyberSurfaceBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onLanguageChange(AppLanguage.EN) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("lang_en_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isEn) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonVioletLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "English",
                                style = MonospaceStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isEn) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isEn) Color.White else TextSecondary
                                )
                            )
                            Text(
                                text = "EN",
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    color = if (isEn) NeonVioletLight else TextSecondary.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    // Arabic Option Button
                    val isAr = language == AppLanguage.AR
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAr) NeonCyan.copy(alpha = 0.25f) else CyberSurfaceElevated)
                            .border(
                                1.dp,
                                if (isAr) NeonCyan else CyberSurfaceBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onLanguageChange(AppLanguage.AR) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("lang_ar_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isAr) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "العربية",
                                style = MonospaceStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (isAr) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAr) Color.White else TextSecondary
                                )
                            )
                            Text(
                                text = "Arabic",
                                style = MonospaceStyle.copy(
                                    fontSize = 10.sp,
                                    color = if (isAr) NeonCyan else TextSecondary.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Auto-Discovery Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(
                        1.dp,
                        if (discoveredGateway != null) NeonGreen else if (isDiscovering) NeonCyan else CyberSurfaceBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
                    .testTag("auto_discovery_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiFind,
                            contentDescription = null,
                            tint = if (discoveredGateway != null) NeonGreen else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = HermesStrings.autoDiscoverTitle(language),
                            style = MonospaceStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    if (isDiscovering) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = HermesStrings.autoDiscoverBtnRescan(language),
                            style = MonospaceStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .clickable { onStartAutoDiscovery() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isDiscovering) {
                    Text(
                        text = HermesStrings.autoDiscoverSearching(language),
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    )
                } else if (discoveredGateway != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C1917))
                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = HermesStrings.autoDiscoverFoundTitle(language, discoveredGateway.hostname),
                                style = MonospaceStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "IP: ${discoveredGateway.ip}:${discoveredGateway.port}" +
                                    (if (!discoveredGateway.tailscaleIp.isNullOrBlank()) " | Tailscale: ${discoveredGateway.tailscaleIp}" else ""),
                            style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onConnectDiscovered(discoveredGateway, false) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = HermesStrings.autoDiscoverBtnConnect(language),
                                    style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                )
                            }

                            if (!discoveredGateway.tailscaleIp.isNullOrBlank()) {
                                Button(
                                    onClick = { onConnectDiscovered(discoveredGateway, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(1.dp, NeonViolet, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = "Tailscale",
                                        style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonVioletLight)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = HermesStrings.autoDiscoverNotFound(language),
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }

        // QR Code Pairing Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .testTag("qr_pairing_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = NeonVioletLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = HermesStrings.qrPairTitle(language),
                        style = MonospaceStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = HermesStrings.qrPairDesc(language),
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text ?: ""
                            if (clipText.isNotBlank()) {
                                val success = onImportFromQr(clipText)
                                qrFeedbackMessage = if (success) {
                                    HermesStrings.qrSuccessToast(language)
                                } else {
                                    HermesStrings.qrErrorToast(language)
                                }
                            } else {
                                qrFeedbackMessage = HermesStrings.qrErrorToast(language)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = HermesStrings.qrBtnPaste(language),
                            style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }

                    Button(
                        onClick = { showQrManualInput = !showQrManualInput },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = if (showQrManualInput) "▲" else "▼",
                            style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        )
                    }
                }

                qrFeedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.startsWith("✅")) NeonGreen else NeonAmber
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                AnimatedVisibility(visible = showQrManualInput) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = qrInput,
                            onValueChange = { qrInput = it },
                            placeholder = { Text(HermesStrings.qrInputPlaceholder(language), style = MonospaceStyle.copy(fontSize = 10.sp)) },
                            singleLine = true,
                            textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = CyberSurfaceBorder,
                                focusedContainerColor = CyberSurfaceElevated,
                                unfocusedContainerColor = CyberSurfaceElevated
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (qrInput.isNotBlank()) {
                                    val success = onImportFromQr(qrInput)
                                    qrFeedbackMessage = if (success) {
                                        qrInput = ""
                                        showQrManualInput = false
                                        HermesStrings.qrSuccessToast(language)
                                    } else {
                                        HermesStrings.qrErrorToast(language)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = HermesStrings.qrBtnApply(language),
                                style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            )
                        }
                    }
                }
            }
        }

        // Remote Gateway Master Feature Card
        item {
            val isRemoteGatewayActive = !config.isDemoMode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(
                        1.dp,
                        if (isRemoteGatewayActive) NeonCyan else CyberSurfaceBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
                    .testTag("remote_gateway_master_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = HermesStrings.remoteGatewayModeTitle(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRemoteGatewayActive) NeonCyan else TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRemoteGatewayActive) NeonCyan else NeonViolet)
                            )
                        }
                        Text(
                            text = HermesStrings.remoteGatewayModeDesc(language),
                            style = MonospaceStyle.copy(
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = isRemoteGatewayActive,
                        onCheckedChange = { active -> onToggleDemoMode(!active) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonCyan,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("remote_gateway_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Mode Badge Ribbon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRemoteGatewayActive) NeonCyan.copy(alpha = 0.1f) else NeonViolet.copy(alpha = 0.1f))
                        .border(
                            1.dp,
                            if (isRemoteGatewayActive) NeonCyan.copy(alpha = 0.4f) else NeonViolet.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRemoteGatewayActive) HermesStrings.remoteGatewayActiveBadge(language) else HermesStrings.remoteGatewayInactiveBadge(language),
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRemoteGatewayActive) NeonCyan else NeonVioletLight
                        )
                    )
                }
            }
        }

        // Configuration Form Card (Remote Gateway Target & Parameters)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = HermesStrings.networkParamsTitle(language),
                    style = MonospaceStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Selector: Tailscale IP vs Custom URL
                Text(
                    text = HermesStrings.gatewayTypeLabel(language),
                    style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Direct Tailscale IP
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!useCustomGatewayUrl) NeonCyan.copy(alpha = 0.15f) else CyberSurfaceElevated)
                            .border(1.dp, if (!useCustomGatewayUrl) NeonCyan else CyberSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { useCustomGatewayUrl = false }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = HermesStrings.gatewayTypeTailscaleIp(language),
                            style = MonospaceStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = if (!useCustomGatewayUrl) FontWeight.Bold else FontWeight.Normal,
                                color = if (!useCustomGatewayUrl) NeonCyan else TextSecondary
                            )
                        )
                    }

                    // Option 2: Custom URL
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (useCustomGatewayUrl) NeonCyan.copy(alpha = 0.15f) else CyberSurfaceElevated)
                            .border(1.dp, if (useCustomGatewayUrl) NeonCyan else CyberSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { useCustomGatewayUrl = true }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = HermesStrings.gatewayTypeCustomUrl(language),
                            style = MonospaceStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = if (useCustomGatewayUrl) FontWeight.Bold else FontWeight.Normal,
                                color = if (useCustomGatewayUrl) NeonCyan else TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (useCustomGatewayUrl) {
                    // Custom Remote Gateway URL Input Field
                    OutlinedTextField(
                        value = remoteGatewayUrlInput,
                        onValueChange = { remoteGatewayUrlInput = it },
                        label = { Text(HermesStrings.remoteGatewayUrlLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        placeholder = { Text(HermesStrings.remoteGatewayUrlPlaceholder(language), style = MonospaceStyle.copy(fontSize = 10.sp)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lan, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberSurfaceBorder,
                            focusedContainerColor = CyberSurfaceElevated,
                            unfocusedContainerColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_remote_gateway_url")
                    )
                } else {
                    // Tailscale IP Field
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text(HermesStrings.tailscaleIpLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        placeholder = { Text("e.g. 100.84.12.93", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lan, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberSurfaceBorder,
                            focusedContainerColor = CyberSurfaceElevated,
                            unfocusedContainerColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_tailscale_ip")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Port Field & HTTPS Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text(HermesStrings.portLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                            placeholder = { Text("8080", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberSurfaceBorder,
                                focusedContainerColor = CyberSurfaceElevated,
                                unfocusedContainerColor = CyberSurfaceElevated
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_port")
                        )

                        // HTTPS Toggle
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceElevated)
                                .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (useHttps) "HTTPS" else "HTTP",
                                style = MonospaceStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (useHttps) NeonGreen else TextSecondary)
                            )
                            Switch(
                                checked = useHttps,
                                onCheckedChange = { useHttps = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NeonGreen,
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Presets Row
                Text(
                    text = HermesStrings.gatewayPresetsTitle(language),
                    style = MonospaceStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preset 1: 100.84.12.93:8080
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                useCustomGatewayUrl = false
                                ipInput = "100.84.12.93"
                                portInput = "8080"
                                useHttps = false
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "100.84.12.93",
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonCyan)
                        )
                    }

                    // Preset 2: Funnel / Tunnel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                useCustomGatewayUrl = true
                                remoteGatewayUrlInput = "https://hermes-pc.tailnet.ts.net"
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tailscale DNS",
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonCyan)
                        )
                    }

                    // Preset 3: 10.0.2.2 Loopback
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                useCustomGatewayUrl = false
                                ipInput = "10.0.2.2"
                                portInput = "8080"
                                useHttps = false
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "10.0.2.2 (Local)",
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // API Key Field
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(HermesStrings.apiKeyLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    placeholder = { Text("hermes_live_key_...", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isKeyVisible) "Hide key" else "Show key",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonAmber,
                        unfocusedBorderColor = CyberSurfaceBorder,
                        focusedContainerColor = CyberSurfaceElevated,
                        unfocusedContainerColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_api_key")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // API Key Fast Setup Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Action: No Key Needed (Ollama / Local)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (apiKeyInput.isEmpty()) NeonGreen.copy(alpha = 0.15f) else CyberSurfaceElevated)
                            .border(1.dp, if (apiKeyInput.isEmpty()) NeonGreen else CyberSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                apiKeyInput = ""
                            }
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = HermesStrings.btnNoKeyNeeded(language),
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = if (apiKeyInput.isEmpty()) NeonGreen else TextSecondary, fontWeight = FontWeight.Bold)
                        )
                    }

                    // Quick Action: OpenRouter Cloud
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                useCustomGatewayUrl = true
                                remoteGatewayUrlInput = "https://openrouter.ai/api/v1"
                            }
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = HermesStrings.btnOpenRouterPreset(language),
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonAmber, fontWeight = FontWeight.Bold)
                        )
                    }

                    // Toggle Guide
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, if (showApiKeyGuide) NeonCyan else CyberSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable { showApiKeyGuide = !showApiKeyGuide }
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showApiKeyGuide) "▲ إخفاء" else "▼ الشرح",
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Interactive API Key Sources Guide Card
                AnimatedVisibility(visible = showApiKeyGuide) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, NeonAmber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = HermesStrings.whereToGetApiKeyTitle(language),
                                style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = HermesStrings.apiKeyGuideInfo(language),
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Source 1: Ollama
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D14))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = HermesStrings.apiKeySourceOllama(language),
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonGreen, lineHeight = 14.sp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ollama run hermes3",
                                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextTerminal)
                                )
                                Text(
                                    text = "تطبيق منفذ 11434",
                                    style = MonospaceStyle.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .clickable {
                                            useCustomGatewayUrl = false
                                            portInput = "11434"
                                            useHttps = false
                                            apiKeyInput = ""
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Source 2: LM Studio
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D14))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = HermesStrings.apiKeySourceLmStudio(language),
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextPrimary, lineHeight = 14.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "تطبيق منفذ 1234",
                                    style = MonospaceStyle.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .clickable {
                                            useCustomGatewayUrl = false
                                            portInput = "1234"
                                            useHttps = false
                                            apiKeyInput = ""
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Source 3: Custom Server
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D14))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = HermesStrings.apiKeySourceCustomServer(language),
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextPrimary, lineHeight = 14.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Source 4: OpenRouter
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D14))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = HermesStrings.apiKeySourceOpenRouter(language),
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonAmber, lineHeight = 14.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Route Target Preview Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C1420))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = HermesStrings.remoteGatewayRoutingBanner(language),
                        style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = calculatedTargetUrl,
                        style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: Test Ping & Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Test Ping Button
                    Button(
                        onClick = {
                            // First save current values to config so test ping tests current target
                            val parsedPort = portInput.toIntOrNull() ?: 8080
                            onSaveConfig(
                                config.copy(
                                    tailscaleIp = ipInput.trim(),
                                    port = parsedPort,
                                    remoteGatewayUrl = remoteGatewayUrlInput.trim(),
                                    useCustomGatewayUrl = useCustomGatewayUrl,
                                    useHttps = useHttps,
                                    apiKey = apiKeyInput.trim(),
                                    isDemoMode = config.isDemoMode
                                )
                            )
                            onTestPing()
                        },
                        enabled = !isPinging,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16202C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .testTag("test_ping_button")
                    ) {
                        if (isPinging) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = NeonCyan, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = HermesStrings.testPingButton(language),
                                style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            )
                        }
                    }

                    // Save Config Button
                    Button(
                        onClick = {
                            val parsedPort = portInput.toIntOrNull() ?: 8080
                            onSaveConfig(
                                config.copy(
                                    tailscaleIp = ipInput.trim(),
                                    port = parsedPort,
                                    remoteGatewayUrl = remoteGatewayUrlInput.trim(),
                                    useCustomGatewayUrl = useCustomGatewayUrl,
                                    useHttps = useHttps,
                                    apiKey = apiKeyInput.trim(),
                                    isDemoMode = false // Explicitly activate Remote Gateway
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = HermesStrings.applyButton(language),
                            style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        )
                    }
                }
            }
        }

        // Ping Result Banner
        item {
            AnimatedVisibility(visible = pingResult != null) {
                pingResult?.let { res ->
                    val isGood = res.isSuccess
                    val bannerBorder = if (isGood) NeonGreen else NeonRed
                    val bannerBg = if (isGood) Color(0xFF0C1F16) else Color(0xFF261014)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bannerBg)
                            .border(1.dp, bannerBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isGood) NeonGreen else NeonRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGood) HermesStrings.handshakeSuccess(language) else HermesStrings.connectionFailed(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGood) NeonGreen else NeonRed
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${res.latencyMs}ms",
                                style = MonospaceStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = res.message,
                            style = MonospaceStyle.copy(
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Tailscale & Windows 11 Firewall Setup Guide Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = HermesStrings.guideTitle(language),
                        style = MonospaceStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = HermesStrings.guideContent(language),
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06090E))
                        .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "netsh advfirewall firewall add rule name=\"HermesAgent\" dir=in action=allow protocol=TCP localport=8080 remoteip=100.64.0.0/10",
                        style = MonospaceStyle.copy(
                            fontSize = 10.sp,
                            color = TextTerminal,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        }

        // Full Hermes Agent Server Python Script Card
        item {
            val clipboardManager = LocalClipboardManager.current
            var isScriptCopied by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = HermesStrings.fullHermesAgentTitle(language),
                            style = MonospaceStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        )
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(com.example.model.HermesServerScript.pythonScript))
                            isScriptCopied = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScriptCopied) NeonGreen else CyberSurfaceElevated
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .border(1.dp, if (isScriptCopied) NeonGreen else CyberSurfaceBorder, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (isScriptCopied) Color.Black else NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isScriptCopied) "تم النسخ ✓" else "نسخ الكود",
                            style = MonospaceStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isScriptCopied) Color.Black else TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = HermesStrings.fullHermesAgentDesc(language),
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = HermesStrings.runServerInstructions(language),
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        color = TextTerminal,
                        lineHeight = 15.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06090E))
                        .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

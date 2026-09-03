package ee.oversight.hermes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.data.PingResult
import ee.oversight.hermes.data.HermesAppLog
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.ConnectionStatus
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.model.DiscoveredGateway
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurface
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.CyberSurfaceElevated
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonGreen
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.NeonVioletLight
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary

/**
 * Gateway configuration screen - cleaned up:
 *  - No QR pairing section (removed)
 *  - No demo/simulator mode (removed)
 *  - Language picker as dropdown
 *  - Network parameters inside a collapsible section
 *  - API key help simplified to .env only
 *  - About us at the bottom
 */
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
    logs: List<HermesAppLog.LogEntry> = emptyList(),
    onRefreshLogs: () -> Unit = {},
    onClearLogs: () -> Unit = {},
    savedProfiles: List<String> = emptyList(),
    activeProfile: String = "",
    onSaveProfile: (String) -> Unit = {},
    onLoadProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    discoveredGateway: DiscoveredGateway? = null,
    isDiscovering: Boolean = false,
    onStartAutoDiscovery: () -> Unit = {},
    onConnectDiscovered: (DiscoveredGateway, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showNetworkParams by remember { mutableStateOf(true) }
    var languageMenuOpen by remember { mutableStateOf(false) }

    var ipInput by remember(config.tailscaleIp) { mutableStateOf(config.tailscaleIp) }
    var portInput by remember(config.port) { mutableStateOf(config.port.toString()) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var remoteGatewayUrlInput by remember(config.remoteGatewayUrl) { mutableStateOf(config.remoteGatewayUrl) }
    var useCustomGatewayUrl by remember(config.useCustomGatewayUrl) { mutableStateOf(config.useCustomGatewayUrl) }
    var useHttpsInput by remember(config.useHttps) { mutableStateOf(config.useHttps) }
    var profileNameInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var apiKeyCopied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ===== Header =====
        Text(
            text = HermesStrings.gatewayTitle(language),
            style = MonospaceStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        )
        Text(
            text = HermesStrings.gatewaySubtitle(language),
            style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ===== Language picker (dropdown) =====
        SectionCard {
            SectionHeader(
                title = HermesStrings.languageSectionTitle(language),
                icon = { Icon(Icons.Default.Language, null, tint = NeonViolet, modifier = Modifier.size(16.dp)) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = HermesStrings.languageSectionDesc(language),
                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { languageMenuOpen = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "العربية" else "English",
                        style = MonospaceStyle.copy(fontSize = 13.sp, color = TextPrimary)
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = languageMenuOpen,
                    onDismissRequest = { languageMenuOpen = false },
                    containerColor = CyberSurfaceElevated
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("English", style = MonospaceStyle.copy(fontSize = 12.sp, color = if (language == AppLanguage.EN) NeonCyan else TextPrimary))
                        },
                        onClick = {
                            languageMenuOpen = false
                            if (language != AppLanguage.EN) onLanguageChange(AppLanguage.EN)
                        },
                        trailingIcon = { if (language == AppLanguage.EN) Icon(Icons.Default.Check, null, tint = NeonCyan, modifier = Modifier.size(14.dp)) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("العربية", style = MonospaceStyle.copy(fontSize = 12.sp, color = if (language == AppLanguage.AR) NeonCyan else TextPrimary))
                        },
                        onClick = {
                            languageMenuOpen = false
                            if (language != AppLanguage.AR) onLanguageChange(AppLanguage.AR)
                        },
                        trailingIcon = { if (language == AppLanguage.AR) Icon(Icons.Default.Check, null, tint = NeonCyan, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== Network parameters (collapsible) =====
        SectionCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNetworkParams = !showNetworkParams },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = HermesStrings.networkParamsTitle(language),
                    icon = { Icon(Icons.Default.NetworkCheck, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) }
                )
                Icon(
                    imageVector = if (showNetworkParams) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            AnimatedVisibility(visible = showNetworkParams) {
                Column {
                    // Gateway type choice
                    Text(
                        text = HermesStrings.gatewayTypeLabel(language),
                        style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoicePill(
                            text = if (language == AppLanguage.AR) "عنوان IP" else "IP:Port",
                            selected = !useCustomGatewayUrl,
                            accent = NeonCyan,
                            onClick = { useCustomGatewayUrl = false }
                        )
                        ChoicePill(
                            text = if (language == AppLanguage.AR) "رابط مخصص" else "Custom URL",
                            selected = useCustomGatewayUrl,
                            accent = NeonViolet,
                            onClick = { useCustomGatewayUrl = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!useCustomGatewayUrl) {
                        // IP field
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text(HermesStrings.tailscaleIpLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                            placeholder = { Text("100.x.x.x", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                            singleLine = true,
                            textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text(HermesStrings.portLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                            placeholder = { Text("8080", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Protocol choice: HTTP / HTTPS
                        Text(
                            text = if (language == AppLanguage.AR) "البروتوكول:" else "Protocol:",
                            style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoicePill(
                                text = "HTTP",
                                selected = !useHttpsInput,
                                accent = NeonGreen,
                                onClick = { useHttpsInput = false }
                            )
                            ChoicePill(
                                text = "HTTPS",
                                selected = useHttpsInput,
                                accent = NeonCyan,
                                onClick = { useHttpsInput = true }
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = remoteGatewayUrlInput,
                            onValueChange = { remoteGatewayUrlInput = it },
                            label = { Text(HermesStrings.remoteGatewayUrlLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                            placeholder = { Text(HermesStrings.remoteGatewayUrlPlaceholder(language), style = MonospaceStyle.copy(fontSize = 10.sp)) },
                            singleLine = true,
                            textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 12.sp),
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Key field
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(HermesStrings.apiKeyLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        placeholder = { Text("API_SERVER_KEY", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 11.sp),
                        colors = fieldColors(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(apiKeyInput))
                                    apiKeyCopied = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = if (apiKeyCopied) NeonGreen else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Where to get the key - SIMPLE, .env only
                    Text(
                        text = if (language == AppLanguage.AR)
                            "💡 المفتاح من ملف .env على جهاز الـ PC: افتح %LOCALAPPDATA%\\hermes\\.env وانسخ القيمة بعد API_SERVER_KEY="
                        else
                            "💡 Key is in .env on the PC: open %LOCALAPPDATA%\\hermes\\.env and copy the value after API_SERVER_KEY=",
                        style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val parsedPort = portInput.toIntOrNull() ?: 8080
                                onSaveConfig(
                                    config.copy(
                                        tailscaleIp = ipInput.trim(),
                                        port = parsedPort,
                                        remoteGatewayUrl = remoteGatewayUrlInput.trim(),
                                        useCustomGatewayUrl = useCustomGatewayUrl,
                                        apiKey = apiKeyInput.trim(),
                                        useHttps = useHttpsInput
                                    )
                                )
                                onTestPing()
                            },
                            enabled = !isPinging,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            if (isPinging) {
                                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.Black, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.NetworkCheck, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = HermesStrings.testPingButton(language),
                                    style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val parsedPort = portInput.toIntOrNull() ?: 8080
                                onSaveConfig(
                                    config.copy(
                                        tailscaleIp = ipInput.trim(),
                                        port = parsedPort,
                                        remoteGatewayUrl = remoteGatewayUrlInput.trim(),
                                        useCustomGatewayUrl = useCustomGatewayUrl,
                                        apiKey = apiKeyInput.trim(),
                                        useHttps = useHttpsInput
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16202C)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = HermesStrings.applyButton(language),
                                style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== Saved connection profiles =====
        SectionCard {
            Text(
                text = if (language == AppLanguage.AR) "الاتصالات المحفوظة" else "SAVED CONNECTIONS",
                style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonViolet)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Save current as named profile
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text(if (language == AppLanguage.AR) "اسم الاتصال" else "Connection name", style = MonospaceStyle.copy(fontSize = 10.sp)) },
                    placeholder = { Text(if (language == AppLanguage.AR) "مثال: لابتوب البيت" else "e.g. Home laptop", style = MonospaceStyle.copy(fontSize = 10.sp)) },
                    singleLine = true,
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 12.sp),
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (profileNameInput.isNotBlank()) {
                            onSaveProfile(profileNameInput.trim())
                            profileNameInput = ""
                        }
                    },
                    enabled = profileNameInput.isNotBlank() && config.tailscaleIp.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) "حفظ" else "SAVE",
                        style = MonospaceStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profile list
            if (savedProfiles.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.AR) "لا توجد اتصالات محفوظة. املأ البيانات وحفظها باسم." else "No saved connections. Fill in the details and save with a name.",
                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                )
            } else {
                savedProfiles.forEach { name ->
                    val isActive = name == activeProfile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) NeonViolet.copy(alpha = 0.15f) else CyberSurfaceElevated)
                            .border(1.dp, if (isActive) NeonViolet else CyberSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { onLoadProfile(name) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Save,
                            contentDescription = null,
                            tint = if (isActive) NeonGreen else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MonospaceStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) NeonGreen else TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        // Load icon (tap row loads too, but give explicit hint)
                        if (!isActive) {
                            Icon(Icons.Default.Save, contentDescription = "Load", tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(onClick = { onDeleteProfile(name) }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== Ping result card =====
        if (pingResult != null) {
            val isGood = pingResult.isSuccess
            val color = if (isGood) NeonGreen else NeonRed
            SectionCard(borderColor = color.copy(alpha = 0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGood) HermesStrings.handshakeSuccess(language) else HermesStrings.connectionFailed(language),
                            style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        )
                        Text(
                            text = pingResult.message,
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ===== Auto-discovery (compact, kept working via beacon) =====
        if (discoveredGateway != null || isDiscovering) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDiscovering) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = HermesStrings.autoDiscoverSearching(language),
                            style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary)
                        )
                    } else if (discoveredGateway != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = HermesStrings.autoDiscoverFoundTitle(language, discoveredGateway.hostname),
                                style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                            )
                            Text(
                                text = "${discoveredGateway.ip}:${discoveredGateway.port}",
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                        }
                        Button(
                            onClick = { onConnectDiscovered(discoveredGateway, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = HermesStrings.autoDiscoverBtnConnect(language),
                                style = MonospaceStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ===== App Logs =====
        SectionCard {
            // Header with refresh + clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = NeonViolet, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "APP LOGS",
                        style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = NeonViolet)
                    )
                }
                Row {
                    // Refresh button
                    IconButton(onClick = onRefreshLogs, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                    // Clear button
                    IconButton(onClick = onClearLogs, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Log list (monospace, scrollable, capped height)
            if (logs.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.AR) "لا توجد سجلات بعد." else "No logs yet.",
                    style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0E14))
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    // Show most recent first (reversed)
                    logs.reversed().forEach { entry ->
                        val color = when (entry.level) {
                            "ERROR" -> NeonRed
                            "WARN" -> Color(0xFFF59E0B)
                            else -> TextSecondary
                        }
                        Text(
                            text = HermesAppLog.formatEntry(entry),
                            style = MonospaceStyle.copy(fontSize = 9.sp, color = color, lineHeight = 12.sp),
                            maxLines = 3
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== About us =====
        SectionCard {
            // Header with icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = NeonViolet, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ABOUT US",
                    style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = NeonViolet)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CyberSurfaceBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Developer card (centered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, NeonViolet.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonViolet.copy(alpha = 0.2f))
                        .border(1.dp, NeonViolet.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MN",
                        style = MonospaceStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = NeonVioletLight)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (language == AppLanguage.AR) "تم التصميم والتطوير بواسطة" else "Designed & developed by",
                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Mohamed Nasr",
                    style = MonospaceStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contact links (clickable)
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

            // Email row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { uriHandler.openUri("mailto:mhmdnsr@oversight.ee") }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Email",
                        style = MonospaceStyle.copy(fontSize = 9.sp, color = TextSecondary)
                    )
                    Text(
                        text = "mhmdnsr@oversight.ee",
                        style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Website row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { uriHandler.openUri("https://oversight.ee") }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Public, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Website",
                        style = MonospaceStyle.copy(fontSize = 9.sp, color = TextSecondary)
                    )
                    Text(
                        text = "oversight.ee",
                        style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = CyberSurfaceBorder,
    focusedContainerColor = CyberSurfaceElevated,
    unfocusedContainerColor = CyberSurfaceElevated
)

@Composable
private fun SectionCard(
    borderColor: Color = CyberSurfaceBorder,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurface)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(title: String, icon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        )
    }
}

@Composable
private fun ChoicePill(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.15f) else CyberSurfaceElevated)
            .border(1.dp, if (selected) accent else CyberSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MonospaceStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (selected) accent else TextSecondary),
            maxLines = 1
        )
    }
}

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
import androidx.compose.material.icons.filled.Computer
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
import androidx.compose.material.icons.filled.School
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
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
 *  - Network parameters with device name and locked/editing mode
 *  - Devices list with live toggle per device
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
    onToggleDeviceConnection: (String, Boolean) -> Unit = { _, _ -> },
    discoveredGateway: DiscoveredGateway? = null,
    isDiscovering: Boolean = false,
    onStartAutoDiscovery: () -> Unit = {},
    onConnectDiscovered: (DiscoveredGateway, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var languageMenuOpen by remember { mutableStateOf(false) }

    val hasSavedDevices = remember(savedProfiles, config.tailscaleIp) {
        savedProfiles.isNotEmpty() || config.tailscaleIp.isNotBlank()
    }
    var isEditing by remember(hasSavedDevices) { mutableStateOf(!hasSavedDevices) }

    var ipInput by remember(config.tailscaleIp) { mutableStateOf(config.tailscaleIp) }
    var portInput by remember(config.port) { mutableStateOf(config.port.toString()) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var remoteGatewayUrlInput by remember(config.remoteGatewayUrl) { mutableStateOf(config.remoteGatewayUrl) }
    var useCustomGatewayUrl by remember(config.useCustomGatewayUrl) { mutableStateOf(config.useCustomGatewayUrl) }
    var useHttpsInput by remember(config.useHttps) { mutableStateOf(config.useHttps) }
    var profileNameInput by remember(activeProfile) { mutableStateOf(activeProfile) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var apiKeyCopied by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

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

        // ===== Network parameters (with device name & lock/edit mode) =====
        SectionCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasSavedDevices) { isEditing = !isEditing },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = HermesStrings.networkParamsTitle(language),
                    icon = { Icon(Icons.Default.NetworkCheck, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) }
                )
                if (hasSavedDevices) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isEditing) NeonViolet.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.12f))
                                .border(1.dp, if (isEditing) NeonViolet.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isEditing) HermesStrings.editingModeBadge(language) else HermesStrings.lockedModeBadge(language),
                                style = MonospaceStyle.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEditing) NeonVioletLight else NeonCyan
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isEditing) "Lock / Collapse" else "Unlock / Edit",
                            tint = if (isEditing) NeonVioletLight else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            val isFieldsLocked = hasSavedDevices && !isEditing

            Column {
                // Device Name (FIRST FIELD)
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { if (!isFieldsLocked) profileNameInput = it },
                    readOnly = isFieldsLocked,
                    enabled = !isFieldsLocked,
                    label = { Text(HermesStrings.deviceNameLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    placeholder = { Text(HermesStrings.deviceNamePlaceholder(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFieldsLocked) Icons.Default.Lock else Icons.Default.Computer,
                            contentDescription = null,
                            tint = if (isFieldsLocked) TextSecondary else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    singleLine = true,
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                    colors = fieldColors(isFieldsLocked),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                        onClick = { if (!isFieldsLocked) useCustomGatewayUrl = false }
                    )
                    ChoicePill(
                        text = if (language == AppLanguage.AR) "رابط مخصص" else "Custom URL",
                        selected = useCustomGatewayUrl,
                        accent = NeonViolet,
                        onClick = { if (!isFieldsLocked) useCustomGatewayUrl = true }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!useCustomGatewayUrl) {
                    // IP field
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { if (!isFieldsLocked) ipInput = it },
                        readOnly = isFieldsLocked,
                        enabled = !isFieldsLocked,
                        label = { Text(HermesStrings.tailscaleIpLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        placeholder = { Text("100.x.x.x", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        singleLine = true,
                        textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                        colors = fieldColors(isFieldsLocked),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { if (!isFieldsLocked) portInput = it },
                        readOnly = isFieldsLocked,
                        enabled = !isFieldsLocked,
                        label = { Text(HermesStrings.portLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        placeholder = { Text("8080", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 13.sp),
                        colors = fieldColors(isFieldsLocked),
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
                            onClick = { if (!isFieldsLocked) useHttpsInput = false }
                        )
                        ChoicePill(
                            text = "HTTPS",
                            selected = useHttpsInput,
                            accent = NeonCyan,
                            onClick = { if (!isFieldsLocked) useHttpsInput = true }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = remoteGatewayUrlInput,
                        onValueChange = { if (!isFieldsLocked) remoteGatewayUrlInput = it },
                        readOnly = isFieldsLocked,
                        enabled = !isFieldsLocked,
                        label = { Text(HermesStrings.remoteGatewayUrlLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                        placeholder = { Text(HermesStrings.remoteGatewayUrlPlaceholder(language), style = MonospaceStyle.copy(fontSize = 10.sp)) },
                        singleLine = true,
                        textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 12.sp),
                        colors = fieldColors(isFieldsLocked),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // API Key field
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { if (!isFieldsLocked) apiKeyInput = it },
                    readOnly = isFieldsLocked,
                    enabled = !isFieldsLocked,
                    label = { Text(HermesStrings.apiKeyLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    placeholder = { Text("API_SERVER_KEY", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 11.sp),
                    colors = fieldColors(isFieldsLocked),
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
                            val updatedConfig = config.copy(
                                tailscaleIp = ipInput.trim(),
                                port = parsedPort,
                                remoteGatewayUrl = remoteGatewayUrlInput.trim(),
                                useCustomGatewayUrl = useCustomGatewayUrl,
                                apiKey = apiKeyInput.trim(),
                                useHttps = useHttpsInput
                            )
                            if (profileNameInput.isNotBlank()) {
                                onSaveProfile(profileNameInput.trim())
                            }
                            onSaveConfig(updatedConfig)
                            onTestPing()
                            if (hasSavedDevices || profileNameInput.isNotBlank() || updatedConfig.tailscaleIp.isNotBlank()) {
                                isEditing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16202C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = HermesStrings.connectButton(language),
                            style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== Saved devices list =====
        SectionCard {
            SectionHeader(
                title = HermesStrings.devicesSectionTitle(language),
                icon = { Icon(Icons.Default.Computer, null, tint = NeonViolet, modifier = Modifier.size(16.dp)) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (savedProfiles.isEmpty()) {
                Text(
                    text = HermesStrings.noSavedDevices(language),
                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    savedProfiles.forEach { name ->
                        val isActive = name == activeProfile
                        val isConnected = isActive && connectionStatus == ConnectionStatus.CONNECTED

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) NeonViolet.copy(alpha = 0.12f) else CyberSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isConnected) NeonGreen.copy(alpha = 0.8f)
                                    else if (isActive) NeonViolet
                                    else CyberSurfaceBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    // Load profile + connect (same as flipping the device switch ON)
                                    onToggleDeviceConnection(name, true)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = if (isConnected) NeonGreen else if (isActive) NeonVioletLight else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = name,
                                        style = MonospaceStyle.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isConnected) NeonGreen else if (isActive) TextPrimary else TextPrimary.copy(alpha = 0.8f)
                                        ),
                                        maxLines = 1
                                    )
                                    if (isActive) {
                                        Text(
                                            text = HermesStrings.activeBadge(language),
                                            style = MonospaceStyle.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isConnected) NeonGreen else NeonVioletLight
                                            )
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(width = 38.dp, height = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Switch(
                                        checked = isConnected,
                                        onCheckedChange = { checked ->
                                            onToggleDeviceConnection(name, checked)
                                        },
                                        modifier = Modifier.scale(0.65f),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeonGreen,
                                            checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                                            checkedBorderColor = NeonGreen.copy(alpha = 0.7f),
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = Color(0xFF16202C),
                                            uncheckedBorderColor = CyberSurfaceBorder
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteProfile(name) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== Connection help / tutorial (opens a popup dialog) =====
        SectionCard(borderColor = NeonViolet.copy(alpha = 0.35f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showHelpDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, null, tint = NeonVioletLight, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.AR) "شرح التوصيل والخطوات" else "HOW TO CONNECT",
                        style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = NeonVioletLight)
                    )
                    Text(
                        text = if (language == AppLanguage.AR) "خطوات التوصيل وجلب مفتاح Hermes API" else "Step-by-step setup + how to get the Hermes API key",
                        style = MonospaceStyle.copy(fontSize = 9.5.sp, color = TextSecondary),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = NeonVioletLight, modifier = Modifier.size(16.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val clipboardManager = LocalClipboardManager.current
                    val ctx = LocalContext.current
                    // Copy all button
                    IconButton(
                        onClick = {
                            val allLogsText = logs.reversed().joinToString("\n") { HermesAppLog.formatEntry(it) }
                            clipboardManager.setText(AnnotatedString(allLogsText))
                            Toast.makeText(ctx, if (language == AppLanguage.AR) "تم نسخ كافة السجلات" else "All logs copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy all logs", tint = NeonCyan, modifier = Modifier.size(14.dp))
                    }
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

            // Log list (monospace, scrollable, selectable, capped height)
            if (logs.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.AR) "لا توجد سجلات بعد." else "No logs yet.",
                    style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0E14))
                            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                style = MonospaceStyle.copy(fontSize = 9.5.sp, color = color, lineHeight = 13.sp)
                            )
                        }
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

    // ===== How-to-connect popup dialog =====
    if (showHelpDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showHelpDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = NeonVioletLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.AR) "شرح التوصيل" else "HOW TO CONNECT",
                            style = MonospaceStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = NeonVioletLight)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Step blocks
                    HelpStep(
                        number = "1",
                        title = if (language == AppLanguage.AR) "ثبّت Hermes على جهازك" else "Install Hermes on your PC",
                        body = if (language == AppLanguage.AR)
                            "نزّل Hermes Agent على الكمبيوتر اللي هيكون عليه الـ Gateway (Windows أو Linux)."
                        else
                            "Install Hermes Agent on the computer that will run the Gateway (Windows or Linux)."
                    )
                    HelpStep(
                        number = "2",
                        title = if (language == AppLanguage.AR) "شغّل الـ API Server" else "Enable the API Server",
                        body = if (language == AppLanguage.AR)
                            "افتح ملف .env في مجلد Hermes وضع:\nAPI_SERVER_ENABLED=true\nAPI_SERVER_PORT=8080\nوبعدين أعد تشغيل Hermes."
                        else
                            "Open the .env file inside the Hermes folder and set:\nAPI_SERVER_ENABLED=true\nAPI_SERVER_PORT=8080\nThen restart Hermes."
                    )
                    HelpStep(
                        number = "3",
                        title = if (language == AppLanguage.AR) "خد الـ API Key" else "Get the API Key",
                        body = if (language == AppLanguage.AR)
                            "نفس ملف .env فيه سطر API_SERVER_KEY=. انسخ القيمة اللي بعده (الـ key كامل من أول حرف لآخر حرف) والصقها هنا في خانة المفتاح."
                        else
                            "In the same .env file, find the line API_SERVER_KEY=. Copy the whole value after it (the full key, first to last char) and paste it into the key field."
                    )
                    HelpStep(
                        number = "4",
                        title = if (language == AppLanguage.AR) "اكتب العنوان والبورت" else "Enter address & port",
                        body = if (language == AppLanguage.AR)
                            "اكتب الـ Tailscale IP بتاع الجهاز (مثل 100.x.x.x) والبورت 8080 في الحقول فوق. لو على نفس شبكة WiFi من غير Tailscale، اكتب IP الشبكة المحلية."
                        else
                            "Type the PC's Tailscale IP (like 100.x.x.x) and port 8080 in the fields above. On the same WiFi without Tailscale, use the local network IP instead."
                    )
                    HelpStep(
                        number = "5",
                        title = if (language == AppLanguage.AR) "دوس TEST PING" else "Press TEST PING",
                        body = if (language == AppLanguage.AR)
                            "دوس على زر Test Ping. لو ظهرت رسالة نجاح خضرا، دوس اتصال والاتصال هيتحفظ ويشتغل."
                        else
                            "Tap Test Ping. If you get a green success message, tap CONNECT and the connection will be stored and used."
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CyberSurfaceBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Extra tip box (Tailscale)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonCyan.copy(alpha = 0.08f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.AR) "💡 نصيحة: من غير Tailscale؟" else "💡 No Tailscale?",
                            style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == AppLanguage.AR)
                                "طالما الجهازين على نفس شبكة WiFi، اكتب IP الموبايل الشبكي بتاع الـ PC (زي 192.168.1.5) في خانة IP. الـ Tailscale مش شرط، بس مطلوب عشان توصل من أي مكان."
                            else
                                "As long as both devices are on the same WiFi, enter the PC's LAN IP (like 192.168.1.5) in the IP field. Tailscale is not required, but needed to connect from anywhere.",
                            style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Close button
                    Button(
                        onClick = { showHelpDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.AR) "فهمت، تمام" else "GOT IT",
                            style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpStep(number: String, title: String, body: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(NeonViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonVioletLight)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
                text = body,
                style = MonospaceStyle.copy(fontSize = 10.5.sp, color = TextSecondary, lineHeight = 15.sp),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun fieldColors(readOnly: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (readOnly) CyberSurfaceBorder else NeonCyan,
    unfocusedBorderColor = CyberSurfaceBorder,
    focusedContainerColor = CyberSurfaceElevated,
    unfocusedContainerColor = CyberSurfaceElevated,
    disabledBorderColor = CyberSurfaceBorder.copy(alpha = 0.5f),
    disabledContainerColor = CyberSurfaceElevated.copy(alpha = 0.6f),
    disabledTextColor = TextPrimary,
    disabledLabelColor = TextSecondary.copy(alpha = 0.7f),
    disabledPlaceholderColor = TextSecondary.copy(alpha = 0.5f)
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

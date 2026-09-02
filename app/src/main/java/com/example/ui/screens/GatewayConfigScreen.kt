package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PingResult
import com.example.model.ConnectionConfig
import com.example.model.ConnectionStatus
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
    onSaveConfig: (ConnectionConfig) -> Unit,
    onTestPing: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var ipInput by remember(config.tailscaleIp) { mutableStateOf(config.tailscaleIp) }
    var portInput by remember(config.port) { mutableStateOf(config.port.toString()) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

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
                    text = "TAILSCALE SECURE GATEWAY",
                    style = MonospaceStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Peer-to-peer encrypted connection between Mobile & Windows 11",
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        // Demo Mode Banner & Switch
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(
                        1.dp,
                        if (config.isDemoMode) NeonViolet else CyberSurfaceBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BUILT-IN DEMO MODE",
                                style = MonospaceStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.isDemoMode) NeonVioletLight else TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (config.isDemoMode) NeonViolet else Color.Gray)
                            )
                        }
                        Text(
                            text = "تشغيل واجهات التطبيق بالكامل تفاعلياً بمحاكاة بث النصوص وتوليد بيانات CPU/RAM دون الحاجة للاتصال بالكمبيوتر أو الإنترنت.",
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
                        checked = config.isDemoMode,
                        onCheckedChange = onToggleDemoMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonViolet,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("demo_mode_switch")
                    )
                }
            }
        }

        // Configuration Form Card
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
                    text = "NETWORK PARAMETERS",
                    style = MonospaceStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tailscale IP Field
                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("Tailscale Node IP", style = MonospaceStyle.copy(fontSize = 11.sp)) },
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

                // Port Field
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text("Port", style = MonospaceStyle.copy(fontSize = 11.sp)) },
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
                        .fillMaxWidth()
                        .testTag("input_port")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // API Key Field
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Hermes Agent API Key", style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    placeholder = { Text("hermes_sec_...", style = MonospaceStyle.copy(fontSize = 11.sp)) },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: Save & Ping
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Test Ping Button
                    Button(
                        onClick = onTestPing,
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
                                text = "TEST PING",
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
                                    apiKey = apiKeyInput.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "APPLY",
                            style = MonospaceStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                text = if (isGood) "PEER HANDSHAKE SUCCESSFUL" else "CONNECTION FAILED",
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
                        text = "TAILSCALE & FIREWALL GUIDE",
                        style = MonospaceStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = """
1. **لا حاجة لفتح منافذ في الراوتر (Zero Port Forwarding):**
يعمل Tailscale عبر إنشاء نفق WireGuard مشفر بين الهاتف وكمبيوتر Windows 11 مباشرة.

2. **عنوان IP الخاص:**
ابحث عن عنوان IP الكمبيوتر في تطبيق Tailscale (يبدأ دائماً بـ 100.x.x.x) وقم بنسخه في الحقل أعلاه.

3. **أمر جدار الحماية في Windows 11 PowerShell:**
للسماح بالاتصال فقط لأجهزة شبكة Tailscale الآمنة:
                    """.trimIndent(),
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

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

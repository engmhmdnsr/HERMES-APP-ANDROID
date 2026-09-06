package ee.oversight.hermes.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ConnectionConfig
import ee.oversight.hermes.model.GatewayHealth
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.model.ProcessInfo
import ee.oversight.hermes.model.SystemTelemetry
import ee.oversight.hermes.ui.components.CpuHistorySparkline
import ee.oversight.hermes.ui.components.SystemMetricCircularCard
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurface
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.CyberSurfaceElevated
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
fun SystemMonitoringScreen(
    telemetry: SystemTelemetry,
    gatewayHealth: GatewayHealth = GatewayHealth(),
    config: ConnectionConfig,
    language: AppLanguage,
    isSupported: Boolean = true,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server doesn't expose /api/system (stock Hermes install) — explain
        // instead of showing all-zero gauges.
        if (!isSupported) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, NeonViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📡",
                        style = MonospaceStyle.copy(fontSize = 30.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (language == AppLanguage.AR)
                            "هذا الخادم لا يدعم مراقبة النظام"
                        else
                            "This server doesn't support system monitoring",
                        style = MonospaceStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == AppLanguage.AR)
                            "خادم Hermes المتصل لا يوفّر نقطة /api/system. حدّث Hermes على جهازك أو تواصل مع مشغّل الخادم."
                        else
                            "The connected Hermes server doesn't expose /api/system. Update Hermes on your machine or contact the server operator.",
                        style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary, lineHeight = 17.sp)
                    )
                }
            }
        }

        // Gateway status card (official /health/detailed — works on every server)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.AR) "حالة البوابة" else "GATEWAY STATUS",
                        style = MonospaceStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Readiness dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (gatewayHealth.readinessOk) NeonGreen else NeonRed)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val connectedPlatforms = gatewayHealth.platformStates.count { it.second == "connected" }
                Text(
                    text = if (language == AppLanguage.AR)
                        "الحالة: ${gatewayHealth.gatewayState}  ·  المنصات المتصلة: $connectedPlatforms/${gatewayHealth.platformStates.size}"
                    else
                        "State: ${gatewayHealth.gatewayState}  ·  Connected platforms: $connectedPlatforms/${gatewayHealth.platformStates.size}",
                    style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary)
                )
                if (gatewayHealth.version.isNotBlank()) {
                    Text(
                        text = "Hermes ${gatewayHealth.version}  ·  ${gatewayHealth.platform}",
                        style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.7f))
                    )
                }
                if (gatewayHealth.diskUsedPercent > 0f) {
                    Text(
                        text = if (language == AppLanguage.AR)
                            "القرص: مستخدم ${String.format(java.util.Locale.US, "%.1f", gatewayHealth.diskUsedPercent)}%  ·  متاح ${String.format(java.util.Locale.US, "%.1f", gatewayHealth.diskFreeGb)} GB"
                        else
                            "Disk: ${String.format(java.util.Locale.US, "%.1f", gatewayHealth.diskUsedPercent)}% used  ·  ${String.format(java.util.Locale.US, "%.1f", gatewayHealth.diskFreeGb)} GB free",
                        style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.7f))
                    )
                }
                if (gatewayHealth.platformStates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    gatewayHealth.platformStates.forEach { (name, state) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (state) {
                                            "connected" -> NeonGreen
                                            "connecting", "starting" -> NeonAmber
                                            else -> NeonRed
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = name,
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = state,
                                style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }
        }

        // Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = HermesStrings.metricsTitle(language),
                        style = MonospaceStyle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = HermesStrings.metricsSubtitleLive(language),
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    )
                }

                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                        .testTag("refresh_metrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = HermesStrings.pollButton(language),
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = HermesStrings.pollButton(language),
                        style = MonospaceStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )
                }
            }
        }

        // Circular Metric Gauges (CPU & RAM)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SystemMetricCircularCard(
                    title = HermesStrings.cpuLoadTitle(language),
                    currentValue = telemetry.cpuUsage,
                    maxValue = 100f,
                    unit = HermesStrings.cpuUnit(language),
                    subtitle = HermesStrings.cpuSubtitle(language),
                    accentColor = NeonVioletLight,
                    modifier = Modifier.weight(1f)
                )

                val ramPercent = if (telemetry.ramTotalGb > 0f) {
                    ((telemetry.ramUsedGb / telemetry.ramTotalGb) * 100f).coerceIn(0f, 100f)
                } else 0f
                val freeGb = (telemetry.ramTotalGb - telemetry.ramUsedGb).coerceAtLeast(0f)
                val ramUnit = if (telemetry.ramTotalGb > 0f) {
                    "${String.format(java.util.Locale.US, "%.1f", telemetry.ramUsedGb)} / ${String.format(java.util.Locale.US, "%.0f", telemetry.ramTotalGb)} GB"
                } else {
                    "-- / -- GB"
                }
                val ramSubtitle = if (telemetry.ramTotalGb > 0f) {
                    HermesStrings.memoryFree(language, freeGb)
                } else {
                    if (language == AppLanguage.AR) "بانتظار قراءة الذاكرة" else "Awaiting telemetry"
                }
                SystemMetricCircularCard(
                    title = HermesStrings.memoryTitle(language),
                    currentValue = ramPercent,
                    maxValue = 100f,
                    unit = ramUnit,
                    subtitle = ramSubtitle,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // CPU Sparkline Waveform
        item {
            CpuHistorySparkline(history = telemetry.cpuHistory)
        }

        // Hermes Host Specs Card
        item {
            HermesHostCard(
                telemetry = telemetry,
                tailscaleIp = config.tailscaleIp,
                language = language
            )
        }

        // GPU & Acceleration Card
        item {
            GpuAccelerationCard(
                telemetry = telemetry,
                language = language
            )
        }

        // Active Tasks / Process Manager
        item {
            ActiveProcessesHeader(language = language)
        }

        items(telemetry.activeProcesses) { process ->
            ProcessRowItem(process = process)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HermesHostCard(
    telemetry: SystemTelemetry,
    tailscaleIp: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = HermesStrings.hostSpecsTitle(language),
                    style = MonospaceStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Text(
                text = HermesStrings.statusOnline(language),
                style = MonospaceStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        HostSpecRow(label = HermesStrings.hostnameLabel(language), value = telemetry.hostname)
        HostSpecRow(label = HermesStrings.osVersionLabel(language), value = telemetry.osVersion)
        HostSpecRow(label = HermesStrings.tailscaleNodeLabel(language), value = "$tailscaleIp (${HermesStrings.directPeer(language)})")
        if (telemetry.diskTotalGb > 0f) {
            HostSpecRow(
                label = HermesStrings.diskLabel(language),
                value = "${String.format(java.util.Locale.US, "%.1f", telemetry.diskUsedGb)} / ${String.format(java.util.Locale.US, "%.0f", telemetry.diskTotalGb)} GB"
            )
        }
        HostSpecRow(label = HermesStrings.uptimeLabel(language), value = telemetry.uptime)
        HostSpecRow(label = HermesStrings.hermesAgentLabel(language), value = telemetry.agentVersion)
        HostSpecRow(label = HermesStrings.activeTasksLabel(language), value = HermesStrings.backgroundProcesses(language, telemetry.activeTasksCount))
    }
}

@Composable
fun HostSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MonospaceStyle.copy(
                fontSize = 11.5.sp,
                color = TextSecondary
            )
        )
        Text(
            text = value,
            style = MonospaceStyle.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}

@Composable
fun GpuAccelerationCard(
    telemetry: SystemTelemetry,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeveloperBoard,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = HermesStrings.gpuTitle(language),
                    style = MonospaceStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Text(
                text = "${String.format("%.1f", telemetry.gpuUsage)}% LOAD",
                style = MonospaceStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val gpuDevice = if (telemetry.gpuName.isNotBlank()) {
            telemetry.gpuName
        } else if (telemetry.gpuUsage > 0f || telemetry.vramTotalGb > 0f) {
            "Host GPU Acceleration"
        } else {
            if (language == AppLanguage.AR) "غير متوفر / مدمج" else "Integrated / Not Reported"
        }
        val vramValue = if (telemetry.vramTotalGb > 0f) {
            "${String.format(java.util.Locale.US, "%.1f", telemetry.vramUsedGb)} / ${String.format(java.util.Locale.US, "%.1f", telemetry.vramTotalGb)} GB"
        } else {
            if (language == AppLanguage.AR) "غير متوفر" else "N/A"
        }
        val engineValue = if (telemetry.agentVersion.isNotBlank()) {
            "Hermes Agent (${telemetry.agentVersion})"
        } else {
            "Hermes Official Gateway"
        }

        HostSpecRow(label = HermesStrings.gpuDeviceLabel(language), value = gpuDevice)
        HostSpecRow(label = HermesStrings.vramLabel(language), value = vramValue)
        HostSpecRow(label = HermesStrings.inferenceEngineLabel(language), value = engineValue)
    }
}

@Composable
fun ActiveProcessesHeader(language: AppLanguage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = HermesStrings.activeProcessesTitle(language),
            style = MonospaceStyle.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = HermesStrings.cpuMemHeader(language),
            style = MonospaceStyle.copy(
                fontSize = 11.sp,
                color = TextSecondary
            )
        )
    }
}

@Composable
fun ProcessRowItem(process: ProcessInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F141B))
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NeonGreen)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = process.name,
                    style = MonospaceStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = process.pid,
                    style = MonospaceStyle.copy(
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = process.cpu,
                style = MonospaceStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber
                )
            )
            Text(
                text = process.memory,
                style = MonospaceStyle.copy(
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            )
        }
    }
}

package ee.oversight.hermes.model

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DEMO_MODE,
    ERROR
}

data class DiscoveredGateway(
    val hostname: String,
    val ip: String,
    val tailscaleIp: String? = null,
    val port: Int = 8642,
    val apiKey: String = ""
)

data class ConnectionConfig(
    val tailscaleIp: String = "100.100.100.100", // placeholder: user must enter their PC's Tailscale IP
    val port: Int = 8642,
    val remoteGatewayUrl: String = "",
    val useCustomGatewayUrl: Boolean = false,
    val apiKey: String = "",
    val useHttps: Boolean = false,
    val isDemoMode: Boolean = false // Default to false so Remote Gateway mode is active!
) {
    val isRemoteGatewayActive: Boolean
        get() = !isDemoMode

    val effectiveGatewayUrl: String
        get() {
            if (useCustomGatewayUrl && remoteGatewayUrl.isNotBlank()) {
                val url = remoteGatewayUrl.trim()
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    url.trimEnd('/')
                } else {
                    val scheme = if (useHttps) "https" else "http"
                    "$scheme://$url".trimEnd('/')
                }
            }
            val scheme = if (useHttps) "https" else "http"
            return "$scheme://${tailscaleIp.trim()}:$port"
        }

    val baseUrl: String
        get() = effectiveGatewayUrl
}

data class SystemTelemetry(
    val cpuUsage: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val gpuUsage: Float = 0f,
    val vramUsedGb: Float = 0f,
    val vramTotalGb: Float = 0f,
    val hostname: String = "WINDOWS-11-PC",
    val osVersion: String = "Windows 11",
    val uptime: String = "",
    val agentVersion: String = "",
    val activeTasksCount: Int = 0,
    val pingMs: Long = 0,
    val cpuHistory: List<Float> = emptyList(),
    val activeProcesses: List<ProcessInfo> = emptyList()
)

data class ProcessInfo(
    val name: String,
    val pid: String,
    val memory: String,
    val cpu: String
)

enum class ToolStatus {
    RUNNING,
    COMPLETED,
    FAILED
}

data class ToolExecutionBlock(
    val id: String,
    val toolName: String, // e.g. "terminal", "web_search"
    val command: String,
    val output: String = "",
    val status: ToolStatus = ToolStatus.RUNNING,
    val exitCode: Int? = null,
    val executionTimeMs: Long? = null
)

enum class MessageSender {
    USER,
    HERMES
}

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val content: String = "",
    val modelName: String? = null,
    val isStreaming: Boolean = false,
    val toolExecutions: List<ToolExecutionBlock> = emptyList()
)

data class AiModelInfo(
    val id: String,
    val displayName: String,
    val provider: String,
    val description: String,
    val isDefault: Boolean = false
)

// Placeholder list, replaced by live /api/model/options once connected.
val AvailableAiModels = listOf(
    AiModelInfo(
        id = "deepseek/deepseek-v4-flash",
        displayName = "DeepSeek V4 Flash",
        provider = "CommandCode",
        description = "Default fast agent model",
        isDefault = true
    )
)

data class HermesSession(
    val id: String,
    val title: String,
    val model: String = "default",
    val startedAt: Long = 0L,
    val messageCount: Int = 0
)

// Provider grouping from /api/model/options (provider slug -> models)
data class ModelProviderGroup(
    val slug: String,
    val name: String,
    val models: List<String>
)

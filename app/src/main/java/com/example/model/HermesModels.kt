package com.example.model

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DEMO_MODE,
    ERROR
}

data class ConnectionConfig(
    val tailscaleIp: String = "100.84.12.93",
    val port: Int = 8080,
    val apiKey: String = "hermes_live_key_99x",
    val useHttps: Boolean = false,
    val isDemoMode: Boolean = true
) {
    val baseUrl: String
        get() {
            val scheme = if (useHttps) "https" else "http"
            return "$scheme://$tailscaleIp:$port"
        }
}

data class SystemTelemetry(
    val cpuUsage: Float = 28.5f,
    val ramUsedGb: Float = 14.8f,
    val ramTotalGb: Float = 32.0f,
    val gpuUsage: Float = 42.0f,
    val vramUsedGb: Float = 8.2f,
    val vramTotalGb: Float = 16.0f,
    val hostname: String = "DESKTOP-HERMES-WIN11",
    val osVersion: String = "Windows 11 Pro 23H2 (Build 22631)",
    val uptime: String = "4d 18h 24m",
    val agentVersion: String = "v2.4.1-edge",
    val activeTasksCount: Int = 3,
    val pingMs: Long = 28,
    val cpuHistory: List<Float> = listOf(22f, 25f, 30f, 28f, 35f, 40f, 32f, 28.5f),
    val activeProcesses: List<ProcessInfo> = listOf(
        ProcessInfo("hermes-engine.exe", "PID 4120", "1.8 GB", "12.4% CPU"),
        ProcessInfo("python3.11.exe", "PID 8904", "4.2 GB", "8.2% CPU"),
        ProcessInfo("tailscale-ipn.exe", "PID 1024", "45 MB", "0.1% CPU"),
        ProcessInfo("powershell.exe", "PID 12844", "120 MB", "2.1% CPU")
    )
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
    val toolName: String, // e.g. "powershell", "python", "bash", "fs_probe"
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

val AvailableAiModels = listOf(
    AiModelInfo(
        id = "claude-3-7-sonnet",
        displayName = "Claude 3.7 Sonnet",
        provider = "Anthropic",
        description = "Hybrid reasoning & deep code execution",
        isDefault = true
    ),
    AiModelInfo(
        id = "deepseek-r1",
        displayName = "DeepSeek R1",
        provider = "Local/Ollama",
        description = "Open reasoning model on Windows 11 RTX GPU"
    ),
    AiModelInfo(
        id = "hermes-local-70b",
        displayName = "Hermes 70B Local",
        provider = "Nous Research",
        description = "Agentic tool-use model running locally"
    )
)

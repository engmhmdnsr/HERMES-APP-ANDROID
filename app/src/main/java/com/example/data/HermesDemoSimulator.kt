package com.example.data

import com.example.model.ProcessInfo
import com.example.model.SystemTelemetry
import com.example.model.ToolExecutionBlock
import com.example.model.ToolStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

object HermesDemoSimulator {

    fun generateSimulatedTelemetry(prev: SystemTelemetry): SystemTelemetry {
        // Subtle realistic jitter
        val cpuJitter = (Random.nextFloat() * 6f - 3f)
        val newCpu = (prev.cpuUsage + cpuJitter).coerceIn(18.0f, 65.0f)

        val ramJitter = (Random.nextFloat() * 0.4f - 0.2f)
        val newRam = (prev.ramUsedGb + ramJitter).coerceIn(12.0f, 22.0f)

        val gpuJitter = (Random.nextFloat() * 5f - 2.5f)
        val newGpu = (prev.gpuUsage + gpuJitter).coerceIn(20.0f, 85.0f)

        val newHistory = (prev.cpuHistory + newCpu).takeLast(16)

        val jitterPing = (Random.nextLong(20, 36))

        return prev.copy(
            cpuUsage = Math.round(newCpu * 10f) / 10f,
            ramUsedGb = Math.round(newRam * 10f) / 10f,
            gpuUsage = Math.round(newGpu * 10f) / 10f,
            pingMs = jitterPing,
            cpuHistory = newHistory
        )
    }

    fun simulatePing(tailscaleIp: String, port: Int): PingResult {
        return PingResult(
            isSuccess = true,
            latencyMs = Random.nextLong(22, 38),
            statusCode = 200,
            message = "Connected to Windows 11 host (Tailscale direct peer: $tailscaleIp:$port)"
        )
    }

    fun simulateChatStream(prompt: String, modelName: String): Flow<StreamChunk> = flow {
        val lower = prompt.lowercase()

        val isProcessCheck = lower.contains("process") || lower.contains("عمليات") || lower.contains("موارد") || lower.contains("cpu")
        val isPythonDiag = lower.contains("python") || lower.contains("بايثون") || lower.contains("سكريبت") || lower.contains("أداء")
        val isTailscaleQuery = lower.contains("tailscale") || lower.contains("شبك") || lower.contains("firewall") || lower.contains("جدار")
        val isDeepSeek = lower.contains("deepseek") || modelName.contains("DeepSeek")

        val greeting = when {
            isProcessCheck -> "جاري استدعاء مراقب مهام Windows 11 وتفقد العمليات المستهلكة للمعالج والذاكرة...\n"
            isPythonDiag -> "جاري تشغيل سكريبت التشخيص المتقدم عبر محرك بايثون وتحليل زمن الوصول...\n"
            isTailscaleQuery -> "تم تلقي طلب فحص النفق المشفر Tailscale وقواعد جدار الحماية في Windows 11...\n"
            isDeepSeek -> "مرحباً! يبدأ نموذج DeepSeek R1 عملية الاستنتاج والتفكير المتسلسل لتحليل استفسارك...\n"
            else -> "تم استلام الأمر بنجاح في سيرفر Hermes Agent على Windows 11. جاري التحليل والتنفيذ...\n"
        }

        // Stream greeting letters/words
        for (char in greeting) {
            emit(StreamChunk.TextDelta(char.toString()))
            delay(12)
        }

        delay(150)

        // Inject Tool Execution Block
        val toolId = "tool_${System.currentTimeMillis()}"
        val (toolName, command, outputResult) = when {
            isProcessCheck -> Triple(
                "powershell.exe",
                "Get-Process | Sort-Object -Property CPU -Descending | Select-Object -First 5 -Property Id, ProcessName, @{Name='CPU(s)';Expression={(\$_.CPU).ToString('N1')}}, @{Name='Mem(MB)';Expression={[math]::Round(\$_.WorkingSet64/1MB,1)}} | Format-Table -AutoSize",
                """
  Id ProcessName         CPU(s)   Mem(MB)
  -- -----------         ------   -------
4120 hermes-engine.exe   184.2    1,824.5
8904 python3.11.exe      92.8     4,210.0
1024 tailscale-ipn.exe   14.6        45.2
12844 powershell.exe     4.1        122.8
 984 csrss.exe           2.9         18.4
                """.trimIndent()
            )
            isPythonDiag -> Triple(
                "python",
                "python -m hermes.diagnostics --benchmark-sse --device=cuda:0",
                """
[INFO] Initializing PyTorch CUDA runtime on NVIDIA RTX 4090...
[BENCHMARK] Matrix Multiplications (FP16): 82.4 TFLOPS
[BENCHMARK] VRAM Allocated: 8.24 GB / 24.00 GB
[NETWORK] Tailscale peer 100.84.12.93 RTT: 24.8 ms (Zero packet drop)
[STATUS] SSE Stream pipeline active & nominal. Exit code: 0
                """.trimIndent()
            )
            isTailscaleQuery -> Triple(
                "tailscale-cli",
                "tailscale status --peers=false && netsh advfirewall firewall show rule name=\"HermesGateway\"",
                """
100.84.12.93    hermes-win11-pc      tailscale-net  windows  -
Tailscale 1.62.0 is running; IPN bus connected.
WireGuard encrypted tunnel active. Direct peer handshakes OK.

Rule Name:                            HermesGateway
----------------------------------------------------------------------
Enabled:                              Yes
Direction:                            In
Profiles:                             Private
LocalIP:                              100.64.0.0/10 (CGNAT Tailscale only)
LocalPort:                            8080
Protocol:                             TCP
Action:                               Allow
                """.trimIndent()
            )
            else -> Triple(
                "powershell.exe",
                "systeminfo | Select-String \"OS Name\",\"Total Physical Memory\",\"Available Physical Memory\"",
                """
OS Name:                   Microsoft Windows 11 Pro
Total Physical Memory:     32,698 MB
Available Physical Memory: 17,250 MB
Agent Heartbeat:           ACTIVE [OK]
                """.trimIndent()
            )
        }

        emit(StreamChunk.ToolStart(
            ToolExecutionBlock(
                id = toolId,
                toolName = toolName,
                command = command,
                status = ToolStatus.RUNNING
            )
        ))

        // Tool execution simulate delay
        delay(700)

        emit(StreamChunk.ToolOutput(
            toolId = toolId,
            output = outputResult,
            status = ToolStatus.COMPLETED
        ))

        delay(250)

        // Stream Conclusion and AI Explanation
        val conclusion = when {
            isProcessCheck -> """
                
✅ **ملخص تقرير الموارد:**
- يتبين أن معالج Windows 11 يعمل بحمل مستقر (حوالي 28% إجمالي).
- أكثر العمليات استهلاكاً هي `hermes-engine.exe` و `python3.11.exe` لتشغيل نماذج الذكاء الاصطناعي ومعالجة البث اللحظي.
- الذاكرة العشوائية المتوفرة تزيد عن 16 جيجابايت، مما يسمح بتشغيل مهام إضافية بسلاسة تامة.
            """.trimIndent()
            isPythonDiag -> """
                
⚡ **نتائج التشخيص:**
- معالج الرسومات RTX 4090 جاهز مع استهلاك 8.2 جيجابايت من VRAM.
- سرعة نقل البيانات داخل شبكة Tailscale بلغت 24.8ms وهي سرعة فائقة تضمن استجابة فورية لبث الـ SSE دون أي تقطيع.
            """.trimIndent()
            isTailscaleQuery -> """
                
🔒 **تقرير أمان الشبكة:**
- اتصال Tailscale مشفر بالكامل بتقنية WireGuard بدون فتح أي منافذ بالراوتر (Zero Port Forwarding).
- جدار حماية Windows 11 يسمح بالاتصال فقط لنطاق شبكة Tailscale (100.64.0.0/10) على المنفذ 8080، مما يمنع أي محاولة وصول من الإنترنت العام.
            """.trimIndent()
            isDeepSeek -> """
                
🧠 **تحليل DeepSeek R1:**
تمت معالجة الاستفسار بنجاح عبر محرك الذكاء الاصطناعي المحلي. يمكنك إرسال أوامر تشغيل إضافية أو مراقبة معدل استهلاك الموارد لحظياً من تبويب المراقبة.
            """.trimIndent()
            else -> """
                
تم تنفيذ المطلوب بنجاح على نظام Windows 11. جميع مؤشرات الاتصال عبر نفق Tailscale تعمل بكفاءة تامة. يمكنك طلب أوامر أخرى أو استعلامات برمجية في أي وقت!
            """.trimIndent()
        }

        for (word in conclusion.split(" ")) {
            emit(StreamChunk.TextDelta("$word "))
            delay(Random.nextLong(20, 45))
        }

        emit(StreamChunk.Done)
    }
}

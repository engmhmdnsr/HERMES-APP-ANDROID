package com.example.data

import com.example.model.AppLanguage
import com.example.model.ProcessInfo
import com.example.model.SystemTelemetry
import com.example.model.ToolExecutionBlock
import com.example.model.ToolStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

object HermesDemoSimulator {

    fun generateSimulatedTelemetry(current: SystemTelemetry): SystemTelemetry {
        val cpuFluctuation = (Random.nextFloat() * 8f - 4f)
        val newCpu = (current.cpuUsage + cpuFluctuation).coerceIn(12f, 75f)

        val ramFluctuation = (Random.nextFloat() * 0.4f - 0.2f)
        val newRam = (current.ramUsedGb + ramFluctuation).coerceIn(10f, 28f)

        val updatedHistory = (current.cpuHistory + newCpu).takeLast(16)
        val newPing = Random.nextLong(20, 36)

        return current.copy(
            cpuUsage = newCpu,
            ramUsedGb = newRam,
            pingMs = newPing,
            cpuHistory = updatedHistory
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

    fun simulateChatStream(prompt: String, modelName: String, lang: AppLanguage = AppLanguage.EN): Flow<StreamChunk> = flow {
        val lower = prompt.lowercase()

        val isProcessCheck = lower.contains("process") || lower.contains("cpu") || lower.contains("task") || lower.contains("top") || lower.contains("معالج") || lower.contains("عمليات") || lower.contains("موارد")
        val isPythonDiag = lower.contains("python") || lower.contains("benchmark") || lower.contains("script") || lower.contains("cuda") || lower.contains("بايثون") || lower.contains("تشخيص") || lower.contains("سكريبت")
        val isTailscaleQuery = lower.contains("tailscale") || lower.contains("tunnel") || lower.contains("firewall") || lower.contains("port") || lower.contains("نفق") || lower.contains("جدار") || lower.contains("حماية")
        val isDeepSeek = lower.contains("deepseek") || modelName.contains("DeepSeek")

        val greeting = if (lang == AppLanguage.AR) {
            when {
                isProcessCheck -> "جاري استدعاء مدير مهام Windows 11 وفحص العمليات المستهلكة للمعالج والذاكرة...\n"
                isPythonDiag -> "جاري تشغيل سكريبت التشخيص المتقدم عبر محرك Python لقياس أداء CUDA وسرعة الشبكة...\n"
                isTailscaleQuery -> "جاري تدقيق نفق WireGuard المشفر عبر Tailscale وقواعد جدار حماية Windows 11...\n"
                isDeepSeek -> "مرحباً! يقوم نموذج DeepSeek R1 بتفعيل التفكير الاستنتاجي المتسلسل لتحليل استفسارك...\n"
                else -> "تم استلام الأمر في خادم وكيل Hermes على Windows 11. جاري التحليل وتنفيذ خط العمليات...\n"
            }
        } else {
            when {
                isProcessCheck -> "Invoking Windows 11 Task Manager & inspecting active processes for CPU/Memory metrics...\n"
                isPythonDiag -> "Executing advanced diagnostic script via Python runtime to benchmark CUDA and network latency...\n"
                isTailscaleQuery -> "Auditing Tailscale WireGuard encrypted tunnel and Windows 11 Defender firewall rules...\n"
                isDeepSeek -> "Hello! DeepSeek R1 is activating its chain-of-thought reasoning process to analyze your query...\n"
                else -> "Command received by Hermes Agent server on Windows 11. Analyzing intent and dispatching execution pipeline...\n"
            }
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
        val conclusion = if (lang == AppLanguage.AR) {
            when {
                isProcessCheck -> """
                    
✅ **ملخص تقرير استهلاك الموارد:**
- معالج Windows 11 يعمل بحمل اسمي مستقر (حوالي 28% إجمالي).
- العمليات الأكثر استهلاكاً هي `hermes-engine.exe` و `python3.11.exe` لتوليد الاستنتاجات وبث تدفق الـ SSE.
- الذاكرة العشوائية المتوفرة تزيد عن 16 جيجابايت، مما يتيح تشغيل مهام مكثفة إضافية بدون أي اختناق.
                """.trimIndent()
                isPythonDiag -> """
                    
⚡ **نتائج التشخيص والأداء:**
- مسرّع NVIDIA GeForce RTX 4090 نشط ومستقر، مع استخدام 8.24 جيجابايت من ذاكرة VRAM.
- زمن استجابة نفق Tailscale تم قياسه بـ 24.8ms مع انعدام فقدان الحزم (Zero packet loss)، ما يضمن بث نصوص SSE سلس وفوري.
                """.trimIndent()
                isTailscaleQuery -> """
                    
🔒 **تدقيق أمان الشبكة ونفق Tailscale:**
- نفق WireGuard مشفر بالكامل بين الطرفين بدون الحاجة لفتح أي منافذ على الراوتر (Zero Port Forwarding).
- قاعدة جدار حماية Windows 11 Defender تحصر الاتصال الوارد على منفذ 8080 بأجهزة شبكة Tailscale فقط (100.64.0.0/10)، مما يحجب الوصول من الإنترنت العام تماماً.
                """.trimIndent()
                isDeepSeek -> """
                    
🧠 **استنتاج DeepSeek R1:**
تمت معالجة الاستفسار بنجاح عبر نموذج التفكير على عتاد الجهاز المضيف. يمكنك إرسال أوامر إضافية أو متابعة مؤشرات الموارد الحية من تبويب المراقبة.
                """.trimIndent()
                else -> """
                    
تم تنفيذ المطلوب بنجاح على نظام Windows 11. جميع مؤشرات الاتصال عبر نفق Tailscale تعمل بكفاءة تامة. يمكنك طلب أوامر أخرى أو تشغيل سكريبتات برمجية في أي وقت!
                """.trimIndent()
            }
        } else {
            when {
                isProcessCheck -> """
                    
✅ **Resource Inspection Summary:**
- Windows 11 host CPU is running under nominal load (approx. 28% overall).
- Top resource consumers are `hermes-engine.exe` and `python3.11.exe` driving LLM inference and SSE streaming.
- Over 16 GB of physical RAM remains free and available for additional workloads.
                """.trimIndent()
                isPythonDiag -> """
                    
⚡ **Diagnostic Results:**
- NVIDIA GeForce RTX 4090 accelerator active with 8.24 GB VRAM allocated.
- Tailscale peer latency measured at 24.8ms with zero packet drop, ensuring crisp, real-time SSE streaming.
                """.trimIndent()
                isTailscaleQuery -> """
                    
🔒 **Network Security Audit:**
- Tailscale tunnel is end-to-end encrypted with WireGuard (Zero Port Forwarding required).
- Windows Defender firewall inbound rule permits connections strictly from Tailscale CGNAT subnet (100.64.0.0/10) on port 8080, shielding against public internet exposure.
                """.trimIndent()
                isDeepSeek -> """
                    
🧠 **DeepSeek R1 Analysis:**
Inference concluded successfully on local host hardware. You can dispatch further execution commands or track live resource metrics from the Telemetry tab.
                """.trimIndent()
                else -> """
                    
Execution completed successfully on Windows 11 host. All telemetry signals and Tailscale tunnel metrics are running nominally. Feel free to submit further commands or scripts at any time!
                """.trimIndent()
            }
        }

        for (word in conclusion.split(" ")) {
            emit(StreamChunk.TextDelta("$word "))
            delay(Random.nextLong(20, 45))
        }

        emit(StreamChunk.Done)
    }
}

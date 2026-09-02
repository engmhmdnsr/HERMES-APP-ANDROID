package com.example.model

object HermesStrings {
    // App Bar & Brand
    fun appTitle(lang: AppLanguage) = "HERMES"
    fun appSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "بوابة التحكم في وكيل Windows 11"
        AppLanguage.EN -> "Windows 11 Agent Gateway"
    }

    // Status Ribbon
    fun statusConnected(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "متصل عبر TAILSCALE"
        AppLanguage.EN -> "TAILSCALE LIVE"
    }
    fun statusConnecting(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "جاري الاتصال..."
        AppLanguage.EN -> "CONNECTING..."
    }
    fun statusDemoMode(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الوضع التجريبي"
        AppLanguage.EN -> "DEMO MODE"
    }
    fun statusDisconnected(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "غير متصل"
        AppLanguage.EN -> "OFFLINE"
    }
    fun statusError(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "خطأ في الاتصال"
        AppLanguage.EN -> "CONN ERROR"
    }
    fun simulated(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "محاكاة"
        AppLanguage.EN -> "SIMULATED"
    }
    fun demoOn(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تجريبي نشط"
        AppLanguage.EN -> "DEMO ON"
    }
    fun demoOff(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تجريبي معطل"
        AppLanguage.EN -> "DEMO OFF"
    }
    fun clearChat(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مسح المحادثة"
        AppLanguage.EN -> "Clear Chat"
    }

    // Navigation Tabs
    fun tabChat(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "المحادثة / SSE"
        AppLanguage.EN -> "CHAT / SSE"
    }
    fun tabTelemetry(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الموارد والمراقبة"
        AppLanguage.EN -> "TELEMETRY"
    }
    fun tabGateway(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "البوابة والإعدادات"
        AppLanguage.EN -> "GATEWAY"
    }

    // Chat Screen
    fun modelLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "النموذج:"
        AppLanguage.EN -> "MODEL:"
    }
    fun you(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "أنت"
        AppLanguage.EN -> "YOU"
    }
    fun hermesAgent(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "وكيل هيرمز"
        AppLanguage.EN -> "HERMES AGENT"
    }
    fun streamingBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "بث لحظي"
        AppLanguage.EN -> "STREAMING"
    }
    fun receivingStream(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "جاري استقبال تدفق النصوص من SSE..."
        AppLanguage.EN -> "Receiving SSE text stream..."
    }
    fun inputPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "اكتب أمراً أو استفساراً (مثل: فحص المعالج، تشغيل سكريبت)..."
        AppLanguage.EN -> "Type prompt or system command (e.g. check cpu, run script)..."
    }
    fun sendCommand(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "إرسال الأمر"
        AppLanguage.EN -> "Send Command"
    }
    fun stopStreaming(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "إيقاف البث"
        AppLanguage.EN -> "Stop Streaming"
    }

    // Presets
    fun presetCpu(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "⚡ فحص استهلاك الـ CPU والعمليات النشطة"
        AppLanguage.EN -> "⚡ Inspect CPU usage and top active processes"
    }
    fun presetPython(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "🐍 تشغيل سكريبت بايثون لفحص أداء CUDA"
        AppLanguage.EN -> "🐍 Run Python performance benchmark script"
    }
    fun presetTailscale(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "🔒 تدقيق اتصال نفق Tailscale وجدار الحماية"
        AppLanguage.EN -> "🔒 Audit Tailscale tunnel and firewall rules"
    }
    fun presetDeepSeek(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "🧠 استنتاج واستكشاف أخطاء بنموذج DeepSeek R1"
        AppLanguage.EN -> "🧠 Reasoning & troubleshooting with DeepSeek R1"
    }

    // Initial greeting in chat
    fun welcomeMessage(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> """
مرحباً بك في مركز التحكم **Hermes Control Center** ⚡
الوكيل الذكي جاهز على نظام Windows 11 عبر نفق Tailscale المشفر.

يمكنك إرسال استفسارات، تشغيل أوامر النظام، ومراقبة الـ CPU والذاكرة مباشرة. جرّب أحد الأوامر الجاهزة بالأسفل لاختبار بث الـ SSE وصناديق التنفيذ البرمجية.
        """.trimIndent()
        AppLanguage.EN -> """
Welcome to **Hermes Control Center** ⚡
Your Windows 11 AI agent is connected via a secure, encrypted Tailscale tunnel.

You can send instructions, execute system commands, and monitor CPU and memory in real time. Try one of the preset prompts below to test SSE streaming and tool execution blocks.
        """.trimIndent()
    }

    // Tool blocks
    fun toolTerminal(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الطرفية"
        AppLanguage.EN -> "TERMINAL"
    }
    fun toolRunning(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "قيد التنفيذ"
        AppLanguage.EN -> "RUNNING"
    }
    fun toolFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "فشل"
        AppLanguage.EN -> "FAILED"
    }
    fun toolSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "نجاح"
        AppLanguage.EN -> "SUCCESS"
    }
    fun toolCopied(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تم نسخ الأمر إلى الحافظة"
        AppLanguage.EN -> "Command copied to clipboard"
    }
    fun toolExecuting(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "جاري التنفيذ على مضيف Windows 11..."
        AppLanguage.EN -> "Executing on Windows 11 host..."
    }

    // Telemetry Screen
    fun metricsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مقاييس النظام والبوابة"
        AppLanguage.EN -> "SYSTEM & GATEWAY METRICS"
    }
    fun metricsSubtitleDemo(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "توليد تلقائي عبر محاكي العرض التوضيحي المدمج"
        AppLanguage.EN -> "Generated by Built-in Demo Engine"
    }
    fun metricsSubtitleLive(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تم الاستعلام مباشرة من مضيف Windows 11"
        AppLanguage.EN -> "Polled from Windows 11 host"
    }
    fun pollButton(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تحديث"
        AppLanguage.EN -> "POLL"
    }
    fun cpuLoadTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "حمولة المعالج CPU"
        AppLanguage.EN -> "CPU LOAD"
    }
    fun cpuUnit(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "% مستهلك"
        AppLanguage.EN -> "% UTILIZED"
    }
    fun cpuSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "16 نواة / 32 خيط معالجة"
        AppLanguage.EN -> "16 Cores / 32 Threads"
    }
    fun memoryTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الذاكرة العشوائية (RAM)"
        AppLanguage.EN -> "MEMORY (RAM)"
    }
    fun memoryFree(lang: AppLanguage, freeGb: Float) = when (lang) {
        AppLanguage.AR -> "${String.format("%.1f", freeGb)} جيجابايت متاح"
        AppLanguage.EN -> "${String.format("%.1f", freeGb)} GB Free"
    }
    fun cpuTimeline(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "المخطط الزمني لاستهلاك المعالج"
        AppLanguage.EN -> "CPU UTILIZATION TIMELINE"
    }
    fun liveBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "لحظي"
        AppLanguage.EN -> "LIVE"
    }
    fun hostSpecsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "بيانات مضيف WINDOWS 11"
        AppLanguage.EN -> "WINDOWS 11 HOST TELEMETRY"
    }
    fun statusOnline(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الحالة: متصل"
        AppLanguage.EN -> "STATUS: ONLINE"
    }
    fun hostnameLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "اسم الجهاز:"
        AppLanguage.EN -> "Hostname:"
    }
    fun osVersionLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "إصدار النظام:"
        AppLanguage.EN -> "OS Edition:"
    }
    fun tailscaleNodeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "عقدة Tailscale:"
        AppLanguage.EN -> "Tailscale Node:"
    }
    fun directPeer(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "(نظير مباشر P2P)"
        AppLanguage.EN -> "(Direct Peer)"
    }
    fun uptimeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مدة التشغيل:"
        AppLanguage.EN -> "Host Uptime:"
    }
    fun hermesAgentLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "إصدار الوكيل:"
        AppLanguage.EN -> "Hermes Agent:"
    }
    fun activeTasksLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "المهام النشطة:"
        AppLanguage.EN -> "Active Tasks:"
    }
    fun backgroundProcesses(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.AR -> "$count عمليات في الخلفية"
        AppLanguage.EN -> "$count background processes"
    }
    fun gpuTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مسرّع الذكاء الاصطناعي (GPU)"
        AppLanguage.EN -> "AI ACCELERATOR (GPU)"
    }
    fun gpuDeviceLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "البطاقة:"
        AppLanguage.EN -> "Device:"
    }
    fun vramLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "ذاكرة الفيديو VRAM:"
        AppLanguage.EN -> "VRAM Usage:"
    }
    fun inferenceEngineLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "محرك الاستنتاج:"
        AppLanguage.EN -> "Inference Engine:"
    }
    fun activeProcessesTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "العمليات النشطة للوكيل"
        AppLanguage.EN -> "ACTIVE AGENT PROCESSES"
    }
    fun cpuMemHeader(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "المعالج / الذاكرة"
        AppLanguage.EN -> "CPU / MEM"
    }

    // Gateway & Settings Screen
    fun gatewayTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "بوابة TAILSCALE الآمنة"
        AppLanguage.EN -> "TAILSCALE SECURE GATEWAY"
    }
    fun gatewaySubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "اتصال مشفر ومباشر بين الهاتف وكمبيوتر Windows 11"
        AppLanguage.EN -> "Peer-to-peer encrypted connection between Mobile & Windows 11"
    }

    // Language Section
    fun languageSectionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "لغة التطبيق / APPLICATION LANGUAGE"
        AppLanguage.EN -> "APPLICATION LANGUAGE"
    }
    fun languageSectionDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "اختر لغة الواجهات والنصوص بين العربية والإنجليزية"
        AppLanguage.EN -> "Choose your preferred interface language (Arabic or English)"
    }

    // Demo Mode
    fun demoModeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الوضع التجريبي المدمج"
        AppLanguage.EN -> "BUILT-IN DEMO MODE"
    }
    fun demoModeDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تشغيل واجهات التطبيق بالكامل تفاعلياً بمحاكاة بث النصوص وتوليد بيانات الـ CPU/RAM دون الحاجة للاتصال بالكمبيوتر أو الإنترنت."
        AppLanguage.EN -> "Interactive demo mode that simulates real-time text streaming, tool execution blocks, and live CPU/RAM metrics without requiring a connection to the host PC."
    }

    // Network Parameters
    fun networkParamsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "إعدادات الشبكة"
        AppLanguage.EN -> "NETWORK PARAMETERS"
    }
    fun tailscaleIpLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "عنوان IP لعقدة Tailscale"
        AppLanguage.EN -> "Tailscale Node IP"
    }
    fun portLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "المنفذ (Port)"
        AppLanguage.EN -> "Port"
    }
    fun apiKeyLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مفتاح الوصول Hermes Agent API Key"
        AppLanguage.EN -> "Hermes Agent API Key"
    }
    fun testPingButton(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "فحص الاتصال"
        AppLanguage.EN -> "TEST PING"
    }
    fun applyButton(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "حفظ وتطبيق"
        AppLanguage.EN -> "APPLY"
    }

    // Ping Results
    fun handshakeSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "نجاح الاتصال المباشر بالنظير"
        AppLanguage.EN -> "PEER HANDSHAKE SUCCESSFUL"
    }
    fun connectionFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "فشل الاتصال"
        AppLanguage.EN -> "CONNECTION FAILED"
    }

    // Remote Gateway Feature
    fun remoteGateway(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "خاصية Remote Gateway"
        AppLanguage.EN -> "REMOTE GATEWAY"
    }
    fun remoteGatewayModeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "خاصية بوابة التحكم عن بعد (Remote Gateway)"
        AppLanguage.EN -> "REMOTE GATEWAY FEATURE"
    }
    fun remoteGatewayModeDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "توجيه كافة أوامر الاستنتاج والتحكم البرمجي وبث SSE وقراءات الموارد عبر خادم البوابة البعيد على Windows 11."
        AppLanguage.EN -> "Route all agent commands, LLM inference, SSE text stream, and system telemetry through your remote Windows 11 gateway."
    }
    fun remoteGatewayActiveBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "⚡ خاصية Remote Gateway نشطة"
        AppLanguage.EN -> "⚡ REMOTE GATEWAY LIVE"
    }
    fun remoteGatewayInactiveBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "🧪 وضع المحاكاة الداخلي"
        AppLanguage.EN -> "🧪 LOCAL SIMULATION"
    }
    fun remoteGatewayToggle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تشغيل بخاصية Remote Gateway"
        AppLanguage.EN -> "Run in Remote Gateway Mode"
    }
    fun gatewayTypeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "طريقة استهداف البوابة:"
        AppLanguage.EN -> "Gateway Addressing Method:"
    }
    fun gatewayTypeTailscaleIp(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "عنوان Tailscale IP والمنفذ"
        AppLanguage.EN -> "Tailscale IP & Port"
    }
    fun gatewayTypeCustomUrl(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "رابط URL مخصص (Tunnel / MagicDNS)"
        AppLanguage.EN -> "Custom URL (Tunnel / MagicDNS)"
    }
    fun remoteGatewayUrlLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "رابط البوابة البعيدة (Gateway URL)"
        AppLanguage.EN -> "Remote Gateway URL"
    }
    fun remoteGatewayUrlPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "https://hermes-pc.tailnet.ts.net أو http://100.84.12.93:8080"
        AppLanguage.EN -> "https://hermes-pc.tailnet.ts.net or http://100.84.12.93:8080"
    }
    fun gatewayPresetsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "نماذج البوابات السريعة (PRESETS)"
        AppLanguage.EN -> "QUICK GATEWAY PRESETS"
    }
    fun presetTailscaleDirect(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "عقدة Tailscale الافتراضية (100.84.12.93:8080)"
        AppLanguage.EN -> "Tailscale Direct (100.84.12.93:8080)"
    }
    fun presetTailscaleFunnel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "نفق Tailscale المشفر (https://hermes.ts.net)"
        AppLanguage.EN -> "Tailscale Funnel HTTPS (https://hermes.ts.net)"
    }
    fun presetLocalhost(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "محاكي أندرويد المحلي (10.0.2.2:8080)"
        AppLanguage.EN -> "Android Host Loopback (10.0.2.2:8080)"
    }
    fun remoteGatewayRoutingBanner(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مسار خاصية Remote Gateway:"
        AppLanguage.EN -> "Remote Gateway Route:"
    }
    fun gatewayDiagnosticsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تشخيصات اتصال البوابة"
        AppLanguage.EN -> "GATEWAY DIAGNOSTICS"
    }
    fun gatewayDiagnosticsInfo(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "عند تفعيل خاصية Remote Gateway، يتم إرسال طلبات الذكاء الاصطناعي وبث النصوص SSE واستعلامات المعالج والذاكرة مباشرة إلى كمبيوتر Windows 11."
        AppLanguage.EN -> "With Remote Gateway enabled, all AI inference requests, SSE text streams, and system telemetry are routed directly to the remote Windows 11 PC."
    }

    // Hermes API Key Guidance
    fun whereToGetApiKeyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "كيف أحصل على Hermes API Key؟"
        AppLanguage.EN -> "Where do I get the Hermes API Key?"
    }
    fun apiKeyGuideInfo(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مفتاح Hermes API ليس اشتراكاً تشتريه من موقع، بل يعتمد على الطريقة التي تشغل بها النموذج:"
        AppLanguage.EN -> "Hermes API is not a closed service you buy from a website, it depends on how you run the model:"
    }
    fun apiKeySourceOllama(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "1. عبر Ollama على Windows (بدون مفتاح مجاناً):\nإذا كنت تشغل Ollama (`ollama run hermes3`)، لا تحتاج لمفتاح إطلاقاً! اترك الحقل فارغاً أو اضغط 'بدون مفتاح'."
        AppLanguage.EN -> "1. Via Ollama on Windows (No Key Needed):\nIf running Ollama (`ollama run hermes3`), no API key is required! Leave the field empty or click 'No Key'."
    }
    fun apiKeySourceLmStudio(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "2. عبر LM Studio على Windows (بدون مفتاح):\nشغّل Local Server في LM Studio، واضبط المنفذ على 1234. لا يلزم مفتاح."
        AppLanguage.EN -> "2. Via LM Studio on Windows (No Key Needed):\nStart Local Server in LM Studio, set port to 1234. No API key required."
    }
    fun apiKeySourceCustomServer(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "3. سيرفر Hermes المخصص (Python / FastAPI):\nالمفتاح هو الرقم السري الذي كتبته أنت بنفسك في ملف الإعدادات .env على جهازك (مثال: hermes_live_key_99x)."
        AppLanguage.EN -> "3. Custom Hermes Server (Python / FastAPI):\nThe API key is the secret password you configured yourself in the .env file on your PC (e.g. hermes_live_key_99x)."
    }
    fun apiKeySourceOpenRouter(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "4. عبر الإنترنت (OpenRouter Cloud API):\nإذا كنت تريد تشغيل نموذج Hermes 3 سحابياً بدون جهازك، أنشئ حساباً مجانياً على openrouter.ai واحصل على مفتاح يبدأ بـ sk-or-v1-..."
        AppLanguage.EN -> "4. Via Cloud (OpenRouter API):\nTo run Hermes 3 in the cloud without running a local PC server, get a key from openrouter.ai starting with sk-or-v1-..."
    }
    fun btnNoKeyNeeded(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "بدون مفتاح (Ollama / محلي)"
        AppLanguage.EN -> "No Key Needed (Local)"
    }
    fun btnOpenRouterPreset(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "سحابي (OpenRouter)"
        AppLanguage.EN -> "Cloud (OpenRouter)"
    }

    // Guide
    fun guideTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "دليل ربط TAILSCALE وجدار الحماية"
        AppLanguage.EN -> "TAILSCALE & FIREWALL GUIDE"
    }
    fun guideContent(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> """
1. **لا حاجة لفتح منافذ في الراوتر (Zero Port Forwarding):**
يقوم Tailscale بإنشاء نفق WireGuard مشفر بين هاتفك وجهاز Windows 11 مباشرة وبأعلى معايير الأمان.

2. **عنوان IP الخاص بالجهاز:**
افتح تطبيق Tailscale على Windows 11 وانسخ عنوان IP الجهاز (يبدأ دائماً بـ 100.x.x.x) ثم ضعه في الحقل أعلاه.

3. **أمر جدار الحماية في Windows 11 PowerShell:**
لتقييد الوصول وحصره فقط على أجهزة شبكة Tailscale الآمنة، نفذ الأمر التالي في PowerShell بصلاحيات المسؤول (Run as Administrator):
        """.trimIndent()
        AppLanguage.EN -> """
1. **Zero Port Forwarding Required:**
Tailscale creates an encrypted WireGuard peer-to-peer tunnel directly between your mobile device and your Windows 11 PC.

2. **Tailscale Node IP:**
Open the Tailscale app on Windows 11 to copy your node's IP address (always begins with 100.x.x.x) and paste it into the field above.

3. **Windows 11 PowerShell Firewall Rule:**
To restrict gateway access strictly to authorized Tailscale peer traffic, run in an elevated PowerShell:
        """.trimIndent()
    }

    // Full Hermes Agent Server Script on Windows
    fun fullHermesAgentTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "⚡ كود تشغيل سيرفر هرمز الكامل على Windows 11"
        AppLanguage.EN -> "⚡ FULL HERMES AGENT WINDOWS 11 SERVER SCRIPT"
    }
    fun fullHermesAgentDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "لتحكم كامل في الحاسوب (تنفيذ أوامر PowerShell، فحص المعالج والذاكرة، وبث ردود هرمز مع أدوات التشغيل Tool Calling)، شغّل هذا الكود الخفيف (FastAPI) على جهازك:"
        AppLanguage.EN -> "For full PC control (PowerShell execution, CPU/RAM telemetry, streaming responses with Tool Calling blocks), run this lightweight FastAPI script on your PC:"
    }
    fun copyServerScriptButton(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "نسخ كود السيرفر (hermes_server.py)"
        AppLanguage.EN -> "Copy Server Script (hermes_server.py)"
    }
    fun scriptCopiedSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تم نسخ كود السيرفر إلى الحافظة!"
        AppLanguage.EN -> "Server script copied to clipboard!"
    }
    fun runServerInstructions(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> """
خطوات تشغيل السيرفر على Windows في دقيقة واحدة:
1. افتح PowerShell وثبت المكتبات المطلوبة:
   pip install fastapi uvicorn psutil requests

2. احفظ الكود أدناه في ملف باسم hermes_server.py
3. شغّل السيرفر بالأمر:
   python hermes_server.py
4. مبروك! سيرفر هرمز يعمل الآن على المنفذ 8080 وبكلمة سر الـ API هي hermes_live_key_99x
        """.trimIndent()
        AppLanguage.EN -> """
One-minute setup on your Windows PC:
1. Open PowerShell and install requirements:
   pip install fastapi uvicorn psutil requests

2. Save the code below as hermes_server.py
3. Start the server:
   python hermes_server.py
4. Done! Hermes Agent is running on port 8080 with API key hermes_live_key_99x
        """.trimIndent()
    }
}

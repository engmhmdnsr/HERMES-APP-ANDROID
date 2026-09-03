package ee.oversight.hermes.model

object HermesStrings {
    // App Bar & Brand
    fun appTitle(lang: AppLanguage) = "HERMES"
    fun appSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "بوابة التحكم • Oversight.ee"
        AppLanguage.EN -> "Agent Gateway • Oversight.ee"
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
    fun statusDisconnected(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "غير متصل"
        AppLanguage.EN -> "OFFLINE"
    }
    fun statusError(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "خطأ في الاتصال"
        AppLanguage.EN -> "CONN ERROR"
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
        AppLanguage.AR -> "https://hermes-pc.tailnet.ts.net أو http://100.x.x.x:8080"
        AppLanguage.EN -> "https://hermes-pc.tailnet.ts.net or http://100.x.x.x:8080"
    }
    fun gatewayPresetsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "نماذج البوابات السريعة (PRESETS)"
        AppLanguage.EN -> "QUICK GATEWAY PRESETS"
    }
    fun presetTailscaleDirect(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "أدخل IP الـ Tailscale بتاع جهازك (من تطبيق Tailscale)"
        AppLanguage.EN -> "Enter your PC's Tailscale IP (from the Tailscale app)"
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
    fun apiKeySourceCustomServer(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "3. Hermes API Server الرسمي على الـ PC:" + "\n" + "المفتاح هو API_SERVER_KEY من ملف .env على جهازك (AppData/Local/hermes/.env)."
        AppLanguage.EN -> "3. Official Hermes API Server on your PC:" + "\n" + "The key is API_SERVER_KEY from the .env file on your PC (AppData/Local/hermes/.env)."
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
خطوات التفعيل على Windows:
1. افتح ملف الإعدادات على جهازك:
   %LOCALAPPDATA%\hermes\.env

2. ضيف في آخره:
   API_SERVER_ENABLED=true
   API_SERVER_KEY=<مفتاح طويل عشوائي>
   API_SERVER_HOST=<IP بتاع Tailscale>
   API_SERVER_PORT=8080

3. ولّد مفتاح قوي بالأمر:
   python -c "import secrets; print(secrets.token_hex(32))"

4. أعد تشغيل البوابة:
   hermes gateway restart

5. التطبيق هيشتغل على: IP:8080 بالمفتاح اللي كتبته
        """.trimIndent()
        AppLanguage.EN -> """
Setup steps on your Windows PC:
1. Open the config file on your PC:
   %LOCALAPPDATA%\hermes\.env

2. Append at the end:
   API_SERVER_ENABLED=true
   API_SERVER_KEY=<long-random-secret>
   API_SERVER_HOST=<your-tailscale-ip>
   API_SERVER_PORT=8080

3. Generate a strong key:
   python -c "import secrets; print(secrets.token_hex(32))"

4. Restart the gateway:
   hermes gateway restart

5. Point the app at IP:8080 with the key you set.
        """.trimIndent()
    }

    // Auto-Discovery & QR Strings
    fun autoDiscoverTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الاكتشاف التلقائي لجهازك"
        AppLanguage.EN -> "AUTO-DISCOVER WINDOWS PC"
    }
    fun autoDiscoverSearching(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "جاري البحث عن سيرفر هيرمز على الشبكة المحلية..."
        AppLanguage.EN -> "Scanning local Wi-Fi network for Hermes Agent..."
    }
    fun autoDiscoverNotFound(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "لم يتم العثور على جهاز بعد. تأكد من تشغيل run_server.bat على الكمبيوتر"
        AppLanguage.EN -> "No PC found yet. Ensure run_server.bat is running on your PC."
    }
    fun autoDiscoverFoundTitle(lang: AppLanguage, host: String) = when (lang) {
        AppLanguage.AR -> "تم العثور على كمبيوترك: $host"
        AppLanguage.EN -> "Found PC: $host"
    }
    fun autoDiscoverBtnConnect(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "⚡ اتصال فوري بنقرة واحدة"
        AppLanguage.EN -> "⚡ 1-Click Instant Connect"
    }
    fun autoDiscoverBtnRescan(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "إعادة البحث"
        AppLanguage.EN -> "Rescan"
    }
}

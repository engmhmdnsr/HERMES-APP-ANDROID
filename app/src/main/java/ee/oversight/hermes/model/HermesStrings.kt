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
        AppLanguage.AR -> "شات"
        AppLanguage.EN -> "CHAT"
    }
    fun tabTerminal(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الطرفية"
        AppLanguage.EN -> "TERMINAL"
    }
    fun tabTelemetry(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "المراقبة"
        AppLanguage.EN -> "TELEMETRY"
    }
    fun tabGateway(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "البوابة"
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
    fun hostSpecsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "بيانات مضيف WINDOWS 11"
        AppLanguage.EN -> "WINDOWS 11 HOST TELEMETRY"
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
    fun uptimeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مدة التشغيل:"
        AppLanguage.EN -> "Host Uptime:"
    }
    fun diskLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مساحة التخزين:"
        AppLanguage.EN -> "Disk Storage:"
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
    fun connectButton(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "اتصال"
        AppLanguage.EN -> "CONNECT"
    }

    // Devices & Profiles Management
    fun deviceNameLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "اسم الجهاز"
        AppLanguage.EN -> "Device Name"
    }
    fun deviceNamePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مثال: كمبيوتر البيت"
        AppLanguage.EN -> "e.g. Home PC"
    }
    fun devicesSectionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الأجهزة"
        AppLanguage.EN -> "DEVICES"
    }
    fun noSavedDevices(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "لا توجد أجهزة محفوظة. املأ البيانات واضغط اتصال."
        AppLanguage.EN -> "No saved devices. Fill the details and press Connect."
    }
    fun lockedModeBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "مقفل"
        AppLanguage.EN -> "LOCKED"
    }
    fun editingModeBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "تعديل"
        AppLanguage.EN -> "EDITING"
    }
    fun activeBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "نشط"
        AppLanguage.EN -> "ACTIVE"
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
    fun gatewayTypeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "طريقة استهداف البوابة:"
        AppLanguage.EN -> "Gateway Addressing Method:"
    }
    fun remoteGatewayUrlLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "رابط البوابة البعيدة (Gateway URL)"
        AppLanguage.EN -> "Remote Gateway URL"
    }
    fun remoteGatewayUrlPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "https://hermes-pc.tailnet.ts.net أو http://100.x.x.x:8080"
        AppLanguage.EN -> "https://hermes-pc.tailnet.ts.net or http://100.x.x.x:8080"
    }
    fun presetTailscaleDirect(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "أدخل IP الـ Tailscale بتاع جهازك (من تطبيق Tailscale)"
        AppLanguage.EN -> "Enter your PC's Tailscale IP (from the Tailscale app)"
    }

    // Hermes API Key Guidance

    // Guide

    // Full Hermes Agent Server Script on Windows

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
        AppLanguage.AR -> "لم يتم العثور على جهاز بعد. تأكد إن الـ PC على نفس شبكة الواي فاي والـ beacon شغال"
        AppLanguage.EN -> "No PC found yet. Make sure the PC is on the same Wi-Fi and the discovery beacon is running."
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

    // Tool execution blocks
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

    // Telemetry extras
    fun statusOnline(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "الحالة: متصل"
        AppLanguage.EN -> "STATUS: ONLINE"
    }
    fun directPeer(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "(نظير مباشر P2P)"
        AppLanguage.EN -> "(Direct Peer)"
    }
    fun cpuTimeline(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "المخطط الزمني لاستهلاك المعالج"
        AppLanguage.EN -> "CPU UTILIZATION TIMELINE"
    }
    fun liveBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "لحظي"
        AppLanguage.EN -> "LIVE"
    }

    // Chat stream extras
    fun receivingStream(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "جاري استقبال تدفق النصوص من SSE..."
        AppLanguage.EN -> "Receiving SSE text stream..."
    }
    fun presetDeepSeek(lang: AppLanguage) = when (lang) {
        AppLanguage.AR -> "🧠 استنتاج واستكشاف أخطاء بنموذج DeepSeek R1"
        AppLanguage.EN -> "🧠 Reasoning & troubleshooting with DeepSeek R1"
    }
}

# هيرمز كونترول - دليل التوصيل السريع (عربي)

## أول حاجة: PC (شغال دلوقتي)

الـ API server الرسمي **شغال حالياً** على جهازك:
- العنوان: `http://100.124.105.88:8642`
- الحالة: gateway running + Telegram connected + api_server connected

الـ API key في الملف ده على الـ PC:
```
%LOCALAPPDATA%\hermes\.env
```
افتحه وانسخ القيمة اللي بعد `API_SERVER_KEY=` (طويلة، 64 حرف).

## تاني حاجة: الموبايل

1. **ركّب التطبيق**: انقل ملف `HermesControl-debug.apk` للموبايل وثبّته.
2. **افتح التطبيق** وروح لتاب **Gateway** (البوابة).
3. **حط البيانات دي**:
   - **Tailscale IP**: `100.124.105.88`
   - **Port**: `8642`
   - **API Key**: انسخ القيمة من `.env` على الـ PC
   - (لو في switch لـ Demo Mode، تأكد إنه **طافي/Off**)
4. دوس **TEST PING**.
   - لو ظهر `PEER HANDSHAKE SUCCESSFUL` يبقى التوصيل نجح.
5. ارجع لتاب **Chat**: هتلاقي الـ sessions القديمة ظاهرة، اختار session أو اعمل واحدة جديدة، وابعت.

## لو مش متصل

| المشكلة | الحل |
|---|---|
| الـ PC مقفول/نايم | شغّله (الـ gateway بيشتغل تلقائياً من Startup) |
| الـ Tailscale مش شغال على الموبايل | افتح تطبيق Tailscale وتأكد إنه متصل |
| IP غلط | اعمل `tailscale ip -4` على الـ PC وانسخ الـ IP |
| HTTP 401 | الـ API key غلط، انسخه تاني من `.env` |
| **الـ gateway مش شغال** | على الـ PC شغّل: `Hermes_Gateway_apiserver.cmd` (في `gateway-service/`) أو اعمل ريستارت للجهاز |

## ملاحظة مهمة

**ممنوع تشغيل** `server/run_server.bat` أو `python server/hermes_gateway.py` تاني.
ده كان السيرفر القديم (port 8080) واللي بيكتب في قاعدة بيانات Hermes مباشرة (خطر على البيانات).
التطبيق دلوقتي بيستخدم الـ API server الرسمي على **8642**.

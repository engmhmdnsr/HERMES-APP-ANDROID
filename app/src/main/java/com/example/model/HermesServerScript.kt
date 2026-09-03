package com.example.model

/**
 * Guide text for enabling the OFFICIAL Hermes Agent API server on the PC.
 *
 * Since Hermes v0.20 the recommended remote-control path is the built-in
 * API server (gateway/platforms/api_server.py), NOT a custom FastAPI shim.
 * Enable it by adding these lines to %LOCALAPPDATA%\hermes\.env and
 * restarting the gateway (`hermes gateway restart`):
 */
object HermesServerScript {
    val pythonScript = """
# ============================================================
# HERMES CONTROL - PC SETUP (OFFICIAL API SERVER)
# ============================================================
# No Python script needed! Hermes ships a built-in API server.
#
# 1) Open this file on your PC in Notepad:
#    %LOCALAPPDATA%\hermes\.env
#
# 2) Add / uncomment these lines at the bottom:
#
#    # Hermes API Server (mobile remote control)
#    API_SERVER_ENABLED=true
#    API_SERVER_KEY=<generate-a-long-random-secret>
#    API_SERVER_HOST=100.x.x.x        <- your Tailscale IP
#    API_SERVER_PORT=8642
#
#    Generate a strong key with:
#      python -c "import secrets; print(secrets.token_hex(32))"
#
# 3) Restart the Hermes gateway:
#    hermes gateway restart
#
# 4) Verify from the same PC:
#    curl -H "Authorization: Bearer <key>" http://127.0.0.1:8642/health
#    -> {"status":"ok","platform":"hermes-agent",...}
#
# 5) In the app on your phone, set:
#    IP      : your PC Tailscale IP (100.x.x.x)
#    Port    : 8642
#    API Key : the API_SERVER_KEY value
#
# The API server is bound to your Tailscale IP only, never 0.0.0.0,
# so it is NOT exposed to the public internet.
""".trimIndent()
}

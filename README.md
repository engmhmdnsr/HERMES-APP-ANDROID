# Hermes Control (Android)

Mobile control center for **Hermes Agent** running on a Windows PC, reached securely over **Tailscale**. Browse sessions, read chat history, switch models, send prompts with live SSE streaming + tool execution blocks, and check gateway health.

> **Public release** — this app is **generic**: every user connects to **their own** Hermes PC. No author-specific IPs or keys are baked in. Package ID: `ee.oversight.hermes`. Built by [Oversight EE](https://cyber.oversight.ee).

This app talks to the **official Hermes API server** built into Hermes Agent (the `api_server` gateway platform) — no custom FastAPI shim required.

---

## Architecture (v4 - official API server)

```
┌─────────────┐   HTTPS/Tailscale    ┌─────────────────────────────┐
│ Android App │ ───────────────────► │ Hermes Gateway (Windows PC) │
│ (Kotlin)    │   http://100.x.x.x:8642  │  platforms.api_server    │
└─────────────┘                     │  • /api/sessions           │
                                   │  • /api/sessions/{id}/chat  │
                                   │  • /api/model/options       │
                                   │  • /health/detailed         │
                                   └─────────────────────────────┘
```

The **old custom FastAPI gateway** (`server/hermes_gateway.py`, port 8080) wrote directly into Hermes' `state.db` and is **no longer needed**. It has been replaced by the official API server, which runs inside the gateway process and uses Hermes' own session store safely. The legacy server files were removed from this repo; only the E2E test script remains under `server/`.

---

## PC Setup (one-time, ~3 minutes)

### 1. Enable the API server

Edit `%LOCALAPPDATA%\hermes\.env` (on the Windows PC) and add:

```ini
# Hermes API Server (mobile remote control)
API_SERVER_ENABLED=true
API_SERVER_KEY=<long-random-secret>
API_SERVER_HOST=<your-tailscale-ip>    # e.g. 100.124.105.88
API_SERVER_PORT=8642
```

Generate a strong key:

```bash
python -c "import secrets; print(secrets.token_hex(32))"
```

### 2. Restart the Hermes gateway

```bash
hermes gateway restart
```

### 3. Verify it is listening

```bash
curl -H "Authorization: Bearer <API_SERVER_KEY>" http://127.0.0.1:8642/health
# → {"status":"ok","platform":"hermes-agent","version":"0.20.x"}
```

> The API server binds to the **Tailscale IP only** - never `0.0.0.0` - so it is not exposed to the public internet. Keep the key secret; it gates full agent control (tools run as your Windows user).

---

## Phone Setup

1. Install the APK (`HermesControl-debug.apk` or `app/build/outputs/apk/debug/app-debug.apk`).
2. Open **Gateway** tab.
3. Enter your PC's **Tailscale IP** (e.g. `100.124.105.88`), **port** `8642`, and the **API_SERVER_KEY** from `.env`.
4. Tap **TEST PING** - you should see `PEER HANDSHAKE SUCCESSFUL`.
5. The app auto-loads your sessions and the live model list.

### QR pairing (optional)

The official API server does not serve a QR page. You can build your own pairing QR by encoding:

```
hermes://connect?ip=100.124.105.88&port=8642&key=<API_SERVER_KEY>
```

The app accepts that deep link / QR text on the Gateway tab.

---

## API endpoints used by the app

| Purpose | Method & Path | Notes |
|---|---|---|
| Health check | `GET /health` | Auth: `Authorization: Bearer <key>` |
| Gateway status | `GET /health/detailed` | platforms state, version, readiness |
| List sessions | `GET /api/sessions?limit=50` | `{object:"list", data:[...]}` |
| Session messages | `GET /api/sessions/{id}/messages?order=oldest` | `{data:[{role,content,...}]}` |
| Create session | `POST /api/sessions` | body `{title?, model?, source}` |
| Lock model | `POST /api/sessions/{id}/model` | body `{model:"provider/model", require_model_lock:true}` |
| Stream chat | `POST /api/sessions/{id}/chat/stream` | SSE events: `run.started`, `message.started`, `assistant.delta`, `tool.started/completed/failed`, `assistant.completed`, `run.completed`, `done` |
| Model catalog | `GET /api/model/options` | `{providers:[{slug,name,models:[...]}]}` |

Full capability discovery: `GET /v1/capabilities`.

---

## Model switching

Model inventory is fetched live from `/api/model/options`, which returns every
provider configured in Hermes (openrouter, gemini, deepseek, commandcode,
lmstudio, custom providers, ...) and their model ids. Choosing a model in the
chat screen locks that model onto the current session via
`POST /api/sessions/{id}/model`, so subsequent turns on that session use it.

---

## Building the APK

Prereqs: JDK 17+, Android SDK with platform 36.

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## End-to-end verification (PC side)

After enabling the API server, run the same flow the app performs (health,
sessions, model catalog, create session, lock model, SSE stream):

```bash
python server/test_official_api.py
# ALL CHECKS PASSED - app should connect and stream fine.
```

> The old custom gateway (port 8080, FastAPI shim) is **not needed anymore** -
> do NOT run it. It wrote directly into Hermes' state.db and exposed an
> unauthenticated surface on `0.0.0.0`. The official API server (8642)
> replaces it.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Connection refused` / ping fails | Gateway not running on PC → `hermes gateway restart`; verify `netstat -ano \| grep 8642` |
| HTTP 401 | Wrong API key → copy `API_SERVER_KEY` exactly from `.env` |
| Sessions list empty | API server can't read state.db → check gateway log: `%LOCALAPPDATA%\hermes\logs\gateway.log` |
| Old app pointed to 8080 | The legacy custom gateway is deprecated. Use port **8642** and the official API |
| Model list empty | Run `hermes model` once on the PC to populate the provider catalog |

---

## Repo layout

```
app/src/main/java/com/example/
  data/HermesNetworkClient.kt     # Official API client (sessions, stream, models)
  data/HermesPreferencesRepository.kt
  model/HermesModels.kt           # Data classes + defaults (Tailscale IP, port 8642)
  model/HermesServerScript.kt     # PC setup guide (official API server)
  ui/HermesViewModel.kt           # State + orchestration
  ui/screens/                     # ChatTerminal / SystemMonitoring / GatewayConfig
server/test_official_api.py       # E2E verification against the live API server
```

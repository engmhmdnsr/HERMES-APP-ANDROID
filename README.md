# Hermes Control (Android)

Mobile control center for **Hermes Agent** running on a Windows PC, reached securely over **Tailscale**. Browse sessions, read chat history, switch models, send prompts with live SSE streaming + tool execution blocks, and check gateway health.

> **v1.2.3** — this app is **generic**: every user connects to **their own** Hermes PC. No author-specific IPs or keys are baked in. Package ID: `ee.oversight.hermes`. Built by Oversight.ee. **This app is not an official Hermes application.**

This app talks to the **official Hermes API server** built into Hermes Agent (the `api_server` gateway platform) — no custom FastAPI shim required.

---

## Architecture (v4 - official API server)

```
┌─────────────┐   HTTPS/Tailscale    ┌─────────────────────────────┐
│ Android App │ ───────────────────► │ Hermes Gateway (Windows PC) │
│ (Kotlin)    │   http://100.x.x.x:8080  │  platforms.api_server    │
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
API_SERVER_PORT=8080
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
curl -H "Authorization: Bearer <API_SERVER_KEY>" http://127.0.0.1:8080/health
# → {"status":"ok","platform":"hermes-agent","version":"0.20.x"}
```

> The API server binds to the **Tailscale IP only** - never `0.0.0.0` - so it is not exposed to the public internet. Keep the key secret; it gates full agent control (tools run as your Windows user).

---

## Phone Setup

1. Install the APK (`HermesControl-v1.2.3.apk` at the repo root, or `app/build/outputs/apk/release/app-release.apk`).
2. Open **Gateway** tab.
3. Enter your PC's **Tailscale IP** (e.g. `100.124.105.88`), **port** `8080`, and the **API_SERVER_KEY** from `.env`.
4. Tap **TEST PING** - you should see `PEER HANDSHAKE SUCCESSFUL`.
5. The app auto-loads your sessions and the live model list.
---

## API endpoints used by the app

| Purpose | Method & Path | Notes |
|---|---|---|
| Health check | `GET /health` | Auth: `Authorization: Bearer *** |
| Gateway status | `GET /health/detailed` | platforms state, version, readiness |
| System telemetry | `GET /api/system` | CPU/RAM/GPU + processes (psutil, added to api_server) |
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
> unauthenticated surface on `0.0.0.0`. The official API server (8080)
> replaces it.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Connection refused` / ping fails | Gateway not running on PC → `hermes gateway restart`; verify `netstat -ano \| grep 8080` |
| HTTP 401 | Wrong API key → copy `API_SERVER_KEY` exactly from `.env` |
| Sessions list empty | API server can't read state.db → check gateway log: `%LOCALAPPDATA%\hermes\logs\gateway.log` |
| Can't connect | Verify gateway running (`hermes gateway restart`), port **8080**, correct API key |
| Model list empty | Run `hermes model` once on the PC to populate the provider catalog |

## Auto-discovery (same Wi-Fi)

To let the app auto-find this PC on the same Wi-Fi (no typing), run the UDP
beacon alongside the gateway:

```bash
python server/hermes_discovery_beacon.py
```

A `Hermes_Discovery_Beacon.vbs` in the Windows Startup folder launches it at
logon automatically. It advertises the API port (default 8080) over UDP 8089;
the app's "Auto-discover" then finds the PC instantly.

---

## Repo layout

```
app/src/main/java/ee/oversight/hermes/
  data/HermesNetworkClient.kt     # Official API client (sessions, stream, models)
  data/HermesPreferencesRepository.kt
  data/HermesAppLog.kt            # Live in-app log stream (StateFlow)
  model/HermesModels.kt           # Data classes + defaults (Tailscale IP, port 8080)
  model/HermesStrings.kt          # AR/EN UI strings
  ui/HermesViewModel.kt           # State + orchestration
  ui/MainScreen.kt                # Navigation shell
  ui/screens/                     # ChatTerminal / SystemMonitoring / GatewayConfig
  ui/components/                  # CyberpunkTopBar, SessionsDrawer, ... 
server/hermes_discovery_beacon.py # Same-Wi-Fi UDP discovery beacon (optional)
server/test_official_api.py       # E2E verification against the live API server
```

## Release builds

Signed release builds need the keystore (`hermes-control-release.jks`) and its
passwords in `keystore-credentials.txt` (gitignored, local only):

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
# Copy to repo root as HermesControl-v1.2.3.apk
```

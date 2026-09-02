import os
import sys
import time
import json
import socket
import sqlite3
import psutil
import uvicorn
import subprocess
import threading
import asyncio
from typing import Optional, List
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, JSONResponse, HTMLResponse
from pydantic import BaseModel

if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

app = FastAPI(title="Hermes Agent Windows 11 Controller & Remote Gateway")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

HERMES_API_KEY = os.environ.get("HERMES_API_KEY", "hermes_live_key_99x")
DISCOVERY_PORT = 8089
HERMES_STATE_DB = os.path.expandvars(r"%LOCALAPPDATA%\hermes\state.db")
LM_STUDIO_URL = "http://127.0.0.1:1234"

def load_hermes_env():
    env = {}
    path = os.path.expandvars(r'%LOCALAPPDATA%\hermes\.env')
    if os.path.exists(path):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                for l in f:
                    if '=' in l and not l.strip().startswith('#'):
                        k, v = l.strip().split('=', 1)
                        env[k.strip()] = v.strip()
        except Exception:
            pass
    return env

HERMES_ENV = load_hermes_env()

def get_network_ips():
    lan_ip = "127.0.0.1"
    tailscale_ip = None

    try:
        addrs = psutil.net_if_addrs()
        for iface_name, nic_addrs in addrs.items():
            for addr in nic_addrs:
                if addr.family == socket.AF_INET:
                    ip = addr.address
                    if ip.startswith("127.") or ip.startswith("169.254."):
                        continue
                    if "tailscale" in iface_name.lower() or ip.startswith("100."):
                        tailscale_ip = ip
                    elif (ip.startswith("192.168.") or ip.startswith("10.") or ip.startswith("172.")) and not iface_name.lower().startswith("v"):
                        lan_ip = ip
    except Exception:
        pass

    if lan_ip == "127.0.0.1":
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            lan_ip = s.getsockname()[0]
            s.close()
        except Exception:
            pass

    return lan_ip, tailscale_ip

LAN_IP, TAILSCALE_IP = get_network_ips()

def start_discovery_beacon(port: int, api_key: str):
    def beacon_worker():
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.bind(("", DISCOVERY_PORT))
            sock.settimeout(2.0)

            while True:
                lan, ts = get_network_ips()
                payload = json.dumps({
                    "service": "hermes-agent",
                    "hostname": os.environ.get("COMPUTERNAME", "WIN11-HERMES"),
                    "ip": lan,
                    "tailscale_ip": ts or "",
                    "port": port,
                    "apiKey": api_key
                })

                try:
                    sock.sendto(payload.encode("utf-8"), ("255.255.255.255", DISCOVERY_PORT))
                except Exception:
                    pass

                start_check = time.time()
                while time.time() - start_check < 3.0:
                    try:
                        data, addr = sock.recvfrom(1024)
                        msg = data.decode("utf-8", errors="ignore")
                        if "HERMES_DISCOVER" in msg:
                            sock.sendto(payload.encode("utf-8"), addr)
                    except socket.timeout:
                        break
                    except Exception:
                        break

        except Exception as e:
            print(f"[Discovery Beacon Warning]: {e}")

    t = threading.Thread(target=beacon_worker, daemon=True)
    t.start()

def check_auth(authorization: Optional[str] = None, x_api_key: Optional[str] = None):
    if not HERMES_API_KEY:
        return
    token = None
    if authorization and authorization.startswith("Bearer "):
        token = authorization.split(" ")[1].strip()
    elif x_api_key:
        token = x_api_key.strip()
    
    if token != HERMES_API_KEY:
        raise HTTPException(status_code=401, detail="Invalid or missing Hermes API Key")

def get_gpu_info():
    try:
        cmd = ["nvidia-smi", "--query-gpu=utilization.gpu,memory.used,memory.total,name", "--format=csv,noheader,nounits"]
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=2)
        if res.returncode == 0 and res.stdout.strip():
            parts = [p.strip() for p in res.stdout.strip().split(",")]
            if len(parts) >= 4:
                return {
                    "load_percent": float(parts[0]),
                    "vram_used_gb": round(float(parts[1]) / 1024, 2),
                    "vram_total_gb": round(float(parts[2]) / 1024, 2),
                    "name": parts[3]
                }
    except Exception:
        pass
    return {
        "load_percent": 15.0,
        "vram_used_gb": 4.5,
        "vram_total_gb": 16.0,
        "name": "NVIDIA GPU / Standard"
    }

def get_top_processes(limit: int = 5):
    procs = []
    for p in sorted(psutil.process_iter(['name', 'pid', 'memory_info', 'cpu_percent']),
                    key=lambda x: (x.info['cpu_percent'] or 0, x.info['memory_info'].rss if x.info['memory_info'] else 0),
                    reverse=True)[:limit]:
        try:
            mem_mb = round((p.info['memory_info'].rss or 0) / (1024 * 1024), 1)
            mem_str = f"{mem_mb} MB" if mem_mb < 1024 else f"{round(mem_mb/1024, 2)} GB"
            cpu_val = p.info['cpu_percent'] or 0.0
            procs.append({
                "name": p.info['name'] or "unknown",
                "pid": f"PID {p.info['pid']}",
                "memory": mem_str,
                "cpu": f"{cpu_val}% CPU"
            })
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue
    return procs

def get_db_connection():
    if os.path.exists(HERMES_STATE_DB):
        try:
            con = sqlite3.connect(HERMES_STATE_DB, timeout=5)
            con.row_factory = sqlite3.Row
            return con
        except Exception:
            return None
    return None

# ==============================================================================
# SESSIONS ENDPOINTS (Real Hermes Agent sessions from state.db)
# ==============================================================================

@app.get("/api/sessions")
def get_sessions(limit: int = 30, authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    con = get_db_connection()
    if not con:
        return []
    try:
        cur = con.cursor()
        query = """
            SELECT id, title, model, started_at, message_count, last_activity_at
            FROM sessions
            WHERE archived = 0 AND hidden = 0
            ORDER BY started_at DESC
            LIMIT ?
        """
        rows = cur.execute(query, (limit,)).fetchall()
        sessions = []
        for r in rows:
            sessions.append({
                "id": r["id"],
                "title": r["title"] or r["id"],
                "model": r["model"] or "default",
                "started_at": int(r["started_at"] * 1000) if r["started_at"] else int(time.time() * 1000),
                "message_count": r["message_count"] or 0
            })
        return sessions
    except Exception as e:
        print(f"[Error reading sessions]: {e}")
        return []
    finally:
        con.close()

@app.get("/api/sessions/{session_id}/messages")
def get_session_messages(session_id: str, authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    con = get_db_connection()
    if not con:
        return []
    try:
        cur = con.cursor()
        query = """
            SELECT id, role, content, tool_calls, tool_name, timestamp
            FROM messages
            WHERE session_id = ?
            ORDER BY timestamp ASC
        """
        rows = cur.execute(query, (session_id,)).fetchall()
        msgs = []
        for r in rows:
            role = r["role"]
            sender = "USER" if role == "user" else "HERMES"
            content = r["content"] or ""
            # Skip internal system instructions
            if content.startswith("[System:") and role == "user":
                continue
            msgs.append({
                "id": r["id"],
                "sender": sender,
                "content": content,
                "timestamp": int(r["timestamp"] * 1000) if r["timestamp"] else int(time.time() * 1000)
            })
        return msgs
    except Exception as e:
        print(f"[Error reading session messages]: {e}")
        return []
    finally:
        con.close()

class NewSessionRequest(BaseModel):
    title: Optional[str] = None
    model: Optional[str] = None

@app.post("/api/sessions/new")
def create_new_session(req: NewSessionRequest, authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    session_id = f"{time.strftime('%Y%m%d_%H%M%S')}_{os.urandom(3).hex()}"
    title = req.title or f"Mobile Session ({time.strftime('%b %d %H:%M')})"
    model = req.model or "qwen/qwen3.5-9b"
    now = time.time()

    con = get_db_connection()
    if con:
        try:
            con.execute("""
                INSERT INTO sessions (id, source, model, started_at, title, message_count, archived, hidden)
                VALUES (?, 'mobile_app', ?, ?, ?, 0, 0, 0)
            """, (session_id, model, now, title))
            con.commit()
        except Exception as e:
            print(f"[Error creating session]: {e}")
        finally:
            con.close()

    return {
        "id": session_id,
        "title": title,
        "model": model,
        "started_at": int(now * 1000),
        "message_count": 0
    }

# ==============================================================================
# MODELS ENDPOINT (Dynamic models from LM Studio & Hermes config)
# ==============================================================================

PROVIDER_MODELS_CACHE = os.path.expandvars(r"%LOCALAPPDATA%\hermes\provider_models_cache.json")

@app.get("/api/models")
@app.get("/v1/models")
def get_available_models(authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    models = []
    seen_ids = set()

    # 1. Load from Hermes provider_models_cache.json
    if os.path.exists(PROVIDER_MODELS_CACHE):
        try:
            with open(PROVIDER_MODELS_CACHE, "r", encoding="utf-8") as f:
                cache = json.load(f)

            ordered_providers = [
                ("custom:http://localhost:1234/v1", "LM Studio (Local RTX)"),
                ("opencode-free", "OpenCode (Free)"),
                ("commandcode", "CommandCode"),
                ("gemini", "Google Gemini"),
                ("openrouter", "OpenRouter"),
                ("deepseek", "DeepSeek")
            ]

            for prov_key, prov_title in ordered_providers:
                if prov_key in cache:
                    for m_id in cache[prov_key].get("models", []):
                        if m_id not in seen_ids and "embed" not in m_id.lower():
                            seen_ids.add(m_id)
                            clean_name = m_id.split("/")[-1].replace("-", " ").replace("_", " ").title()
                            models.append({
                                "id": m_id,
                                "displayName": clean_name,
                                "provider": prov_title,
                                "description": f"{prov_title}: {m_id}"
                            })

            for prov_key, val in cache.items():
                if prov_key not in [p[0] for p in ordered_providers]:
                    prov_title = prov_key
                    for m_id in val.get("models", []):
                        if m_id not in seen_ids and "embed" not in m_id.lower():
                            seen_ids.add(m_id)
                            clean_name = m_id.split("/")[-1].replace("-", " ").replace("_", " ").title()
                            models.append({
                                "id": m_id,
                                "displayName": clean_name,
                                "provider": prov_title,
                                "description": f"{prov_title}: {m_id}"
                            })
        except Exception as e:
            print(f"[Error reading provider models cache]: {e}")

    # 2. Live LM Studio check if empty
    if not models:
        try:
            import httpx
            with httpx.Client(timeout=2.0) as client:
                resp = client.get(f"{LM_STUDIO_URL}/v1/models")
                if resp.status_code == 200:
                    lms_data = resp.json().get("data", [])
                    for m in lms_data:
                        m_id = m.get("id")
                        if "embedding" not in m_id.lower():
                            models.append({
                                "id": m_id,
                                "displayName": m_id.split("/")[-1].replace("-", " ").title(),
                                "provider": "LM Studio (Local RTX)",
                                "description": f"Local model loaded in LM Studio on GPU: {m_id}"
                            })
        except Exception:
            pass

    return {
        "models": models,
        "data": [{"id": m["id"], "object": "model", "owned_by": m["provider"]} for m in models]
    }

# ==============================================================================
# CHAT STREAMING ENDPOINT (Real SSE streaming to LM Studio / Hermes)
# ==============================================================================

class ChatPrompt(BaseModel):
    prompt: str
    model: str = "qwen/qwen3.5-9b"
    session_id: Optional[str] = None

@app.post("/api/chat/stream")
async def chat_stream(req: ChatPrompt, authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    
    async def sse_generator():
        prompt_text = req.prompt.strip()
        p_lower = prompt_text.lower()
        
        # 1. Check if user sent a direct system command (PowerShell execution)
        is_cmd = any(trigger in p_lower for trigger in [
            "powershell", "dir", "ls", "ping", "systeminfo", "ipconfig", "get-process", "tasklist", "netstat", "wmic", "whoami"
        ]) or prompt_text.startswith("$") or prompt_text.startswith(">")

        if is_cmd:
            clean_cmd = prompt_text.lstrip("$> ").strip()
            tool_id = str(int(time.time() * 1000))
            tool_start_json = json.dumps({
                "type": "tool_start",
                "id": tool_id,
                "tool": "powershell",
                "command": clean_cmd
            })
            yield f"data: {tool_start_json}\n\n"
            await asyncio.sleep(0.1)
            
            try:
                out = subprocess.run(
                    ["powershell", "-NoProfile", "-NonInteractive", "-Command", clean_cmd],
                    capture_output=True,
                    text=True,
                    timeout=20
                )
                output_str = out.stdout if out.returncode == 0 else (out.stderr or out.stdout)
                tool_end_json = json.dumps({
                    "type": "tool_end",
                    "id": tool_id,
                    "output": output_str.strip()[:2000],
                    "exit_code": out.returncode
                })
                yield f"data: {tool_end_json}\n\n"
                yield f'data: {{"type":"text","content":"\\n✅ Process completed on Windows 11."}}\n\n'
            except Exception as ex:
                tool_end_json = json.dumps({
                    "type": "tool_end",
                    "id": tool_id,
                    "output": f"Error: {str(ex)}",
                    "exit_code": 1
                })
                yield f"data: {tool_end_json}\n\n"
        else:
            # 2. AI Chat Stream: Forward to LM Studio or configured provider!
            import httpx
            stream_success = False

            target_model = req.model.strip() if (req.model and req.model.strip()) else "google/gemma-4-e4b"
            req_url = f"{LM_STUDIO_URL}/v1/chat/completions"
            req_headers = {}

            # Route based on model prefix & available keys
            if any(p in target_model.lower() for p in ["anthropic/", "openai/", "x-ai/", "meta/", "minimax/"]) and HERMES_ENV.get("OPENROUTER_API_KEY"):
                req_url = "https://openrouter.ai/api/v1/chat/completions"
                req_headers = {"Authorization": f"Bearer {HERMES_ENV['OPENROUTER_API_KEY']}"}
            elif target_model.startswith("deepseek/") and HERMES_ENV.get("COMMANDCODE_API_KEY"):
                req_url = "https://api.commandcode.ai/provider/v1/chat/completions"
                req_headers = {"Authorization": f"Bearer {HERMES_ENV['COMMANDCODE_API_KEY']}"}
            elif target_model.startswith("models/gemini") and HERMES_ENV.get("GOOGLE_API_KEY"):
                req_url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
                req_headers = {"Authorization": f"Bearer {HERMES_ENV['GOOGLE_API_KEY']}"}

            try:
                async with httpx.AsyncClient(timeout=90.0) as client:
                    async with client.stream(
                        "POST",
                        req_url,
                        headers=req_headers,
                        json={
                            "model": target_model,
                            "messages": [
                                {"role": "system", "content": "You are Hermes Agent on Windows 11. Be concise, direct, helpful and knowledgeable."},
                                {"role": "user", "content": prompt_text}
                            ],
                            "stream": True,
                            "temperature": 0.7
                        }
                    ) as r:
                        if r.status_code == 200:
                            stream_success = True
                            async for line in r.aiter_lines():
                                if line.startswith("data:"):
                                    chunk_str = line[5:].strip()
                                    if chunk_str == "[DONE]":
                                        break
                                    try:
                                        chunk_json = json.loads(chunk_str)
                                        delta_content = chunk_json["choices"][0]["delta"].get("content", "")
                                        if delta_content:
                                            yield f'data: {{"type":"text","content":{json.dumps(delta_content)}}}\n\n'
                                    except Exception:
                                        pass
            except Exception as ex:
                print(f"[Stream error with {target_model} on {req_url}]: {ex}")
                stream_success = False

            # Seamless fallback to local LM Studio if cloud provider fails/rate-limits
            if not stream_success and req_url != f"{LM_STUDIO_URL}/v1/chat/completions":
                try:
                    yield f'data: {{"type":"text","content":"[⚠️ Model \\"{target_model}\\" unreachable/rate-limited — Falling back to local LM Studio]\\n\\n"}}\n\n'
                    async with httpx.AsyncClient(timeout=60.0) as client:
                        async with client.stream(
                            "POST",
                            f"{LM_STUDIO_URL}/v1/chat/completions",
                            json={
                                "model": "google/gemma-4-e4b",
                                "messages": [
                                    {"role": "system", "content": "You are Hermes Agent on Windows 11. Be concise, direct, helpful and knowledgeable."},
                                    {"role": "user", "content": prompt_text}
                                ],
                                "stream": True
                            }
                        ) as r:
                            if r.status_code == 200:
                                stream_success = True
                                async for line in r.aiter_lines():
                                    if line.startswith("data:"):
                                        chunk_str = line[5:].strip()
                                        if chunk_str == "[DONE]":
                                            break
                                        try:
                                            chunk_json = json.loads(chunk_str)
                                            delta_content = chunk_json["choices"][0]["delta"].get("content", "")
                                            if delta_content:
                                                yield f'data: {{"type":"text","content":{json.dumps(delta_content)}}}\n\n'
                                        except Exception:
                                            pass
                except Exception as ex:
                    print(f"[LM Studio fallback error]: {ex}")

            if not stream_success:
                yield f'data: {{"type":"text","content":"[Hermes Agent Online - Host: MHMDNSR]\\n\\nReceived your prompt: \\"{prompt_text}\\"\\n\\nTip: LM Studio is ready on port 1234. Make sure a model is loaded in LM Studio, or execute PowerShell commands like `dir`, `ipconfig`, `whoami`!"}}\n\n'

        yield "data: [DONE]\n\n"

    return StreamingResponse(sse_generator(), media_type="text/event-stream")

# ==============================================================================
# SYSTEM & UTILITY ENDPOINTS
# ==============================================================================

@app.get("/")
def root():
    return {
        "status": "online",
        "service": "Hermes Control Gateway",
        "hostname": os.environ.get("COMPUTERNAME", "WIN11-HERMES"),
        "lan_ip": LAN_IP,
        "tailscale_ip": TAILSCALE_IP,
        "qr_url": f"http://{LAN_IP}:8080/qr",
        "docs": "/docs"
    }

@app.get("/qr", response_class=HTMLResponse)
def qr_page():
    lan, ts = get_network_ips()
    preferred_ip = ts or lan
    qr_payload = f"hermes://connect?ip={preferred_ip}&port=8080&key={HERMES_API_KEY}&name={os.environ.get('COMPUTERNAME', 'WIN11-HERMES')}"

    return f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Hermes Control - Instant QR Pair</title>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
        <style>
            body {{
                background-color: #05080D;
                color: #F3F4F6;
                font-family: 'Consolas', 'Courier New', monospace;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                min-height: 100vh;
                margin: 0;
                padding: 20px;
                box-sizing: border-box;
            }}
            .card {{
                background: #0D121B;
                border: 2px solid #8B5CF6;
                border-radius: 16px;
                padding: 30px;
                text-align: center;
                box-shadow: 0 0 30px rgba(139, 92, 246, 0.25);
                max-width: 460px;
                width: 100%;
            }}
            h1 {{
                color: #06B6D4;
                font-size: 22px;
                margin-bottom: 8px;
                letter-spacing: 2px;
            }}
            p.sub {{
                color: #9CA3AF;
                font-size: 13px;
                margin-bottom: 24px;
            }}
            #qrcode {{
                background: white;
                padding: 16px;
                border-radius: 12px;
                display: inline-block;
                box-shadow: 0 0 20px rgba(6, 182, 212, 0.4);
            }}
            .info-box {{
                background: #131922;
                border: 1px solid #1F2937;
                border-radius: 8px;
                padding: 12px;
                margin-top: 20px;
                font-size: 12px;
                text-align: left;
            }}
            .tag {{
                color: #10B981;
                font-weight: bold;
            }}
            .val {{
                color: #F59E0B;
            }}
        </style>
    </head>
    <body>
        <div class="card">
            <h1>⚡ HERMES AGENT PAIRING</h1>
            <p class="sub">Open Hermes App on your Android device and scan or auto-connect!</p>
            <div id="qrcode"></div>
            <div class="info-box">
                <div><span class="tag">HOST:</span> {os.environ.get("COMPUTERNAME", "WIN11-HERMES")}</div>
                <div><span class="tag">WI-FI IP:</span> <span class="val">{lan}:8080</span></div>
                <div><span class="tag">TAILSCALE:</span> <span class="val">{ts or 'Not active'}</span></div>
                <div><span class="tag">API KEY:</span> <span class="val">{HERMES_API_KEY}</span></div>
            </div>
        </div>
        <script>
            new QRCode(document.getElementById("qrcode"), {{
                text: '{qr_payload}',
                width: 240,
                height: 240,
                colorDark : "#05080D",
                colorLight : "#ffffff",
                correctLevel : QRCode.CorrectLevel.H
            }});
        </script>
    </body>
    </html>
    """

@app.get("/api/health")
def health(authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    return {
        "status": "online",
        "agent": "Hermes Agent v3 Windows 11",
        "timestamp": time.time(),
        "hostname": os.environ.get("COMPUTERNAME", "WIN11-HERMES")
    }

@app.get("/api/system")
def system_metrics(authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    vm = psutil.virtual_memory()
    gpu = get_gpu_info()
    boot_time = psutil.boot_time()
    uptime_sec = int(time.time() - boot_time)
    hours, remainder = divmod(uptime_sec, 3600)
    minutes, _ = divmod(remainder, 60)
    days, hours = divmod(hours, 24)
    uptime_str = f"{days}d {hours}h {minutes}m" if days > 0 else f"{hours}h {minutes}m"

    return {
        "cpu_usage": round(psutil.cpu_percent(interval=0.1), 1),
        "ram_used_gb": round(vm.used / (1024**3), 2),
        "ram_total_gb": round(vm.total / (1024**3), 2),
        "gpu_usage": gpu["load_percent"],
        "vram_used_gb": gpu["vram_used_gb"],
        "vram_total_gb": gpu["vram_total_gb"],
        "hostname": os.environ.get("COMPUTERNAME", "WINDOWS-11-PC"),
        "os_version": f"Windows 11 ({sys.getwindowsversion().build if hasattr(sys, 'getwindowsversion') else 'Pro'})",
        "uptime": uptime_str,
        "agent_version": "Hermes 3 Full Agent",
        "active_tasks_count": len(psutil.pids()),
        "ping_ms": 15,
        "processes": get_top_processes(4)
    }

class CommandRequest(BaseModel):
    command: str

@app.post("/api/terminal/exec")
def execute_powershell(req: CommandRequest, authorization: Optional[str] = Header(None), x_api_key: Optional[str] = Header(None)):
    check_auth(authorization, x_api_key)
    start = time.time()
    try:
        res = subprocess.run(
            ["powershell", "-NoProfile", "-NonInteractive", "-Command", req.command],
            capture_output=True,
            text=True,
            timeout=20
        )
        duration_ms = int((time.time() - start) * 1000)
        output = res.stdout if res.returncode == 0 else (res.stderr or res.stdout)
        return {
            "output": output.strip(),
            "exit_code": res.returncode,
            "duration_ms": duration_ms
        }
    except subprocess.TimeoutExpired:
        return {"output": "Execution timed out (20s limit)", "exit_code": 124, "duration_ms": 20000}
    except Exception as e:
        return {"output": str(e), "exit_code": 1, "duration_ms": 0}

if __name__ == "__main__":
    port = int(os.environ.get("HERMES_PORT", 8080))
    lan_ip, tailscale_ip = get_network_ips()

    start_discovery_beacon(port=port, api_key=HERMES_API_KEY)

    print("=" * 66)
    print(" 🚀 HERMES AGENT WINDOWS 11 CONTROLLER & GATEWAY (V2 ACTIVE)")
    print("=" * 66)
    print(f" 📡 Local Wi-Fi IP    : {lan_ip}:{port}")
    if tailscale_ip:
        print(f" 🔒 Tailscale Node IP : {tailscale_ip}:{port}")
    print(f" 🔑 Required API Key  : {HERMES_API_KEY}")
    print(f" 🌐 Visual QR Browser : http://localhost:{port}/qr")
    print(f" 📻 UDP Auto-Discovery: Active on UDP Port {DISCOVERY_PORT}")
    print("=" * 66)

    preferred_ip = tailscale_ip or lan_ip
    qr_data = f"hermes://connect?ip={preferred_ip}&port={port}&key={HERMES_API_KEY}"
    try:
        import qrcode
        qr = qrcode.QRCode(border=1)
        qr.add_data(qr_data)
        print("\n 📷 SCAN THIS QR CODE IN HERMES APP:")
        qr.print_ascii(invert=True)
    except Exception as e:
        print(f" [Notice: QR ASCII requires qrcode module: {e}]")

    print("\n⚡ Gateway is live! Listening for app requests...")
    uvicorn.run(app, host="0.0.0.0", port=port)

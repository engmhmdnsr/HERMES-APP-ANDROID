package com.example.model

object HermesServerScript {
    val pythonScript = """
import os
import psutil
import uvicorn
import subprocess
import time
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel

app = FastAPI(title="Hermes Agent Windows 11 Controller")

# Secret API key for authentication (Match with Android App)
HERMES_API_KEY = os.environ.get("HERMES_API_KEY", "hermes_live_key_99x")

def check_auth(authorization: str = None, x_api_key: str = None):
    token = None
    if authorization and authorization.startswith("Bearer "):
        token = authorization.split(" ")[1].strip()
    elif x_api_key:
        token = x_api_key.strip()
    
    if HERMES_API_KEY and token != HERMES_API_KEY:
        raise HTTPException(status_code=401, detail="Invalid Hermes API Key")

@app.get("/api/health")
def health(authorization: str = Header(None), x_api_key: str = Header(None)):
    check_auth(authorization, x_api_key)
    return {
        "status": "online",
        "agent": "Hermes Agent v3 Windows 11",
        "timestamp": time.time()
    }

@app.get("/api/system")
def system_metrics(authorization: str = Header(None), x_api_key: str = Header(None)):
    check_auth(authorization, x_api_key)
    vm = psutil.virtual_memory()
    return {
        "cpu_usage": psutil.cpu_percent(interval=0.2),
        "ram_used_gb": round(vm.used / (1024**3), 2),
        "ram_total_gb": round(vm.total / (1024**3), 2),
        "gpu_usage": 18.5,
        "hostname": os.environ.get("COMPUTERNAME", "WIN11-HERMES"),
        "os_version": "Windows 11 Pro 64-bit",
        "uptime": "Active",
        "agent_version": "Hermes 3 Full Agent",
        "active_tasks_count": 1,
        "ping_ms": 12
    }

class CommandRequest(BaseModel):
    command: str

@app.post("/api/terminal/exec")
def execute_powershell(req: CommandRequest, authorization: str = Header(None), x_api_key: str = Header(None)):
    check_auth(authorization, x_api_key)
    start = time.time()
    try:
        res = subprocess.run(["powershell", "-Command", req.command], capture_output=True, text=True, timeout=15)
        duration_ms = int((time.time() - start) * 1000)
        return {
            "output": res.stdout if res.returncode == 0 else (res.stderr or res.stdout),
            "exit_code": res.returncode,
            "duration_ms": duration_ms
        }
    except Exception as e:
        return {"output": str(e), "exit_code": 1, "duration_ms": 0}

class ChatPrompt(BaseModel):
    prompt: str
    model: str = "hermes3"

@app.post("/api/chat/stream")
async def chat_stream(req: ChatPrompt, authorization: str = Header(None), x_api_key: str = Header(None)):
    check_auth(authorization, x_api_key)
    
    async def sse_generator():
        # Optional: You can forward req.prompt to Ollama locally on 11434 if installed,
        # or execute Hermes commands directly!
        yield "data: {\"type\":\"text\",\"delta\":\"[Hermes Agent] Executing on Windows 11: \"}\n\n"
        
        # Check if prompt contains a PowerShell command request
        p_lower = req.prompt.lower()
        if "powershell" in p_lower or "dir" in p_lower or "ping" in p_lower or "systeminfo" in p_lower:
            cmd = req.prompt
            yield "data: {\"type\":\"tool_start\",\"id\":\"1\",\"tool\":\"powershell\",\"command\":\"" + cmd + "\"}\n\n"
            try:
                out = subprocess.run(["powershell", "-Command", cmd], capture_output=True, text=True, timeout=10)
                result_text = out.stdout.replace("\n", "\\n").replace("\"", "\\\"")[:400]
                yield f"data: {{\"type\":\"tool_output\",\"id\":\"1\",\"output\":\"{result_text}\",\"exit_code\":{out.returncode}}}\n\n"
            except Exception as ex:
                yield f"data: {{\"type\":\"tool_output\",\"id\":\"1\",\"output\":\"{str(ex)}\",\"exit_code\":1}}\n\n"
        else:
            yield f"data: {{\"type\":\"text\",\"delta\":\"Ready to control your Windows 11 PC! You sent: {req.prompt}\"}}\n\n"
            
        yield "data: [DONE]\n\n"

    return StreamingResponse(sse_generator(), media_type="text/event-stream")

if __name__ == "__main__":
    print("⚡ Starting Hermes Agent Windows 11 Controller on 0.0.0.0:8080...")
    print(f"🔑 Required API Key: {HERMES_API_KEY}")
    uvicorn.run(app, host="0.0.0.0", port=8080)
""".trimIndent()
}

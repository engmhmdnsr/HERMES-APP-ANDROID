"""
End-to-end verification for the Hermes Control Android app -> official Hermes API server.

Run on the Windows PC (from the repo or anywhere):
    python server/test_official_api.py

Requires the API_SERVER_KEY from %LOCALAPPDATA%\\hermes\\.env (auto-read) or set
API_SERVER_KEY / API_SERVER_HOST / API_SERVER_PORT env vars.
"""
import json
import os
import sys
import time
import urllib.request

def load_env_key():
    env_path = os.path.expandvars(r"%LOCALAPPDATA%\hermes\.env")
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("API_SERVER_KEY="):
                    return line.split("=", 1)[1].strip()
    return os.environ.get("API_SERVER_KEY", "")

def req(base, path, key, method="GET", body=None, timeout=30, stream=False):
    url = f"{base}{path}"
    headers = {"Authorization": f"Bearer {key}"}
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r, timeout=timeout) as resp:
            if stream:
                return resp.status, resp.read().decode("utf-8", errors="replace")
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:500]

def main():
    key = load_env_key()
    host = os.environ.get("API_SERVER_HOST", "100.124.105.88")
    port = os.environ.get("API_SERVER_PORT", "8642")
    base = f"http://{host}:{port}"
    if not key:
        print("FATAL: no API_SERVER_KEY found")
        sys.exit(1)

    print(f"== Hermes API end-to-end test against {base} ==")

    # 1. Health
    code, data = req(base, "/health", key)
    print(f"[1] /health                  -> {code} {data.get('status') if isinstance(data, dict) else data}")
    assert code == 200 and data.get("status") == "ok"

    # 2. Sessions list
    code, data = req(base, "/api/sessions?limit=3", key)
    sessions = data.get("data", []) if isinstance(data, dict) else []
    print(f"[2] /api/sessions            -> {code} got {len(sessions)} sessions (has_more={data.get('has_more') if isinstance(data, dict) else '?'})")
    assert code == 200

    # 3. Model catalog
    code, data = req(base, "/api/model/options", key)
    provs = data.get("providers", []) if isinstance(data, dict) else []
    total_models = sum(len(p.get("models") or []) for p in provs)
    print(f"[3] /api/model/options       -> {code} {len(provs)} providers, {total_models} models")
    assert code == 200 and len(provs) > 0

    # 4. Create a scratch session
    code, data = req(base, "/api/sessions", key, method="POST",
                     body={"title": "API e2e test", "source": "e2e_test"})
    sid = data.get("session", {}).get("id", "") if isinstance(data, dict) else ""
    print(f"[4] POST /api/sessions       -> {code} session={sid}")
    assert code in (200, 201) and sid

    try:
        # 5. Lock model on session
        code, data = req(base, f"/api/sessions/{sid}/model", key, method="POST",
                         body={"model": "deepseek/deepseek-v4-flash", "require_model_lock": True})
        print(f"[5] POST .../model         -> {code} {data.get('runtime', {}).get('model') if isinstance(data, dict) else data}")
        assert code == 200

        # 6. Stream chat (wait up to ~40s for deltas)
        code, raw = req(base, f"/api/sessions/{sid}/chat/stream", key, method="POST",
                        body={"message": "Reply with exactly: PONG"}, timeout=45, stream=True)
        has_delta = "assistant.delta" in raw
        has_completed = "run.completed" in raw
        print(f"[6] POST .../chat/stream    -> {code} delta_events={has_delta} run_completed={has_completed}")
        assert code == 200 and has_delta
    finally:
        # cleanup
        try:
            req(base, f"/api/sessions/{sid}", key, method="DELETE")
            print("    (scratch session cleaned up)")
        except Exception:
            pass

    print("\nALL CHECKS PASSED - app should connect and stream fine.")

if __name__ == "__main__":
    main()

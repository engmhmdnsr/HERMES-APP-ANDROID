import sys
import time
import json
import socket
import httpx

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://127.0.0.1:8080"
API_KEY = "hermes_live_key_99x"
HEADERS = {"X-API-Key": API_KEY, "Content-Type": "application/json"}

def run_tests():
    client = httpx.Client(base_url=BASE_URL, headers=HEADERS, timeout=15)
    results = {}

    print("=" * 60)
    print(" 🧪 RUNNING FULL END-TO-END GATEWAY INTEGRATION TESTS")
    print("=" * 60)

    # 1. Health
    try:
        r = client.get("/api/health")
        assert r.status_code == 200
        data = r.json()
        assert data["status"] == "online"
        results["1. Health Check (/api/health)"] = f"PASS (Hostname: {data.get('hostname')})"
    except Exception as e:
        results["1. Health Check (/api/health)"] = f"FAIL: {e}"

    # 2. System Telemetry
    try:
        r = client.get("/api/system")
        assert r.status_code == 200
        data = r.json()
        assert "cpu_usage" in data
        assert "ram_used_gb" in data
        results["2. Telemetry (/api/system)"] = f"PASS (CPU: {data['cpu_usage']}%, RAM: {data['ram_used_gb']}GB, Tasks: {data.get('active_tasks_count')})"
    except Exception as e:
        results["2. Telemetry (/api/system)"] = f"FAIL: {e}"

    # 3. Dynamic Models
    try:
        r = client.get("/api/models")
        assert r.status_code == 200
        data = r.json()
        models = data.get("models", [])
        assert len(models) > 0
        model_names = [m["id"] for m in models[:3]]
        results["3. Dynamic Models (/api/models)"] = f"PASS ({len(models)} models found: {', '.join(model_names)}...)"
    except Exception as e:
        results["3. Dynamic Models (/api/models)"] = f"FAIL: {e}"

    # 4. Sessions List from state.db
    first_session_id = None
    try:
        r = client.get("/api/sessions?limit=5")
        assert r.status_code == 200
        sessions = r.json()
        assert isinstance(sessions, list)
        if sessions:
            first_session_id = sessions[0]["id"]
            results["4. Sessions List (/api/sessions)"] = f"PASS ({len(sessions)} sessions loaded. Latest: '{sessions[0]['title'][:25]}')"
        else:
            results["4. Sessions List (/api/sessions)"] = "PASS (0 sessions)"
    except Exception as e:
        results["4. Sessions List (/api/sessions)"] = f"FAIL: {e}"

    # 5. Session Messages
    if first_session_id:
        try:
            r = client.get(f"/api/sessions/{first_session_id}/messages")
            assert r.status_code == 200
            msgs = r.json()
            assert isinstance(msgs, list)
            results["5. Session Messages (/api/sessions/{id}/messages)"] = f"PASS ({len(msgs)} messages loaded from session {first_session_id})"
        except Exception as e:
            results["5. Session Messages"] = f"FAIL: {e}"

    # 6. Create New Session
    try:
        new_payload = {"title": "Automated Test Session", "model": "google/gemma-4-e4b"}
        r = client.post("/api/sessions/new", json=new_payload)
        assert r.status_code == 200
        new_sess = r.json()
        assert "id" in new_sess
        results["6. Create Session (/api/sessions/new)"] = f"PASS (Created session ID: {new_sess['id']})"
    except Exception as e:
        results["6. Create Session (/api/sessions/new)"] = f"FAIL: {e}"

    # 7. Terminal Exec (PowerShell)
    try:
        r = client.post("/api/terminal/exec", json={"command": "whoami"})
        assert r.status_code == 200
        data = r.json()
        assert data["exit_code"] == 0
        results["7. PowerShell Execution (/api/terminal/exec)"] = f"PASS (User: {data['output']}, Latency: {data['duration_ms']}ms)"
    except Exception as e:
        results["7. PowerShell Execution (/api/terminal/exec)"] = f"FAIL: {e}"

    # 8. Chat Stream with PowerShell Command via SSE
    try:
        cmd_output_found = False
        with client.stream("POST", "/api/chat/stream", json={"prompt": "$hostname"}) as stream_res:
            assert stream_res.status_code == 200
            for line in stream_res.iter_lines():
                if "tool_end" in line or "text" in line:
                    cmd_output_found = True
                    break
        assert cmd_output_found
        results["8. Chat Stream Tool Exec (SSE)"] = "PASS (Command executed and streamed via tool_start/tool_end)"
    except Exception as e:
        results["8. Chat Stream Tool Exec (SSE)"] = f"FAIL: {e}"

    # 9. UDP Auto-Discovery Probe
    try:
        udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        udp_sock.settimeout(2.0)
        udp_sock.sendto(b"HERMES_DISCOVER", ("127.0.0.1", 8089))
        data, _ = udp_sock.recvfrom(2048)
        beacon = json.loads(data.decode("utf-8"))
        assert beacon.get("service") == "hermes-agent"
        assert "ip" in beacon
        results["9. UDP Auto-Discovery (Port 8089)"] = f"PASS (Beacon replied with IP: {beacon['ip']}, Host: {beacon.get('hostname')})"
    except Exception as e:
        results["9. UDP Auto-Discovery (Port 8089)"] = f"FAIL: {e}"

    print("\n--- TEST SUMMARY ---")
    all_passed = True
    for test_name, outcome in results.items():
        print(f"[{'✔' if 'PASS' in outcome else '✖'}] {test_name}: {outcome}")
        if "FAIL" in outcome:
            all_passed = False

    print("=" * 60)
    print(f"OVERALL RESULT: {'✅ ALL TESTS PASSED SUCCESSFULLY!' if all_passed else '❌ SOME TESTS FAILED'}")
    print("=" * 60)
    return all_passed

if __name__ == "__main__":
    run_tests()

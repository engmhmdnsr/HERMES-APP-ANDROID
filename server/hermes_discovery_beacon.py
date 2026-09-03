"""
Hermes Control - UDP Discovery Beacon (standalone)

Broadcasts a small JSON payload over UDP so the Hermes Control Android app
can auto-discover this PC on the same Wi-Fi network (like the old custom
gateway did on port 8089) -- without needing the legacy FastAPI shim.

The beacon advertises the OFFICIAL Hermes API server (API_SERVER_PORT from
.env, default 8080). Run this alongside the gateway:

    python beacon.py

Recommended: add to startup (Startup folder .vbs or Task Scheduler) so
discovery works whenever the PC is on.

Payload: {"service":"hermes-agent","hostname":...,"ip":...,"tailscale_ip":...,"port":8080,"apiKey":""}
"""
import json
import os
import socket
import threading
import time

DISCOVERY_PORT = 8089
BROADCAST_INTERVAL = 3.0  # seconds

def load_env(path):
    env = {}
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if "=" in line and not line.startswith("#"):
                    k, v = line.split("=", 1)
                    env[k.strip()] = v.strip()
    return env

def get_network_ips():
    """Return (lan_ip, tailscale_ip)."""
    lan_ip = "127.0.0.1"
    tailscale_ip = None
    try:
        import psutil
        for iface_name, nic_addrs in psutil.net_if_addrs().items():
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

def main():
    env = load_env(os.path.expandvars(r"%LOCALAPPDATA%\hermes\.env"))
    api_port = int(env.get("API_SERVER_PORT", "8080"))

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("", DISCOVERY_PORT))
        sock.settimeout(1.0)
    except Exception as e:
        print(f"[beacon] Failed to bind UDP {DISCOVERY_PORT}: {e}")
        return

    print(f"[beacon] Hermes discovery beacon running on UDP {DISCOVERY_PORT} (advertising API port {api_port})")
    print("[beacon] Press Ctrl+C to stop.")

    while True:
        lan, ts = get_network_ips()
        payload = json.dumps({
            "service": "hermes-agent",
            "hostname": os.environ.get("COMPUTERNAME", "WIN11-HERMES"),
            "ip": lan,
            "tailscale_ip": ts or "",
            "port": api_port,
            "apiKey": "",
        }).encode("utf-8")

        # Periodic broadcast
        try:
            sock.sendto(payload, ("255.255.255.255", DISCOVERY_PORT))
        except Exception:
            pass

        # Respond to discovery probes from the app
        deadline = time.time() + BROADCAST_INTERVAL
        while time.time() < deadline:
            try:
                data, addr = sock.recvfrom(1024)
                msg = data.decode("utf-8", errors="ignore")
                if "HERMES_DISCOVER" in msg:
                    try:
                        sock.sendto(payload, addr)
                    except Exception:
                        pass
            except socket.timeout:
                break
            except Exception:
                break

if __name__ == "__main__":
    main()

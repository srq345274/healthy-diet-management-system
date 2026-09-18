#!/usr/bin/env bash
set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/srq345274/healthy-diet-management-system.git}"
PROJECT_DIR="${PROJECT_DIR:-$PWD}"
AGENT_DIR="${AGENT_DIR:-$HOME/.cloudstudio-agent}"
STATE_DIR="$AGENT_DIR/state"
BRIDGE_PORT="${BRIDGE_PORT:-18765}"
SSH_PORT="${SSH_PORT:-22222}"
PUBLIC_KEY="${CLOUDSTUDIO_AGENT_PUBLIC_KEY:-}"

if [ -z "$PUBLIC_KEY" ]; then
  echo "Set CLOUDSTUDIO_AGENT_PUBLIC_KEY to the local agent public key." >&2
  exit 2
fi

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 3
  }
}

need_cmd git
need_cmd ssh-keygen

PYTHON_BIN="${PYTHON_BIN:-}"
if [ -z "$PYTHON_BIN" ]; then
  if command -v python3 >/dev/null 2>&1; then
    PYTHON_BIN="$(command -v python3)"
  elif command -v python >/dev/null 2>&1; then
    PYTHON_BIN="$(command -v python)"
  else
    echo "Missing required command: python3" >&2
    exit 3
  fi
fi

find_sshd() {
  if command -v sshd >/dev/null 2>&1; then
    command -v sshd
  elif [ -x /usr/sbin/sshd ]; then
    printf '%s\n' /usr/sbin/sshd
  elif [ -x /usr/local/sbin/sshd ]; then
    printf '%s\n' /usr/local/sbin/sshd
  fi
}

install_sshd() {
  if command -v apt-get >/dev/null 2>&1; then
    if [ "$(id -u)" -eq 0 ]; then
      apt-get update
      apt-get install -y openssh-server openssh-client
    elif command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
      sudo apt-get update
      sudo apt-get install -y openssh-server openssh-client
    else
      return 1
    fi
  elif command -v apk >/dev/null 2>&1; then
    if [ "$(id -u)" -eq 0 ]; then
      apk add --no-cache openssh
    elif command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
      sudo apk add --no-cache openssh
    else
      return 1
    fi
  elif command -v dnf >/dev/null 2>&1; then
    if [ "$(id -u)" -eq 0 ]; then
      dnf install -y openssh-server openssh-clients
    elif command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
      sudo dnf install -y openssh-server openssh-clients
    else
      return 1
    fi
  elif command -v yum >/dev/null 2>&1; then
    if [ "$(id -u)" -eq 0 ]; then
      yum install -y openssh-server openssh-clients
    elif command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
      sudo yum install -y openssh-server openssh-clients
    else
      return 1
    fi
  else
    return 1
  fi
}

SSHD_BIN="$(find_sshd || true)"
if [ -z "$SSHD_BIN" ] && [ "${INSTALL_OPENSSH_SERVER:-0}" = "1" ]; then
  install_sshd || true
  SSHD_BIN="$(find_sshd || true)"
fi

if [ -z "$SSHD_BIN" ]; then
  echo "OpenSSH server (sshd) is not available in this Cloud Studio workspace." >&2
  echo "Current user: $(id -un 2>/dev/null || printf unknown); uid: $(id -u 2>/dev/null || printf unknown)" >&2
  echo "Package managers: apt-get=$(command -v apt-get || printf no), apk=$(command -v apk || printf no), dnf=$(command -v dnf || printf no), yum=$(command -v yum || printf no)" >&2
  echo "If you have permission to install packages, retry from /workspace with:" >&2
  echo "  export INSTALL_OPENSSH_SERVER=1" >&2
  echo "  curl -fsSL https://raw.githubusercontent.com/srq345274/healthy-diet-management-system/main/scripts/cloudstudio-bootstrap-agent.sh | bash" >&2
  echo "If installation is not permitted, ask Cloud Studio to use a workspace image with openssh-server enabled." >&2
  exit 3
fi

mkdir -p "$AGENT_DIR" "$STATE_DIR"
chmod 700 "$STATE_DIR"

normalize_repo_url() {
  printf '%s' "$1" | sed -E 's#/$##; s#\.git$##'
}

if [ -d "$PROJECT_DIR/.git" ]; then
  CURRENT_ORIGIN="$(git -C "$PROJECT_DIR" remote get-url origin 2>/dev/null || true)"
  if [ -z "$CURRENT_ORIGIN" ]; then
    echo "PROJECT_DIR has no origin remote: $PROJECT_DIR" >&2
    echo "Set PROJECT_DIR to the uploaded healthy-diet-management-system checkout." >&2
    exit 4
  fi
  if [ "$(normalize_repo_url "$CURRENT_ORIGIN")" != "$(normalize_repo_url "$REPO_URL")" ]; then
    echo "PROJECT_DIR points to a different repository." >&2
    echo "current_origin=$CURRENT_ORIGIN" >&2
    echo "expected_origin=$REPO_URL" >&2
    echo "Use PROJECT_DIR=/workspace/healthy-diet-management-system and retry." >&2
    exit 5
  fi
  git -C "$PROJECT_DIR" fetch origin main
  git -C "$PROJECT_DIR" checkout main
  git -C "$PROJECT_DIR" pull --ff-only origin main
elif [ ! -e "$PROJECT_DIR" ] || [ -z "$(find "$PROJECT_DIR" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]; then
  git clone "$REPO_URL" "$PROJECT_DIR"
else
  echo "PROJECT_DIR is not a Git checkout and is not empty: $PROJECT_DIR" >&2
  echo "Set PROJECT_DIR to the uploaded project directory or clone the repository there first." >&2
  exit 4
fi

cat > "$AGENT_DIR/bridge.py" <<'PY'
#!/usr/bin/env python3
"""Carry binary SSH traffic through a Cloud Studio WebSocket preview endpoint."""
import argparse
import asyncio
from aiohttp import web


def make_app(ssh_port):
    async def bridge(request):
        try:
            reader, writer = await asyncio.open_connection('127.0.0.1', ssh_port)
        except OSError:
            raise web.HTTPServiceUnavailable(text='SSH listener unavailable')
        ws = web.WebSocketResponse(max_msg_size=1024 * 1024, heartbeat=30)
        tasks = []
        try:
            await ws.prepare(request)

            async def upload():
                async for msg in ws:
                    if msg.type != web.WSMsgType.BINARY:
                        break
                    writer.write(msg.data)
                    await writer.drain()

            async def download():
                while data := await reader.read(65536):
                    await ws.send_bytes(data)

            tasks = [asyncio.create_task(upload()), asyncio.create_task(download())]
            await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
        finally:
            for task in tasks:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)
            writer.close()
            await writer.wait_closed()
            await ws.close()
        return ws

    app = web.Application()
    app.router.add_get('/ssh', bridge)
    return app


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--port', type=int, default=18765)
    p.add_argument('--ssh-port', type=int, default=22222)
    args = p.parse_args()
    web.run_app(make_app(args.ssh_port), host='0.0.0.0', port=args.port, access_log=None)
PY
chmod 700 "$AGENT_DIR/bridge.py"

printf '%s\n' "$PUBLIC_KEY" > "$STATE_DIR/authorized_keys"
chmod 600 "$STATE_DIR/authorized_keys"

if [ ! -f "$STATE_DIR/ssh_host_ed25519_key" ]; then
  ssh-keygen -t ed25519 -f "$STATE_DIR/ssh_host_ed25519_key" -N "" -C "cloudstudio-agent-host" >/dev/null
fi
chmod 600 "$STATE_DIR/ssh_host_ed25519_key"
chmod 644 "$STATE_DIR/ssh_host_ed25519_key.pub"

USER_NAME="$(id -un)"
cat > "$STATE_DIR/sshd_config" <<EOF
Port $SSH_PORT
ListenAddress 127.0.0.1
HostKey $STATE_DIR/ssh_host_ed25519_key
PidFile $STATE_DIR/sshd.pid
AuthorizedKeysFile $STATE_DIR/authorized_keys
AllowUsers $USER_NAME
PubkeyAuthentication yes
AuthenticationMethods publickey
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitEmptyPasswords no
PermitRootLogin prohibit-password
UsePAM no
AllowAgentForwarding no
AllowTcpForwarding no
X11Forwarding no
PermitTunnel no
Subsystem sftp internal-sftp
EOF
chmod 600 "$STATE_DIR/sshd_config"

port_listening() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "(^|:)$port$"
  elif command -v netstat >/dev/null 2>&1; then
    netstat -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "(^|:)$port$"
  else
    return 1
  fi
}

if [ "$(id -u)" -eq 0 ]; then
  mkdir -p /run/sshd
  chmod 755 /run/sshd
fi

"$SSHD_BIN" -t -f "$STATE_DIR/sshd_config"
if ! port_listening "$SSH_PORT"; then
  "$SSHD_BIN" -f "$STATE_DIR/sshd_config" -E "$STATE_DIR/sshd.log"
fi

if [ ! -d "$AGENT_DIR/venv" ]; then
  "$PYTHON_BIN" -m venv "$AGENT_DIR/venv"
fi
"$AGENT_DIR/venv/bin/python" -m pip install --quiet aiohttp==3.13.3

if ! port_listening "$BRIDGE_PORT"; then
  nohup "$AGENT_DIR/venv/bin/python" "$AGENT_DIR/bridge.py" --port "$BRIDGE_PORT" --ssh-port "$SSH_PORT" > "$STATE_DIR/bridge.log" 2>&1 &
  echo $! > "$STATE_DIR/bridge.pid"
  sleep 1
fi

cat <<EOF
=== CLOUDSTUDIO_AGENT_ACCESS_READY ===
workspace_user=$USER_NAME
workspace_hostname=$(hostname)
project_dir=$PROJECT_DIR
ssh_port=$SSH_PORT
bridge_port=$BRIDGE_PORT
host_key_pub=$(cat "$STATE_DIR/ssh_host_ed25519_key.pub")
host_key_fingerprint=$(ssh-keygen -lf "$STATE_DIR/ssh_host_ed25519_key.pub")
sshd_log=$STATE_DIR/sshd.log
bridge_log=$STATE_DIR/bridge.log

Next: in Cloud Studio, open the web preview for port $BRIDGE_PORT.
Send Codex the preview URL. If the URL is https://HOST, the SSH WebSocket URL will be wss://HOST/ssh.
EOF

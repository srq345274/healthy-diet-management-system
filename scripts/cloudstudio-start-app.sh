#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/.cloudstudio"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${PORT:-5173}"
BACKEND_HEALTH="http://127.0.0.1:${BACKEND_PORT}/api/health"
BACKEND_LOG="$STATE_DIR/backend.log"

mkdir -p "$STATE_DIR"

if curl -fsS "$BACKEND_HEALTH" >/dev/null 2>&1; then
  echo "Backend is already healthy on port ${BACKEND_PORT}."
else
  echo "Starting backend on port ${BACKEND_PORT}..."
  (
    cd "$ROOT_DIR/healthy-diet-backend"
    SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-cloud}" SERVER_PORT="$BACKEND_PORT" \
      mvn -DskipTests spring-boot:run > "$BACKEND_LOG" 2>&1
  ) &
  echo $! > "$STATE_DIR/backend.pid"

  for _ in $(seq 1 90); do
    if curl -fsS "$BACKEND_HEALTH" >/dev/null 2>&1; then
      echo "Backend is healthy."
      break
    fi
    sleep 2
  done

  if ! curl -fsS "$BACKEND_HEALTH" >/dev/null 2>&1; then
    echo "Backend did not become healthy. Last backend log lines:" >&2
    tail -n 120 "$BACKEND_LOG" >&2 || true
    exit 1
  fi
fi

cd "$ROOT_DIR/healthy-diet-app"

if [ ! -x node_modules/.bin/vue-cli-service ]; then
  echo "Installing frontend dependencies..."
  npm install --no-audit --no-fund
fi

export PORT="$FRONTEND_PORT"
export HOST="${HOST:-0.0.0.0}"

echo "Starting frontend on port ${FRONTEND_PORT}..."
npm run dev:h5
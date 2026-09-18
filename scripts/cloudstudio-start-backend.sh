#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_PORT="${SERVER_PORT:-8080}"
BACKEND_HEALTH="http://127.0.0.1:${BACKEND_PORT}/api/health"

if curl -fsS "$BACKEND_HEALTH" >/dev/null 2>&1; then
  echo "Backend is already healthy on port ${BACKEND_PORT}."
  echo "Keeping this preview command alive; stop the existing backend process to restart it."
  tail -f /dev/null
fi

cd "$ROOT_DIR/healthy-diet-backend"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-cloud}"
export SERVER_PORT="$BACKEND_PORT"

mvn -DskipTests spring-boot:run
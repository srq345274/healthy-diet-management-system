#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/healthy-diet-backend"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-cloud}"
export SERVER_PORT="${SERVER_PORT:-8080}"

mvn -DskipTests spring-boot:run

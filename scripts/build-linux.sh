#!/usr/bin/env bash
set -Eeuo pipefail
REPO_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd -- "$REPO_DIR/frontend"
if [[ ! -d node_modules ]]; then npm ci; fi
npm run build:linux
cd -- "$REPO_DIR/backend"
bash ./gradlew test bootJar
python3 "$REPO_DIR/scripts/package-linux.py"

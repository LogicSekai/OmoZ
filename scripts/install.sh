#!/usr/bin/env bash
# scripts/install.sh [ip:port]   build + install the debug APK over USB or ADB Wi-Fi, then launch (Linux & macOS)
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh
[ -n "${1:-}" ] && "$ADB" connect "$1"
if ! "$ADB" devices | awk 'NR>1 && $2=="device"' | grep -q .; then
  echo "[OmoZ] No device. Enable USB debugging, plug in, accept the RSA prompt."; exit 1
fi
./gradlew :app:installDebug --console=plain
"$ADB" shell am start -n com.logicsekai.omoz.debug/com.logicsekai.omoz.app.ui.MainActivity >/dev/null
echo "[OmoZ] Installed and launched."

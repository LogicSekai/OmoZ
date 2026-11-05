#!/usr/bin/env bash
# scripts/build.sh [release]   build the APK into dist/ (Linux & macOS)
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh
VARIANT=Debug; [ "${1:-}" = "release" ] && VARIANT=Release
VERSION=$(grep '^omo.versionName=' gradle.properties | cut -d= -f2)
./gradlew :engine-core:test ":app:assemble$VARIANT" --console=plain
mkdir -p dist
if [ "$VARIANT" = Debug ]; then
  cp app/build/outputs/apk/debug/app-debug.apk "dist/OmoZ-$VERSION-debug.apk"
  echo "[OmoZ] Done: dist/OmoZ-$VERSION-debug.apk"
else
  cp app/build/outputs/apk/release/app-release.apk "dist/OmoZ-$VERSION-release.apk"
  echo "[OmoZ] Done: dist/OmoZ-$VERSION-release.apk"
fi

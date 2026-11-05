#!/usr/bin/env bash
# Shared environment detection for scripts/build.sh and scripts/install.sh (Linux & macOS)
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -z "${JAVA_HOME:-}" ]; then
  for c in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" "$HOME/android-studio/jbr" "/opt/android-studio/jbr"; do
    [ -x "$c/bin/java" ] && export JAVA_HOME="$c" && break
  done
fi
JAVA_EXE="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if ! "$JAVA_EXE" -version >/dev/null 2>&1; then
  echo "[OmoZ] JDK 17+ not found. Install it and set JAVA_HOME."; exit 1
fi
JMAJOR=$("$JAVA_EXE" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [ "$JMAJOR" -lt 17 ]; then echo "[OmoZ] Java $JMAJOR is too old, need 17+."; exit 1; fi
[ "$JMAJOR" -gt 24 ] && echo "[OmoZ] Warning: Java $JMAJOR is newer than Gradle 8.14 supports; use 17 or 21 if the build fails."
echo "[OmoZ] Java $JMAJOR"

SDK_DIR=""
[ -f "$ROOT/local.properties" ] && SDK_DIR=$(grep '^sdk.dir=' "$ROOT/local.properties" | cut -d= -f2-)
SDK_DIR="${SDK_DIR:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}"
for c in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
  [ -z "$SDK_DIR" ] && [ -d "$c" ] && SDK_DIR="$c"
done
if [ -z "$SDK_DIR" ]; then echo "[OmoZ] Android SDK not found. Set ANDROID_HOME or create local.properties (sdk.dir=...)."; exit 1; fi
[ -f "$ROOT/local.properties" ] || echo "sdk.dir=$SDK_DIR" > "$ROOT/local.properties"
echo "[OmoZ] Android SDK: $SDK_DIR"
ADB="$SDK_DIR/platform-tools/adb"; [ -x "$ADB" ] || ADB="adb"

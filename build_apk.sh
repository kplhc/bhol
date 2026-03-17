#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f "./gradlew" ]; then
  if command -v gradle >/dev/null 2>&1; then
    echo "[+] 未找到 gradlew，正在生成 wrapper..."
    gradle wrapper
  else
    echo "[x] 需要先安装 Gradle（或先运行一次 Android Studio 生成 gradlew）。"
    exit 1
  fi
fi

chmod +x ./gradlew
./gradlew assembleDebug

echo "[+] APK 生成完成: app/build/outputs/apk/debug/app-debug.apk"

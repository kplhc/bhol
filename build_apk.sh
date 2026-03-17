#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

required_gradle_major=8
required_gradle_minor=7

java_major() {
  java -version 2>&1 | awk -F '[".]' '/version/ {print $2}'
}

gradle_version() {
  "$1" --version 2>/dev/null | awk '/Gradle / {print $2; exit}'
}

check_java() {
  local major
  major="$(java_major)"
  if [ -z "$major" ]; then
    echo "[x] 未检测到可用 JDK。请安装 JDK 17 或 21。"
    exit 1
  fi
  if [ "$major" -lt 17 ] || [ "$major" -gt 21 ]; then
    echo "[x] 当前 JDK 版本为 ${major}，Android 构建建议使用 JDK 17~21。"
    echo "    请切换 JAVA_HOME 后重试，例如：export JAVA_HOME=/path/to/jdk17"
    exit 1
  fi
}

version_ge() {
  local ver="$1"
  local major="${ver%%.*}"
  local minor="${ver#*.}"
  minor="${minor%%.*}"
  [ "$major" -gt "$required_gradle_major" ] || { [ "$major" -eq "$required_gradle_major" ] && [ "$minor" -ge "$required_gradle_minor" ]; }
}

run_build() {
  local cmd="$1"
  local gv
  gv="$(gradle_version "$cmd")"
  if [ -z "$gv" ]; then
    echo "[x] 无法识别 Gradle 版本。"
    exit 1
  fi
  if ! version_ge "$gv"; then
    echo "[x] 当前 Gradle 为 ${gv}，需要 >= ${required_gradle_major}.${required_gradle_minor}。"
    echo "    解决方式："
    echo "    1) 使用 Gradle Wrapper（推荐）"
    echo "    2) 或升级系统 Gradle 后重试"
    exit 1
  fi
  "$cmd" --version
  "$cmd" assembleDebug
}

check_java

if [ -f "./gradlew" ]; then
  chmod +x ./gradlew
  run_build ./gradlew
elif command -v gradle >/dev/null 2>&1; then
  echo "[+] 未找到 gradlew，使用系统 gradle 构建"
  run_build gradle
else
  echo "[x] 未检测到 gradle 或 gradlew，无法构建 APK"
  exit 1
fi

echo "[+] APK 生成完成: app/build/outputs/apk/debug/app-debug.apk"

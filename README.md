# 匿名云盘 Android App（bhol）

这个项目把你原先 Python 版上传服务改成了 Android App 方案：

- 主界面中间有一个圆形按钮（约占屏幕面积 1/8）
- 点击按钮后：
  - 本地上传服务启动（`http://127.0.0.1:8080/upload`）
  - 自动启动 `cloudflared tunnel --url http://127.0.0.1:8080`
  - 页面显示新的外网临时地址（`trycloudflare.com`），并支持一键复制
- 再次点击按钮后：
  - 同时关闭本地上传服务与 cloudflared 进程
- 首次使用必须在“设置”页面先选本地存储目录（用 SAF 目录授权）

## 目录说明

- `app/src/main/java/com/example/bhol/MainActivity.kt`：主界面、启动/关闭逻辑
- `app/src/main/java/com/example/bhol/SettingsActivity.kt`：选择存储目录
- `app/src/main/java/com/example/bhol/CloudDiskServer.kt`：内置 HTTP 上传服务（NanoHTTPD）
- `app/src/main/java/com/example/bhol/CloudflaredManager.kt`：下载并启动 cloudflared
- `app/src/main/java/com/example/bhol/UploadRepository.kt`：目录配置与文件保存
- `build_apk.sh`：一键构建 APK 脚本

## 一键生成 APK

```bash
./build_apk.sh
# 如无 gradlew，需要本机已安装 gradle >= 8.7，且 JDK 17~21
```

构建产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 上传示例

当 App 显示外网地址后，可在任意设备执行：

```bash
curl -F "file=@/path/to/file" https://xxxx.trycloudflare.com/upload
```

## 注意事项

1. `trycloudflare` 地址每次启动都会变，这是 Cloudflare 临时隧道机制。
2. 本项目下载的是 `cloudflared-linux-arm64`，目标是常见 ARM64 Android 设备。
3. 如果设备架构不是 ARM64，需要在 `CloudflaredManager.kt` 里改下载链接。

4. 构建 Android APK 需要 JDK 17~21 和 Gradle >= 8.7。

package com.example.bhol

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class CloudflaredManager(private val context: Context) {

    private var process: Process? = null

    suspend fun start(localPort: Int, onUrl: (String) -> Unit) = withContext(Dispatchers.IO) {
        if (process?.isAlive == true) return@withContext
        val binary = ensureBinary()
        val pb = ProcessBuilder(
            binary.absolutePath,
            "tunnel",
            "--url",
            "http://127.0.0.1:$localPort",
            "--no-autoupdate"
        )
        pb.redirectErrorStream(true)
        process = pb.start()

        process?.inputStream?.bufferedReader()?.useLines { lines ->
            lines.forEach { line ->
                val url = Regex("https://[a-zA-Z0-9\\-]+\\.trycloudflare\\.com").find(line)?.value
                if (url != null) {
                    onUrl(url)
                    return@forEach
                }
            }
        }
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    private fun ensureBinary(): File {
        val target = File(context.filesDir, "cloudflared")
        if (target.exists()) {
            target.setExecutable(true)
            return target
        }
        val source = URL("https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64")
        source.openStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        target.setExecutable(true)
        return target
    }
}

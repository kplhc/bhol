package com.example.bhol

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var toggleButton: MaterialButton
    private lateinit var urlText: TextView
    private lateinit var statusText: TextView

    private var server: CloudDiskServer? = null
    private lateinit var cloudflaredManager: CloudflaredManager
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cloudflaredManager = CloudflaredManager(this)
        toggleButton = findViewById(R.id.toggleButton)
        urlText = findViewById(R.id.urlText)
        statusText = findViewById(R.id.statusText)

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.copyButton).setOnClickListener {
            val text = urlText.text.toString().removePrefix("外网地址：")
            if (text.isNotBlank() && text != "-") {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("cloud_url", text))
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
            }
        }

        resizeToggleButton()
        toggleButton.setOnClickListener {
            if (running) stopService() else startService()
        }
    }

    private fun resizeToggleButton() {
        val metrics = resources.displayMetrics
        val screenArea = metrics.widthPixels.toDouble() * metrics.heightPixels.toDouble()
        val diameter = sqrt(screenArea / (2 * Math.PI)).toInt()
        val lp = toggleButton.layoutParams
        lp.width = diameter
        lp.height = diameter
        toggleButton.layoutParams = lp as ViewGroup.LayoutParams
    }

    private fun startService() {
        if (UploadRepository.getTreeUri(this) == null) {
            Toast.makeText(this, "请先在设置里选择存储目录", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        try {
            val localServer = CloudDiskServer(this)
            localServer.start()
            server = localServer
        } catch (ex: Exception) {
            Toast.makeText(this, "本地服务启动失败：${ex.message}", Toast.LENGTH_LONG).show()
            return
        }

        running = true
        toggleButton.text = "关闭"
        toggleButton.setBackgroundColor(Color.parseColor("#E53935"))
        statusText.text = "状态：运行中"
        urlText.text = "外网地址：启动中..."

        lifecycleScope.launch {
            try {
                cloudflaredManager.start(8080) { url ->
                    runOnUiThread {
                        server?.publicUrl = url
                        urlText.text = "外网地址：$url"
                    }
                }
            } catch (ex: Exception) {
                runOnUiThread {
                    stopService()
                    Toast.makeText(
                        this@MainActivity,
                        "隧道启动失败：${ex.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun stopService() {
        cloudflaredManager.stop()
        server?.stop()
        server = null
        running = false
        toggleButton.text = "启动"
        toggleButton.setBackgroundColor(Color.parseColor("#43A047"))
        statusText.text = "状态：未启动"
        urlText.text = "外网地址：-"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }
}

package com.example.bhol

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    private lateinit var pathText: TextView

    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        persistUriPermission(uri)
        UploadRepository.saveTreeUri(this, uri)
        renderPath(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        pathText = findViewById(R.id.pathText)
        UploadRepository.getTreeUri(this)?.let(::renderPath)

        findViewById<MaterialButton>(R.id.selectFolderButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            pickFolderLauncher.launch(intent)
        }
    }

    private fun persistUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    private fun renderPath(uri: Uri) {
        pathText.text = "当前目录：$uri"
    }
}

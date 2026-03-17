package com.example.bhol

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

object UploadRepository {
    private const val PREFS = "bhol_prefs"
    private const val KEY_TREE_URI = "tree_uri"

    fun saveTreeUri(context: Context, treeUri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TREE_URI, treeUri.toString())
            .apply()
    }

    fun getTreeUri(context: Context): Uri? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)
        return value?.let(Uri::parse)
    }

    fun requireTreeUri(context: Context): Uri = getTreeUri(context)
        ?: throw IllegalStateException("请先在设置中选择文件夹")

    fun persistToFolder(context: Context, tmpFile: File, originalName: String?): String {
        val resolver = context.contentResolver
        val treeUri = requireTreeUri(context)
        val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val parent = DocumentFile.fromTreeUri(context, treeDocumentUri)
            ?: throw IllegalStateException("无法访问目标目录")

        val safeName = if (originalName.isNullOrBlank()) "upload.bin" else originalName
        val targetName = resolveUniqueName(parent, safeName)
        val ext = targetName.substringAfterLast('.', "")
        val mime = if (ext.isBlank()) "application/octet-stream" else "application/$ext"

        val target = parent.createFile(mime, targetName)
            ?: throw IllegalStateException("创建文件失败")

        resolver.openOutputStream(target.uri)?.use { output ->
            tmpFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("写入失败")

        return targetName
    }

    private fun resolveUniqueName(parent: DocumentFile, sourceName: String): String {
        var candidate = sourceName
        var index = 1
        while (parent.findFile(candidate) != null) {
            val dot = sourceName.lastIndexOf('.')
            candidate = if (dot > 0) {
                val name = sourceName.substring(0, dot)
                val ext = sourceName.substring(dot)
                "${name}_$index$ext"
            } else {
                "${sourceName}_$index"
            }
            index++
        }
        return candidate
    }
}

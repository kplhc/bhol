package com.example.bhol

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File

class CloudDiskServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {

    @Volatile
    var publicUrl: String? = null

    override fun serve(session: IHTTPSession): Response {
        return when (session.method) {
            Method.GET -> {
                val text = buildString {
                    appendLine("匿名云盘上传服务已启动")
                    appendLine("上传命令：")
                    appendLine("curl -F \"file=@/path/to/file\" http://127.0.0.1:$listeningPort/upload")
                    appendLine("当前外网地址：${publicUrl ?: "等待 cloudflared..."}")
                }
                newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", text)
            }

            Method.POST -> {
                if (session.uri != "/upload") {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Use /upload")
                }
                try {
                    val files = HashMap<String, String>()
                    session.parseBody(files)
                    val tempPath = files["file"]
                        ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "未找到 file 字段")
                    val tempFile = File(tempPath)
                    val originalName = session.parameters["file"]?.firstOrNull()
                    val finalName = UploadRepository.persistToFolder(context, tempFile, originalName)
                    tempFile.delete()
                    newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", "上传成功：$finalName")
                } catch (ex: Exception) {
                    newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        "text/plain; charset=utf-8",
                        "上传失败：${ex.message}"
                    )
                }
            }

            else -> newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Only GET/POST")
        }
    }
}

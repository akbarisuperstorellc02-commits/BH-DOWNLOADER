package com.bh.turbodownloader.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class DownloadUiItem(
    val id: String,
    val url: String,
    val fileName: String,
    val downloaded: Long = 0L,
    val total: Long = -1L,
    val status: String = "Queued",
    val error: String? = null
) {
    val progress: Float?
        get() = if (total > 0L) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
}

/** HTTP engine to UI bridge. Current output is app-specific; SAF and durable history are next. */
class DownloadCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _items = MutableStateFlow<List<DownloadUiItem>>(emptyList())
    val items: StateFlow<List<DownloadUiItem>> = _items.asStateFlow()

    fun start(rawUrl: String) {
        val url = rawUrl.trim()
        require(url.startsWith("https://", true) || url.startsWith("http://", true)) {
            "Enter a valid HTTP or HTTPS URL"
        }
        val id = UUID.randomUUID().toString()
        val name = safeFileName(Uri.parse(url).lastPathSegment ?: "download")
        update { it + DownloadUiItem(id, url, name, status = "Starting") }

        scope.launch {
            val base = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(appContext.filesDir, "downloads")
            val destination = File(base, name)
            val engine = DownloadEngine()
            setStatus(id, "Downloading")
            try {
                val result = engine.download(url, destination) { downloaded, total ->
                    update { list -> list.map {
                        if (it.id == id) it.copy(downloaded = downloaded, total = total, status = "Downloading") else it
                    } }
                }
                update { list -> list.map {
                    if (it.id == id) it.copy(downloaded = result.bytes, total = result.bytes, status = "Completed") else it
                } }
            } catch (error: Exception) {
                update { list -> list.map {
                    if (it.id == id) it.copy(status = "Failed", error = error.message ?: "Download failed") else it
                } }
            }
        }
    }

    private fun safeFileName(value: String): String {
        val cleaned = value.substringBefore('?').substringBefore('#')
            .replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_')
        return cleaned.take(100).ifBlank { "download_" + System.currentTimeMillis() }
    }

    private fun setStatus(id: String, status: String) =
        update { list -> list.map { if (it.id == id) it.copy(status = status) else it } }

    private fun update(transform: (List<DownloadUiItem>) -> List<DownloadUiItem>) {
        _items.value = transform(_items.value)
    }

    fun close() { scope.cancel() }
}

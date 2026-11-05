package com.bh.turbodownloader.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

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

class DownloadCoordinator private constructor(context: Context) {
    companion object {
        @Volatile private var instance: DownloadCoordinator? = null
        fun get(context: Context): DownloadCoordinator = instance ?: synchronized(this) {
            instance ?: DownloadCoordinator(context.applicationContext).also { instance = it }
        }
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val engines = ConcurrentHashMap<String, DownloadEngine>()
    private val _items = MutableStateFlow(loadItems())
    val items: StateFlow<List<DownloadUiItem>> = _items.asStateFlow()

    fun start(rawUrl: String) {
        val url = rawUrl.trim()
        require(url.startsWith("https://", true) || url.startsWith("http://", true)) {
            "Enter a valid HTTP or HTTPS link"
        }
        val id = UUID.randomUUID().toString()
        val name = safeFileName(Uri.parse(url).lastPathSegment ?: "download")
        update { it + DownloadUiItem(id, url, name, status = "Queued") }
        launchTask(id, url, name, resume = false)
    }

    fun pause(id: String) {
        engines[id]?.cancel()
        update { list -> list.map { if (it.id == id) it.copy(status = "Pausing") else it } }
    }

    fun resume(id: String) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        if (item.status !in listOf("Paused", "Interrupted", "Failed")) return
        launchTask(id, item.url, item.fileName, resume = true)
    }

    private fun launchTask(id: String, url: String, name: String, resume: Boolean) {
        if (jobs[id]?.isActive == true) return
        val job = scope.launch {
            val tempDir = File(appContext.filesDir, "downloads").apply { mkdirs() }
            val temp = File(tempDir, "bh_" + id + ".download")
            val engine = DownloadEngine()
            engines[id] = engine
            update { list -> list.map { if (it.id == id) it.copy(status = "Downloading", error = null) else it } }
            try {
                var lastPersistedBytes = 0L
                val result = engine.download(url, temp, resume) { downloaded, total ->
                    val checkpoint = downloaded - lastPersistedBytes >= 512L * 1024L
                    if (checkpoint) lastPersistedBytes = downloaded
                    update(persist = checkpoint) { list -> list.map {
                        if (it.id == id) it.copy(downloaded = downloaded, total = total, status = "Downloading") else it
                    } }
                }
                val selectedTree = appContext.getSharedPreferences("bh_downloads", Context.MODE_PRIVATE)
                    .getString("destination_tree", null)?.let(Uri::parse)
                if (selectedTree != null) {
                    publishToSelectedFolder(result.file, name, selectedTree)
                } else if (Build.VERSION.SDK_INT >= 29) {
                    publishToPublicDownloads(result.file, name)
                } else {
                    val legacyDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: File(appContext.filesDir, "downloads")
                    legacyDir.mkdirs()
                    val legacyFile = File(legacyDir, name)
                    val finalFile = if (legacyFile.exists()) File(legacyDir, id.take(8) + "_" + name) else legacyFile
                    if (!result.file.renameTo(finalFile)) result.file.copyTo(finalFile, overwrite = true)
                }
                update { list -> list.map {
                    if (it.id == id) it.copy(downloaded = result.bytes, total = result.bytes, status = "Completed", error = null) else it
                } }
            } catch (error: Exception) {
                val paused = engine.isCancelled()
                update { list -> list.map {
                    if (it.id == id) it.copy(status = if (paused) "Paused" else "Failed", error = if (paused) null else (error.message ?: "Download failed")) else it
                } }
            } finally {
                engines.remove(id)
                jobs.remove(id)
            }
        }
        jobs[id] = job
    }

    fun setDestinationTree(uri: Uri) {
        appContext.getSharedPreferences("bh_downloads", Context.MODE_PRIVATE)
            .edit().putString("destination_tree", uri.toString()).apply()
    }

    private fun publishToSelectedFolder(source: File, fileName: String, treeUri: Uri) {
        val tree = DocumentFile.fromTreeUri(appContext, treeUri)
            ?: throw IllegalStateException("Selected folder is no longer available")
        if (!tree.canWrite()) throw IllegalStateException("Selected folder is not writable")
        val dot = fileName.lastIndexOf('.')
        val stem = if (dot > 0) fileName.substring(0, dot) else fileName
        val extension = if (dot > 0) fileName.substring(dot) else ""
        var candidate = fileName
        var suffix = 1
        while (tree.findFile(candidate) != null) candidate = stem + "_" + suffix++ + extension
        val target = tree.createFile("application/octet-stream", candidate)
            ?: throw IllegalStateException("Could not create file in selected folder")
        try {
            appContext.contentResolver.openOutputStream(target.uri)?.use { output ->
                source.inputStream().buffered().use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Could not open selected destination")
        } catch (error: Exception) {
            target.delete()
            throw error
        }
        // Keep the durable source file if publishing fails so the user can retry.
        source.delete()
    }

    private fun publishToPublicDownloads(source: File, fileName: String) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TurboBH")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = appContext.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Unable to create file in Download/TurboBH")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().buffered().use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Unable to open destination file")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
        // Delete the app-private copy only after MediaStore confirms publication.
        source.delete()
    }

    private fun safeFileName(value: String): String {
        val cleaned = value.substringBefore('?').substringBefore('#')
            .replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_')
        return cleaned.take(100).ifBlank { "download_" + System.currentTimeMillis() }
    }

    @Synchronized
    private fun update(persist: Boolean = true, transform: (List<DownloadUiItem>) -> List<DownloadUiItem>) {
        val next = transform(_items.value).takeLast(100)
        _items.value = next
        if (persist) persistItems(next)
    }

    private fun loadItems(): List<DownloadUiItem> = try {
        val raw = appContext.getSharedPreferences("bh_downloads", Context.MODE_PRIVATE)
            .getString("items", "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val saved = item.optString("status", "Queued")
            DownloadUiItem(
                id = item.optString("id"), url = item.optString("url"),
                fileName = item.optString("fileName"), downloaded = item.optLong("downloaded"),
                total = item.optLong("total"),
                status = if (saved == "Downloading" || saved == "Starting" || saved == "Pausing") "Interrupted" else saved,
                error = item.optString("error").takeIf { it.isNotBlank() && it != "null" }
            )
        }.filter { it.id.isNotBlank() && it.fileName.isNotBlank() }
    } catch (_: Exception) { emptyList() }

    private fun persistItems(items: List<DownloadUiItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("url", item.url); put("fileName", item.fileName)
                put("downloaded", item.downloaded); put("total", item.total)
                put("status", item.status); put("error", item.error ?: "")
            })
        }
        appContext.getSharedPreferences("bh_downloads", Context.MODE_PRIVATE)
            .edit().putString("items", array.toString()).apply()
    }
}

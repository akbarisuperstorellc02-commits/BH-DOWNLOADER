package com.bh.turbodownloader.download

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit

/**
 * HTTP downloader with safe single-stream fallback.
 * Segmentation is used only when HEAD confirms byte-range support.
 * Destination is an app-owned writable file; SAF is a later storage adapter.
 */
class DownloadEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val parallelism: Int = 8
) {
    data class Result(val file: File, val bytes: Long, val segmented: Boolean)
    private val cancelled = AtomicBoolean(false)

    fun cancel() { cancelled.set(true) }
    fun isCancelled(): Boolean = cancelled.get()

    @Throws(IOException::class)
    fun download(
        url: String,
        destination: File,
        resume: Boolean = false,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result {
        cancelled.set(false)
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Only HTTP and HTTPS URLs are supported"
        }
        destination.parentFile?.mkdirs()
        val temp = File(destination.parentFile, destination.name + ".bhpart")
        val existingBytes = if (resume && temp.exists()) temp.length() else 0L
        if (!resume && temp.exists()) temp.delete()

        val metadata = probe(url)
        val total = metadata.first
        val useRanges = metadata.second && total > 0L && existingBytes == 0L
        return try {
            if (existingBytes > 0L) {
                val resumed = downloadSingle(url, temp, total, onProgress, existingBytes)
                Result(commit(temp, destination), resumed, false)
            } else if (useRanges) {
                try {
                    downloadInRanges(url, temp, total, onProgress)
                    Result(commit(temp, destination), total, true)
                } catch (rangeError: IOException) {
                    if (cancelled.get()) throw rangeError
                    temp.delete()
                    val bytes = downloadSingle(url, temp, total, onProgress)
                    Result(commit(temp, destination), bytes, false)
                }
            } else {
                val bytes = downloadSingle(url, temp, total, onProgress)
                Result(commit(temp, destination), bytes, false)
            }
        } catch (e: Exception) {
            if (!cancelled.get() && e.message != "Download cancelled") temp.delete()
            throw e
        }
    }

    private fun probe(url: String): Pair<Long, Boolean> {
        val request = Request.Builder().url(url).head().build()
        return try {
            client.newCall(request).execute().use { response ->
                val length = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val ranges = response.header("Accept-Ranges")?.equals("bytes", true) == true
                length to ranges
            }
        } catch (_: Exception) {
            -1L to false
        }
    }

    private fun downloadInRanges(
        url: String,
        temp: File,
        total: Long,
        onProgress: (Long, Long) -> Unit
    ) {
        val count = parallelism.coerceIn(2, 8).coerceAtMost(total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(2))
        val chunkSize = (total + count - 1) / count
        val completed = AtomicLong(0)
        val executor = Executors.newFixedThreadPool(count)
        val futures = ArrayList<Future<Unit>>()
        RandomAccessFile(temp, "rw").use { it.setLength(total) }
        try {
            for (index in 0 until count) {
                val start = index * chunkSize
                val end = minOf(total - 1, start + chunkSize - 1)
                if (start > end) continue
                futures += executor.submit(Callable {
                    val request = Request.Builder().url(url)
                        .header("Range", "bytes=$start-$end").build()
                    client.newCall(request).execute().use { response ->
                        if (response.code != 206) {
                            throw IOException("Byte-range request rejected; HTTP " + response.code)
                        }
                        val body = response.body ?: throw IOException("Empty response body")
                        var position = start
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                            while (true) {
                                checkCancelled()
                                val read = input.read(buffer)
                                if (read < 0) break
                                RandomAccessFile(temp, "rw").use { file ->
                                    file.seek(position)
                                    file.write(buffer, 0, read)
                                }
                                position += read
                                onProgress(completed.addAndGet(read.toLong()), total)
                            }
                        }
                        if (position != end + 1) throw IOException("Incomplete byte range")
                    }
                    Unit
                })
            }
            futures.forEach { it.get() }
        } catch (e: Exception) {
            futures.forEach { it.cancel(true) }
            if (cancelled.get()) temp.delete()
            throw (e.cause as? IOException ?: IOException("Segmented download failed", e))
        } finally {
            executor.shutdownNow()
        }
    }

    private fun downloadSingle(
        url: String,
        temp: File,
        expectedTotal: Long,
        onProgress: (Long, Long) -> Unit,
        resumeFrom: Long = 0L
    ): Long {
        val requestBuilder = Request.Builder().url(url).get()
        if (resumeFrom > 0L) requestBuilder.header("Range", "bytes=$resumeFrom-")
        client.newCall(requestBuilder.build()).execute().use { response ->
            val contentRange = response.header("Content-Range")
            if (resumeFrom > 0L && (response.code != 206 || contentRange?.startsWith("bytes $resumeFrom-") != true)) {
                temp.delete()
                return downloadSingle(url, temp, expectedTotal, onProgress, 0L)
            }
            if (!response.isSuccessful) throw IOException("Download failed: HTTP " + response.code)
            val body = response.body ?: throw IOException("Empty response body")
            val total = if (resumeFrom > 0L) {
                val rangeLength = body.contentLength()
                if (rangeLength > 0L) resumeFrom + rangeLength else expectedTotal
            } else body.contentLength().takeIf { it > 0 } ?: expectedTotal
            var downloaded = resumeFrom
            body.byteStream().use { input ->
                val outputStream = if (resumeFrom > 0L) java.io.FileOutputStream(temp, true).buffered() else temp.outputStream().buffered()
                outputStream.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                    while (true) {
                        checkCancelled()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            return downloaded
        }
    }

    private fun checkCancelled() {
        if (cancelled.get() || Thread.currentThread().isInterrupted) {
            throw IOException("Download cancelled")
        }
    }

    private fun commit(temp: File, destination: File): File {
        if (destination.exists() && !destination.delete()) {
            throw IOException("Cannot replace destination file")
        }
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
        return destination
    }
}

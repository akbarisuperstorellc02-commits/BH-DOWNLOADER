package com.bh.turbodownloader.download

enum class DownloadState {
    QUEUED, PROBING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

data class DownloadItem(
    val id: String,
    val url: String,
    val fileName: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val state: DownloadState = DownloadState.QUEUED,
    val errorMessage: String? = null
) {
    val progress: Float?
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        } else null
}

package com.bh.turbodownloader.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
    const val KEY_ID = "download_id"
    const val KEY_URL = "download_url"
    const val KEY_NAME = "download_name"
    const val KEY_RESUME = "resume_download"
    private const val CHANNEL_ID = "bh_downloads"
    private const val NOTIFICATION_ID = 4107
    }

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME) ?: return Result.failure()
        val resume = inputData.getBoolean(KEY_RESUME, false)

        createNotificationChannel()
        setForeground(foregroundInfo(name, 0, false))
        return try {
            DownloadCoordinator.get(applicationContext).executeWorker(
                id = id,
                url = url,
                name = name,
                resume = resume,
                onProgress = { downloaded, total ->
                    val percent = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else 0
                    setProgress(workDataOf("downloaded" to downloaded, "total" to total, "percent" to percent))
                }
            )
            Result.success()
        } catch (_: kotlinx.coroutines.CancellationException) {
            Result.failure()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    override fun onStopped() {
        inputData.getString(KEY_ID)?.let {
            DownloadCoordinator.get(applicationContext).cancelDownload(it)
        }
        super.onStopped()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows active file downloads"
                }
            )
        }
    }

    private fun foregroundInfo(fileName: String, percent: Int, indeterminate: Boolean): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("BH Turbo Downloader")
            .setContentText(if (percent > 0) "$fileName • $percent%" else "Downloading $fileName")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, indeterminate)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID + (fileName.hashCode() and 0x3FFF),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID + (fileName.hashCode() and 0x3FFF), notification)
        }
    }
}
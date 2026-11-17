package com.instasaver.feature.downloader.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.instasaver.core.model.MediaType
import com.instasaver.core.storage.MediaStorageService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val storageService: MediaStorageService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "instasaver_download_channel"
        const val NOTIFICATION_ID = 1001
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_AUTHOR = "author"
        const val KEY_SHORTCODE = "shortcode"
        const val KEY_MEDIA_TYPE = "media_type"
        const val KEY_URI = "saved_uri"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(progress = 0, author = "Instagram Media")
    }

    private fun createForegroundInfo(progress: Int, author: String): ForegroundInfo {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "InstaSaver Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing media download notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Downloading @$author")
            .setContentText(if (progress in 0..100) "Progress: $progress%" else "Downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress.coerceIn(0, 100), progress < 0)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val author = inputData.getString(KEY_AUTHOR) ?: "instagram_user"
        val shortcode = inputData.getString(KEY_SHORTCODE) ?: "media"
        val mediaTypeStr = inputData.getString(KEY_MEDIA_TYPE) ?: MediaType.REEL.name
        val mediaType = try {
            MediaType.valueOf(mediaTypeStr)
        } catch (_: Exception) {
            MediaType.REEL
        }

        try {
            setForeground(createForegroundInfo(0, author))
        } catch (_: Exception) {
            // Foreground may be ignored if background execution limits apply
        }

        val saveResult = storageService.saveMediaToGallery(
            downloadUrl = downloadUrl,
            authorUsername = author,
            shortcode = shortcode,
            mediaType = mediaType
        ) { progress, downloaded, total ->
            val progressInt = (progress * 100).toInt()
            setProgressAsync(
                workDataOf(
                    "progress" to progress,
                    "downloaded" to downloaded,
                    "total" to total
                )
            )
            try {
                val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val updateNotif = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setContentTitle("Downloading @$author")
                    .setContentText("Progress: $progressInt%")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .setProgress(100, progressInt.coerceIn(0, 100), false)
                    .build()
                notificationManager.notify(NOTIFICATION_ID, updateNotif)
            } catch (_: Exception) {}
        }

        return if (saveResult.isSuccess) {
            val uri = saveResult.getOrNull()
            try {
                val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
            } catch (_: Exception) {}
            Result.success(workDataOf(KEY_URI to (uri?.toString() ?: "")))
        } else {
            Result.retry()
        }
    }
}

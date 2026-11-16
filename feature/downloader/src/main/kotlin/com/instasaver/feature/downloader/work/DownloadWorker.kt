package com.instasaver.feature.downloader.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_AUTHOR = "author"
        const val KEY_SHORTCODE = "shortcode"
        const val KEY_MEDIA_TYPE = "media_type"
        const val KEY_URI = "saved_uri"
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val author = inputData.getString(KEY_AUTHOR) ?: "instagram_user"
        val shortcode = inputData.getString(KEY_SHORTCODE) ?: "media"
        val mediaTypeStr = inputData.getString(KEY_MEDIA_TYPE) ?: MediaType.REEL.name
        val mediaType = try {
            MediaType.valueOf(mediaTypeStr)
        } catch (e: Exception) {
            MediaType.REEL
        }

        val saveResult = storageService.saveMediaToGallery(
            downloadUrl = downloadUrl,
            authorUsername = author,
            shortcode = shortcode,
            mediaType = mediaType
        ) { progress, downloaded, total ->
            setProgressAsync(
                workDataOf(
                    "progress" to progress,
                    "downloaded" to downloaded,
                    "total" to total
                )
            )
        }

        return if (saveResult.isSuccess) {
            val uri = saveResult.getOrNull()
            Result.success(workDataOf(KEY_URI to (uri?.toString() ?: "")))
        } else {
            Result.retry()
        }
    }
}

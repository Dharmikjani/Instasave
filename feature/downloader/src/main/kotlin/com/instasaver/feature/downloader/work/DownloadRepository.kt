package com.instasaver.feature.downloader.work

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.instasaver.core.database.DatabaseRepository
import com.instasaver.core.model.DownloadProgress
import com.instasaver.core.model.MediaItem
import com.instasaver.core.storage.MediaStorageService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageService: MediaStorageService,
    private val databaseRepository: DatabaseRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(2)

    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    private val progressMap = ConcurrentHashMap<String, DownloadProgress>()

    fun enqueueDownload(
        item: MediaItem,
        customUrl: String? = null,
        onSuccess: (Uri) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            semaphore.acquire()
            try {
                updateProgress(
                    DownloadProgress(
                        mediaId = item.id,
                        fileName = item.title,
                        progress = 0f,
                        bytesDownloaded = 0,
                        totalBytes = 0
                    )
                )

                val targetUrl = customUrl ?: item.downloadUrl
                val result = storageService.saveMediaToGallery(
                    downloadUrl = targetUrl,
                    authorUsername = item.authorUsername,
                    shortcode = item.shortcode,
                    mediaType = item.type,
                    onProgress = { progress, downloaded, total ->
                        updateProgress(
                            DownloadProgress(
                                mediaId = item.id,
                                fileName = item.title,
                                progress = progress,
                                bytesDownloaded = downloaded,
                                totalBytes = total
                            )
                        )
                    }
                )

                result.onSuccess { uri ->
                    updateProgress(
                        DownloadProgress(
                            mediaId = item.id,
                            fileName = item.title,
                            progress = 1f,
                            bytesDownloaded = 0,
                            totalBytes = 0,
                            isCompleted = true
                        )
                    )
                    databaseRepository.recordDownload(
                        shortcode = item.shortcode,
                        fileName = item.title.ifBlank { "Media by @${item.authorUsername}" },
                        savedUri = uri.toString()
                    )
                    withContext(Dispatchers.Main) {
                        onSuccess(uri)
                    }
                }.onFailure { ex ->
                    val errorMsg = ex.message ?: "Download failed"
                    updateProgress(
                        DownloadProgress(
                            mediaId = item.id,
                            fileName = item.title,
                            progress = 0f,
                            bytesDownloaded = 0,
                            totalBytes = 0,
                            error = errorMsg
                        )
                    )
                    withContext(Dispatchers.Main) {
                        onError(errorMsg)
                    }
                }
            } finally {
                semaphore.release()
            }
        }
    }

    fun enqueueBackgroundDownload(item: MediaItem, customUrl: String? = null) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val targetUrl = customUrl ?: item.downloadUrl
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_DOWNLOAD_URL to targetUrl,
                    DownloadWorker.KEY_AUTHOR to item.authorUsername,
                    DownloadWorker.KEY_SHORTCODE to item.shortcode,
                    DownloadWorker.KEY_MEDIA_TYPE to item.type.name
                )
            )
            .addTag("download_${item.shortcode}")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun enqueueBatch(items: List<MediaItem>) {
        scope.launch {
            for (item in items) {
                enqueueDownload(item)
                delay(350)
            }
        }
    }

    private fun updateProgress(progress: DownloadProgress) {
        progressMap[progress.mediaId] = progress
        _downloadStates.value = progressMap.toMap()
    }
}

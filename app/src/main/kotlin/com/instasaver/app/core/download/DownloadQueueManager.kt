package com.instasaver.app.core.download

import android.content.Context
import android.net.Uri
import com.instasaver.app.core.model.DownloadProgress
import com.instasaver.app.core.model.MediaItem
import com.instasaver.app.core.storage.MediaStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentHashMap

object DownloadQueueManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(2) // Max 2 concurrent downloads to avoid rate limits

    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    private val progressMap = ConcurrentHashMap<String, DownloadProgress>()

    fun enqueueDownload(
        context: Context,
        item: MediaItem,
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

                val result = MediaStorageManager.saveMediaToGallery(
                    context = context,
                    downloadUrl = item.downloadUrl,
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
                    onSuccess(uri)
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
                    onError(errorMsg)
                }
            } finally {
                semaphore.release()
            }
        }
    }

    /**
     * Enqueues batch download with controlled pacing (300ms gap)
     */
    fun enqueueBatch(context: Context, items: List<MediaItem>) {
        scope.launch {
            for (item in items) {
                enqueueDownload(context, item)
                delay(350) // polite delay to protect IP reputation
            }
        }
    }

    private fun updateProgress(progress: DownloadProgress) {
        progressMap[progress.mediaId] = progress
        _downloadStates.value = progressMap.toMap()
    }
}


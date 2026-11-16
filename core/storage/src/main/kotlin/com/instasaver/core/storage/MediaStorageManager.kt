package com.instasaver.core.storage

import android.content.Context
import android.net.Uri
import com.instasaver.core.model.MediaType
import com.instasaver.core.network.InstagramHttpClient

object MediaStorageManager {

    private val audioDemuxer = AudioDemuxer()

    fun hasSufficientSpace(context: Context, requiredBytes: Long = 20 * 1024 * 1024): Boolean {
        val service = MediaStorageServiceImpl(context, InstagramHttpClient.client, audioDemuxer)
        return service.hasSufficientSpace(requiredBytes)
    }

    suspend fun saveMediaToGallery(
        context: Context,
        downloadUrl: String,
        authorUsername: String,
        shortcode: String,
        mediaType: MediaType,
        onProgress: (Float, Long, Long) -> Unit = { _, _, _ -> }
    ): Result<Uri> {
        val service = MediaStorageServiceImpl(context, InstagramHttpClient.client, audioDemuxer)
        return service.saveMediaToGallery(downloadUrl, authorUsername, shortcode, mediaType, onProgress)
    }
}

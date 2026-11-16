package com.instasaver.core.storage

import android.net.Uri
import com.instasaver.core.model.MediaType

interface MediaStorageService {
    fun hasSufficientSpace(requiredBytes: Long = 20 * 1024 * 1024): Boolean
    suspend fun saveMediaToGallery(
        downloadUrl: String,
        authorUsername: String,
        shortcode: String,
        mediaType: MediaType,
        onProgress: (Float, Long, Long) -> Unit = { _, _, _ -> }
    ): Result<Uri>
}

package com.instasaver.core.extractor

import com.instasaver.core.model.MediaItem

interface ExtractorRepository {
    fun extractShortcode(rawUrl: String): String?
    suspend fun extractMedia(rawUrl: String): Result<MediaItem>
}

package com.instasaver.core.extractor

import com.instasaver.core.model.MediaItem
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractorRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : ExtractorRepository {

    override fun extractShortcode(rawUrl: String): String? {
        return InstagramExtractor.extractShortcode(rawUrl)
    }

    override suspend fun extractMedia(rawUrl: String): Result<MediaItem> {
        return InstagramExtractor.extractMedia(rawUrl)
    }
}

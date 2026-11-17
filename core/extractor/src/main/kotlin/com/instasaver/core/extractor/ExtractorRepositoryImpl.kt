package com.instasaver.core.extractor

import com.instasaver.core.database.DatabaseRepository
import com.instasaver.core.model.MediaItem
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractorRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val databaseRepository: DatabaseRepository
) : ExtractorRepository {

    override fun extractShortcode(rawUrl: String): String? {
        return InstagramExtractor.extractShortcode(rawUrl)
    }

    override suspend fun extractMedia(rawUrl: String): Result<MediaItem> {
        val shortcode = extractShortcode(rawUrl)
        if (!shortcode.isNullOrBlank()) {
            // Check Tier 0 Cache (2h TTL)
            val cached = databaseRepository.getCachedMedia(shortcode)
            if (cached != null) {
                return Result.success(cached)
            }
        }

        val result = InstagramExtractor.extractMedia(rawUrl)
        result.onSuccess { media ->
            val codeToUse = media.shortcode.ifBlank { shortcode ?: "" }
            if (codeToUse.isNotBlank()) {
                databaseRepository.saveMediaCache(codeToUse, media)
            }
        }
        return result
    }
}

package com.instasaver.core.database

import com.instasaver.core.database.entity.DownloadHistoryEntity
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.ProfileData
import kotlinx.coroutines.flow.Flow

interface DatabaseRepository {
    suspend fun getCachedMedia(shortcode: String, maxAgeMillis: Long = 2 * 60 * 60 * 1000L): MediaItem?
    suspend fun saveMediaCache(shortcode: String, mediaItem: MediaItem)
    suspend fun cleanExpiredMediaCache(maxAgeMillis: Long = 2 * 60 * 60 * 1000L)

    suspend fun getCachedProfile(username: String, maxAgeMillis: Long = 6 * 60 * 60 * 1000L): ProfileData?
    suspend fun saveProfileCache(username: String, profileData: ProfileData)
    suspend fun cleanExpiredProfileCache(maxAgeMillis: Long = 6 * 60 * 60 * 1000L)

    fun observePinnedUsernames(): Flow<List<String>>
    suspend fun getPinnedUsernames(): List<String>
    suspend fun isPinned(username: String): Boolean
    suspend fun pinCreator(username: String)
    suspend fun unpinCreator(username: String)

    fun observeDownloadHistory(): Flow<List<DownloadHistoryEntity>>
    suspend fun recordDownload(shortcode: String, fileName: String, savedUri: String)
}

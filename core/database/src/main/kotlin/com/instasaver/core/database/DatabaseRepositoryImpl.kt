package com.instasaver.core.database

import com.instasaver.core.database.dao.DownloadHistoryDao
import com.instasaver.core.database.dao.MediaCacheDao
import com.instasaver.core.database.dao.ProfileCacheDao
import com.instasaver.core.database.dao.WatchlistDao
import com.instasaver.core.database.entity.CreatorEntity
import com.instasaver.core.database.entity.DownloadHistoryEntity
import com.instasaver.core.database.entity.MediaCacheEntity
import com.instasaver.core.database.entity.ProfileCacheEntity
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.ProfileData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val mediaCacheDao: MediaCacheDao,
    private val profileCacheDao: ProfileCacheDao,
    private val downloadHistoryDao: DownloadHistoryDao
) : DatabaseRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun getCachedMedia(shortcode: String, maxAgeMillis: Long): MediaItem? = withContext(Dispatchers.IO) {
        val entity = mediaCacheDao.getMedia(shortcode) ?: return@withContext null
        val now = System.currentTimeMillis()
        if (now - entity.cachedAt > maxAgeMillis) {
            return@withContext null
        }
        try {
            json.decodeFromString<MediaItem>(entity.mediaJson)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun saveMediaCache(shortcode: String, mediaItem: MediaItem): Unit = withContext(Dispatchers.IO) {
        try {
            val jsonStr = json.encodeToString(mediaItem)
            mediaCacheDao.insert(
                MediaCacheEntity(
                    shortcode = shortcode,
                    mediaJson = jsonStr,
                    cachedAt = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    override suspend fun cleanExpiredMediaCache(maxAgeMillis: Long): Unit = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - maxAgeMillis
        mediaCacheDao.deleteExpired(threshold)
    }

    override suspend fun getCachedProfile(username: String, maxAgeMillis: Long): ProfileData? = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@").lowercase()
        val entity = profileCacheDao.getProfile(clean) ?: return@withContext null
        val now = System.currentTimeMillis()
        if (now - entity.cachedAt > maxAgeMillis) {
            return@withContext null
        }
        try {
            json.decodeFromString<ProfileData>(entity.profileJson)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun saveProfileCache(username: String, profileData: ProfileData): Unit = withContext(Dispatchers.IO) {
        try {
            val clean = username.trim().removePrefix("@").lowercase()
            val jsonStr = json.encodeToString(profileData)
            profileCacheDao.insert(
                ProfileCacheEntity(
                    username = clean,
                    profileJson = jsonStr,
                    cachedAt = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    override suspend fun cleanExpiredProfileCache(maxAgeMillis: Long): Unit = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - maxAgeMillis
        profileCacheDao.deleteExpired(threshold)
    }

    override fun observePinnedUsernames(): Flow<List<String>> {
        return watchlistDao.observePinnedUsernames()
    }

    override suspend fun getPinnedUsernames(): List<String> = withContext(Dispatchers.IO) {
        watchlistDao.getPinnedUsernames()
    }

    override suspend fun isPinned(username: String): Boolean = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@").lowercase()
        watchlistDao.isPinned(clean)
    }

    override suspend fun pinCreator(username: String): Unit = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@").lowercase()
        watchlistDao.insert(CreatorEntity(clean))
    }

    override suspend fun unpinCreator(username: String): Unit = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@").lowercase()
        watchlistDao.delete(clean)
    }

    override fun observeDownloadHistory(): Flow<List<DownloadHistoryEntity>> {
        return downloadHistoryDao.observeHistory()
    }

    override suspend fun recordDownload(shortcode: String, fileName: String, savedUri: String): Unit = withContext(Dispatchers.IO) {
        downloadHistoryDao.insert(
            DownloadHistoryEntity(
                shortcode = shortcode,
                fileName = fileName,
                savedUri = savedUri,
                downloadedAt = System.currentTimeMillis()
            )
        )
    }
}

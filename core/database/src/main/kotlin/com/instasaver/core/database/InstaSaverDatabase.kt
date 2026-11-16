package com.instasaver.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.instasaver.core.database.dao.DownloadHistoryDao
import com.instasaver.core.database.dao.MediaCacheDao
import com.instasaver.core.database.dao.ProfileCacheDao
import com.instasaver.core.database.dao.WatchlistDao
import com.instasaver.core.database.entity.CreatorEntity
import com.instasaver.core.database.entity.DownloadHistoryEntity
import com.instasaver.core.database.entity.MediaCacheEntity
import com.instasaver.core.database.entity.ProfileCacheEntity

@Database(
    entities = [
        CreatorEntity::class,
        MediaCacheEntity::class,
        ProfileCacheEntity::class,
        DownloadHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InstaSaverDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun mediaCacheDao(): MediaCacheDao
    abstract fun profileCacheDao(): ProfileCacheDao
    abstract fun downloadHistoryDao(): DownloadHistoryDao
}

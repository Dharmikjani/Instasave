package com.instasaver.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.instasaver.core.database.entity.MediaCacheEntity

@Dao
interface MediaCacheDao {

    @Query("SELECT * FROM media_cache WHERE shortcode = :shortcode LIMIT 1")
    fun getMedia(shortcode: String): MediaCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(media: MediaCacheEntity): Long

    @Query("DELETE FROM media_cache WHERE cachedAt < :thresholdMillis")
    fun deleteExpired(thresholdMillis: Long): Int
}

package com.instasaver.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.instasaver.core.database.entity.ProfileCacheEntity

@Dao
interface ProfileCacheDao {

    @Query("SELECT * FROM profile_cache WHERE username = :username LIMIT 1")
    fun getProfile(username: String): ProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(profile: ProfileCacheEntity): Long

    @Query("DELETE FROM profile_cache WHERE cachedAt < :thresholdMillis")
    fun deleteExpired(thresholdMillis: Long): Int
}

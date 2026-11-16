package com.instasaver.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.instasaver.core.database.entity.CreatorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT username FROM pinned_creators ORDER BY pinnedAt DESC")
    fun observePinnedUsernames(): Flow<List<String>>

    @Query("SELECT username FROM pinned_creators ORDER BY pinnedAt DESC")
    fun getPinnedUsernames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_creators WHERE username = :username)")
    fun isPinned(username: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(creator: CreatorEntity): Long

    @Query("DELETE FROM pinned_creators WHERE username = :username")
    fun delete(username: String): Int
}

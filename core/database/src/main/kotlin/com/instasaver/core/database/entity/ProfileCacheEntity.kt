package com.instasaver.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_cache")
data class ProfileCacheEntity(
    @PrimaryKey
    val username: String,
    val profileJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

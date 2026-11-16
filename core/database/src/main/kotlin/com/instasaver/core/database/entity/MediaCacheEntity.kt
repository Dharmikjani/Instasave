package com.instasaver.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_cache")
data class MediaCacheEntity(
    @PrimaryKey
    val shortcode: String,
    val mediaJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

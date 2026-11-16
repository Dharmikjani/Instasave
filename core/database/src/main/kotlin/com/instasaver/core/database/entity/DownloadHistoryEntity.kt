package com.instasaver.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shortcode: String,
    val fileName: String,
    val savedUri: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

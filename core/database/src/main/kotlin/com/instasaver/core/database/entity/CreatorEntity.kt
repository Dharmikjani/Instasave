package com.instasaver.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_creators")
data class CreatorEntity(
    @PrimaryKey
    val username: String,
    val pinnedAt: Long = System.currentTimeMillis()
)

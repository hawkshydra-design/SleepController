package com.sleepcontroller.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val isPermanentlyWhitelisted: Boolean = false, // Phone, Dialer, Clock, Settings, Emergency
    val category: String = "other",                // social, games, media, etc.
    val addedAt: Long = System.currentTimeMillis()
)

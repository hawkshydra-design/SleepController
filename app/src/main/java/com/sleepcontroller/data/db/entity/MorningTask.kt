package com.sleepcontroller.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskType {
    CHECKBOX,   // Simple check-off
    TIMED,      // Must wait N seconds/minutes
    PHOTO       // Requires photo with EXIF verification
}

@Entity(tableName = "morning_tasks")
data class MorningTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: TaskType = TaskType.CHECKBOX,
    val timedDurationSeconds: Int = 0,   // For TIMED tasks
    val sortOrder: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

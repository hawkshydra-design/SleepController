package com.sleepcontroller.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_history")
data class SleepHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,                          // Date (start of day millis)
    val bedtimeActual: Long,                 // Actual time sleep mode activated
    val wakeUpActual: Long? = null,          // Actual time tasks completed
    val sleepDurationMinutes: Int? = null,    // Calculated
    val tasksCompleted: Int = 0,
    val totalTasks: Int = 0,
    val emergencyUnlockUsed: Boolean = false,
    val skippedTasks: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

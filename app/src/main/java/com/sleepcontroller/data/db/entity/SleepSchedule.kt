package com.sleepcontroller.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_schedules")
data class SleepSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bedtimeHour: Int = 22,         // 10 PM
    val bedtimeMinute: Int = 0,
    val sleepDurationMinutes: Int = 480, // 8 hours default
    val wakeUpHour: Int = 6,            // Calculated from bedtime + duration
    val wakeUpMinute: Int = 0,
    val mondayEnabled: Boolean = true,
    val tuesdayEnabled: Boolean = true,
    val wednesdayEnabled: Boolean = true,
    val thursdayEnabled: Boolean = true,
    val fridayEnabled: Boolean = true,
    val saturdayEnabled: Boolean = true,
    val sundayEnabled: Boolean = true,
    val windDownMinutes: Int = 30,      // Warning before lockdown
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

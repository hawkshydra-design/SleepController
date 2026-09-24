package com.sleepcontroller.data.db

import androidx.room.TypeConverter
import com.sleepcontroller.data.db.entity.TaskType

class Converters {

    @TypeConverter
    fun fromTaskType(type: TaskType): String = type.name

    @TypeConverter
    fun toTaskType(value: String): TaskType = TaskType.valueOf(value)
}

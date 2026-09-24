package com.sleepcontroller.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TimeUtils {

    fun calculateWakeUpTime(bedtimeHour: Int, bedtimeMinute: Int, sleepDurationMinutes: Int): Pair<Int, Int> {
        val bedtime = LocalTime.of(bedtimeHour, bedtimeMinute)
        val wakeUp = bedtime.plusMinutes(sleepDurationMinutes.toLong())
        return Pair(wakeUp.hour, wakeUp.minute)
    }

    fun formatTime(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour, minute)
        return time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
    }

    fun minutesUntilBedtime(bedtimeHour: Int, bedtimeMinute: Int): Long {
        val now = LocalDateTime.now()
        var bedtime = LocalDateTime.of(
            now.toLocalDate(),
            LocalTime.of(bedtimeHour, bedtimeMinute)
        )
        if (bedtime.isBefore(now)) {
            bedtime = bedtime.plusDays(1)
        }
        return ChronoUnit.MINUTES.between(now, bedtime)
    }

    fun getStartOfDayMillis(date: LocalDate = LocalDate.now()): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val date1 = java.time.Instant.ofEpochMilli(millis1).atZone(ZoneId.systemDefault()).toLocalDate()
        val date2 = java.time.Instant.ofEpochMilli(millis2).atZone(ZoneId.systemDefault()).toLocalDate()
        return date1 == date2
    }

    fun isDayEnabled(dayOfWeek: Int, schedule: com.sleepcontroller.data.db.entity.SleepSchedule): Boolean {
        return when (dayOfWeek) {
            1 -> schedule.mondayEnabled
            2 -> schedule.tuesdayEnabled
            3 -> schedule.wednesdayEnabled
            4 -> schedule.thursdayEnabled
            5 -> schedule.fridayEnabled
            6 -> schedule.saturdayEnabled
            7 -> schedule.sundayEnabled
            else -> false
        }
    }

    fun getNextBedtimeMillis(bedtimeHour: Int, bedtimeMinute: Int): Long {
        val now = LocalDateTime.now()
        var bedtime = LocalDateTime.of(
            now.toLocalDate(),
            LocalTime.of(bedtimeHour, bedtimeMinute)
        )
        if (bedtime.isBefore(now)) {
            bedtime = bedtime.plusDays(1)
        }
        return bedtime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

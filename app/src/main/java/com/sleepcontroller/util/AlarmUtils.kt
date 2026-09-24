package com.sleepcontroller.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import com.sleepcontroller.receiver.AlarmReceiver
import com.sleepcontroller.receiver.EmergencyExpiryReceiver
import com.sleepcontroller.receiver.SleepModeReceiver
import com.sleepcontroller.receiver.WindDownReceiver
import java.util.Calendar

/**
 * Utility for scheduling exact alarms via AlarmManager.
 * Handles bedtime, wake-up, wind-down, alarm triggers, and emergency expiry.
 * All alarms use setExactAndAllowWhileIdle for reliability through Doze.
 */
object AlarmUtils {

    private const val TAG = "AlarmUtils"
    private const val SLEEP_MODE_REQUEST_CODE = 1001
    private const val WAKE_UP_REQUEST_CODE = 1002
    private const val ALARM_REQUEST_CODE = 1003
    private const val WIND_DOWN_REQUEST_CODE = 1004
    private const val EMERGENCY_EXPIRY_REQUEST_CODE = 1005

    /**
     * Schedule bedtime alarm — triggers SleepModeReceiver with ACTION_SLEEP_ON
     */
    fun scheduleSleepMode(context: Context, hour: Int, minute: Int) {
        val intent = Intent(context, SleepModeReceiver::class.java).apply {
            action = SleepModeReceiver.ACTION_SLEEP_ON
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, SLEEP_MODE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactAlarm(context, pendingIntent, hour, minute)
        Log.d(TAG, "Bedtime scheduled for $hour:${minute.toString().padStart(2, '0')}")
    }

    /**
     * Schedule wake-up alarm — triggers SleepModeReceiver with ACTION_SLEEP_OFF
     */
    fun scheduleWakeUp(context: Context, hour: Int, minute: Int) {
        val intent = Intent(context, SleepModeReceiver::class.java).apply {
            action = SleepModeReceiver.ACTION_SLEEP_OFF
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, WAKE_UP_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactAlarm(context, pendingIntent, hour, minute)
        Log.d(TAG, "Wake-up scheduled for $hour:${minute.toString().padStart(2, '0')}")
    }

    /**
     * Schedule in-app alarm — triggers AlarmReceiver to start AlarmService
     */
    fun scheduleAlarmTrigger(context: Context, hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactAlarm(context, pendingIntent, hour, minute)
        Log.d(TAG, "Alarm trigger scheduled for $hour:${minute.toString().padStart(2, '0')}")
    }

    /**
     * Schedule wind-down notification — fires N minutes before bedtime.
     */
    fun scheduleWindDown(context: Context, bedtimeHour: Int, bedtimeMinute: Int, minutesBefore: Int) {
        val calendar = getNextOccurrence(bedtimeHour, bedtimeMinute)
        calendar.add(Calendar.MINUTE, -minutesBefore)

        // Don't schedule if it's already past the wind-down time
        if (calendar.before(Calendar.getInstance())) {
            Log.d(TAG, "Wind-down time already passed for today")
            return
        }

        val intent = Intent(context, WindDownReceiver::class.java).apply {
            putExtra("minutes_left", minutesBefore)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, WIND_DOWN_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExact(context, calendar.timeInMillis, pendingIntent)
        Log.d(TAG, "Wind-down scheduled $minutesBefore min before bedtime")
    }

    /**
     * Set system alarm via the device's clock app.
     * Falls back silently if no clock app is found.
     */
    fun setSystemAlarm(context: Context, hour: Int, minute: Int, message: String = "Wake Up!") {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "System alarm set for $hour:${minute.toString().padStart(2, '0')}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set system alarm", e)
        }
    }

    /**
     * Schedule emergency unlock expiry — fires EmergencyExpiryReceiver after N minutes.
     * Used for temporary 20-min unlocks (uses 1-4 of 5).
     */
    fun scheduleEmergencyExpiry(context: Context, minutes: Int) {
        val intent = Intent(context, EmergencyExpiryReceiver::class.java).apply {
            action = EmergencyExpiryReceiver.ACTION_EMERGENCY_EXPIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, EMERGENCY_EXPIRY_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + (minutes * 60 * 1000L)
        setExact(context, triggerAt, pendingIntent)
        Log.d(TAG, "Emergency expiry scheduled in $minutes minutes")
    }

    /**
     * Cancel emergency unlock expiry alarm.
     * Called when sleep mode ends or user uses 5th unlock.
     */
    fun cancelEmergencyExpiry(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EmergencyExpiryReceiver::class.java).apply {
            action = EmergencyExpiryReceiver.ACTION_EMERGENCY_EXPIRED
        }
        val pi = PendingIntent.getBroadcast(
            context, EMERGENCY_EXPIRY_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
        Log.d(TAG, "Emergency expiry alarm cancelled")
    }

    /**
     * Cancel all scheduled alarms (bedtime, wake-up, alarm, wind-down, emergency).
     */
    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(
            SLEEP_MODE_REQUEST_CODE to SleepModeReceiver::class.java,
            WAKE_UP_REQUEST_CODE to SleepModeReceiver::class.java,
            ALARM_REQUEST_CODE to AlarmReceiver::class.java,
            WIND_DOWN_REQUEST_CODE to WindDownReceiver::class.java,
            EMERGENCY_EXPIRY_REQUEST_CODE to EmergencyExpiryReceiver::class.java
        ).forEach { (requestCode, receiverClass) ->
            val pi = PendingIntent.getBroadcast(
                context, requestCode,
                Intent(context, receiverClass),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        }
        Log.d(TAG, "All alarms cancelled")
    }

    /**
     * Calculate the next Calendar occurrence of a given hour:minute.
     * If the time has already passed today, rolls over to tomorrow.
     */
    private fun getNextOccurrence(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    /**
     * Schedule an exact alarm at the next occurrence of hour:minute.
     */
    private fun scheduleExactAlarm(context: Context, pendingIntent: PendingIntent, hour: Int, minute: Int) {
        val calendar = getNextOccurrence(hour, minute)
        setExact(context, calendar.timeInMillis, pendingIntent)
    }

    /**
     * Set exact alarm with Doze compatibility.
     */
    private fun setExact(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            } else {
                // Fallback to inexact for SDK 31+ without SCHEDULE_EXACT_ALARM permission
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
                Log.w(TAG, "Exact alarm permission not granted — using inexact")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }
}

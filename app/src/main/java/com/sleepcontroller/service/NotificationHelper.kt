package com.sleepcontroller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.sleepcontroller.ui.MainActivity

/**
 * Centralized notification management for all Sleep Controller notifications.
 * Creates channels, builds notifications for sleep mode, morning mode, wind-down, and alarms.
 */
object NotificationHelper {

    const val CHANNEL_SLEEP = "sleep_mode"
    const val CHANNEL_ALARM = "alarm_channel"
    const val CHANNEL_WIND_DOWN = "wind_down"
    const val CHANNEL_SESSION = "session_updates"

    const val NOTIFICATION_ID_SLEEP = 2001
    const val NOTIFICATION_ID_SCHEDULE = 2002
    const val NOTIFICATION_ID_ALARM = 2003
    const val NOTIFICATION_ID_WIND_DOWN = 2004

    fun createAllChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val sleepChannel = NotificationChannel(
            CHANNEL_SLEEP,
            "Sleep Mode",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when sleep mode is active and blocking apps"
            setShowBadge(false)
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Wake Up Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Wake-up alarm notifications"
            enableVibration(true)
            enableLights(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val windDownChannel = NotificationChannel(
            CHANNEL_WIND_DOWN,
            "Wind-Down Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications before sleep mode activates"
        }

        val sessionChannel = NotificationChannel(
            CHANNEL_SESSION,
            "Session Updates",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Updates about your sleep session"
            setShowBadge(false)
        }

        manager.createNotificationChannels(
            listOf(sleepChannel, alarmChannel, windDownChannel, sessionChannel)
        )
    }

    fun buildSleepActiveNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_SLEEP)
            .setContentTitle("😴 Sleep Mode Active")
            .setContentText("Apps are blocked. Get some rest!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(mainActivityPendingIntent(context))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .build()
    }

    fun buildMorningModeNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_SLEEP)
            .setContentTitle("🌅 Good Morning!")
            .setContentText("Complete your morning tasks to unlock apps")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(mainActivityPendingIntent(context))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(true)
            .build()
    }

    fun showWindDownNotification(context: Context, minutesLeft: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_WIND_DOWN)
            .setContentTitle("🌙 Bedtime in $minutesLeft minutes")
            .setContentText("Time to start winding down. Sleep mode will activate soon.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(mainActivityPendingIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_WIND_DOWN, notification)
    }

    fun buildAlarmNotification(context: Context): Notification {
        val dismissIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        val dismissPi = PendingIntent.getService(
            context, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
        }
        val snoozePi = PendingIntent.getService(
            context, 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setContentTitle("🌅 Good Morning!")
            .setContentText("Time to wake up! Complete your morning tasks.")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(mainActivityPendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(mainActivityPendingIntent(context), true)
            .addAction(android.R.drawable.ic_delete, "Dismiss", dismissPi)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze 5m", snoozePi)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    private fun mainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}

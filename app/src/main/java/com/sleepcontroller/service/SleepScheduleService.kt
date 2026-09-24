package com.sleepcontroller.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.sleepcontroller.di.ServiceEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Foreground service managing the overall sleep mode lifecycle.
 * Runs persistently while sleep or morning mode is active.
 *
 * Responsibilities:
 *  - Shows persistent notification indicating current mode
 *  - Keeps the app process alive for AccessibilityService reliability
 *
 * NOTE: This service no longer mutates session state directly.
 * All state changes are handled by [SleepSessionManager] via [SessionRepository].
 * This service only manages its own foreground lifecycle and notifications.
 */
class SleepScheduleService : Service() {

    companion object {
        private const val TAG = "SleepScheduleSvc"
        const val ACTION_START_SLEEP = "com.sleepcontroller.ACTION_START_SLEEP"
        const val ACTION_START_MORNING = "com.sleepcontroller.ACTION_START_MORNING"
        const val ACTION_STOP = "com.sleepcontroller.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createAllChannels(this)
        Log.d(TAG, "Sleep schedule service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SLEEP -> {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_SCHEDULE,
                    NotificationHelper.buildSleepActiveNotification(this)
                )
                Log.d(TAG, "Sleep mode activated — apps are blocked")
            }
            ACTION_START_MORNING -> {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_SCHEDULE,
                    NotificationHelper.buildMorningModeNotification(this)
                )
                Log.d(TAG, "Morning mode activated — complete tasks to unlock")
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Sleep schedule service stopped")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Sleep schedule service destroyed")
    }
}

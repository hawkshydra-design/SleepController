package com.sleepcontroller.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.sleepcontroller.di.ServiceEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*

/**
 * Handles the wake-up alarm trigger.
 * Plays alarm sound, vibrates with modern VibrationEffect API,
 * and shows a full-screen notification with dismiss/snooze actions.
 *
 * Updated: Alarm dismiss now delegates morning mode transition
 * to SessionManager instead of directly starting SleepScheduleService.
 */
class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        const val ACTION_DISMISS = "com.sleepcontroller.ACTION_ALARM_DISMISS"
        const val ACTION_SNOOZE = "com.sleepcontroller.ACTION_ALARM_SNOOZE"
        private const val SNOOZE_MINUTES = 5
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createAllChannels(this)
        startForeground(
            NotificationHelper.NOTIFICATION_ID_ALARM,
            NotificationHelper.buildAlarmNotification(this)
        )
        startAlarm()
        Log.d(TAG, "Alarm service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                Log.d(TAG, "Alarm dismissed")
                stopAlarm()
                // Delegate to SessionManager — single source of truth
                serviceScope.launch {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            applicationContext, ServiceEntryPoint::class.java
                        )
                        entryPoint.sessionManager().startMorningMode(this@AlarmService)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error transitioning to morning mode", e)
                    }
                }
                stopSelf()
            }
            ACTION_SNOOZE -> {
                Log.d(TAG, "Alarm snoozed for $SNOOZE_MINUTES minutes")
                stopAlarm()
                // Reschedule alarm in SNOOZE_MINUTES
                val calendar = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.MINUTE, SNOOZE_MINUTES)
                }
                com.sleepcontroller.util.AlarmUtils.scheduleAlarmTrigger(
                    this,
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE)
                )
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
        Log.d(TAG, "Alarm service stopped")
    }

    private fun startAlarm() {
        playSound()
        startVibration()
    }

    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun playSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Rhythmic vibration pattern: buzz-pause-buzz-pause-long buzz
            val pattern = longArrayOf(0, 500, 200, 500, 200, 800)
            val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = repeat from start
            vibrator?.vibrate(effect)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }
}

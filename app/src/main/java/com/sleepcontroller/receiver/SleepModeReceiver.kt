package com.sleepcontroller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleepcontroller.di.ServiceEntryPoint
import com.sleepcontroller.service.SleepScheduleService
import com.sleepcontroller.util.AlarmUtils
import com.sleepcontroller.util.TimeUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Receives scheduled alarms to toggle sleep mode on/off.
 * Triggered by AlarmManager at bedtime and wake-up time.
 * Also reschedules for the next enabled day after firing.
 *
 * Fixed: Uses goAsync() to prevent coroutine work from being killed,
 * and Hilt EntryPoint for proper DI.
 */
class SleepModeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepModeReceiver"
        const val ACTION_SLEEP_ON = "com.sleepcontroller.ACTION_SLEEP_MODE_ON"
        const val ACTION_SLEEP_OFF = "com.sleepcontroller.ACTION_SLEEP_MODE_OFF"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received: ${intent.action}")

        // goAsync() keeps the receiver alive while coroutines complete
        val pendingResult = goAsync()

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ServiceEntryPoint::class.java
        )
        val sessionManager = entryPoint.sessionManager()
        val scheduleRepository = entryPoint.scheduleRepository()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_SLEEP_ON -> {
                        val schedule = scheduleRepository.getActiveScheduleOnce()

                        if (schedule != null) {
                            val today = LocalDate.now().dayOfWeek.value // 1=Monday..7=Sunday
                            if (TimeUtils.isDayEnabled(today, schedule)) {
                                // Delegate to SessionManager — single source of truth
                                sessionManager.startSleepMode(context)
                                Log.d(TAG, "Sleep mode ON — day $today is enabled")
                            } else {
                                Log.d(TAG, "Sleep mode skipped — day $today is not enabled")
                            }

                            // Reschedule for tomorrow
                            AlarmUtils.scheduleSleepMode(
                                context, schedule.bedtimeHour, schedule.bedtimeMinute
                            )
                        }
                    }
                    ACTION_SLEEP_OFF -> {
                        // Delegate to SessionManager — single source of truth
                        sessionManager.startMorningMode(context)
                        Log.d(TAG, "Morning mode triggered")

                        // Reschedule wake-up for tomorrow
                        val schedule = scheduleRepository.getActiveScheduleOnce()
                        if (schedule != null) {
                            AlarmUtils.scheduleWakeUp(
                                context, schedule.wakeUpHour, schedule.wakeUpMinute
                            )
                            if (schedule.windDownMinutes > 0) {
                                AlarmUtils.scheduleWindDown(
                                    context,
                                    schedule.bedtimeHour,
                                    schedule.bedtimeMinute,
                                    schedule.windDownMinutes
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing ${intent.action}", e)
            } finally {
                // Signal that async work is done — prevents process kill
                pendingResult.finish()
            }
        }
    }
}

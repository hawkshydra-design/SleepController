package com.sleepcontroller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleepcontroller.di.ServiceEntryPoint
import com.sleepcontroller.util.AlarmUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules sleep mode alarms after device reboot.
 * Loads the active schedule from the database and reschedules all alarms.
 *
 * Fixed: Uses goAsync() and Hilt EntryPoint.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        Log.d(TAG, "Boot completed — rescheduling alarms")

        // goAsync() keeps the receiver alive while coroutines complete
        val pendingResult = goAsync()

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ServiceEntryPoint::class.java
        )
        val scheduleRepository = entryPoint.scheduleRepository()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedule = scheduleRepository.getActiveScheduleOnce()
                if (schedule != null && schedule.isActive) {
                    AlarmUtils.scheduleSleepMode(context, schedule.bedtimeHour, schedule.bedtimeMinute)
                    AlarmUtils.scheduleWakeUp(context, schedule.wakeUpHour, schedule.wakeUpMinute)
                    if (schedule.windDownMinutes > 0) {
                        AlarmUtils.scheduleWindDown(
                            context,
                            schedule.bedtimeHour,
                            schedule.bedtimeMinute,
                            schedule.windDownMinutes
                        )
                    }
                    Log.d(TAG, "Alarms rescheduled: bedtime ${schedule.bedtimeHour}:${schedule.bedtimeMinute}, wake ${schedule.wakeUpHour}:${schedule.wakeUpMinute}")
                } else {
                    Log.d(TAG, "No active schedule found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

package com.sleepcontroller.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.util.AlarmUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that schedules daily sleep/wake alarms.
 * This is a backup mechanism to ensure alarms are always set,
 * even if the app is killed or the user doesn't open the app.
 *
 * Updated: Uses ScheduleRepository instead of raw DAO for consistency.
 * Also schedules wind-down alarm which was previously missed.
 */
@HiltWorker
class SleepScheduleWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleRepository: ScheduleRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SleepScheduleWorker"
        const val WORK_NAME = "sleep_schedule_worker"
    }

    override suspend fun doWork(): Result {
        return try {
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
                Log.d(TAG, "All alarms scheduled successfully (bedtime, wake-up, wind-down)")
            } else {
                Log.d(TAG, "No active schedule found — skipping")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarms", e)
            Result.retry()
        }
    }
}

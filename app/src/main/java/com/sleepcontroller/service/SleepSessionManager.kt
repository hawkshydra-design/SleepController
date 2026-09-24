package com.sleepcontroller.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleepcontroller.data.db.entity.SleepHistory
import com.sleepcontroller.data.db.entity.SleepSchedule
import com.sleepcontroller.data.repository.HistoryRepository
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.domain.model.SessionState
import com.sleepcontroller.util.AlarmUtils
import com.sleepcontroller.util.TimeUtils
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the full sleep session lifecycle.
 *
 * This is the **single source of truth** for all session state mutations.
 * ViewModels, services, and receivers MUST delegate to this class — they
 * should never mutate session state (preferences) directly.
 *
 * Lifecycle:
 *  1. Schedule activation — sets AlarmManager triggers for bedtime/wake-up
 *  2. Sleep mode start — records session start, resets emergency unlock
 *  3. Morning mode — transition after alarm, app blocking continues until tasks completed
 *  4. Session end — records history, updates streak, cancels services
 *
 * Emergency unlock system:
 *  - 5 total unlocks per session
 *  - Uses 1-4: 20-min temporary access, then auto-returns to sleep via AlarmManager
 *  - Use 5: fully disables sleep mode for the rest of the night
 */
@Singleton
class SleepSessionManager @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val sessionRepository: SessionRepository,
    private val historyRepository: HistoryRepository
) {
    companion object {
        private const val TAG = "SleepSessionManager"
    }

    /**
     * Called when user saves a schedule. Sets up AlarmManager triggers for
     * bedtime, wake-up, and optional wind-down.
     */
    suspend fun activateSchedule(context: Context, schedule: SleepSchedule) {
        // Check if today is an enabled day
        val today = LocalDate.now().dayOfWeek
        if (!isDayEnabled(today, schedule)) {
            Log.d(TAG, "Schedule not active for today ($today)")
            // Still schedule for next enabled day
        }

        // Schedule bedtime alarm
        AlarmUtils.scheduleSleepMode(context, schedule.bedtimeHour, schedule.bedtimeMinute)
        Log.d(TAG, "Bedtime alarm set for ${schedule.bedtimeHour}:${schedule.bedtimeMinute}")

        // Schedule wake-up alarm
        AlarmUtils.scheduleWakeUp(context, schedule.wakeUpHour, schedule.wakeUpMinute)
        Log.d(TAG, "Wake-up alarm set for ${schedule.wakeUpHour}:${schedule.wakeUpMinute}")

        // Schedule wake-up system alarm (uses clock app)
        AlarmUtils.scheduleAlarmTrigger(context, schedule.wakeUpHour, schedule.wakeUpMinute)
        Log.d(TAG, "In-app alarm set for ${schedule.wakeUpHour}:${schedule.wakeUpMinute}")

        // Schedule wind-down notification
        if (schedule.windDownMinutes > 0) {
            AlarmUtils.scheduleWindDown(
                context,
                schedule.bedtimeHour,
                schedule.bedtimeMinute,
                schedule.windDownMinutes
            )
            Log.d(TAG, "Wind-down set ${schedule.windDownMinutes}min before bedtime")
        }
    }

    /**
     * Called when bedtime triggers. Starts sleep mode.
     * Also starts the foreground service to keep the process alive.
     */
    suspend fun startSleepMode(context: Context) {
        sessionRepository.startSleepMode()

        val serviceIntent = Intent(context, SleepScheduleService::class.java).apply {
            action = SleepScheduleService.ACTION_START_SLEEP
        }
        context.startForegroundService(serviceIntent)

        Log.d(TAG, "Sleep mode started")
    }

    /**
     * Called when wake-up triggers. Transitions to morning mode.
     */
    suspend fun startMorningMode(context: Context) {
        sessionRepository.startMorningMode()

        val serviceIntent = Intent(context, SleepScheduleService::class.java).apply {
            action = SleepScheduleService.ACTION_START_MORNING
        }
        context.startForegroundService(serviceIntent)

        Log.d(TAG, "Morning mode started — tasks required to unlock")
    }

    /**
     * Called when all morning tasks are completed or grace period expires.
     * Records sleep history and cleans up.
     */
    suspend fun endSession(context: Context, tasksCompleted: Int = 0, totalTasks: Int = 0) {
        val sessionStart = sessionRepository.currentSessionStart.first()
        val sessionEnd = System.currentTimeMillis()

        // Record to history
        if (sessionStart > 0) {
            val state = sessionRepository.getSessionStateOnce()
            val history = SleepHistory(
                date = TimeUtils.getStartOfDayMillis(),
                bedtimeActual = sessionStart,
                wakeUpActual = sessionEnd,
                sleepDurationMinutes = ((sessionEnd - sessionStart) / 60000).toInt(),
                tasksCompleted = tasksCompleted,
                totalTasks = totalTasks,
                emergencyUnlockUsed = state is SessionState.FullyUnlocked ||
                    state is SessionState.TemporaryUnlock
            )
            historyRepository.saveHistory(history)
            Log.d(TAG, "Session recorded: ${history.sleepDurationMinutes} min, tasks=$tasksCompleted/$totalTasks")

            // Update streak
            val currentStreak = sessionRepository.sleepStreak.first()
            sessionRepository.updateStreak(currentStreak + 1)
        }

        // Reset session state
        sessionRepository.resetSession()

        // Stop foreground service
        val stopIntent = Intent(context, SleepScheduleService::class.java).apply {
            action = SleepScheduleService.ACTION_STOP
        }
        context.startService(stopIntent)

        // Re-schedule for next cycle
        val schedule = scheduleRepository.getActiveScheduleOnce()
        if (schedule != null) {
            activateSchedule(context, schedule)
        }

        Log.d(TAG, "Session ended — next cycle scheduled")
    }

    /**
     * Emergency unlock — 5 uses per session, 20-min temporary access each.
     *
     * Uses 1-4: Blocking pauses for 20 minutes, then resumes automatically
     *           via EmergencyExpiryReceiver (scheduled by AlarmUtils).
     * Use 5:    Sleep mode fully disabled for the rest of the night.
     */
    suspend fun useEmergencyUnlock(context: Context): Boolean {
        val remaining = sessionRepository.useEmergencyUnlock()

        if (remaining > 0) {
            // Temporary unlock — schedule auto-return after 20 min
            AlarmUtils.scheduleEmergencyExpiry(context, 20)
            Log.d(TAG, "Emergency unlock used — 20 min access, $remaining remaining")
        } else {
            // 5th unlock — fully stop all services
            val stopIntent = Intent(context, SleepScheduleService::class.java).apply {
                action = SleepScheduleService.ACTION_STOP
            }
            context.startService(stopIntent)
            Log.d(TAG, "All 5 emergency unlocks used — sleep mode disabled for tonight")
        }

        // Dismiss any active overlay
        if (OverlayBlockerService.isShowing) {
            val dismissIntent = Intent(context, OverlayBlockerService::class.java).apply {
                action = OverlayBlockerService.ACTION_DISMISS
            }
            context.startService(dismissIntent)
        }

        return true
    }

    /**
     * Manual toggle from Dashboard.
     *
     * During Inactive: checks current time against schedule.
     *  - If within bedtime window → activates sleep mode immediately.
     *  - If outside window → schedules via AlarmManager only.
     *
     * During active session (Sleep/Morning): BLOCKED.
     *  User must use Emergency Unlock or complete morning tasks.
     */
    suspend fun manualToggle(context: Context) {
        val state = sessionRepository.getSessionStateOnce()

        if (state.isActive) {
            // BLOCKED: Cannot disable during active sleep/morning
            // User must use Emergency Unlock
            Log.d(TAG, "Manual disable blocked — use emergency unlock")
            return
        }

        // Check if current time is within the schedule's sleep window
        val schedule = scheduleRepository.getActiveScheduleOnce()
        if (schedule == null) {
            // No schedule set — activate immediately anyway
            startSleepMode(context)
            Log.d(TAG, "No schedule — activating sleep mode immediately")
            return
        }

        val now = LocalTime.now()
        val bedtime = LocalTime.of(schedule.bedtimeHour, schedule.bedtimeMinute)
        val wakeUp = LocalTime.of(schedule.wakeUpHour, schedule.wakeUpMinute)

        val isInSleepWindow = if (bedtime > wakeUp) {
            // Crosses midnight (e.g., 22:00 → 06:00)
            now >= bedtime || now <= wakeUp
        } else {
            // Same day (e.g., 01:00 → 06:00)
            now in bedtime..wakeUp
        }

        if (isInSleepWindow) {
            // Time matches — activate immediately
            startSleepMode(context)
            Log.d(TAG, "Within sleep window — activating immediately")
        } else {
            // Outside window — only schedule via AlarmManager
            activateSchedule(context, schedule)
            Log.d(TAG, "Outside sleep window — scheduled for ${schedule.bedtimeHour}:${schedule.bedtimeMinute}")
        }
    }

    private fun isDayEnabled(day: DayOfWeek, schedule: SleepSchedule): Boolean {
        return when (day) {
            DayOfWeek.MONDAY -> schedule.mondayEnabled
            DayOfWeek.TUESDAY -> schedule.tuesdayEnabled
            DayOfWeek.WEDNESDAY -> schedule.wednesdayEnabled
            DayOfWeek.THURSDAY -> schedule.thursdayEnabled
            DayOfWeek.FRIDAY -> schedule.fridayEnabled
            DayOfWeek.SATURDAY -> schedule.saturdayEnabled
            DayOfWeek.SUNDAY -> schedule.sundayEnabled
        }
    }
}

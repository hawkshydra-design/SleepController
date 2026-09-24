package com.sleepcontroller.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepcontroller.data.db.entity.SleepSchedule
import com.sleepcontroller.data.preferences.SleepPreferences
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.data.repository.TaskRepository
import com.sleepcontroller.domain.model.SessionState
import com.sleepcontroller.service.SleepSessionManager
import com.sleepcontroller.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val sessionState: SessionState = SessionState.Inactive,
    val schedule: SleepSchedule? = null,
    val sleepStreak: Int = 0,
    val minutesUntilBedtime: Long = 0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val bedtimeFormatted: String = "--:--",
    val wakeUpFormatted: String = "--:--",
    val sleepDurationFormatted: String = "--",
    val emergencyUnlockCount: Int = 0,
    val emergencyUnlockExpiresAt: Long = 0L
) {
    // Convenience properties derived from SessionState
    val isSleepModeActive: Boolean get() = sessionState is SessionState.Sleep
    val isMorningMode: Boolean get() = sessionState is SessionState.Morning
    val isTemporaryUnlock: Boolean get() = sessionState is SessionState.TemporaryUnlock
    val isFullyUnlocked: Boolean get() = sessionState is SessionState.FullyUnlocked
    val isAnyModeActive: Boolean get() = sessionState.isActive
    val isBlocking: Boolean get() = sessionState.isBlocking

    val remainingUnlocks: Int get() = SessionRepository.MAX_EMERGENCY_UNLOCKS - emergencyUnlockCount
    val hasUnlocksRemaining: Boolean get() = emergencyUnlockCount < SessionRepository.MAX_EMERGENCY_UNLOCKS
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val scheduleRepository: ScheduleRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository,
    private val sessionManager: SleepSessionManager,
    private val preferences: SleepPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeState()
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                scheduleRepository.getActiveSchedule(),
                sessionRepository.sessionState,
                sessionRepository.sleepStreak,
                taskRepository.getEnabledTasks(),
                sessionRepository.emergencyUnlockCount,
                sessionRepository.emergencyUnlockExpires
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val schedule = values[0] as SleepSchedule?
                val state = values[1] as SessionState
                val streak = values[2] as Int
                val tasks = values[3] as List<*>
                val unlockCount = values[4] as Int
                val unlockExpires = values[5] as Long

                val minutesUntil = if (schedule != null) {
                    TimeUtils.minutesUntilBedtime(schedule.bedtimeHour, schedule.bedtimeMinute)
                } else 0L

                DashboardUiState(
                    sessionState = state,
                    schedule = schedule,
                    sleepStreak = streak,
                    minutesUntilBedtime = minutesUntil,
                    totalTasks = tasks.size,
                    completedTasks = 0, // Tracked during active morning session
                    bedtimeFormatted = schedule?.let {
                        TimeUtils.formatTime(it.bedtimeHour, it.bedtimeMinute)
                    } ?: "--:--",
                    wakeUpFormatted = schedule?.let {
                        TimeUtils.formatTime(it.wakeUpHour, it.wakeUpMinute)
                    } ?: "--:--",
                    sleepDurationFormatted = schedule?.let {
                        TimeUtils.formatDuration(it.sleepDurationMinutes)
                    } ?: "--",
                    emergencyUnlockCount = unlockCount,
                    emergencyUnlockExpiresAt = unlockExpires
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Toggle sleep mode ON from dashboard.
     * During active session (Sleep/Morning), this is a NO-OP.
     * User must use Emergency Unlock or complete tasks.
     */
    fun toggleSleepMode() {
        viewModelScope.launch {
            sessionManager.manualToggle(getApplication())
        }
    }

    /**
     * Verify the entered PIN against stored hash, then trigger emergency unlock if valid.
     * Callback receives true on success, false on wrong PIN.
     */
    fun verifyPinAndUnlock(pin: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isValid = preferences.verifyPin(pin)
            if (isValid) {
                sessionManager.useEmergencyUnlock(getApplication())
            }
            callback(isValid)
        }
    }

    /**
     * Emergency unlock — 5 uses per session, 20-min temporary access each.
     * Delegates to SessionManager which handles service cleanup and alarm scheduling.
     */
    fun emergencyUnlock() {
        viewModelScope.launch {
            sessionManager.useEmergencyUnlock(getApplication())
        }
    }
}


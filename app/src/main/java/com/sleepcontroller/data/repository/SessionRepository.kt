package com.sleepcontroller.data.repository

import com.sleepcontroller.data.preferences.SleepPreferences
import com.sleepcontroller.domain.model.SessionState
import com.sleepcontroller.domain.repository.ISessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for sleep session state management.
 * Single source of truth for session state — replaces the scattered
 * boolean preference reads spread across services and ViewModels.
 *
 * All session state mutations MUST go through this repository.
 *
 * Emergency unlock system:
 *  - 5 total unlocks per sleep session
 *  - Uses 1-4: temporary 20-min access, then auto-returns to sleep
 *  - Use 5: fully disables sleep mode for the rest of the night
 */
@Singleton
class SessionRepository @Inject constructor(
    private val preferences: SleepPreferences
) : ISessionRepository {

    companion object {
        const val MAX_EMERGENCY_UNLOCKS = 5
        const val TEMPORARY_UNLOCK_DURATION_MS = 20 * 60 * 1000L  // 20 minutes
    }

    /**
     * Observe the current session state as a reactive [SessionState] sealed class.
     * Combines individual preferences into a single, type-safe state.
     *
     * State priority:
     *  1. FullyUnlocked (5 unlocks used) — highest priority
     *  2. TemporaryUnlock (active, not expired) — user is in a 20-min window
     *  3. Morning — wake-up alarm fired, tasks required
     *  4. Sleep — bedtime active, apps blocked
     *  5. Inactive — default
     */
    override val sessionState: Flow<SessionState> = combine(
        preferences.isSleepModeActive,
        preferences.isMorningModeActive,
        preferences.emergencyUnlockCount,
        preferences.emergencyUnlockExpires,
        preferences.currentSessionStart
    ) { sleepActive, morningActive, unlockCount, unlockExpires, sessionStart ->
        when {
            unlockCount >= MAX_EMERGENCY_UNLOCKS ->
                SessionState.FullyUnlocked(sessionStart)
            unlockCount > 0 && unlockExpires > System.currentTimeMillis() ->
                SessionState.TemporaryUnlock(unlockCount, unlockExpires)
            morningActive -> SessionState.Morning(sessionStart)
            sleepActive -> SessionState.Sleep(sessionStart)
            else -> SessionState.Inactive
        }
    }

    override val sleepStreak: Flow<Int> = preferences.sleepStreak
    override val currentSessionStart: Flow<Long> = preferences.currentSessionStart
    override val emergencyUnlockCount: Flow<Int> = preferences.emergencyUnlockCount
    override val emergencyUnlockExpires: Flow<Long> = preferences.emergencyUnlockExpires

    /** Transition to Sleep state. */
    override suspend fun startSleepMode() {
        preferences.setSleepModeActive(true)
        preferences.setMorningModeActive(false)
        preferences.setCurrentSessionStart(System.currentTimeMillis())
    }

    /** Transition from Sleep to Morning state. */
    override suspend fun startMorningMode() {
        preferences.setSleepModeActive(false)
        preferences.setMorningModeActive(true)
    }

    /**
     * Use one emergency unlock.
     * @return remaining unlocks (5 - newCount). 0 means all used.
     *
     * Uses 1-4: caller should schedule a 20-min expiry alarm.
     * Use 5: caller should stop all blocking services.
     */
    override suspend fun useEmergencyUnlock(): Int {
        val count = preferences.emergencyUnlockCount.first()
        if (count >= MAX_EMERGENCY_UNLOCKS) return 0

        val newCount = count + 1
        preferences.incrementEmergencyUnlock()

        if (newCount >= MAX_EMERGENCY_UNLOCKS) {
            // 5th unlock — fully disable for the night
            preferences.setSleepModeActive(false)
            preferences.setMorningModeActive(false)
        } else {
            // Uses 1-4: set 20-min temporary unlock expiry
            val expiresAt = System.currentTimeMillis() + TEMPORARY_UNLOCK_DURATION_MS
            preferences.setEmergencyUnlockExpiry(expiresAt)
        }

        return MAX_EMERGENCY_UNLOCKS - newCount
    }

    /** Set the emergency unlock expiry timestamp. */
    override suspend fun setEmergencyUnlockExpiry(expiresAt: Long) {
        preferences.setEmergencyUnlockExpiry(expiresAt)
    }

    /** Whether any emergency unlocks remain. */
    override suspend fun isEmergencyUnlockAvailable(): Boolean {
        return preferences.emergencyUnlockCount.first() < MAX_EMERGENCY_UNLOCKS
    }

    /** Reset all session state back to Inactive. */
    override suspend fun resetSession() {
        preferences.resetSession()
    }

    /** Update the sleep streak counter. */
    override suspend fun updateStreak(streak: Int) {
        preferences.setSleepStreak(streak)
    }

    /** Get the current session state snapshot (non-reactive). */
    override suspend fun getSessionStateOnce(): SessionState {
        return sessionState.first()
    }
}

package com.sleepcontroller.domain.repository

import com.sleepcontroller.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

/**
 * Contract for session state management.
 * Single source of truth for all session state reads and mutations.
 */
interface ISessionRepository {
    val sessionState: Flow<SessionState>
    val sleepStreak: Flow<Int>
    val currentSessionStart: Flow<Long>
    val emergencyUnlockCount: Flow<Int>
    val emergencyUnlockExpires: Flow<Long>

    suspend fun startSleepMode()
    suspend fun startMorningMode()

    /**
     * Use one emergency unlock. Returns remaining unlocks (0 if none left).
     * Uses 1-4: temporary 20-min access.
     * Use 5: fully disables sleep mode for the night.
     */
    suspend fun useEmergencyUnlock(): Int

    suspend fun setEmergencyUnlockExpiry(expiresAt: Long)
    suspend fun isEmergencyUnlockAvailable(): Boolean
    suspend fun resetSession()
    suspend fun updateStreak(streak: Int)
    suspend fun getSessionStateOnce(): SessionState
}

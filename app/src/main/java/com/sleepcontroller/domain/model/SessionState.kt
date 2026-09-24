package com.sleepcontroller.domain.model

/**
 * Single source of truth for the current sleep session state.
 *
 * Replaces the previous 3 independent booleans (isSleepModeActive,
 * isMorningModeActive, isEmergencyUnlockUsed) which could represent
 * 8 possible combinations — most of which were illegal.
 *
 * This sealed class guarantees only valid state transitions.
 */
sealed class SessionState {

    /** No active sleep session. Normal phone usage. */
    data object Inactive : SessionState()

    /** Sleep mode is active — apps are blocked, user should be sleeping. */
    data class Sleep(
        val startedAt: Long = System.currentTimeMillis()
    ) : SessionState()

    /** Morning mode — alarm has fired, tasks must be completed to unlock. */
    data class Morning(
        val startedAt: Long = System.currentTimeMillis()
    ) : SessionState()

    /** Temporary unlock — 20 min access, then returns to sleep/morning. */
    data class TemporaryUnlock(
        val usedCount: Int,
        val expiresAt: Long
    ) : SessionState()

    /** All 5 emergency unlocks used — session fully disabled for the night. */
    data class FullyUnlocked(
        val at: Long = System.currentTimeMillis()
    ) : SessionState()

    /** Whether apps should currently be blocked. */
    val isBlocking: Boolean
        get() = this is Sleep || this is Morning

    /** Whether the user is in any active session (excluding fully unlocked). */
    val isActive: Boolean
        get() = this !is Inactive && this !is FullyUnlocked
}

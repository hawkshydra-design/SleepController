package com.sleepcontroller.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SessionState sealed class.
 * Verifies state properties, blocking behavior, and transition safety.
 */
class SessionStateTest {

    @Test
    fun `Inactive state - isActive should be false`() {
        val state = SessionState.Inactive
        // Inactive is NOT active (isActive = this !is Inactive → false)
        assertFalse(state.isActive)
    }

    @Test
    fun `Sleep state - isActive should be true`() {
        val state = SessionState.Sleep(startedAt = System.currentTimeMillis())
        assertTrue(state.isActive)
    }

    @Test
    fun `Morning state - isActive should be true`() {
        val state = SessionState.Morning(startedAt = System.currentTimeMillis())
        assertTrue(state.isActive)
    }

    @Test
    fun `EmergencyUnlocked state - isActive should be true`() {
        // EmergencyUnlocked IS active (isActive = this !is Inactive → true)
        val state = SessionState.EmergencyUnlocked(at = System.currentTimeMillis())
        assertTrue(state.isActive)
    }

    // ═══════════════════════════════════════
    // Blocking behavior
    // ═══════════════════════════════════════

    @Test
    fun `Inactive state - isBlocking should be false`() {
        assertFalse(SessionState.Inactive.isBlocking)
    }

    @Test
    fun `Sleep state - isBlocking should be true`() {
        assertTrue(SessionState.Sleep().isBlocking)
    }

    @Test
    fun `Morning state - isBlocking should be true`() {
        assertTrue(SessionState.Morning().isBlocking)
    }

    @Test
    fun `EmergencyUnlocked state - isBlocking should be false`() {
        assertFalse(SessionState.EmergencyUnlocked().isBlocking)
    }

    // ═══════════════════════════════════════
    // Timestamp preservation
    // ═══════════════════════════════════════

    @Test
    fun `Sleep state preserves startedAt timestamp`() {
        val timestamp = 1700000000000L
        val state = SessionState.Sleep(startedAt = timestamp)
        assertEquals(timestamp, state.startedAt)
    }

    @Test
    fun `Morning state preserves startedAt timestamp`() {
        val timestamp = 1700000000000L
        val state = SessionState.Morning(startedAt = timestamp)
        assertEquals(timestamp, state.startedAt)
    }

    @Test
    fun `EmergencyUnlocked preserves at timestamp`() {
        val timestamp = 1700000000000L
        val state = SessionState.EmergencyUnlocked(at = timestamp)
        assertEquals(timestamp, state.at)
    }

    // ═══════════════════════════════════════
    // Equality
    // ═══════════════════════════════════════

    @Test
    fun `two Inactive states are equal`() {
        assertEquals(SessionState.Inactive, SessionState.Inactive)
    }

    @Test
    fun `Sleep with same timestamp are equal`() {
        val ts = 1700000000000L
        assertEquals(SessionState.Sleep(ts), SessionState.Sleep(ts))
    }

    @Test
    fun `Sleep and Morning with same timestamp are not equal`() {
        val ts = 1700000000000L
        assertNotEquals(SessionState.Sleep(ts), SessionState.Morning(ts))
    }

    // ═══════════════════════════════════════
    // Exhaustive matching
    // ═══════════════════════════════════════

    @Test
    fun `exhaustive when covers all states`() {
        val states = listOf(
            SessionState.Inactive,
            SessionState.Sleep(0L),
            SessionState.Morning(0L),
            SessionState.EmergencyUnlocked(0L)
        )

        states.forEach { state ->
            val label = when (state) {
                is SessionState.Inactive -> "inactive"
                is SessionState.Sleep -> "sleep"
                is SessionState.Morning -> "morning"
                is SessionState.EmergencyUnlocked -> "emergency"
            }
            assertTrue(label.isNotEmpty())
        }
    }
}

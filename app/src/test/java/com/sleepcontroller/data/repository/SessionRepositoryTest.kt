package com.sleepcontroller.data.repository

import com.sleepcontroller.domain.model.SessionState
import com.sleepcontroller.domain.repository.ISessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ISessionRepository contract.
 * Uses a FakeSessionRepository backed by in-memory state flows.
 *
 * This validates the contract any SessionRepository implementation
 * must satisfy, and can be reused to test the real implementation
 * with instrumented tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {

    private lateinit var repository: FakeSessionRepository

    @Before
    fun setup() {
        repository = FakeSessionRepository()
    }

    @Test
    fun `initial state is Inactive`() = runTest {
        val state = repository.sessionState.first()
        assertEquals(SessionState.Inactive, state)
    }

    @Test
    fun `startSleepMode transitions to Sleep`() = runTest {
        repository.startSleepMode()
        val state = repository.sessionState.first()
        assertTrue(state is SessionState.Sleep)
    }

    @Test
    fun `startSleepMode resets emergency unlock`() = runTest {
        repository.useEmergencyUnlock()
        repository.startSleepMode()
        assertTrue(repository.isEmergencyUnlockAvailable())
    }

    @Test
    fun `startMorningMode transitions from Sleep to Morning`() = runTest {
        repository.startSleepMode()
        repository.startMorningMode()
        val state = repository.sessionState.first()
        assertTrue(state is SessionState.Morning)
    }

    @Test
    fun `useEmergencyUnlock transitions to EmergencyUnlocked`() = runTest {
        repository.startSleepMode()
        val result = repository.useEmergencyUnlock()
        assertTrue(result)
        val state = repository.sessionState.first()
        assertTrue(state is SessionState.EmergencyUnlocked)
    }

    @Test
    fun `useEmergencyUnlock returns false on second attempt`() = runTest {
        repository.startSleepMode()
        repository.useEmergencyUnlock()
        val secondAttempt = repository.useEmergencyUnlock()
        assertFalse(secondAttempt)
    }

    @Test
    fun `isEmergencyUnlockAvailable true initially`() = runTest {
        assertTrue(repository.isEmergencyUnlockAvailable())
    }

    @Test
    fun `isEmergencyUnlockAvailable false after use`() = runTest {
        repository.startSleepMode()
        repository.useEmergencyUnlock()
        assertFalse(repository.isEmergencyUnlockAvailable())
    }

    @Test
    fun `resetSession returns to Inactive`() = runTest {
        repository.startSleepMode()
        repository.resetSession()
        val state = repository.sessionState.first()
        assertEquals(SessionState.Inactive, state)
    }

    @Test
    fun `resetSession clears emergency unlock`() = runTest {
        repository.startSleepMode()
        repository.useEmergencyUnlock()
        repository.resetSession()
        assertTrue(repository.isEmergencyUnlockAvailable())
    }

    @Test
    fun `updateStreak persists value`() = runTest {
        repository.updateStreak(7)
        assertEquals(7, repository.sleepStreak.first())
    }

    @Test
    fun `getSessionStateOnce matches flow emission`() = runTest {
        repository.startSleepMode()
        val snapshot = repository.getSessionStateOnce()
        val flowState = repository.sessionState.first()
        assertEquals(flowState, snapshot)
    }

    @Test
    fun `Sleep state isBlocking is true`() = runTest {
        repository.startSleepMode()
        val state = repository.sessionState.first()
        assertTrue(state.isBlocking)
    }

    @Test
    fun `Morning state isBlocking is true`() = runTest {
        repository.startSleepMode()
        repository.startMorningMode()
        val state = repository.sessionState.first()
        assertTrue(state.isBlocking)
    }

    @Test
    fun `EmergencyUnlocked state isBlocking is false`() = runTest {
        repository.startSleepMode()
        repository.useEmergencyUnlock()
        val state = repository.sessionState.first()
        assertFalse(state.isBlocking)
    }

    @Test
    fun `Inactive state isBlocking is false`() = runTest {
        val state = repository.sessionState.first()
        assertFalse(state.isBlocking)
    }
}

/**
 * In-memory fake implementation of [ISessionRepository].
 * Mirrors the real SessionRepository behavior without DataStore.
 */
class FakeSessionRepository : ISessionRepository {

    private val _state = MutableStateFlow<SessionState>(SessionState.Inactive)
    private val _streak = MutableStateFlow(0)
    private val _sessionStart = MutableStateFlow(0L)
    private var _emergencyUsed = false

    override val sessionState: Flow<SessionState> = _state
    override val sleepStreak: Flow<Int> = _streak
    override val currentSessionStart: Flow<Long> = _sessionStart

    override suspend fun startSleepMode() {
        _emergencyUsed = false
        val now = System.currentTimeMillis()
        _sessionStart.value = now
        _state.value = SessionState.Sleep(now)
    }

    override suspend fun startMorningMode() {
        _state.value = SessionState.Morning(_sessionStart.value)
    }

    override suspend fun useEmergencyUnlock(): Boolean {
        if (_emergencyUsed) return false
        _emergencyUsed = true
        _state.value = SessionState.EmergencyUnlocked(_sessionStart.value)
        return true
    }

    override suspend fun isEmergencyUnlockAvailable(): Boolean = !_emergencyUsed

    override suspend fun resetSession() {
        _emergencyUsed = false
        _sessionStart.value = 0L
        _state.value = SessionState.Inactive
    }

    override suspend fun updateStreak(streak: Int) {
        _streak.value = streak
    }

    override suspend fun getSessionStateOnce(): SessionState = _state.value
}

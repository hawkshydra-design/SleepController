package com.sleepcontroller.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sleep_preferences")

class SleepPreferences(private val context: Context) {

    companion object {
        val SLEEP_MODE_ACTIVE = booleanPreferencesKey("sleep_mode_active")
        val MORNING_MODE_ACTIVE = booleanPreferencesKey("morning_mode_active")
        val EMERGENCY_UNLOCK_COUNT = intPreferencesKey("emergency_unlock_count")
        val EMERGENCY_UNLOCK_EXPIRES = longPreferencesKey("emergency_unlock_expires")
        val CURRENT_SESSION_DATE = longPreferencesKey("current_session_date")
        val OVERRIDE_PIN = stringPreferencesKey("override_pin")
        val OVERRIDE_PIN_SALT = stringPreferencesKey("override_pin_salt")
        val PIN_CONFIGURED = booleanPreferencesKey("pin_configured")
        val WIND_DOWN_MINUTES = intPreferencesKey("wind_down_minutes")
        val ALARM_SOUND_URI = stringPreferencesKey("alarm_sound_uri")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val GRACE_PERIOD_MINUTES = intPreferencesKey("grace_period_minutes")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val SLEEP_STREAK = intPreferencesKey("sleep_streak")
        val CURRENT_SESSION_START = longPreferencesKey("current_session_start")

        /**
         * Hash a PIN with the given salt using SHA-256.
         * Returns the hex-encoded hash string.
         */
        fun hashPin(pin: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = (salt + pin).toByteArray(Charsets.UTF_8)
            val hashBytes = digest.digest(input)
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        /**
         * Generate a cryptographically secure random salt (16 bytes, hex-encoded).
         */
        fun generateSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    val isSleepModeActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SLEEP_MODE_ACTIVE] ?: false
    }

    val isMorningModeActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[MORNING_MODE_ACTIVE] ?: false
    }

    val emergencyUnlockCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[EMERGENCY_UNLOCK_COUNT] ?: 0
    }

    val emergencyUnlockExpires: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[EMERGENCY_UNLOCK_EXPIRES] ?: 0L
    }

    /**
     * Whether the user has configured a custom PIN (not using default).
     */
    val isPinConfigured: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PIN_CONFIGURED] ?: false
    }

    val sleepStreak: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SLEEP_STREAK] ?: 0
    }

    val currentSessionStart: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[CURRENT_SESSION_START] ?: 0L
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FIRST_LAUNCH] ?: true
    }

    val gracePeriodMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[GRACE_PERIOD_MINUTES] ?: 60
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setSleepModeActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SLEEP_MODE_ACTIVE] = active
        }
    }

    suspend fun setMorningModeActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[MORNING_MODE_ACTIVE] = active
        }
    }

    suspend fun incrementEmergencyUnlock() {
        context.dataStore.edit { prefs ->
            prefs[EMERGENCY_UNLOCK_COUNT] = (prefs[EMERGENCY_UNLOCK_COUNT] ?: 0) + 1
        }
    }

    suspend fun setEmergencyUnlockExpiry(expiresAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[EMERGENCY_UNLOCK_EXPIRES] = expiresAt
        }
    }

    /**
     * Store the PIN as a SHA-256 hash with a random salt.
     * The plaintext PIN is never persisted.
     */
    suspend fun setOverridePin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        context.dataStore.edit { prefs ->
            prefs[OVERRIDE_PIN] = hash
            prefs[OVERRIDE_PIN_SALT] = salt
            prefs[PIN_CONFIGURED] = true
        }
    }

    /**
     * Verify a PIN attempt against the stored hash.
     * Returns true if the PIN matches, false otherwise.
     * Returns true if no PIN is configured (graceful fallback).
     */
    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[OVERRIDE_PIN] ?: return true // No PIN set
        val storedSalt = prefs[OVERRIDE_PIN_SALT] ?: return true // No salt = legacy

        val attemptHash = hashPin(pin, storedSalt)
        return attemptHash == storedHash
    }

    suspend fun setSleepStreak(streak: Int) {
        context.dataStore.edit { prefs ->
            prefs[SLEEP_STREAK] = streak
        }
    }

    suspend fun setCurrentSessionStart(millis: Long) {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_SESSION_START] = millis
        }
    }

    suspend fun setFirstLaunch(first: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[FIRST_LAUNCH] = first
        }
    }

    suspend fun setGracePeriodMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[GRACE_PERIOD_MINUTES] = minutes
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun resetSession() {
        context.dataStore.edit { prefs ->
            prefs[EMERGENCY_UNLOCK_COUNT] = 0
            prefs[EMERGENCY_UNLOCK_EXPIRES] = 0L
            prefs[MORNING_MODE_ACTIVE] = false
            prefs[SLEEP_MODE_ACTIVE] = false
            prefs[CURRENT_SESSION_START] = 0L
        }
    }
}

package com.sleepcontroller.ui.screens.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepcontroller.data.preferences.SleepPreferences
import com.sleepcontroller.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isPinConfigured: Boolean = false,
    val gracePeriodMinutes: Int = 60,
    val notificationsEnabled: Boolean = true,
    val pinSaveSuccess: Boolean? = null,
    val permissionStatus: PermissionUtils.PermissionStatus = PermissionUtils.PermissionStatus(
        usageStats = false,
        overlay = false,
        accessibility = false,
        batteryOptimization = false
    )
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: SleepPreferences,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                preferences.isPinConfigured,
                preferences.gracePeriodMinutes,
                preferences.notificationsEnabled
            ) { pinConfigured, grace, notifications ->
                SettingsUiState(
                    isPinConfigured = pinConfigured,
                    gracePeriodMinutes = grace,
                    notificationsEnabled = notifications,
                    permissionStatus = PermissionUtils.checkAllPermissions(application)
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(permissionStatus = PermissionUtils.checkAllPermissions(application))
        }
    }

    /**
     * Save a new PIN (hashed with SHA-256 + salt).
     * The plaintext PIN is never stored.
     */
    fun updatePin(newPin: String) {
        viewModelScope.launch {
            preferences.setOverridePin(newPin)
            _uiState.update { it.copy(pinSaveSuccess = true, isPinConfigured = true) }
        }
    }

    fun clearPinSaveStatus() {
        _uiState.update { it.copy(pinSaveSuccess = null) }
    }

    fun updateGracePeriod(minutes: Int) {
        viewModelScope.launch {
            preferences.setGracePeriodMinutes(minutes)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationsEnabled(enabled)
        }
    }

    fun openUsageStatsSettings() = PermissionUtils.openUsageStatsSettings(application)
    fun openOverlaySettings() = PermissionUtils.openOverlaySettings(application)
    fun openAccessibilitySettings() = PermissionUtils.openAccessibilitySettings(application)
    fun openBatterySettings() = PermissionUtils.openBatteryOptimizationSettings(application)
}

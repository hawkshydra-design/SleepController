package com.sleepcontroller.ui.screens.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepcontroller.data.db.entity.SleepSchedule
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.service.SleepSessionManager
import com.sleepcontroller.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val bedtimeHour: Int = 22,
    val bedtimeMinute: Int = 0,
    val sleepDurationMinutes: Int = 480,
    val wakeUpHour: Int = 6,
    val wakeUpMinute: Int = 0,
    val daysEnabled: List<Boolean> = List(7) { true },
    val windDownMinutes: Int = 30,
    val isSaved: Boolean = false,
    val isScheduleActive: Boolean = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    application: Application,
    private val repository: ScheduleRepository,
    private val sessionManager: SleepSessionManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            repository.getActiveSchedule().collect { schedule ->
                schedule?.let {
                    _uiState.value = ScheduleUiState(
                        bedtimeHour = it.bedtimeHour,
                        bedtimeMinute = it.bedtimeMinute,
                        sleepDurationMinutes = it.sleepDurationMinutes,
                        wakeUpHour = it.wakeUpHour,
                        wakeUpMinute = it.wakeUpMinute,
                        daysEnabled = listOf(
                            it.mondayEnabled, it.tuesdayEnabled, it.wednesdayEnabled,
                            it.thursdayEnabled, it.fridayEnabled, it.saturdayEnabled,
                            it.sundayEnabled
                        ),
                        windDownMinutes = it.windDownMinutes,
                        isScheduleActive = it.isActive
                    )
                }
            }
        }
    }

    fun updateBedtime(hour: Int, minute: Int) {
        val (wakeHour, wakeMinute) = TimeUtils.calculateWakeUpTime(
            hour, minute, _uiState.value.sleepDurationMinutes
        )
        _uiState.update {
            it.copy(
                bedtimeHour = hour,
                bedtimeMinute = minute,
                wakeUpHour = wakeHour,
                wakeUpMinute = wakeMinute,
                isSaved = false
            )
        }
    }

    fun updateSleepDuration(minutes: Int) {
        val clamped = minutes.coerceIn(360, 600) // 6h to 10h — expanded range
        val (wakeHour, wakeMinute) = TimeUtils.calculateWakeUpTime(
            _uiState.value.bedtimeHour, _uiState.value.bedtimeMinute, clamped
        )
        _uiState.update {
            it.copy(
                sleepDurationMinutes = clamped,
                wakeUpHour = wakeHour,
                wakeUpMinute = wakeMinute,
                isSaved = false
            )
        }
    }

    fun toggleDay(index: Int) {
        _uiState.update {
            val newDays = it.daysEnabled.toMutableList()
            newDays[index] = !newDays[index]
            it.copy(daysEnabled = newDays, isSaved = false)
        }
    }

    fun updateWindDown(minutes: Int) {
        _uiState.update { it.copy(windDownMinutes = minutes, isSaved = false) }
    }

    fun saveSchedule() {
        viewModelScope.launch {
            val state = _uiState.value
            val schedule = SleepSchedule(
                bedtimeHour = state.bedtimeHour,
                bedtimeMinute = state.bedtimeMinute,
                sleepDurationMinutes = state.sleepDurationMinutes,
                wakeUpHour = state.wakeUpHour,
                wakeUpMinute = state.wakeUpMinute,
                mondayEnabled = state.daysEnabled[0],
                tuesdayEnabled = state.daysEnabled[1],
                wednesdayEnabled = state.daysEnabled[2],
                thursdayEnabled = state.daysEnabled[3],
                fridayEnabled = state.daysEnabled[4],
                saturdayEnabled = state.daysEnabled[5],
                sundayEnabled = state.daysEnabled[6],
                windDownMinutes = state.windDownMinutes,
                isActive = true
            )
            repository.saveSchedule(schedule)

            // Schedule AlarmManager triggers via SessionManager
            sessionManager.activateSchedule(getApplication(), schedule)

            _uiState.update { it.copy(isSaved = true, isScheduleActive = true) }
        }
    }
}

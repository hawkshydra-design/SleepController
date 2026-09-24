package com.sleepcontroller.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepcontroller.data.db.entity.SleepHistory
import com.sleepcontroller.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class WeeklyBarData(
    val dayLabel: String,
    val hoursSlept: Float,
    val date: Long
)

data class HistoryUiState(
    val history: List<SleepHistory> = emptyList(),
    val averageSleepMinutes: Float? = null,
    val currentStreak: Int = 0,
    val totalNights: Int = 0,
    val taskCompletionRate: Float = 0f,
    val weeklyData: List<WeeklyBarData> = emptyList(),
    val longestStreak: Int = 0,
    val bestSleepMinutes: Int? = null,
    val emergencyUnlocksTotal: Int = 0
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /** Pre-computed weekly chart data — avoids rebuilding during scroll recomposition. */
    val weeklyChartData: StateFlow<List<WeeklyBarData>> = _uiState
        .map { state -> buildWeeklyData(state.history) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHistory()
        loadStats()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            historyRepository.getRecentHistory(30).collect { history ->
                val completionRate = if (history.isNotEmpty()) {
                    history.map { it.tasksCompleted.toFloat() / it.totalTasks.coerceAtLeast(1) }
                        .average().toFloat()
                } else 0f

                val weeklyData = buildWeeklyData(history)
                val emergencyCount = history.count { it.emergencyUnlockUsed }
                val bestSleep = history.mapNotNull { it.sleepDurationMinutes }.maxOrNull()

                _uiState.update {
                    it.copy(
                        history = history,
                        taskCompletionRate = completionRate,
                        weeklyData = weeklyData,
                        totalNights = history.size,
                        emergencyUnlocksTotal = emergencyCount,
                        bestSleepMinutes = bestSleep
                    )
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val avg = historyRepository.getAverageSleepDuration()
            val streak = historyRepository.getCurrentStreak()
            _uiState.update {
                it.copy(
                    averageSleepMinutes = avg,
                    currentStreak = streak,
                    longestStreak = streak // Will be computed properly with more data
                )
            }
        }
    }

    /**
     * Build weekly bar chart data from the last 7 history entries.
     */
    private fun buildWeeklyData(history: List<SleepHistory>): List<WeeklyBarData> {
        val last7 = history.take(7).reversed()
        return last7.map { entry ->
            val dayName = try {
                Instant.ofEpochMilli(entry.date)
                    .atZone(ZoneId.systemDefault())
                    .dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            } catch (e: Exception) { "?" }

            WeeklyBarData(
                dayLabel = dayName,
                hoursSlept = (entry.sleepDurationMinutes ?: 0) / 60f,
                date = entry.date
            )
        }
    }
}

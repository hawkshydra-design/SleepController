package com.sleepcontroller.ui.screens.tasks

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepcontroller.data.db.entity.MorningTask
import com.sleepcontroller.data.db.entity.TaskType
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.data.repository.TaskRepository
import com.sleepcontroller.domain.model.SessionState
import com.sleepcontroller.service.SleepSessionManager
import com.sleepcontroller.util.ExifVerifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TaskCompletionState(
    val taskId: Long,
    val isCompleted: Boolean = false,
    val timerSecondsRemaining: Int? = null,
    val timerRunning: Boolean = false,
    val photoUri: Uri? = null,
    val photoVerified: Boolean = false,
    val verificationMessage: String = "",
    val cooldownSecondsRemaining: Int? = null  // Checkbox cooldown tracker
)

data class TasksUiState(
    val tasks: List<MorningTask> = emptyList(),
    val completionStates: Map<Long, TaskCompletionState> = emptyMap(),
    val showAddDialog: Boolean = false,
    val editingTask: MorningTask? = null,
    val isMorningModeActive: Boolean = false,
    val allTasksCompleted: Boolean = false,
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

@HiltViewModel
class MorningTasksViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val sessionManager: SleepSessionManager
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MorningTasksVM"
        private const val CHECKBOX_COOLDOWN_MS = 30_000L  // 30 seconds between checkbox completions
    }

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val timerJobs = mutableMapOf<Long, Job>()
    private val cooldownJobs = mutableMapOf<Long, Job>()
    private var lastCheckboxCompletionTime = 0L

    /**
     * Pre-computed enabled tasks — avoids filtering inside LazyColumn during scroll.
     */
    val enabledTasks: StateFlow<List<MorningTask>> = _uiState
        .map { state -> state.tasks.filter { it.isEnabled } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Pre-computed disabled tasks — avoids filtering inside LazyColumn during scroll.
     */
    val disabledTasks: StateFlow<List<MorningTask>> = _uiState
        .map { state -> state.tasks.filter { !it.isEnabled } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            combine(
                repository.getAllTasks(),
                sessionRepository.sessionState
            ) { tasks, state ->
                val isMorning = state is SessionState.Morning
                val enabledTasks = tasks.filter { it.isEnabled }
                val states = _uiState.value.completionStates.toMutableMap()

                // Initialize states for new tasks
                enabledTasks.forEach { task ->
                    if (!states.containsKey(task.id)) {
                        states[task.id] = TaskCompletionState(
                            taskId = task.id,
                            timerSecondsRemaining = if (task.type == TaskType.TIMED) task.timedDurationSeconds else null
                        )
                    }
                }

                val completedCount = states.values.count { it.isCompleted }
                val allDone = enabledTasks.isNotEmpty() && completedCount >= enabledTasks.size

                _uiState.update {
                    it.copy(
                        tasks = tasks,
                        completionStates = states,
                        isMorningModeActive = isMorning,
                        allTasksCompleted = allDone,
                        completedCount = completedCount,
                        totalCount = enabledTasks.size
                    )
                }

                // If all tasks done and morning mode is active, end session
                if (allDone && isMorning) {
                    sessionManager.endSession(getApplication(), completedCount, enabledTasks.size)
                }
            }.collect()
        }
    }

    // ═══════════════════════════════════════
    // Task Completion (Morning Mode)
    // ═══════════════════════════════════════

    /**
     * Complete a CHECKBOX task with 30-second cooldown.
     * Prevents spam-tapping "Done" to skip morning mode instantly.
     */
    fun completeCheckboxTask(taskId: Long) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCheckboxCompletionTime

        if (elapsed < CHECKBOX_COOLDOWN_MS && lastCheckboxCompletionTime > 0) {
            // Too soon — show cooldown timer
            val remaining = ((CHECKBOX_COOLDOWN_MS - elapsed) / 1000).toInt()
            updateCompletionState(taskId) { it.copy(cooldownSecondsRemaining = remaining) }
            startCooldownTicker(taskId, remaining)
            Log.d(TAG, "Checkbox cooldown: $remaining seconds remaining for task $taskId")
            return
        }

        lastCheckboxCompletionTime = now
        updateCompletionState(taskId) { it.copy(isCompleted = true, cooldownSecondsRemaining = null) }
        recheckCompletion()
        Log.d(TAG, "Checkbox task $taskId completed")
    }

    /**
     * Tick down the cooldown timer every second, updating the UI.
     */
    private fun startCooldownTicker(taskId: Long, startSeconds: Int) {
        cooldownJobs[taskId]?.cancel()
        cooldownJobs[taskId] = viewModelScope.launch {
            var remaining = startSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                updateCompletionState(taskId) { it.copy(cooldownSecondsRemaining = remaining) }
            }
            // Cooldown finished — button re-enables
            updateCompletionState(taskId) { it.copy(cooldownSecondsRemaining = null) }
        }
    }

    /** Start a TIMED task countdown */
    fun startTimer(taskId: Long) {
        val task = _uiState.value.tasks.find { it.id == taskId } ?: return
        val duration = task.timedDurationSeconds

        updateCompletionState(taskId) {
            it.copy(timerRunning = true, timerSecondsRemaining = duration)
        }

        timerJobs[taskId]?.cancel()
        timerJobs[taskId] = viewModelScope.launch {
            var remaining = duration
            while (remaining > 0) {
                delay(1000)
                remaining--
                updateCompletionState(taskId) {
                    it.copy(timerSecondsRemaining = remaining)
                }
            }
            // Timer completed
            updateCompletionState(taskId) {
                it.copy(isCompleted = true, timerRunning = false, timerSecondsRemaining = 0)
            }
            recheckCompletion()
        }
    }

    /**
     * Submit a photo for EXIF + face detection verification.
     * After verification, the photo file is DELETED for privacy.
     */
    fun submitPhoto(taskId: Long, photoUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = ExifVerifier.verifyPhoto(context, photoUri)

            updateCompletionState(taskId) {
                it.copy(
                    photoUri = photoUri,
                    photoVerified = result.isValid,
                    isCompleted = result.isValid,
                    verificationMessage = result.reason
                )
            }

            // SECURITY: Delete photo after verification — we only keep the result
            try {
                context.contentResolver.delete(photoUri, null, null)
            } catch (_: Exception) {
                // Fallback: delete file directly
                try {
                    val path = photoUri.path
                    if (path != null) File(path).delete()
                } catch (_: Exception) { }
            }
            Log.d(TAG, "Photo verified (valid=${result.isValid}), file deleted for privacy")

            if (result.isValid) {
                recheckCompletion()
            }
        }
    }

    private fun recheckCompletion() {
        val enabledTasks = _uiState.value.tasks.filter { it.isEnabled }
        val states = _uiState.value.completionStates
        val completedCount = enabledTasks.count { states[it.id]?.isCompleted == true }
        val allDone = enabledTasks.isNotEmpty() && completedCount >= enabledTasks.size

        _uiState.update {
            it.copy(
                allTasksCompleted = allDone,
                completedCount = completedCount,
                totalCount = enabledTasks.size
            )
        }

        if (allDone && _uiState.value.isMorningModeActive) {
            viewModelScope.launch {
                sessionManager.endSession(
                    getApplication(),
                    completedCount,
                    enabledTasks.size
                )
            }
        }
    }

    private fun updateCompletionState(taskId: Long, transform: (TaskCompletionState) -> TaskCompletionState) {
        _uiState.update { state ->
            val current = state.completionStates[taskId] ?: TaskCompletionState(taskId)
            state.copy(completionStates = state.completionStates + (taskId to transform(current)))
        }
    }

    // ═══════════════════════════════════════
    // Task Management (Configuration)
    // ═══════════════════════════════════════

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingTask = null) }
    }

    fun showEditDialog(task: MorningTask) {
        _uiState.update { it.copy(showAddDialog = true, editingTask = task) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingTask = null) }
    }

    fun addTask(title: String, type: TaskType, timedSeconds: Int = 0) {
        viewModelScope.launch {
            val task = MorningTask(
                title = title,
                type = type,
                timedDurationSeconds = timedSeconds,
                sortOrder = _uiState.value.tasks.size
            )
            repository.addTask(task)
            dismissDialog()
        }
    }

    fun updateTask(task: MorningTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            dismissDialog()
        }
    }

    fun deleteTask(task: MorningTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskEnabled(task: MorningTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isEnabled = !task.isEnabled))
        }
    }

    override fun onCleared() {
        timerJobs.values.forEach { it.cancel() }
        cooldownJobs.values.forEach { it.cancel() }
        super.onCleared()
    }
}

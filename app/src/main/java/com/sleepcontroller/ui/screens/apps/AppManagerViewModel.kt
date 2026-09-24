package com.sleepcontroller.ui.screens.apps

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepcontroller.data.db.entity.BlockedApp
import com.sleepcontroller.data.repository.AppBlockRepository
import com.sleepcontroller.data.repository.InstalledAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppManagerUiState(
    val apps: List<BlockedApp> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "all", // all, blocked, whitelisted
    val isLoading: Boolean = true
)

@HiltViewModel
class AppManagerViewModel @Inject constructor(
    private val repository: AppBlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppManagerUiState())
    val uiState: StateFlow<AppManagerUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            repository.syncInstalledApps()
            repository.getAllApps().collect { apps ->
                _uiState.update {
                    it.copy(apps = apps, isLoading = false)
                }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleAppBlocked(app: BlockedApp) {
        if (app.isPermanentlyWhitelisted) return
        viewModelScope.launch {
            repository.setAppBlocked(app.packageName, !app.isBlocked)
        }
    }

    fun blockAllSocial() {
        viewModelScope.launch {
            repository.blockByCategory("social")
        }
    }

    fun blockAllGames() {
        viewModelScope.launch {
            repository.blockByCategory("games")
        }
    }

    fun blockAll() {
        viewModelScope.launch {
            repository.blockAll()
        }
    }

    fun unblockAll() {
        viewModelScope.launch {
            repository.unblockAll()
        }
    }

    /**
     * Derived StateFlow that recomputes only when uiState changes,
     * NOT on every scroll recomposition frame.
     */
    val filteredApps: StateFlow<List<BlockedApp>> = _uiState
        .map { state -> computeFilteredApps(state) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeFilteredApps(state: AppManagerUiState): List<BlockedApp> {
        var filtered = state.apps

        // Apply search
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(state.searchQuery, ignoreCase = true) ||
                    it.packageName.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Apply filter
        filtered = when (state.selectedFilter) {
            "blocked" -> filtered.filter { it.isBlocked && !it.isPermanentlyWhitelisted }
            "whitelisted" -> filtered.filter { !it.isBlocked || it.isPermanentlyWhitelisted }
            else -> filtered
        }

        return filtered.sortedWith(compareBy({ it.isPermanentlyWhitelisted }, { it.appName }))
    }
}

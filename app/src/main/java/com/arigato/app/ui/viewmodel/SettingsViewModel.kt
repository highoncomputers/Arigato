package com.arigato.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.data.local.datastore.UserPreferences
import com.arigato.app.domain.repository.IExecutionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val termuxPath: String = "",
    val defaultShell: String = "sh",
    val executionTimeoutMinutes: Long = 5,
    val showRootWarning: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val executionRepository: IExecutionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferences.isDarkMode,
                userPreferences.termuxPath,
                userPreferences.defaultShell,
                userPreferences.executionTimeout
            ) { darkMode, termuxPath, shell, timeout ->
                SettingsUiState(
                    isDarkMode = darkMode,
                    termuxPath = termuxPath,
                    defaultShell = shell,
                    executionTimeoutMinutes = timeout / 60_000
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkMode(enabled) }
    }

    fun setTermuxPath(path: String) {
        viewModelScope.launch { userPreferences.setTermuxPath(path) }
    }

    fun setDefaultShell(shell: String) {
        viewModelScope.launch { userPreferences.setDefaultShell(shell) }
    }

    fun setExecutionTimeout(minutes: Long) {
        viewModelScope.launch { userPreferences.setExecutionTimeout(minutes * 60_000) }
    }

    fun clearHistory() {
        viewModelScope.launch { executionRepository.clearHistory() }
    }
}

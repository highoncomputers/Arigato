package com.arigato.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.core.root.RootDetector
import com.arigato.app.core.runtime.RuntimeManager
import com.arigato.app.domain.repository.IToolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val message: String = "Initializing Arigato...",
    val progress: Float = 0f,
    val isReady: Boolean = false,
    val isRooted: Boolean? = null,
    val error: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val runtimeManager: RuntimeManager,
    private val toolRepository: IToolRepository,
    private val rootDetector: RootDetector
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            runCatching {
                runtimeManager.initialize { step, progress ->
                    _uiState.value = _uiState.value.copy(message = step, progress = progress)
                }

                val count = toolRepository.getToolCount().first()
                if (count == 0) {
                    _uiState.value = _uiState.value.copy(message = "Loading tool schemas")
                    toolRepository.loadToolsFromAssets()
                }

                _uiState.value = _uiState.value.copy(message = "Checking root access")
                val rooted = rootDetector.isDeviceRooted()

                _uiState.value = _uiState.value.copy(
                    message = "Ready",
                    progress = 1f,
                    isReady = true,
                    isRooted = rooted
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Initialization failed",
                    message = "Initialization failed"
                )
            }
        }
    }
}

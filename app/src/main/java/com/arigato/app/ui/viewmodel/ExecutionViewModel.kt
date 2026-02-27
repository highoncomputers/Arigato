package com.arigato.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.core.execution.ShellExecutor
import com.arigato.app.core.generator.InputValidator
import com.arigato.app.core.generator.ValidationResult
import com.arigato.app.core.output.OutputParser
import com.arigato.app.core.output.ParsedFindings
import com.arigato.app.domain.entity.ExecutionStatus
import com.arigato.app.domain.entity.OutputLine
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.usecase.ExecuteToolUseCase
import com.arigato.app.domain.usecase.GetToolUseCase
import com.arigato.app.utils.helpers.CommandBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExecutionUiState(
    val tool: Tool? = null,
    val isLoading: Boolean = true,
    val parameterValues: Map<String, String> = emptyMap(),
    val validationErrors: Map<String, ValidationResult> = emptyMap(),
    val outputLines: List<OutputLine> = emptyList(),
    val executionStatus: ExecutionStatus? = null,
    val executionId: Long? = null,
    val commandPreview: String = "",
    val isTermuxAvailable: Boolean = false,
    val parsedFindings: ParsedFindings = ParsedFindings(),
    val error: String? = null
)

@HiltViewModel
class ExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getToolUseCase: GetToolUseCase,
    private val executeToolUseCase: ExecuteToolUseCase,
    private val inputValidator: InputValidator,
    private val commandBuilder: CommandBuilder,
    private val shellExecutor: ShellExecutor,
    private val outputParser: OutputParser
) : ViewModel() {
    private val toolId: String = checkNotNull(savedStateHandle["toolId"])

    private val _uiState = MutableStateFlow(ExecutionUiState())
    val uiState: StateFlow<ExecutionUiState> = _uiState.asStateFlow()

    init {
        loadTool()
    }

    private fun loadTool() {
        viewModelScope.launch {
            val tool = getToolUseCase(toolId)
            _uiState.value = _uiState.value.copy(
                tool = tool,
                isLoading = false,
                isTermuxAvailable = shellExecutor.isTermuxInstalled(),
                parameterValues = tool?.parameters
                    ?.filter { it.defaultValue != null }
                    ?.associate { it.name to it.defaultValue!! }
                    ?: emptyMap()
            )
            updateCommandPreview()
        }
    }

    fun updateParameter(name: String, value: String) {
        val currentValues = _uiState.value.parameterValues.toMutableMap()
        currentValues[name] = value
        val tool = _uiState.value.tool ?: return
        val param = tool.parameters.find { it.name == name }
        val validationResult = param?.let { inputValidator.validate(it, value) } ?: ValidationResult(true)
        val errors = _uiState.value.validationErrors.toMutableMap()
        if (validationResult.isValid) {
            errors.remove(name)
        } else {
            errors[name] = validationResult
        }
        _uiState.value = _uiState.value.copy(
            parameterValues = currentValues,
            validationErrors = errors
        )
        updateCommandPreview()
    }

    fun execute() {
        val tool = _uiState.value.tool ?: return
        val params = _uiState.value.parameterValues

        val validationErrors = inputValidator.validateAll(tool.parameters, params)
        val hasErrors = validationErrors.values.any { !it.isValid }
        if (hasErrors) {
            _uiState.value = _uiState.value.copy(validationErrors = validationErrors)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                outputLines = emptyList(),
                executionStatus = ExecutionStatus.RUNNING,
                error = null
            )

            runCatching {
                val (executionId, outputFlow) = executeToolUseCase.execute(tool, params)
                _uiState.value = _uiState.value.copy(executionId = executionId)

                outputFlow.collect { line ->
                    val current = _uiState.value.outputLines.toMutableList()
                    current.add(line)
                    val isExit = line is OutputLine.Exit
                    val newStatus = if (isExit) {
                        if ((line as OutputLine.Exit).code == 0) ExecutionStatus.COMPLETED else ExecutionStatus.FAILED
                    } else {
                        ExecutionStatus.RUNNING
                    }
                    val findings = if (isExit) {
                        val stdoutLines = current.filterIsInstance<OutputLine.StdOut>().map { it.content }
                        outputParser.parse(stdoutLines)
                    } else {
                        _uiState.value.parsedFindings
                    }
                    _uiState.value = _uiState.value.copy(
                        outputLines = current,
                        executionStatus = newStatus,
                        parsedFindings = findings
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    executionStatus = ExecutionStatus.FAILED,
                    error = e.message ?: "Execution failed"
                )
            }
        }
    }

    fun terminate() {
        val executionId = _uiState.value.executionId ?: return
        viewModelScope.launch {
            executeToolUseCase.terminate(executionId)
            _uiState.value = _uiState.value.copy(executionStatus = ExecutionStatus.CANCELLED)
        }
    }

    fun clearOutput() {
        _uiState.value = _uiState.value.copy(
            outputLines = emptyList(),
            executionStatus = null,
            executionId = null,
            parsedFindings = ParsedFindings()
        )
    }

    fun launchInTermux() {
        val command = _uiState.value.commandPreview
        if (command.isNotBlank()) {
            shellExecutor.launchInTermux(command)
        }
    }

    private fun updateCommandPreview() {
        val tool = _uiState.value.tool ?: return
        val preview = commandBuilder.preview(tool, _uiState.value.parameterValues)
        _uiState.value = _uiState.value.copy(commandPreview = preview)
    }
}

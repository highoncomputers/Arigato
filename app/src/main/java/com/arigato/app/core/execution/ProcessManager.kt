package com.arigato.app.core.execution

import com.arigato.app.domain.entity.OutputLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessInfo(
    val executionId: Long,
    val command: String,
    val startTime: Long = System.currentTimeMillis()
)

private data class ExecutionState(
    val processInfo: ProcessInfo,
    val job: Job,
    val outputFlow: MutableSharedFlow<Pair<Long, OutputLine>>
)

@Singleton
class ProcessManager @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeExecutions = ConcurrentHashMap<Long, ExecutionState>()
    private val globalOutput = MutableSharedFlow<Pair<Long, OutputLine>>(replay = 0, extraBufferCapacity = 1000)

    fun execute(executionId: Long, command: String): Flow<OutputLine> {
        val outputFlow = MutableSharedFlow<Pair<Long, OutputLine>>(replay = 100, extraBufferCapacity = 1000)

        val job = managerScope.launch {
            shellExecutor.executeCommand(command)
                .onCompletion { cause ->
                    if (cause != null) {
                        outputFlow.emit(executionId to OutputLine.StdErr("Process interrupted: ${cause.message}"))
                        outputFlow.emit(executionId to OutputLine.Exit(-1))
                    }
                    activeExecutions.remove(executionId)
                }
                .collect { line ->
                    outputFlow.emit(executionId to line)
                    globalOutput.emit(executionId to line)
                }
        }

        val state = ExecutionState(
            processInfo = ProcessInfo(executionId = executionId, command = command),
            job = job,
            outputFlow = outputFlow
        )
        activeExecutions[executionId] = state

        return outputFlow.asSharedFlow()
            .filter { it.first == executionId }
            .map { it.second }
    }

    suspend fun terminate(executionId: Long): Boolean {
        val state = activeExecutions[executionId] ?: return false
        state.job.cancel()
        activeExecutions.remove(executionId)
        return true
    }

    fun getActiveProcesses(): List<ProcessInfo> =
        activeExecutions.values.map { it.processInfo }

    fun isRunning(executionId: Long): Boolean = activeExecutions.containsKey(executionId)

    suspend fun terminateAll() {
        activeExecutions.values.forEach { it.job.cancel() }
        activeExecutions.clear()
    }

    fun observeAll(): Flow<Pair<Long, OutputLine>> = globalOutput.asSharedFlow()
}

package com.arigato.app.domain.usecase

import com.arigato.app.core.analytics.ExecutionAnalytics
import com.arigato.app.core.execution.ProcessManager
import com.arigato.app.core.security.CommandAuditLogger
import com.arigato.app.domain.entity.AnalyticsEvent
import com.arigato.app.domain.entity.ExecutionRecord
import com.arigato.app.domain.entity.ExecutionStatus
import com.arigato.app.domain.entity.OutputLine
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.repository.IExecutionRepository
import com.arigato.app.utils.helpers.CommandBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ExecuteToolUseCase @Inject constructor(
    private val processManager: ProcessManager,
    private val executionRepository: IExecutionRepository,
    private val commandBuilder: CommandBuilder,
    private val executionAnalytics: ExecutionAnalytics,
    private val auditLogger: CommandAuditLogger
) {
    suspend fun execute(tool: Tool, params: Map<String, String>): Pair<Long, Flow<OutputLine>> {
        val command = commandBuilder.build(tool, params)
        val startTime = System.currentTimeMillis()
        val record = ExecutionRecord(
            toolId = tool.id,
            toolName = tool.name,
            command = command,
            parameters = params,
            startTime = startTime,
            status = ExecutionStatus.RUNNING
        )
        val executionId = executionRepository.saveExecution(record)
        val outputFlow = processManager.execute(executionId, command)
            .onEach { line ->
                executionRepository.appendOutput(executionId, line)
                if (line is OutputLine.Exit) {
                    val endTime = System.currentTimeMillis()
                    val status = if (line.code == 0) ExecutionStatus.COMPLETED else ExecutionStatus.FAILED
                    executionRepository.updateExecutionStatus(
                        id = executionId,
                        status = status,
                        exitCode = line.code,
                        endTime = endTime
                    )
                    val durationMs = endTime - startTime
                    executionAnalytics.record(
                        AnalyticsEvent(
                            toolId = tool.id,
                            toolName = tool.name,
                            durationMs = durationMs,
                            exitCode = line.code
                        )
                    )
                    auditLogger.log(
                        toolId = tool.id,
                        toolName = tool.name,
                        command = command,
                        exitCode = line.code,
                        durationMs = durationMs
                    )
                }
            }
        return executionId to outputFlow
    }

    suspend fun terminate(executionId: Long) {
        processManager.terminate(executionId)
        executionRepository.updateExecutionStatus(
            id = executionId,
            status = ExecutionStatus.CANCELLED,
            endTime = System.currentTimeMillis()
        )
    }
}

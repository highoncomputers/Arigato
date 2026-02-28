package com.arigato.app.domain.entity

import kotlinx.serialization.Serializable

@Serializable
enum class ExecutionStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMEOUT
}

@Serializable
sealed class OutputLine {
    abstract val timestamp: Long

    @Serializable
    data class StdOut(val content: String, override val timestamp: Long = System.currentTimeMillis()) : OutputLine()

    @Serializable
    data class StdErr(val content: String, override val timestamp: Long = System.currentTimeMillis()) : OutputLine()

    @Serializable
    data class SystemMessage(val content: String, override val timestamp: Long = System.currentTimeMillis()) : OutputLine()

    @Serializable
    data class Exit(val code: Int, override val timestamp: Long = System.currentTimeMillis()) : OutputLine()
}

@Serializable
data class ExecutionRecord(
    val id: Long = 0,
    val toolId: String,
    val toolName: String,
    val command: String,
    val parameters: Map<String, String>,
    val startTime: Long,
    val endTime: Long? = null,
    val status: ExecutionStatus = ExecutionStatus.QUEUED,
    val exitCode: Int? = null,
    val outputLines: List<OutputLine> = emptyList()
)

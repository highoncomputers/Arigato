package com.arigato.app.domain.entity

data class ToolUsageStat(
    val toolId: String,
    val toolName: String,
    val executionCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val averageDurationMs: Long,
    val lastUsedAt: Long
) {
    val successRate: Float
        get() = if (executionCount == 0) 0f else successCount.toFloat() / executionCount
}

data class AnalyticsEvent(
    val toolId: String,
    val toolName: String,
    val durationMs: Long,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isSuccess: Boolean get() = exitCode == 0
}

data class PlatformStats(
    val totalExecutions: Int,
    val successfulExecutions: Int,
    val totalToolsUsed: Int,
    val topTools: List<ToolUsageStat>,
    val averageSessionDurationMs: Long
)

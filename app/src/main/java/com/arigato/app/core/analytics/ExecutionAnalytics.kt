package com.arigato.app.core.analytics

import com.arigato.app.domain.entity.AnalyticsEvent
import com.arigato.app.domain.entity.PlatformStats
import com.arigato.app.domain.entity.ToolUsageStat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionAnalytics @Inject constructor() {
    private val mutex = Mutex()
    private val events = mutableListOf<AnalyticsEvent>()

    suspend fun record(event: AnalyticsEvent) {
        mutex.withLock {
            events.add(event)
            if (events.size > MAX_EVENTS) {
                events.removeAt(0)
            }
        }
    }

    suspend fun getToolStats(): List<ToolUsageStat> = mutex.withLock {
        events.groupBy { it.toolId }
            .map { (toolId, toolEvents) ->
                val successes = toolEvents.count { it.isSuccess }
                val durations = toolEvents.map { it.durationMs }
                ToolUsageStat(
                    toolId = toolId,
                    toolName = toolEvents.last().toolName,
                    executionCount = toolEvents.size,
                    successCount = successes,
                    failureCount = toolEvents.size - successes,
                    averageDurationMs = if (durations.isEmpty()) 0L else durations.average().toLong(),
                    lastUsedAt = toolEvents.maxOf { it.timestamp }
                )
            }
            .sortedByDescending { it.executionCount }
    }

    suspend fun getPlatformStats(): PlatformStats = mutex.withLock {
        val toolGroups = events.groupBy { it.toolId }
        val totalExecutions = events.size
        val successCount = events.count { it.isSuccess }
        val topTools = toolGroups.map { (toolId, toolEvents) ->
            val successes = toolEvents.count { it.isSuccess }
            val durations = toolEvents.map { it.durationMs }
            ToolUsageStat(
                toolId = toolId,
                toolName = toolEvents.last().toolName,
                executionCount = toolEvents.size,
                successCount = successes,
                failureCount = toolEvents.size - successes,
                averageDurationMs = if (durations.isEmpty()) 0L else durations.average().toLong(),
                lastUsedAt = toolEvents.maxOf { it.timestamp }
            )
        }.sortedByDescending { it.executionCount }.take(5)

        PlatformStats(
            totalExecutions = totalExecutions,
            successfulExecutions = successCount,
            totalToolsUsed = toolGroups.size,
            topTools = topTools,
            averageSessionDurationMs = if (events.isEmpty()) 0L
            else events.map { it.durationMs }.average().toLong()
        )
    }

    suspend fun getRelevanceScore(toolId: String, afterToolId: String): Float = mutex.withLock {
        val afterEvents = events.zipWithNext()
            .filter { (first, _) -> first.toolId == afterToolId }
        if (afterEvents.isEmpty()) return@withLock 0.5f

        val followCount = afterEvents.count { (_, second) -> second.toolId == toolId }
        (followCount.toFloat() / afterEvents.size).coerceIn(0f, 1f)
    }

    private companion object {
        const val MAX_EVENTS = 1000
    }
}

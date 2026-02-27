package com.arigato.app.core.execution

import com.arigato.app.domain.entity.QueuedExecution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

data class QueueState(
    val pending: List<QueuedExecution> = emptyList(),
    val activeCount: Int = 0,
    val maxConcurrent: Int = DEFAULT_MAX_CONCURRENT
)

@Singleton
class ExecutionQueue @Inject constructor() {
    private val queueScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val semaphore = Semaphore(DEFAULT_MAX_CONCURRENT)
    private val enqueueChannel = Channel<QueuedExecution>(Channel.UNLIMITED)

    private val pendingQueue = PriorityQueue<QueuedExecution>(
        compareByDescending<QueuedExecution> { it.priority.level }
            .thenBy { it.enqueuedAt }
    )

    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()
    private var onExecute: (suspend (QueuedExecution) -> Unit)? = null

    init {
        startDispatcher()
    }

    fun setExecutor(executor: suspend (QueuedExecution) -> Unit) {
        onExecute = executor
    }

    fun enqueue(item: QueuedExecution): String {
        enqueueChannel.trySend(item)
        return item.id
    }

    fun cancel(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        synchronized(pendingQueue) { pendingQueue.removeIf { it.id == id } }
        updateState()
    }

    fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        synchronized(pendingQueue) { pendingQueue.clear() }
        updateState()
    }

    private fun startDispatcher() {
        queueScope.launch {
            for (item in enqueueChannel) {
                synchronized(pendingQueue) { pendingQueue.add(item) }
                updateState()
                dispatch()
            }
        }
    }

    private fun dispatch() {
        queueScope.launch {
            val item = synchronized(pendingQueue) { pendingQueue.poll() } ?: return@launch
            updateState()

            val job = launch {
                semaphore.withPermit {
                    updateActiveCount(delta = +1)
                    runCatching { onExecute?.invoke(item) }
                    updateActiveCount(delta = -1)
                    activeJobs.remove(item.id)
                    updateState()
                }
            }
            activeJobs[item.id] = job
        }
    }

    private fun updateActiveCount(delta: Int) {
        _queueState.value = _queueState.value.copy(
            activeCount = (_queueState.value.activeCount + delta).coerceAtLeast(0)
        )
    }

    private fun updateState() {
        _queueState.value = _queueState.value.copy(
            pending = synchronized(pendingQueue) { pendingQueue.toList() }
        )
    }

    companion object {
        const val DEFAULT_MAX_CONCURRENT = 3
    }
}

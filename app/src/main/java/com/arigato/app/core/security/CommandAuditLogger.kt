package com.arigato.app.core.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AuditEntry(
    val timestamp: Long,
    val toolId: String,
    val toolName: String,
    val commandHash: String,
    val exitCode: Int?,
    val durationMs: Long,
    val integrity: String = ""
)

@Singleton
class CommandAuditLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }
    private val auditDir = File(context.filesDir, "audit")
    private val currentLog get() = File(auditDir, "audit.log")

    init {
        auditDir.mkdirs()
    }

    suspend fun log(
        toolId: String,
        toolName: String,
        command: String,
        exitCode: Int?,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val commandHash = sha256(command)
            val entry = AuditEntry(
                timestamp = System.currentTimeMillis(),
                toolId = toolId,
                toolName = toolName,
                commandHash = commandHash,
                exitCode = exitCode,
                durationMs = durationMs
            )
            val entryJson = json.encodeToString(entry.copy(integrity = computeIntegrity(entry)))
            rotateLogs()
            currentLog.appendText(entryJson + "\n")
        }
    }

    suspend fun readEntries(): List<AuditEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!currentLog.exists()) return@withLock emptyList()
            currentLog.readLines()
                .mapNotNull { line -> runCatching { json.decodeFromString<AuditEntry>(line) }.getOrNull() }
        }
    }

    suspend fun exportAsJson(): String = withContext(Dispatchers.IO) {
        val entries = readEntries()
        json.encodeToString(entries)
    }

    private fun rotateLogs() {
        if (currentLog.exists() && currentLog.length() >= MAX_FILE_SIZE_BYTES) {
            for (i in MAX_ROTATIONS downTo 1) {
                val old = File(auditDir, "audit.$i.log")
                val newer = if (i == 1) currentLog else File(auditDir, "audit.${i - 1}.log")
                if (newer.exists()) newer.renameTo(old)
            }
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun computeIntegrity(entry: AuditEntry): String {
        val content = "${entry.timestamp}${entry.toolId}${entry.commandHash}${entry.exitCode}"
        return sha256(content)
    }

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024
        private const val MAX_ROTATIONS = 5
    }
}

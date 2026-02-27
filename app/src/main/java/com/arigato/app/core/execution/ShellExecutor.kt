package com.arigato.app.core.execution

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.arigato.app.domain.entity.OutputLine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_RUN_COMMAND = "com.termux.RUN_COMMAND"
        private const val TERMUX_EXEC_SERVICE = "com.termux.app.RunCommandService"
        private const val DEFAULT_SHELL = "/system/bin/sh"
    }

    fun isTermuxInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    }.getOrDefault(false)

    fun isToolInstalled(toolName: String): Boolean {
        val paths = listOf(
            "/data/data/com.termux/files/usr/bin/$toolName",
            "/usr/bin/$toolName",
            "/usr/local/bin/$toolName",
            "/bin/$toolName"
        )
        return paths.any { java.io.File(it).exists() }
    }

    fun executeCommand(command: String, workingDir: String? = null, env: Map<String, String> = emptyMap()): Flow<OutputLine> =
        callbackFlow {
            val processEnv = buildEnvironment(env)
            val fullCommand = wrapCommand(command)

            val process = ProcessBuilder(fullCommand)
                .apply {
                    workingDir?.let { directory(java.io.File(it)) }
                    environment().putAll(processEnv)
                    redirectErrorStream(false)
                }
                .start()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdoutJob = launch(Dispatchers.IO) {
                try {
                    stdoutReader.lineSequence().forEach { line ->
                        trySend(OutputLine.StdOut(line))
                    }
                } catch (_: Exception) {}
            }

            val stderrJob = launch(Dispatchers.IO) {
                try {
                    stderrReader.lineSequence().forEach { line ->
                        trySend(OutputLine.StdErr(line))
                    }
                } catch (_: Exception) {}
            }

            stdoutJob.join()
            stderrJob.join()

            val exitCode = process.waitFor()
            trySend(OutputLine.Exit(exitCode))
            close()

            awaitClose {
                runCatching { process.destroy() }
            }
        }.flowOn(Dispatchers.IO)

    fun launchInTermux(command: String) {
        if (!isTermuxInstalled()) return

        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, TERMUX_EXEC_SERVICE)
            action = TERMUX_RUN_COMMAND
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_TERMINAL", true)
        }
        runCatching { context.startService(intent) }
    }

    private fun wrapCommand(command: String): List<String> {
        val shell = if (isTermuxInstalled()) {
            "/data/data/com.termux/files/usr/bin/bash"
        } else {
            DEFAULT_SHELL
        }
        return listOf(shell, "-c", command)
    }

    private fun buildEnvironment(extra: Map<String, String>): Map<String, String> {
        val env = mutableMapOf<String, String>()

        if (isTermuxInstalled()) {
            val termuxPrefix = "/data/data/com.termux/files/usr"
            env["PATH"] = "$termuxPrefix/bin:$termuxPrefix/bin/applets:/usr/bin:/bin"
            env["HOME"] = "/data/data/com.termux/files/home"
            env["TMPDIR"] = "/data/data/com.termux/files/usr/tmp"
            env["PREFIX"] = termuxPrefix
            env["TERM"] = "xterm-256color"
        } else {
            env["PATH"] = "/usr/bin:/bin:/system/bin"
            env["HOME"] = context.filesDir.absolutePath
        }

        env.putAll(extra)
        return env
    }
}

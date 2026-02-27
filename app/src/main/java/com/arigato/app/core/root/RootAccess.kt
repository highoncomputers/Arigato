package com.arigato.app.core.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootAccess @Inject constructor() {
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder("su", "-c", "echo root")
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        }.getOrDefault(false)
    }
}

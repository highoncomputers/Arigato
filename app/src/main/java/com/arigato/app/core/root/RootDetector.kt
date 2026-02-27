package com.arigato.app.core.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootDetector @Inject constructor() {
    suspend fun isDeviceRooted(): Boolean = withContext(Dispatchers.IO) {
        val paths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/app/Magisk.apk"
        )
        if (paths.any { File(it).exists() }) return@withContext true

        runCatching {
            val process = ProcessBuilder("su", "-c", "id")
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        }.getOrDefault(false)
    }
}

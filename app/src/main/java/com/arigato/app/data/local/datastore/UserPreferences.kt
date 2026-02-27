package com.arigato.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val TERMUX_PATH = stringPreferencesKey("termux_path")
        val RECENT_TOOLS = stringSetPreferencesKey("recent_tools")
        val DEFAULT_SHELL = stringPreferencesKey("default_shell")
        val SHOW_ROOT_WARNING = booleanPreferencesKey("show_root_warning")
        val EXECUTION_TIMEOUT = stringPreferencesKey("execution_timeout")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val isDisclaimerAccepted: Flow<Boolean> = context.dataStore.data.map { it[Keys.DISCLAIMER_ACCEPTED] ?: false }
    val termuxPath: Flow<String> = context.dataStore.data.map { it[Keys.TERMUX_PATH] ?: "/data/data/com.termux/files/usr/bin" }
    val recentTools: Flow<Set<String>> = context.dataStore.data.map { it[Keys.RECENT_TOOLS] ?: emptySet() }
    val defaultShell: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_SHELL] ?: "sh" }
    val showRootWarning: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_ROOT_WARNING] ?: true }
    val executionTimeout: Flow<Long> = context.dataStore.data.map {
        it[Keys.EXECUTION_TIMEOUT]?.toLongOrNull() ?: 300_000L
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { it[Keys.DISCLAIMER_ACCEPTED] = accepted }
    }

    suspend fun setTermuxPath(path: String) {
        context.dataStore.edit { it[Keys.TERMUX_PATH] = path }
    }

    suspend fun addRecentTool(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_TOOLS] ?: emptySet()
            val updated = (setOf(toolId) + current).take(10).toSet()
            prefs[Keys.RECENT_TOOLS] = updated
        }
    }

    suspend fun setDefaultShell(shell: String) {
        context.dataStore.edit { it[Keys.DEFAULT_SHELL] = shell }
    }

    suspend fun setExecutionTimeout(timeoutMs: Long) {
        context.dataStore.edit { it[Keys.EXECUTION_TIMEOUT] = timeoutMs.toString() }
    }
}

package com.arigato.app.plugins.registry

import com.arigato.app.plugins.base.IToolPlugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor() {
    private val plugins = mutableMapOf<String, IToolPlugin>()

    fun register(plugin: IToolPlugin) {
        plugins[plugin.toolId] = plugin
    }

    fun getPlugin(toolId: String): IToolPlugin? = plugins[toolId]

    fun getAllPlugins(): List<IToolPlugin> = plugins.values.toList()

    fun hasPlugin(toolId: String): Boolean = plugins.containsKey(toolId)
}

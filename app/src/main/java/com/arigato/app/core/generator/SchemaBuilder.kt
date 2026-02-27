package com.arigato.app.core.generator

import com.arigato.app.domain.entity.Tool
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemaBuilder @Inject constructor() {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun buildJson(tool: Tool): String = json.encodeToString(Tool.serializer(), tool)

    fun parseToolFromJson(jsonString: String): Tool? = runCatching {
        json.decodeFromString(Tool.serializer(), jsonString)
    }.getOrNull()
}

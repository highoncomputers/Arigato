package com.arigato.app.plugins.base

import com.arigato.app.domain.entity.Tool

interface IToolPlugin {
    val toolId: String
    val version: String

    fun getTool(): Tool
    fun buildCommand(params: Map<String, String>): String
    fun validateParams(params: Map<String, String>): Map<String, String>
    fun isAvailable(): Boolean
}

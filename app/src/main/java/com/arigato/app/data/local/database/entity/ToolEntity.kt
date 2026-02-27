package com.arigato.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tools")
data class ToolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val description: String,
    val category: String,
    val schemaJson: String,
    val isInstalled: Boolean = false,
    val isFavorite: Boolean = false,
    val requiresRoot: Boolean = false,
    val tags: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

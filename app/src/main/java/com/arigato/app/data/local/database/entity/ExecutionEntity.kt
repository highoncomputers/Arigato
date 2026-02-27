package com.arigato.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "executions",
    foreignKeys = [
        ForeignKey(
            entity = ToolEntity::class,
            parentColumns = ["id"],
            childColumns = ["toolId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("toolId")]
)
data class ExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolId: String,
    val toolName: String,
    val command: String,
    val parametersJson: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String,
    val exitCode: Int? = null,
    val outputJson: String = "[]"
)

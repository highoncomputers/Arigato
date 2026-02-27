package com.arigato.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arigato.app.data.local.database.entity.ExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionDao {
    @Query("SELECT * FROM executions ORDER BY startTime DESC")
    fun getAllExecutions(): Flow<List<ExecutionEntity>>

    @Query("SELECT * FROM executions WHERE toolId = :toolId ORDER BY startTime DESC")
    fun getExecutionsByTool(toolId: String): Flow<List<ExecutionEntity>>

    @Query("SELECT * FROM executions WHERE id = :id")
    suspend fun getExecutionById(id: Long): ExecutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: ExecutionEntity): Long

    @Update
    suspend fun updateExecution(execution: ExecutionEntity)

    @Query("UPDATE executions SET status = :status, exitCode = :exitCode, endTime = :endTime WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, exitCode: Int?, endTime: Long?)

    @Query("UPDATE executions SET outputJson = :outputJson WHERE id = :id")
    suspend fun updateOutput(id: Long, outputJson: String)

    @Query("DELETE FROM executions")
    suspend fun deleteAll()

    @Query("DELETE FROM executions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

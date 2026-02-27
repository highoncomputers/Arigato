package com.arigato.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arigato.app.data.local.database.entity.ToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolDao {
    @Query("SELECT * FROM tools ORDER BY name ASC")
    fun getAllTools(): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools WHERE category = :category ORDER BY name ASC")
    fun getToolsByCategory(category: String): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteTools(): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools WHERE id = :id")
    suspend fun getToolById(id: String): ToolEntity?

    @Query("""
        SELECT * FROM tools 
        WHERE name LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        OR tags LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            name ASC
    """)
    suspend fun searchTools(query: String): List<ToolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: ToolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<ToolEntity>)

    @Update
    suspend fun updateTool(tool: ToolEntity)

    @Query("UPDATE tools SET isFavorite = :isFavorite WHERE id = :toolId")
    suspend fun setFavorite(toolId: String, isFavorite: Boolean)

    @Query("UPDATE tools SET isInstalled = :isInstalled WHERE id = :toolId")
    suspend fun updateInstallStatus(toolId: String, isInstalled: Boolean)

    @Query("SELECT COUNT(*) FROM tools")
    fun getToolCount(): Flow<Int>

    @Query("DELETE FROM tools")
    suspend fun deleteAll()
}

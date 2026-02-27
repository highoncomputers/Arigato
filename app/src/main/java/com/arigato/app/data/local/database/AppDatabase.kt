package com.arigato.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arigato.app.data.local.database.dao.ExecutionDao
import com.arigato.app.data.local.database.dao.ToolDao
import com.arigato.app.data.local.database.entity.ExecutionEntity
import com.arigato.app.data.local.database.entity.ToolEntity

@Database(
    entities = [ToolEntity::class, ExecutionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun toolDao(): ToolDao
    abstract fun executionDao(): ExecutionDao

    companion object {
        const val DATABASE_NAME = "arigato_db"
    }
}

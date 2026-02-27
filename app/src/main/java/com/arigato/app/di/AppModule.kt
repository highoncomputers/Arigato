package com.arigato.app.di

import android.content.Context
import androidx.room.Room
import com.arigato.app.data.local.database.AppDatabase
import com.arigato.app.data.local.database.dao.ExecutionDao
import com.arigato.app.data.local.database.dao.ToolDao
import com.arigato.app.data.repository.ExecutionRepositoryImpl
import com.arigato.app.data.repository.ToolRepositoryImpl
import com.arigato.app.domain.repository.IExecutionRepository
import com.arigato.app.domain.repository.IToolRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideToolDao(db: AppDatabase): ToolDao = db.toolDao()

    @Provides
    fun provideExecutionDao(db: AppDatabase): ExecutionDao = db.executionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindToolRepository(impl: ToolRepositoryImpl): IToolRepository

    @Binds
    @Singleton
    abstract fun bindExecutionRepository(impl: ExecutionRepositoryImpl): IExecutionRepository
}

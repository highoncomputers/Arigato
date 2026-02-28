package com.arigato.app.di

import com.arigato.app.data.repository.ExecutionRepositoryImpl
import com.arigato.app.data.repository.ToolRepositoryImpl
import com.arigato.app.domain.repository.IExecutionRepository
import com.arigato.app.domain.repository.IToolRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

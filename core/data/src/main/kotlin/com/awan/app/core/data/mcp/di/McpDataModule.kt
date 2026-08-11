package com.awan.app.core.data.mcp.di

import com.awan.app.core.data.mcp.repository.McpRepositoryImpl
import com.awan.app.core.domain.mcp.repository.McpRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class McpDataModule {

    @Binds
    @Singleton
    abstract fun bindMcpRepository(
        impl: McpRepositoryImpl,
    ): McpRepository
}

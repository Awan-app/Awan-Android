package com.awan.app.core.data.mcp.di

import com.awan.app.core.data.mcp.repository.McpRepositoryImpl
import com.awan.app.core.data.mcp.remote.McpRemoteDataSource
import com.awan.app.core.data.mcp.remote.McpRemoteDataSourceImpl
import com.awan.app.core.domain.mcp.repository.McpRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class McpDataModule {

    @Binds
    @Singleton
    abstract fun bindMcpRemoteDataSource(
        impl: McpRemoteDataSourceImpl,
    ): McpRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindMcpRepository(
        impl: McpRepositoryImpl,
    ): McpRepository
}

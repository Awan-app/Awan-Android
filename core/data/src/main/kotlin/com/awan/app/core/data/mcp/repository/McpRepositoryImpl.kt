package com.awan.app.core.data.mcp.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.mcp.mapper.toDomain
import com.awan.app.core.data.mcp.remote.McpRemoteDataSource
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.repository.McpRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Singleton
internal class McpRepositoryImpl @Inject constructor(
    private val remoteDataSource: McpRemoteDataSource,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : McpRepository {

    override fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>> = flow {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            emit(Result.Error(AppError.Network))
            return@flow
        }

        emit(Result.Loading)
        when (val result = remoteDataSource.getConnectionDetails()) {
            is Result.Success -> emit(Result.Success(result.data.toDomain()))
            is Result.Error -> emit(Result.Error(result.error))
            Result.Loading -> emit(Result.Loading)
        }
    }.flowOn(ioDispatcher)
}

package com.awan.app.core.data.mcp.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.mcp.mapper.toDomain
import com.awan.app.core.data.mcp.mapper.toEntity
import com.awan.app.core.data.mcp.remote.McpRemoteDataSource
import com.awan.app.core.database.dao.McpTokenDao
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.app.core.domain.mcp.repository.McpRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.network.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Singleton
internal class McpRepositoryImpl @Inject constructor(
    private val remoteDataSource: McpRemoteDataSource,
    private val mcpTokenDao: McpTokenDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : McpRepository {

    override fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>> = flow {
        emit(
            Result.Success(
                McpConnectionDetails(
                    mcpUrl = "${BuildConfig.AWAN_BASE_URL.trimEnd('/')}/v1/mcp",
                    clientId = "awan-android-client",
                ),
            ),
        )
    }.flowOn(ioDispatcher)

    override fun getMcpTokens(): Flow<Result<List<McpToken>>> = flow {
        if (connectivityMonitor.isCurrentlyOnline()) {
            try {
                when (val result = remoteDataSource.getApiKeys()) {
                    is Result.Success -> mcpTokenDao.replaceMcpTokens(result.data.map { it.toEntity() })
                    is Result.Error,
                    Result.Loading -> Unit
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // If network sync fails, fallback to Room cached tokens
            }
        }
        emitAll(
            mcpTokenDao.getMcpTokens().map { entities ->
                Result.Success(entities.map { it.toDomain() })
            },
        )
    }.flowOn(ioDispatcher)

    override suspend fun createMcpToken(name: String): Result<CreatedMcpToken> {
        if (!connectivityMonitor.isCurrentlyOnline()) return Result.Error(AppError.Network)

        return when (val result = remoteDataSource.createApiKey(name)) {
            is Result.Success -> {
                val dto = result.data
                try {
                    mcpTokenDao.upsertMcpTokens(listOf(dto.toEntity()))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // Local DB cache write failure must NOT drop or cause failure of the returned raw token
                }
                Result.Success(dto.toDomain())
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun deleteMcpToken(id: String): Result<Unit> {
        if (!connectivityMonitor.isCurrentlyOnline()) return Result.Error(AppError.Network)

        val result = remoteDataSource.revokeApiKey(id)
        if (result is Result.Success) {
            try {
                mcpTokenDao.deleteMcpToken(id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
        return result
    }

    override suspend fun regenerateMcpToken(id: String): Result<CreatedMcpToken> {
        if (!connectivityMonitor.isCurrentlyOnline()) return Result.Error(AppError.Network)

        val name = mcpTokenDao.getMcpTokens().first().find { it.id == id }?.name ?: "MCP Token"
        return when (val createResult = remoteDataSource.createApiKey(name)) {
            is Result.Success -> when (val revokeResult = remoteDataSource.revokeApiKey(id)) {
                is Result.Success -> {
                    val dto = createResult.data
                    try {
                        mcpTokenDao.deleteMcpToken(id)
                        mcpTokenDao.upsertMcpTokens(listOf(dto.toEntity()))
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        // Local DB cache write failure must NOT drop or cause failure of the returned raw token
                    }
                    Result.Success(dto.toDomain())
                }
                is Result.Error -> Result.Error(revokeResult.error)
                Result.Loading -> Result.Loading
            }
            is Result.Error -> Result.Error(createResult.error)
            Result.Loading -> Result.Loading
        }
    }
}

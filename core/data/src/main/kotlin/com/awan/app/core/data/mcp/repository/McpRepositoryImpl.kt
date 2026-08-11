package com.awan.app.core.data.mcp.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.mcp.mapper.toDomain
import com.awan.app.core.data.mcp.mapper.toEntity
import com.awan.app.core.database.dao.McpTokenDao
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.app.core.domain.mcp.repository.McpRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.network.BuildConfig
import com.awan.app.core.network.api.McpApiService
import com.awan.app.core.network.dto.mcp.CreateApiKeyRequestDto
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpRepositoryImpl @Inject constructor(
    private val mcpApiService: McpApiService,
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
                )
            )
        )
    }.flowOn(ioDispatcher)

    override fun getMcpTokens(): Flow<Result<List<McpToken>>> = flow {
        if (connectivityMonitor.isCurrentlyOnline()) {
            try {
                val response = mcpApiService.getApiKeys()
                if (response.isSuccessful) {
                    val dtos = response.body().orEmpty()
                    val entities = dtos.map { it.toEntity() }
                    mcpTokenDao.replaceMcpTokens(entities)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // If network sync fails, fallback to Room cached tokens
            }
        }
        emitAll(
            mcpTokenDao.getMcpTokens().map { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun createMcpToken(name: String): Result<CreatedMcpToken> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return safeApiCall(ioDispatcher) {
            val response = mcpApiService.createApiKey(CreateApiKeyRequestDto(name = name))
            if (!response.isSuccessful) throw HttpException(response)
            val dto = response.body() ?: throw IllegalStateException("Empty response body")
            val createdToken = dto.toDomain()
            try {
                mcpTokenDao.upsertMcpTokens(listOf(dto.toEntity()))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Local DB cache write failure must NOT drop or cause failure of the returned raw token
            }
            createdToken
        }
    }

    override suspend fun deleteMcpToken(id: String): Result<Unit> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        val result = safeApiCall(ioDispatcher) {
            val response = mcpApiService.revokeApiKey(id)
            if (!response.isSuccessful) throw HttpException(response)
        }
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
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        val existingTokenName = mcpTokenDao.getMcpTokens().first().find { it.id == id }?.name ?: "MCP Token"
        return safeApiCall(ioDispatcher) {
            val createResponse = mcpApiService.createApiKey(CreateApiKeyRequestDto(name = existingTokenName))
            if (!createResponse.isSuccessful) throw HttpException(createResponse)
            val dto = createResponse.body() ?: throw IllegalStateException("Empty response body")
            val createdToken = dto.toDomain()

            val revokeResponse = mcpApiService.revokeApiKey(id)
            if (!revokeResponse.isSuccessful) throw HttpException(revokeResponse)

            try {
                mcpTokenDao.deleteMcpToken(id)
                mcpTokenDao.upsertMcpTokens(listOf(dto.toEntity()))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Local DB cache write failure must NOT drop or cause failure of the returned raw token
            }
            createdToken
        }
    }
}


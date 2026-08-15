package com.awan.app.core.domain.mcp.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.app.core.domain.mcp.repository.McpRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMcpTokensUseCase @Inject constructor(
    private val repository: McpRepository,
) {
    operator fun invoke(): Flow<Result<List<McpToken>>> = repository.getMcpTokens()
}

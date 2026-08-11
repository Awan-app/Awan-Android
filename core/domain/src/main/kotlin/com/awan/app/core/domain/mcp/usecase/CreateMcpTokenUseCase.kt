package com.awan.app.core.domain.mcp.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.repository.McpRepository
import javax.inject.Inject

class CreateMcpTokenUseCase @Inject constructor(
    private val repository: McpRepository,
) {
    suspend operator fun invoke(name: String): Result<CreatedMcpToken> = repository.createMcpToken(name)
}

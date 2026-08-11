package com.awan.app.core.domain.mcp.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.repository.McpRepository
import javax.inject.Inject

class RegenerateMcpTokenUseCase @Inject constructor(
    private val repository: McpRepository,
) {
    suspend operator fun invoke(id: String): Result<CreatedMcpToken> = repository.regenerateMcpToken(id)
}

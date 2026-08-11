package com.awan.app.core.domain.mcp.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.repository.McpRepository
import javax.inject.Inject

class DeleteMcpTokenUseCase @Inject constructor(
    private val repository: McpRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteMcpToken(id)
}

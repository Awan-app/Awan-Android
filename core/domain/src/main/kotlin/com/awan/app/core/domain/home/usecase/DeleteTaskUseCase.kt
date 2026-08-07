package com.awan.app.core.domain.home.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.repository.HomeRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    suspend operator fun invoke(taskId: String): Result<Unit> =
        homeRepository.deleteTask(taskId)
}

package com.awan.app.core.domain.home.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.repository.HomeRepository
import javax.inject.Inject

class UpdateTaskDetailUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    suspend operator fun invoke(
        taskId: String,
        title: String? = null,
        description: String? = null,
        estimatedDuration: Int? = null,
        estimatedPoints: Int? = null,
        mandatory: Boolean? = null,
        allowTaskSplitting: Boolean? = null,
    ): Result<Unit> = homeRepository.updateTaskDetails(
        taskId = taskId,
        title = title,
        description = description,
        estimatedDuration = estimatedDuration,
        estimatedPoints = estimatedPoints,
        mandatory = mandatory,
        allowTaskSplitting = allowTaskSplitting,
    )
}

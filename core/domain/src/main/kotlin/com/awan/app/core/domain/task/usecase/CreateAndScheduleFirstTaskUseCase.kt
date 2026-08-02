package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.AiTaskRepository
import com.awan.app.core.domain.onboarding.model.FirstTask
import javax.inject.Inject

class CreateAndScheduleFirstTaskUseCase @Inject constructor(
    private val aiTaskRepository: AiTaskRepository,
) {
    suspend operator fun invoke(title: String): Result<FirstTask?> =
        aiTaskRepository.createAndScheduleTask(title.trim())
}

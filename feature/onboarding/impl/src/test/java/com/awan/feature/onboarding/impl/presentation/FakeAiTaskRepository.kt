package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.AiTaskRepository
import com.awan.app.core.domain.onboarding.model.FirstTask

class FakeAiTaskRepository : AiTaskRepository {
    var requestedTitle: String? = null
    var failWith: AppError? = null

    /** Null means "created, but the engine found no slot" — the same contract as the real one. */
    var scheduled: FirstTask? = FirstTask(
        id = "task-123",
        title = "Write brief",
        zoneId = FakeTemplateRepository.SERVER_ID_PREFIX + "study",
        startMinutes = 9 * 60,
        durationMinutes = 45,
    )

    override suspend fun createAndScheduleTask(title: String): Result<FirstTask?> {
        requestedTitle = title
        return failWith?.let { Result.Error(it) } ?: Result.Success(scheduled)
    }
}

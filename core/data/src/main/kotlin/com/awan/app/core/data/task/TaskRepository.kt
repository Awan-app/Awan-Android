package com.awan.app.core.data.task

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.TaskInfoResponse

interface TaskRepository {
    suspend fun createTask(
        title: String,
        description: String? = null,
        estimatedDurationMinutes: Int? = null,
        mandatory: Boolean? = false,
        estimatedPoints: Int? = 0,
        allowTaskSplitting: Boolean? = false,
        goalId: String? = null,
    ): Result<TaskInfoResponse>
}

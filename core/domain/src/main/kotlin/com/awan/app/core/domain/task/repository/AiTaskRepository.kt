package com.awan.app.core.domain.task.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.FirstTask

interface AiTaskRepository {

    /**
     * Creates the task from a bare [title] — the AI fills in duration, points and category — then
     * runs it through the scheduling engine. A success carrying `null` means the task was created
     * but the engine found no slot for it; the task still exists, unscheduled.
     */
    suspend fun createAndScheduleTask(title: String): Result<FirstTask?>
}

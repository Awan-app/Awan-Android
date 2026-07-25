package com.awan.app.core.domain.goal.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.Goal

interface GoalRepository {
    suspend fun getGoals(): Result<List<Goal>>
}

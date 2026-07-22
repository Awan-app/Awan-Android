package com.awan.app.core.scheduling.repositories

import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.entities.Goal
import com.awan.app.core.scheduling.entities.Session
import com.awan.app.core.scheduling.entities.Zone
import java.util.UUID

interface GoalRepository {
    suspend fun fetchGoals(): List<Goal>
    suspend fun addGoal(goal: Goal)
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(id: UUID)
    suspend fun deleteAllGoals()
}

interface SessionRepository {
    suspend fun fetchSessions(): List<Session>
    suspend fun addSession(session: Session)
    suspend fun updateSession(session: Session)
    suspend fun deleteSession(id: UUID)
    suspend fun deleteSessions(taskID: UUID)
    suspend fun deleteAllSessions()
}

interface TaskRepository {
    suspend fun fetchTasks(): List<AwanTask>
    suspend fun addTask(task: AwanTask)
    suspend fun updateTask(task: AwanTask)
    suspend fun deleteTask(id: UUID)
    suspend fun deleteAllTasks()
}

interface ZoneRepository {
    suspend fun fetchZones(): List<Zone>
    suspend fun updateZone(zone: Zone)
    suspend fun resetZones()
}

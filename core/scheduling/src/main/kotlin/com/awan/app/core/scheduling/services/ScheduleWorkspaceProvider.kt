package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.repositories.GoalRepository
import com.awan.app.core.scheduling.repositories.SessionRepository
import com.awan.app.core.scheduling.repositories.TaskRepository
import com.awan.app.core.scheduling.repositories.ZoneRepository
import com.awan.app.core.scheduling.valueobjects.ScheduleWorkspace
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface ScheduleWorkspaceProviding {
    suspend fun load(): ScheduleWorkspace
}

class DefaultScheduleWorkspaceProvider(
    private val zoneRepository: ZoneRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository
) : ScheduleWorkspaceProviding {
    override suspend fun load(): ScheduleWorkspace = coroutineScope {
        val zonesDeferred = async { zoneRepository.fetchZones() }
        val goalsDeferred = async { goalRepository.fetchGoals() }
        val tasksDeferred = async { taskRepository.fetchTasks() }
        val sessionsDeferred = async { sessionRepository.fetchSessions() }

        ScheduleWorkspace(
            zones = zonesDeferred.await().sortedBy { it.startTime.minutesSinceMidnight },
            goals = goalsDeferred.await(),
            tasks = tasksDeferred.await(),
            sessions = sessionsDeferred.await()
        )
    }
}

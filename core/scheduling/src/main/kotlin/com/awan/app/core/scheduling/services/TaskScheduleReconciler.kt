package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.conflicts.ScheduleNudge
import com.awan.app.core.scheduling.conflicts.TaskZoneChange
import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.entities.Session
import com.awan.app.core.scheduling.errors.SchedulingException
import com.awan.app.core.scheduling.repositories.SessionRepository
import com.awan.app.core.scheduling.valueobjects.*
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class TaskReconciliationRequest(
    val taskID: UUID,
    val pendingZoneChange: TaskZoneChange?,
    val selectedDay: Instant,
    val now: Instant,
    val timeZone: ZoneId,
    val ignoresFixedOverAllocation: Boolean = false,
    val ignoresFixedZoneMismatch: Boolean = false
)

interface TaskScheduleReconciling {
    suspend fun reconcile(request: TaskReconciliationRequest): ScheduleOperationResult
}

class DefaultTaskScheduleReconciler(
    private val workspaceProvider: ScheduleWorkspaceProviding,
    private val sessionRepository: SessionRepository,
    private val engine: ScheduleEngine,
    private val zoneWindowResolver: ZoneWindowResolving,
    private val idGenerator: UUIDGenerating = SystemUUIDGenerator()
) : TaskScheduleReconciling {

    override suspend fun reconcile(request: TaskReconciliationRequest): ScheduleOperationResult {
        val workspace = workspaceProvider.load()
        val task = workspace.tasks.firstOrNull { it.id == request.taskID }
            ?: throw SchedulingException.EntityNotFound(request.taskID)

        val taskSessions = workspace.sessions.filter { it.taskID == request.taskID }
        val completedSessions = taskSessions.filter { it.status == Session.Status.COMPLETED }
        val plannedFixedSessions = taskSessions.filter { it.status == Session.Status.PLANNED && it.blocking }

        val completedMinutes = completedSessions.sumOf { it.timeRange.durationMinutes }
        val fixedMinutes = plannedFixedSessions.sumOf { it.timeRange.durationMinutes }
        val protectedMinutes = completedMinutes + fixedMinutes

        // Guard: over-allocation by fixed/completed sessions
        if (protectedMinutes > task.duration.minutes && !request.ignoresFixedOverAllocation) {
            return ScheduleOperationResult(
                workspace = workspace,
                nudge = ScheduleNudge.FixedSessionOverAllocation(
                    taskID = request.taskID,
                    pendingZoneChange = request.pendingZoneChange,
                    sessionIDs = (completedSessions + plannedFixedSessions).map { it.id },
                    scheduledMinutes = protectedMinutes,
                    taskMinutes = task.duration.minutes,
                    canTrim = completedMinutes <= task.duration.minutes && plannedFixedSessions.isNotEmpty(),
                    selectedDay = request.selectedDay,
                    timeZone = request.timeZone
                )
            )
        }

        // Guard: fixed sessions outside of new zone
        if (request.pendingZoneChange != null && !request.ignoresFixedZoneMismatch) {
            val zoneID = task.zoneID
            val zone = if (zoneID != null) workspace.zones.firstOrNull { it.id == zoneID } else null

            if (zone != null) {
                val affectedSessionIDs = plannedFixedSessions.mapNotNull { session ->
                    val window = zoneWindowResolver.window(zone, session.timeRange.start, request.timeZone)
                    val isInsideZone = !session.timeRange.start.isBefore(window.start) &&
                            !session.timeRange.end.isAfter(window.end)
                    if (isInsideZone) null else session.id
                }
                if (affectedSessionIDs.isNotEmpty()) {
                    return ScheduleOperationResult(
                        workspace = workspace,
                        nudge = ScheduleNudge.FixedSessionsOutsideTaskZone(
                            taskID = request.taskID,
                            previousZoneID = request.pendingZoneChange.previousZoneID,
                            zoneID = zoneID!!,
                            sessionIDs = affectedSessionIDs,
                            selectedDay = request.selectedDay,
                            timeZone = request.timeZone
                        )
                    )
                }
            }
        }

        // Guard: task has no zone — nothing to schedule
        if (task.zoneID == null) {
            return ScheduleOperationResult(workspace = workspace, nudge = null)
        }

        // Build dependency closure and check for unavailable dependencies
        val schedulingTasks = dependencyClosure(task, workspace.tasks)
        val unavailableDependencies = task.dependencyIDs.filter { depID ->
            val dependency = workspace.tasks.firstOrNull { it.id == depID } ?: return@filter true
            val scheduled = workspace.sessions
                .filter { it.taskID == depID && it.contributesScheduledWork }
                .sumOf { it.timeRange.durationMinutes }
            scheduled < dependency.duration.minutes
        }.toSet()

        if (unavailableDependencies.isNotEmpty()) {
            val remainingMinutes = maxOf(0, task.duration.minutes - protectedMinutes)
            return ScheduleOperationResult(
                workspace = workspace,
                nudge = ScheduleNudge.SchedulingIssueNudge(
                    SchedulingIssue(
                        taskID = request.taskID,
                        reason = SchedulingIssueReason.DependencyUnavailable(unavailableDependencies),
                        requiredMinutes = remainingMinutes,
                        availableMinutes = 0,
                        resolutionCandidates = emptyList()
                    )
                )
            )
        }

        // Run the engine and persist any new sessions
        val result = engine.makePlan(
            SchedulingSnapshot(
                planningDay = request.selectedDay,
                now = request.now,
                timeZone = request.timeZone,
                zones = workspace.zones,
                goals = workspace.goals,
                tasks = schedulingTasks,
                sessions = workspace.sessions
            )
        )

        for (draft in result.todaySessionDrafts.filter { it.taskID == request.taskID }) {
            sessionRepository.addSession(makeSession(draft))
        }

        for (updated in result.sessionUpdates.filter { it.taskID == request.taskID }) {
            sessionRepository.updateSession(updated)
        }

        val updatedWorkspace = workspaceProvider.load()
        val nudge = result.issues
            .firstOrNull { it.taskID == request.taskID }
            ?.let { ScheduleNudge.SchedulingIssueNudge(it) }

        return ScheduleOperationResult(workspace = updatedWorkspace, nudge = nudge)
    }

    private fun dependencyClosure(task: AwanTask, allTasks: List<AwanTask>): List<AwanTask> {
        val tasksByID = allTasks.associateBy { it.id }
        val resultByID = mutableMapOf<UUID, AwanTask>()

        fun include(current: AwanTask) {
            if (resultByID.containsKey(current.id)) return
            resultByID[current.id] = current
            for (depID in current.dependencyIDs) {
                val dep = tasksByID[depID]
                    ?: throw SchedulingException.MissingDependency(current.id, depID)
                include(dep)
            }
        }

        include(task)
        return resultByID.values.toList()
    }

    private fun makeSession(draft: SessionDraft): Session {
        return Session(
            id = idGenerator.makeUUID(),
            taskID = draft.taskID,
            zoneID = draft.zoneID,
            timeRange = draft.timeRange,
            blocking = false,
            status = Session.Status.PLANNED
        )
    }
}

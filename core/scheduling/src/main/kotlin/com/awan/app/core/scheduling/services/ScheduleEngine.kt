package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.entities.Session
import com.awan.app.core.scheduling.entities.Zone
import com.awan.app.core.scheduling.errors.SchedulingException
import com.awan.app.core.scheduling.valueobjects.*
import java.time.Instant
import java.util.UUID

interface ScheduleEngine {
    fun makePlan(snapshot: SchedulingSnapshot): SchedulingResult
}

class DefaultScheduleEngine(
    private val dependencyOrdering: TaskDependencyOrdering = StableTaskDependencySorter(),
    private val zoneWindowResolver: ZoneWindowResolving = CalendarZoneWindowResolver(),
    private val availabilityCalculator: AvailabilityCalculating = DefaultAvailabilityCalculator(),
    private val resolutionCandidateGenerator: ResolutionCandidateGenerating? = null
) : ScheduleEngine {

    private val candidateGenerator: ResolutionCandidateGenerating = resolutionCandidateGenerator
        ?: DefaultResolutionCandidateGenerator(zoneWindowResolver, availabilityCalculator)

    override fun makePlan(snapshot: SchedulingSnapshot): SchedulingResult {
        val zonesByID = snapshot.zones.associateBy { it.id }

        // Validate all zone references before starting
        validateZoneReferences(snapshot.tasks, zonesByID)

        val orderedTasks = dependencyOrdering.order(snapshot.tasks)
        val activeSessions = snapshot.sessions.filter { it.occupiesTime }
        val workSessions = snapshot.sessions.filter { it.contributesScheduledWork }

        val occupiedRanges = (activeSessions.map { it.timeRange } + snapshot.unavailableTime).toMutableList()
        val todayDrafts = mutableListOf<SessionDraft>()
        val sessionUpdates = mutableListOf<Session>()
        val issues = mutableListOf<SchedulingIssue>()
        val completionByTaskID = existingCompletionTimes(orderedTasks, workSessions).toMutableMap()

        orderedTasksLoop@for (task in orderedTasks) {
            val taskSessions = workSessions.filter { it.taskID == task.id }
            val existingMinutes = taskSessions.sumOf { it.timeRange.durationMinutes }
            val remainingMinutes = maxOf(0, task.duration.minutes - existingMinutes)

            if (remainingMinutes <= 0) continue

            // Guard: all dependencies must be completed first
            val unavailableDependencies = task.dependencyIDs.filter { completionByTaskID[it] == null }.toSet()
            if (unavailableDependencies.isNotEmpty()) {
                issues.add(
                    SchedulingIssue(
                        taskID = task.id,
                        reason = SchedulingIssueReason.DependencyUnavailable(unavailableDependencies),
                        requiredMinutes = remainingMinutes,
                        availableMinutes = 0,
                        resolutionCandidates = emptyList()
                    )
                )
                continue
            }

            // Guard: task must have a zone assigned
            val zoneID = task.zoneID ?: run {
                issues.add(
                    SchedulingIssue(
                        taskID = task.id,
                        reason = SchedulingIssueReason.ZoneRequiredForAutomaticScheduling,
                        requiredMinutes = remainingMinutes,
                        availableMinutes = 0,
                        resolutionCandidates = emptyList()
                    )
                )
                continue
            }

            val zone = zonesByID[zoneID]
                ?: throw SchedulingException.MissingZone(task.id, zoneID)

            val earliestDependencyEnd = task.dependencyIDs
                .mapNotNull { completionByTaskID[it] }
                .maxOrNull() ?: snapshot.planningDay
            val earliestAllowedStart = if (earliestDependencyEnd.isAfter(snapshot.now)) earliestDependencyEnd else snapshot.now

            val todayWindow = zoneWindowResolver.window(zone, snapshot.planningDay, snapshot.timeZone)
            val freeRanges = availabilityCalculator.freeRanges(todayWindow, occupiedRanges, earliestAllowedStart)

            val minimum = snapshot.configuration.minimumSessionMinutes

            // Intended rule: A sub-minimum remainder is never its own session — it is absorbed into 
            // one of the task's PLANNED sessions by extending that session's end.
            if (remainingMinutes < minimum && existingMinutes > 0) {
                val extendable = taskSessions.firstOrNull { it.status == Session.Status.PLANNED }
                if (extendable != null) {
                    val extendedEnd = extendable.timeRange.end.plusSeconds(remainingMinutes * 60L)
                    val proposedRange = TimeRange(extendable.timeRange.start, extendedEnd)
                    val extraSlice = TimeRange(extendable.timeRange.end, extendedEnd)
                    
                    val isSliceFree = occupiedRanges.none { it.overlaps(extraSlice) }
                    val isInsideWindow = !extendedEnd.isAfter(todayWindow.end)

                    if (isSliceFree && isInsideWindow) {
                        val updated = extendable.replacing(timeRange = proposedRange)
                        sessionUpdates.add(updated)
                        occupiedRanges.add(extraSlice)
                        completionByTaskID[task.id] = latestCompletion(task.id, workSessions, todayDrafts, sessionUpdates)!!
                        continue
                    }
                }
            }

            // Happy path: task fits into a free range
            val fitRange = freeRanges.firstOrNull { it.durationMinutes >= remainingMinutes }
            if (fitRange != null) {
                val end = fitRange.start.plusSeconds(remainingMinutes * 60L)
                val draft = SessionDraft(task.id, zoneID, TimeRange(fitRange.start, end))
                todayDrafts.add(draft)
                occupiedRanges.add(draft.timeRange)
                // Fix: latestCompletion returns Instant? but map expects Instant. 
                // Since we just added a draft, it won't be null.
                completionByTaskID[task.id] = latestCompletion(task.id, workSessions, todayDrafts, sessionUpdates)!!
                continue
            }

            // Task doesn't fit — generate resolution candidates
            val context = ResolutionContext(
                snapshot = snapshot,
                task = task,
                zone = zone,
                remainingMinutes = remainingMinutes,
                todayWindow = todayWindow,
                todayFreeRanges = freeRanges,
                occupiedRanges = occupiedRanges,
                earliestAllowedStart = earliestAllowedStart
            )
            val candidates = candidateGenerator.candidates(context)
            issues.add(
                SchedulingIssue(
                    taskID = task.id,
                    reason = SchedulingIssueReason.InsufficientZoneTime,
                    requiredMinutes = remainingMinutes,
                    availableMinutes = freeRanges.sumOf { it.durationMinutes },
                    resolutionCandidates = candidates
                )
            )
        }

        return SchedulingResult(
            todaySessionDrafts = todayDrafts,
            sessionUpdates = sessionUpdates,
            issues = issues
        )
    }

    private fun validateZoneReferences(tasks: List<AwanTask>, zonesByID: Map<UUID, Zone>) {
        for (task in tasks) {
            val zoneID = task.zoneID ?: continue
            if (zonesByID[zoneID] == null) {
                throw SchedulingException.MissingZone(task.id, zoneID)
            }
        }
    }

    private fun existingCompletionTimes(
        tasks: List<AwanTask>,
        sessions: List<Session>
    ): Map<UUID, Instant> {
        val result = mutableMapOf<UUID, Instant>()
        for (task in tasks) {
            val taskSessions = sessions.filter { it.taskID == task.id }
            val scheduledMinutes = taskSessions.sumOf { it.timeRange.durationMinutes }
            if (scheduledMinutes >= task.duration.minutes) {
                taskSessions.mapNotNull { it.timeRange.end }.maxOrNull()?.let {
                    result[task.id] = it
                }
            }
        }
        return result
    }

    private fun latestCompletion(
        taskID: UUID,
        sessions: List<Session>,
        drafts: List<SessionDraft>,
        updates: List<Session>
    ): Instant? {
        val sessionEnds = sessions.filter { it.taskID == taskID }.map { it.timeRange.end }
        val draftEnds = drafts.filter { it.taskID == taskID }.map { it.timeRange.end }
        val updateEnds = updates.filter { it.taskID == taskID }.map { it.timeRange.end }
        return (sessionEnds + draftEnds + updateEnds).maxOrNull()
    }
}

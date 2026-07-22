package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.entities.Zone
import com.awan.app.core.scheduling.valueobjects.*
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

data class ResolutionContext(
    val snapshot: SchedulingSnapshot,
    val task: AwanTask,
    val zone: Zone,
    val remainingMinutes: Int,
    val todayWindow: TimeRange,
    val todayFreeRanges: List<TimeRange>,
    val occupiedRanges: List<TimeRange>,
    val earliestAllowedStart: Instant?
)

interface ResolutionCandidateGenerating {
    fun candidates(context: ResolutionContext): List<ResolutionCandidate>
}

class DefaultResolutionCandidateGenerator(
    private val zoneWindowResolver: ZoneWindowResolving = CalendarZoneWindowResolver(),
    private val availabilityCalculator: AvailabilityCalculating = DefaultAvailabilityCalculator()
) : ResolutionCandidateGenerating {

    override fun candidates(context: ResolutionContext): List<ResolutionCandidate> {
        val candidates = mutableListOf<ResolutionCandidate>()

        // Strategy 1: Split sessions within today (only if task is splittable)
        if (context.task.isSplittable) {
            val drafts = splitWithinToday(context)
            if (drafts != null && drafts.size > 1) {
                candidates.add(
                    ResolutionCandidate(
                        taskID = context.task.id,
                        kind = ResolutionKind.SPLIT_WITHIN_TODAY,
                        sessionDrafts = drafts,
                        consequences = listOf(ResolutionConsequence.SplitsTask(drafts.size))
                    )
                )
            }
        }

        // Strategy 2: Continue past the zone end time
        continuePastZone(context)?.let { candidates.add(it) }

        // Strategy 3: Split across days (only if task is splittable)
        if (context.task.isSplittable) {
            splitAcrossDays(context)?.let { candidates.add(it) }
        }

        // Strategy 4: Schedule on the next available future day
        scheduleOnNextAvailableDay(context)?.let { candidates.add(it) }

        return candidates
    }

    private fun splitWithinToday(context: ResolutionContext): List<SessionDraft>? {
        return buildDrafts(
            task = context.task,
            zoneID = context.zone.id,
            minutes = context.remainingMinutes,
            freeRanges = context.todayFreeRanges,
            minimumSessionMinutes = context.snapshot.configuration.minimumSessionMinutes
        )
    }

    private fun continuePastZone(context: ResolutionContext): ResolutionCandidate? {
        val durationSeconds = context.remainingMinutes * 60L
        val options = context.todayFreeRanges.mapNotNull { freeRange ->
            val end = freeRange.start.plusSeconds(durationSeconds)
            if (!end.isAfter(context.todayWindow.end)) return@mapNotNull null

            val proposed = TimeRange(freeRange.start, end)
            if (context.occupiedRanges.any { proposed.overlaps(it) }) return@mapNotNull null

            SessionDraft(taskID = context.task.id, zoneID = context.zone.id, timeRange = proposed)
        }

        val draft = options.minByOrNull { session ->
            val extension = session.timeRange.end.epochSecond - context.todayWindow.end.epochSecond
            extension * 1_000_000_000L + session.timeRange.start.epochSecond
        } ?: return null

        val extensionMinutes = ((draft.timeRange.end.epochSecond - context.todayWindow.end.epochSecond) / 60).toInt()
        return ResolutionCandidate(
            taskID = context.task.id,
            kind = ResolutionKind.CONTINUE_PAST_ZONE,
            sessionDrafts = listOf(draft),
            consequences = listOf(ResolutionConsequence.ExtendsZone(extensionMinutes))
        )
    }

    private fun splitAcrossDays(context: ResolutionContext): ResolutionCandidate? {
        val minimum = context.snapshot.configuration.minimumSessionMinutes
        for (todayRange in context.todayFreeRanges) {
            if (todayRange.durationMinutes < minimum) continue
            val todayMinutes = minOf(todayRange.durationMinutes, context.remainingMinutes - minimum)
            if (todayMinutes < minimum) continue

            val remaining = context.remainingMinutes - todayMinutes
            val futureDraft = firstFutureDraft(context, remaining) ?: continue

            val todayEnd = todayRange.start.plusSeconds(todayMinutes * 60L)
            val todayDraft = SessionDraft(
                taskID = context.task.id,
                zoneID = context.zone.id,
                timeRange = TimeRange(todayRange.start, todayEnd)
            )
            val drafts = listOf(todayDraft, futureDraft)
            return ResolutionCandidate(
                taskID = context.task.id,
                kind = ResolutionKind.SPLIT_ACROSS_DAYS,
                sessionDrafts = drafts,
                consequences = listOf(
                    ResolutionConsequence.SplitsTask(drafts.size),
                    ResolutionConsequence.UsesFutureDay
                )
            )
        }
        return null
    }

    private fun scheduleOnNextAvailableDay(context: ResolutionContext): ResolutionCandidate? {
        val draft = firstFutureDraft(context, context.remainingMinutes) ?: return null
        return ResolutionCandidate(
            taskID = context.task.id,
            kind = ResolutionKind.SCHEDULE_NEXT_AVAILABLE_DAY,
            sessionDrafts = listOf(draft),
            consequences = listOf(ResolutionConsequence.UsesFutureDay)
        )
    }

    private fun firstFutureDraft(context: ResolutionContext, requiredMinutes: Int): SessionDraft? {
        val planningZoned = ZonedDateTime.ofInstant(context.snapshot.planningDay, context.snapshot.timeZone)
        for (dayOffset in 1..context.snapshot.configuration.futureSearchDayLimit) {
            val futureDay = planningZoned.plusDays(dayOffset.toLong()).toInstant()
            val window = zoneWindowResolver.window(context.zone, futureDay, context.snapshot.timeZone)
            val freeRanges = availabilityCalculator.freeRanges(window, context.occupiedRanges, context.earliestAllowedStart)
            val freeRange = freeRanges.firstOrNull { it.durationMinutes >= requiredMinutes } ?: continue
            val end = freeRange.start.plusSeconds(requiredMinutes * 60L)
            return SessionDraft(
                taskID = context.task.id,
                zoneID = context.zone.id,
                timeRange = TimeRange(freeRange.start, end)
            )
        }
        return null
    }

    private fun buildDrafts(
        task: AwanTask,
        zoneID: UUID,
        minutes: Int,
        freeRanges: List<TimeRange>,
        minimumSessionMinutes: Int
    ): List<SessionDraft>? {
        var remaining = minutes
        val result = mutableListOf<SessionDraft>()

        for (range in freeRanges) {
            if (remaining <= 0) break
            if (remaining <= range.durationMinutes) {
                if (remaining < minimumSessionMinutes) return null
                val end = range.start.plusSeconds(remaining * 60L)
                result.add(SessionDraft(task.id, zoneID, TimeRange(range.start, end)))
                remaining = 0
                break
            }
            val maxWithoutShortRemainder = remaining - minimumSessionMinutes
            val allocated = minOf(range.durationMinutes, maxWithoutShortRemainder)
            if (allocated < minimumSessionMinutes) continue
            val end = range.start.plusSeconds(allocated * 60L)
            result.add(SessionDraft(task.id, zoneID, TimeRange(range.start, end)))
            remaining -= allocated
        }

        return if (remaining == 0) result else null
    }
}

package com.awan.app.core.data.home.mapper

import com.awan.app.core.data.util.minutesOfDay
import com.awan.app.core.data.util.parseIsoDateTime
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.DaySession
import com.awan.app.core.domain.home.model.DayZone
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.zone.ZoneDto
import java.time.LocalDate

internal object HomeMapper {

    fun toDaySchedule(
        date: LocalDate,
        zones: List<ZoneDto>,
        taskEntries: List<TaskWithSessionsDto>,
    ): DaySchedule {
        val dayZones = zones.map { it.toDayZone() }
        val daySessions = taskEntries.flatMap { entry ->
            entry.sessions.map { session ->
                session.toDaySession(
                    task = entry.task,
                )
            }
        }
        return DaySchedule(date = date, zones = dayZones, sessions = daySessions)
    }

    // ── Zone ─────────────────────────────────────────────────────────────

    private fun ZoneDto.toDayZone(): DayZone = DayZone(
        id = id.orEmpty(),
        name = name,
        categoryId = category?.id ?: id.orEmpty(),
        categoryName = category?.name ?: name,
        startMinutes = parseTimeToMinutes(startTime),
        endMinutes = parseTimeToMinutes(endTime),
        color = color,
    )

    // ── Session ───────────────────────────────────────────────────────────

    private fun SessionDto.toDaySession(task: TaskInfoResponse): DaySession {
        val startDt = parseIsoDateTime(start)
        val endDt = parseIsoDateTime(end)
        val startMin: Int
        val duration: Int
        if (startDt != null && endDt != null) {
            startMin = startDt.minutesOfDay()
            val betweenMins = java.time.Duration.between(startDt, endDt).toMinutes().toInt()
            duration = when {
                betweenMins > 0 -> betweenMins
                betweenMins < 0 && startDt.toLocalDate() == endDt.toLocalDate() -> {
                    val adjustedEnd = endDt.plusDays(1)
                    val adjMins = java.time.Duration.between(startDt, adjustedEnd).toMinutes().toInt()
                    if (adjMins > 0) adjMins else 0
                }
                else -> 0
            }
        } else {
            startMin = parseIsoTimeToMinutes(start)
            val rawEndMin = parseIsoTimeToMinutes(end)
            duration = when {
                rawEndMin > startMin -> rawEndMin - startMin
                rawEndMin < startMin -> (rawEndMin + 24 * 60) - startMin
                else -> 0
            }
        }

        return DaySession(
            id = id,
            taskId = task.id,
            taskTitle = task.title,
            zoneId = zoneId,
            startMinutes = startMin,
            durationMinutes = duration,
            status = parseSessionStatus(status.orEmpty()),
            locked = locked,
            points = task.estimatedPoints,
            categoryId = task.category?.id,
            categoryName = task.category?.name,
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Parses "HH:mm:ss" (or "HH:mm") into total minutes since midnight.
     * Returns 0 on parse failure.
     */
    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        val hours   = parts.getOrNull(0)?.toIntOrNull() ?: return 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    private fun parseIsoTimeToMinutes(isoString: String): Int {
        return try {
            val timePart = if (isoString.contains("T")) isoString.substringAfter("T") else isoString
            val parts = timePart.split(":")
            val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
            hours * 60 + minutes
        } catch (_: Exception) {
            0
        }
    }

    private fun parseSessionStatus(raw: String?): SessionStatus = when (raw?.uppercase()) {
        "COMPLETED"   -> SessionStatus.COMPLETED
        "IN_PROGRESS" -> SessionStatus.IN_PROGRESS
        "CANCELLED"   -> SessionStatus.CANCELLED
        else          -> SessionStatus.SCHEDULED
    }
}

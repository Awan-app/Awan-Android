package com.awan.app.core.data.home.mapper

import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.DaySession
import com.awan.app.core.domain.home.model.DayZone
import com.awan.app.core.domain.home.model.SessionStatus
import com.awan.app.core.network.dto.SessionDto
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskWithSessionsDto
import com.awan.app.core.network.dto.ZoneDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


internal object HomeMapper {

    private val dtFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

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
        id = id,
        name = name,
        categoryId = category?.id ?: id,
        categoryName = category?.name ?: name,
        startMinutes = parseTimeToMinutes(startTime),
        endMinutes = parseTimeToMinutes(endTime),
        color = color,
    )

    // ── Session ───────────────────────────────────────────────────────────

    private fun SessionDto.toDaySession(task: TaskInfoResponse): DaySession {
        val startDt = runCatching { LocalDateTime.parse(start, dtFormatter) }.getOrNull()
        val endDt   = runCatching { LocalDateTime.parse(end, dtFormatter) }.getOrNull()

        val startMin = startDt?.let { it.hour * 60 + it.minute } ?: 0
        val endMin   = endDt?.let { it.hour * 60 + it.minute } ?: (startMin + 30)
        val duration = (endMin - startMin).coerceAtLeast(1)

        return DaySession(
            id = id,
            taskId = task.id,
            taskTitle = task.title,
            zoneId = zoneId,
            startMinutes = startMin,
            durationMinutes = duration,
            status = parseSessionStatus(status),
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

    private fun parseSessionStatus(raw: String): SessionStatus = when (raw.uppercase()) {
        "COMPLETED"   -> SessionStatus.COMPLETED
        "IN_PROGRESS" -> SessionStatus.IN_PROGRESS
        "CANCELLED"   -> SessionStatus.CANCELLED
        else          -> SessionStatus.SCHEDULED
    }
}

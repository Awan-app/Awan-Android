package com.awan.feature.home.impl.ui

import androidx.compose.ui.graphics.Color
import com.awan.app.core.designsystem.CategoryProgressSegment
import com.awan.app.core.designsystem.ScheduleSession
import com.awan.app.core.designsystem.ScheduleZone
import com.awan.app.core.designsystem.TaskCategory
import com.awan.app.core.designsystem.TaskStatus
import com.awan.app.core.domain.home.model.DaySession
import com.awan.app.core.domain.home.model.DayZone
import com.awan.app.core.domain.home.model.SessionStatus

internal fun DayZone.toUiZone(): ScheduleZone = ScheduleZone(
    id = id,
    categoryId = categoryId,
    category = resolveCategoryWithColor(categoryName, color),
    startHour = startMinutes / 60,
    endHour = ceilHour(endMinutes),
    isCollapsed = false,
)

internal fun resolveNonOverlappingZones(zones: List<ScheduleZone>): List<ScheduleZone> {
    if (zones.size <= 1) return zones

    val sorted = zones.sortedBy { it.startHour }
    val result = mutableListOf<ScheduleZone>()

    var lastEndHour = 0

    for (zone in sorted) {
        val adjustedStart = zone.startHour.coerceAtLeast(lastEndHour).coerceIn(0, 23)
        val originalDuration = (zone.endHour - zone.startHour).coerceAtLeast(1)
        val adjustedEnd = (adjustedStart + originalDuration).coerceIn(adjustedStart + 1, 24)

        val updatedZone = zone.copy(
            startHour = adjustedStart,
            endHour = adjustedEnd,
        )

        result.add(updatedZone)
        lastEndHour = adjustedEnd
    }

    return result
}

internal fun DaySession.toUiSession(zoneById: Map<String, ScheduleZone>): ScheduleSession? {
    val zone = (if (zoneId != null) zoneById[zoneId] else null)
        ?: findMatchingZoneForSession(this, zoneById)
        ?: return null
    val resolvedZoneId = zone.id
    val catName = categoryName
    val category = if (!catName.isNullOrBlank()) {
        resolveCategory(catName)
    } else {
        zone.category
    }
    return ScheduleSession(
        id = id,
        zoneId = resolvedZoneId,
        taskId = taskId,
        taskTitle = taskTitle,
        startMinutes = startMinutes,
        durationMinutes = durationMinutes,
        category = category,
        status = resolveTaskStatus(status, locked),
        isFixed = locked,
        points = points,
    )
}

private fun findMatchingZoneForSession(
    session: DaySession,
    zoneById: Map<String, ScheduleZone>,
): ScheduleZone? {
    if (zoneById.isEmpty()) return null
    val sessionCategory = session.categoryName?.let { resolveCategory(it) } ?: TaskCategory.Personal
    return zoneById.values.find { it.category == sessionCategory }
        ?: zoneById.values.find { it.category == TaskCategory.Personal }
        ?: zoneById.values.firstOrNull()
}

internal fun resolveCategoryWithColor(name: String, hexColor: String?): TaskCategory {
    val namedCategory = resolveCategoryByName(name)
    if (namedCategory != TaskCategory.Personal) return namedCategory
    return if (!hexColor.isNullOrBlank()) {
        hexToTaskCategory(id = name, name = name, hex = hexColor)
    } else {
        TaskCategory.Personal
    }
}

internal fun resolveCategory(name: String): TaskCategory = resolveCategoryByName(name)

internal fun resolveCategoryByName(name: String): TaskCategory {
    val lower = name.lowercase().trim()
    return when {
        lower == "study" || lower.contains("study") ||
            lower.contains("education") || lower.contains("learning") -> TaskCategory.Study
        lower == "work" || lower.contains("work") ||
            lower.contains("professional") || lower.contains("career") -> TaskCategory.Work
        lower.contains("play") || lower.contains("gaming") ||
            lower.contains("entertainment") || lower.contains("fun") ||
            lower.contains("hobby") -> TaskCategory.Play
        else -> TaskCategory.Personal
    }
}

internal fun hexToTaskCategory(id: String, name: String, hex: String): TaskCategory {
    return try {
        val clean = hex.trimStart('#')
        val argb = when (clean.length) {
            6 -> (0xFF shl 24) or clean.toLong(16).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return TaskCategory.Personal
        }
        val base = Color(argb)
        TaskCategory(
            id = id,
            name = name,
            color = base,
            containerColor = base.copy(alpha = 0.10f),
            borderColor = base.copy(alpha = 0.30f),
        )
    } catch (_: Exception) {
        TaskCategory.Personal
    }
}

internal fun resolveTaskStatus(status: SessionStatus, locked: Boolean): TaskStatus = when {
    status == SessionStatus.COMPLETED -> TaskStatus.Completed
    locked                            -> TaskStatus.Fixed
    else                              -> TaskStatus.Pending
}

internal fun ceilHour(minutes: Int): Int =
    minutes / 60 + if (minutes % 60 > 0) 1 else 0

internal fun buildProgressSegments(sessions: List<ScheduleSession>): List<CategoryProgressSegment> {
    val sorted = sessions.sortedBy { it.startMinutes }
    return sorted.map { session ->
        CategoryProgressSegment(
            color = session.category.color,
            weight = session.durationMinutes.toFloat().coerceAtLeast(15f),
            isCompleted = session.status is TaskStatus.Completed,
        )
    }
}

package com.awan.app.core.scheduling.entities

import com.awan.app.core.scheduling.valueobjects.TaskDuration
import com.awan.app.core.scheduling.valueobjects.TimeRange
import com.awan.app.core.scheduling.valueobjects.LocalTime
import com.awan.app.core.scheduling.valueobjects.ZoneColor
import java.time.Instant
import java.util.UUID

data class AwanTask(
    val id: UUID,
    val title: String = "Untitled Task",
    val goalID: UUID? = null,
    val zoneID: UUID? = null,
    val duration: TaskDuration,
    val isSplittable: Boolean,
    val dependencyIDs: Set<UUID> = emptySet()
)

data class Goal(
    val id: UUID,
    val name: String,
    val deadline: Instant
)

data class Session(
    val id: UUID,
    val taskID: UUID,
    val zoneID: UUID?,
    val timeRange: TimeRange,
    val blocking: Boolean,
    val status: Status
) {
    enum class Status {
        PLANNED,
        COMPLETED,
        MISSED,
        CANCELLED
    }

    val contributesScheduledWork: Boolean
        get() = status == Status.PLANNED || status == Status.COMPLETED

    val occupiesTime: Boolean
        get() = status != Status.MISSED && status != Status.CANCELLED

    fun replacing(
        zoneID: UpdateField<UUID?> = UpdateField.Keep,
        timeRange: TimeRange? = null,
        blocking: Boolean? = null,
        status: Status? = null
    ): Session {
        return Session(
            id = this.id,
            taskID = this.taskID,
            zoneID = when (zoneID) {
                is UpdateField.Keep -> this.zoneID
                is UpdateField.Set -> zoneID.value
            },
            timeRange = timeRange ?: this.timeRange,
            blocking = blocking ?: this.blocking,
            status = status ?: this.status
        )
    }
}

// Sealed class to represent changes where a field can be set to null, set to a value, or kept as-is.
sealed class UpdateField<out T> {
    object Keep : UpdateField<Nothing>()
    data class Set<out T>(val value: T) : UpdateField<T>()
}

data class Zone(
    val id: UUID,
    val name: String,
    val color: ZoneColor,
    val startTime: LocalTime,
    val endTime: LocalTime
)

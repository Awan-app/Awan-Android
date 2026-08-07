package com.awan.app.core.data.goal

import com.awan.app.core.data.task.toTaskModel
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto

internal fun GoalStatusDto.toModel(): GoalStatus = when (this) {
    GoalStatusDto.ACTIVE -> GoalStatus.ACTIVE
    GoalStatusDto.ACHIEVED -> GoalStatus.ACHIEVED
    GoalStatusDto.UNKNOWN -> GoalStatus.UNKNOWN
}

internal fun GoalInfoResponse.toModel(): Goal {
    val (extractedEmoji, cleanTitle) = if (title.isNotEmpty()) {
        val firstCodePoint = title.codePointAt(0)
        val charCount = Character.charCount(firstCodePoint)
        val type = Character.getType(firstCodePoint)
        if (type == Character.OTHER_SYMBOL.toInt() || type == Character.SURROGATE.toInt()) {
            val emojiStr = title.take(charCount)
            val rest = title.substring(charCount).trim()
            emojiStr to rest
        } else {
            "🎯" to title
        }
    } else {
        "🎯" to title
    }

    return Goal(
        id = id,
        title = cleanTitle,
        description = description,
        emoji = extractedEmoji,
        status = status.toModel(),
        tasks = tasks.map { it.toTaskModel() },
    )
}

/**
 * Maps a [GoalEntity] (Room row) to the [Goal] domain model.
 * The entity's [GoalEntity.title] is stored as received from the API and may
 * contain a leading emoji; this mapper extracts it with the same logic.
 */
internal fun GoalEntity.toModel(): Goal {
    val (extractedEmoji, cleanTitle) = if (title.isNotEmpty()) {
        val firstCodePoint = title.codePointAt(0)
        val charCount = Character.charCount(firstCodePoint)
        val type = Character.getType(firstCodePoint)
        if (type == Character.OTHER_SYMBOL.toInt() || type == Character.SURROGATE.toInt()) {
            val emojiStr = title.take(charCount)
            val rest = title.substring(charCount).trim()
            emojiStr to rest
        } else {
            "🎯" to title
        }
    } else {
        "🎯" to title
    }

    val goalStatus = runCatching { GoalStatus.valueOf(status) }.getOrDefault(GoalStatus.UNKNOWN)
    return Goal(
        id = id,
        title = cleanTitle,
        description = description,
        emoji = extractedEmoji,
        status = goalStatus,
        tasks = emptyList(), // tasks are stored separately in TaskEntity
    )
}

/** Maps a network response to a Room entity for local persistence. */
internal fun GoalInfoResponse.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    description = description,
    status = status.name,
    targetDate = targetDate,
    createdAt = createdAt ?: "",
    isInbox = inbox,
)

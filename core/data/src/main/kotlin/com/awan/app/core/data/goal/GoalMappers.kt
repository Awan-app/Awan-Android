package com.awan.app.core.data.goal

import com.awan.app.core.data.task.toTaskModel
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

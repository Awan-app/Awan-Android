package com.awan.app.core.data.goal

import com.awan.app.core.data.task.toModel
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto

internal fun GoalStatusDto.toModel(): GoalStatus = when (this) {
    GoalStatusDto.ACTIVE -> GoalStatus.ACTIVE
    GoalStatusDto.ACHIEVED -> GoalStatus.ACHIEVED
}

internal fun GoalInfoResponse.toModel(): Goal {
    // If the title starts with an emoji, extract it, otherwise default to a target emoji
    val emoji = if (title.isNotEmpty() && Character.isSurrogate(title[0])) {
        title.take(2)
    } else if (title.isNotEmpty() && Character.getType(title[0]) == Character.OTHER_SYMBOL.toInt()) {
        title.take(1)
    } else {
        "🎯"
    }

    // Clean up the title if we extracted an emoji
    val cleanTitle = if (emoji != "🎯" && title.startsWith(emoji)) {
        title.removePrefix(emoji).trim()
    } else {
        title
    }

    return Goal(
        id = id,
        title = cleanTitle,
        description = description,
        emoji = emoji,
        status = status.toModel(),
        tasks = tasks.map { it.toModel() }
    )
}

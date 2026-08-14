package com.awan.app.core.data.goal

import com.awan.app.core.data.task.toTaskModel
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto
import java.text.BreakIterator

internal fun GoalStatusDto.toModel(): GoalStatus = when (this) {
    GoalStatusDto.ACTIVE -> GoalStatus.ACTIVE
    GoalStatusDto.ACHIEVED -> GoalStatus.ACHIEVED
    GoalStatusDto.UNKNOWN -> GoalStatus.UNKNOWN
}

/**
 * Extracts the leading emoji grapheme cluster from a string.
 * Returns a pair of (emoji, remainingTitle). Uses [BreakIterator] to handle
 * multi-codepoint sequences like skin-tone modifiers and ZWJ sequences correctly.
 */
private fun String.extractLeadingEmoji(): Pair<String?, String> {
    if (isEmpty()) return null to this
    val bi = BreakIterator.getCharacterInstance().apply { setText(this@extractLeadingEmoji) }
    val firstEnd = bi.next()
    if (firstEnd == BreakIterator.DONE) return null to this
    val firstGrapheme = substring(0, firstEnd)
    val firstCodePoint = firstGrapheme.codePointAt(0)
    val type = Character.getType(firstCodePoint)
    return if (type == Character.OTHER_SYMBOL.toInt() || type == Character.SURROGATE.toInt()) {
        firstGrapheme to substring(firstEnd).trim()
    } else {
        null to this
    }
}

private fun extractEmojiAndTitle(title: String): Pair<String, String> {
    if (title.isEmpty()) return "\uD83C\uDFAF" to title
    return title.extractLeadingEmoji().let { (emoji, rest) ->
        (emoji ?: "\uD83C\uDFAF") to rest
    }
}

internal fun GoalInfoResponse.toModel(): Goal {
    val (extractedEmoji, cleanTitle) = extractEmojiAndTitle(title)
    return Goal(
        id = id,
        title = cleanTitle,
        description = description,
        emoji = extractedEmoji,
        status = status.toModel(),
        tasks = tasks.map { it.copy(goalId = id).toTaskModel() },
    )
}

/**
 * Maps a [GoalEntity] (Room row) to the [Goal] domain model.
 * The entity's [GoalEntity.title] is stored as received from the API and may
 * contain a leading emoji; this mapper extracts it with the same logic.
 */
internal fun GoalEntity.toModel(): Goal {
    val (extractedEmoji, cleanTitle) = extractEmojiAndTitle(title)
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

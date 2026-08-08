package com.awan.app.core.data.goal

import com.awan.app.core.data.task.toTaskModel
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

internal fun GoalInfoResponse.toModel(): Goal {
    val (extractedEmoji, cleanTitle) = if (title.isNotEmpty()) {
        title.extractLeadingEmoji().let { (emoji, rest) ->
            (emoji ?: "\uD83C\uDFAF") to rest
        }
    } else {
        "\uD83C\uDFAF" to title
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

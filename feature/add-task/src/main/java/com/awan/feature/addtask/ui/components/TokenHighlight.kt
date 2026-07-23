package com.awan.feature.addtask.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.task.parser.TaskToken
import com.awan.app.core.domain.task.parser.TaskTokenKind

private const val TOKEN_BACKGROUND_ALPHA = 0.18f

@Composable
fun rememberTokenHighlight(tokens: List<TaskToken>): VisualTransformation {
    val colors = AwanTheme.colors
    val toneOf: (TaskTokenKind) -> Color = { kind ->
        when (kind) {
            TaskTokenKind.DATE_TIME -> colors.zoneSkyPressed
            TaskTokenKind.DURATION -> colors.zoneVioletPressed
            TaskTokenKind.ZONE -> colors.zoneTangerinePressed
        }
    }
    return TokenHighlightTransformation(tokens, toneOf)
}

/**
 * Tints the recognised spans in place so the user can see exactly which words were taken as
 * scheduling. Character count is untouched, so [OffsetMapping.Identity] is correct and the cursor
 * never drifts.
 */
private class TokenHighlightTransformation(
    private val tokens: List<TaskToken>,
    private val toneOf: (TaskTokenKind) -> Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (tokens.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val annotated = AnnotatedString.Builder(text).apply {
            tokens.forEach { token ->
                val start = token.range.first.coerceIn(0, text.length)
                val end = (token.range.last + 1).coerceIn(start, text.length)
                if (start == end) return@forEach
                val tone = toneOf(token.kind)
                addStyle(
                    SpanStyle(color = tone, background = tone.copy(alpha = TOKEN_BACKGROUND_ALPHA)),
                    start,
                    end,
                )
            }
        }.toAnnotatedString()

        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

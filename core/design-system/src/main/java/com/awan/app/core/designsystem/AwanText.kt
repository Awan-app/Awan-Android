package com.awan.app.core.designsystem

import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * The text appearance a container hands down to the [AwanText]s inside it — a button giving its
 * label the button face, for instance. Compose's own composition-local inheritance, rather than the
 * Styles API's inherited-text-style path, which is unsound in foundation 1.11.4 (see [AwanTextStyle]).
 */
val LocalAwanTextStyle = compositionLocalOf<AwanTextStyle?> { null }

@Composable
fun AwanText(
    text: String,
    modifier: Modifier = Modifier,
    style: AwanTextStyle? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = style ?: LocalAwanTextStyle.current ?: AwanTheme.styles.bodyText
    val color = resolved.color.takeOrElse { LocalContentColor.current }
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved.textStyle.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun AwanText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val color = style.color.takeOrElse { LocalContentColor.current }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun AwanText(
    text: String,
    style: Style,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = LocalAwanTextStyle.current ?: AwanTheme.styles.bodyText
    val color = resolved.color.takeOrElse { LocalContentColor.current }
    BasicText(
        text = text,
        modifier = modifier.styleable(null, style),
        style = resolved.textStyle.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}


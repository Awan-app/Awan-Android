package com.awan.app.core.designsystem

import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

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
    textAlign: TextAlign? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = style ?: LocalAwanTextStyle.current ?: AwanTheme.styles.bodyText
    val textColor = color.takeOrElse { resolved.color }.takeOrElse { LocalContentColor.current }
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved.textStyle.copy(
            color = textColor,
            textAlign = textAlign ?: resolved.textStyle.textAlign,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else resolved.textStyle.fontSize,
            fontWeight = fontWeight ?: resolved.textStyle.fontWeight,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun AwanText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val textColor = color.takeOrElse { style.color }.takeOrElse { LocalContentColor.current }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = textColor,
            textAlign = textAlign ?: style.textAlign,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
            fontWeight = fontWeight ?: style.fontWeight,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun AwanText(
    text: String,
    style: Style,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = LocalAwanTextStyle.current ?: AwanTheme.styles.bodyText
    val textColor = color.takeOrElse { resolved.color }.takeOrElse { LocalContentColor.current }
    BasicText(
        text = text,
        modifier = modifier.styleable(null, style),
        style = resolved.textStyle.copy(
            color = textColor,
            textAlign = textAlign ?: resolved.textStyle.textAlign,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else resolved.textStyle.fontSize,
            fontWeight = fontWeight ?: resolved.textStyle.fontWeight,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

package com.awan.app.core.designsystem

import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun AwanText(
    text: String,
    modifier: Modifier = Modifier,
    style: Style = Style,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    BasicText(
        text = text,
        modifier = modifier.styleable(null, style),
        maxLines = maxLines,
        overflow = overflow,
    )
}

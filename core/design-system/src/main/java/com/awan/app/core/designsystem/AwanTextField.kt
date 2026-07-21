package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/** Skyward rim text field: white surface, sky border + glow on focus. */
@Composable
fun AwanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val colors = AwanTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(if (focused) colors.sky else colors.line, label = "fieldBorder")

    Box(
        modifier = modifier
            .clip(AwanTheme.shapes.card)
            .background(colors.surface)
            .border(2.dp, borderColor, AwanTheme.shapes.card)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            AwanText(placeholder, style = AwanTheme.styles.fieldPlaceholder)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            interactionSource = interactionSource,
            textStyle = AwanTheme.typography.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.sky),
            keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        )
    }
}

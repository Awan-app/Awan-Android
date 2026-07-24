package com.awan.app.core.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Skyward rim text field with Compose Styles API integration. */
@Composable
fun AwanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    placeholder: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    contentDescriptionText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val styleState = rememberUpdatedStyleState(interactionSource) {
        it.isEnabled = enabled
    }

    val semanticsModifier = if (contentDescriptionText != null) {
        Modifier.semantics { contentDescription = contentDescriptionText }
    } else {
        Modifier
    }

    val baseModifier = if (isError) {
        Modifier.styleable(styleState, AwanTheme.styles.textField, AwanTheme.styles.textFieldError)
    } else {
        Modifier.styleable(styleState, AwanTheme.styles.textField)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(semanticsModifier)
            .then(baseModifier)
            .styleable(styleState, style),
        enabled = enabled,
        textStyle = AwanTheme.typography.body.copy(color = AwanTheme.colors.textPrimary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        placeholder()
                    }
                    innerTextField()
                }
                if (trailingContent != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        trailingContent()
                    }
                }
            }
        },
    )
}

/** Convenience overload for String placeholder and simple keyboard options. */
@Composable
fun AwanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: Style = Style,
    trailingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    contentDescriptionText: String? = null,
    imeAction: ImeAction = ImeAction.Default,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = capitalization,
        imeAction = imeAction,
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    AwanTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        style = style,
        placeholder = {
            AwanText(
                text = placeholder,
                style = AwanTheme.styles.placeholderText,
            )
        },
        trailingContent = trailingContent,
        isError = isError,
        enabled = enabled,
        singleLine = singleLine,
        contentDescriptionText = contentDescriptionText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "AwanTextField · Light", showBackground = true)
@Composable
private fun LightTextFieldPreview() {
    TextFieldPreview(dark = false)
}

@Preview(name = "AwanTextField · Dark", showBackground = true)
@Composable
private fun DarkTextFieldPreview() {
    TextFieldPreview(dark = true)
}

@Composable
private fun TextFieldPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            // Empty
            AwanTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = { AwanText("you@email.com", style = AwanTheme.styles.placeholderText) },
            )
            // Filled
            AwanTextField(
                value = "sam@cloud.com",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
            )
            // Error
            AwanTextField(
                value = "sam@cloud",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                isError = true,
            )
            // Disabled
            AwanTextField(
                value = "sam@cloud.com",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}

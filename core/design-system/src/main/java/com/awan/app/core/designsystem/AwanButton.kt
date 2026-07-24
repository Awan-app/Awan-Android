package com.awan.app.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class AwanButtonVariant {
    Primary,
    Secondary,
    Destructive,
    Quiet,
    Google,
}

@Composable
fun AwanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    variant: AwanButtonVariant = AwanButtonVariant.Primary,
    enabled: Boolean = true,
    haptic: HapticFeedbackType? = awanButtonHaptic(variant),
    icon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) { it.isEnabled = enabled }
    val rimStyle = when (variant) {
        AwanButtonVariant.Primary -> AwanTheme.styles.primaryButtonRim
        AwanButtonVariant.Secondary -> AwanTheme.styles.secondaryButtonRim
        AwanButtonVariant.Destructive -> AwanTheme.styles.destructiveButtonRim
        AwanButtonVariant.Quiet -> AwanTheme.styles.quietButtonRim
        AwanButtonVariant.Google -> AwanTheme.styles.socialButtonGoogleRim
    }
    val faceStyle = when (variant) {
        AwanButtonVariant.Primary -> AwanTheme.styles.primaryButtonFace
        AwanButtonVariant.Secondary -> AwanTheme.styles.secondaryButtonFace
        AwanButtonVariant.Destructive -> AwanTheme.styles.destructiveButtonFace
        AwanButtonVariant.Quiet -> AwanTheme.styles.quietButtonFace
        AwanButtonVariant.Google -> AwanTheme.styles.socialButtonGoogleFace
    }
    val contentColor by animateColorAsState(
        targetValue = buttonContentColor(variant = variant, enabled = enabled),
        animationSpec = tween(durationMillis = AwanTheme.motion.standardMillis),
        label = "AwanButtonContentColor",
    )
    val rimDepth = if (variant == AwanButtonVariant.Quiet) 0.dp else AwanButtonRimDepth
    val rimSide = if (variant == AwanButtonVariant.Quiet) 0.dp else AwanButtonRimSide
    val rimTopInset = animateDpAsState(
        targetValue = if (styleState.isPressed) AwanButtonRimDepth else 0.dp,
        animationSpec = tween(
            durationMillis = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "AwanButtonRimTopInset",
    ).value
    val rimStartInset = animateDpAsState(
        targetValue = if (styleState.isPressed) rimSide else 0.dp,
        animationSpec = tween(
            durationMillis = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "AwanButtonRimStartInset",
    ).value

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic?.let(hapticFeedback::performHapticFeedback)
                    onClick()
                },
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .styleable(styleState, AwanTheme.styles.buttonFocus)
            // Touch-target floor as a real min, not the style's minWidth — the Styles API's
            // minWidth overrides the incoming constraint, which would clobber a caller's
            // fillMaxWidth back to 48dp before propagateMinConstraints reaches the face.
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.TopCenter,
        // The caller's modifier sizes this Box, but the face Row is what the rim matches. Passing
        // the min constraints down makes a caller's fillMaxWidth reach the face too, while a
        // wrap-content caller still lets the face size itself.
        propagateMinConstraints = true,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = rimTopInset, end = rimStartInset)
                .styleable(styleState, rimStyle)
        )
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalAwanTextStyle provides AwanTheme.styles.buttonLabel,
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = rimDepth, start = rimSide)
                    .styleable(styleState, faceStyle, style),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.size(AwanTheme.spacing.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        icon()
                    }
                    Spacer(modifier = Modifier.width(AwanTheme.spacing.xs))
                }
                content()
            }
        }
    }
}

@Composable
fun AwanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    variant: AwanButtonVariant = AwanButtonVariant.Primary,
    enabled: Boolean = true,
    haptic: HapticFeedbackType? = awanButtonHaptic(variant),
    icon: ImageVector,
    content: @Composable RowScope.() -> Unit,
) {
    AwanButton(
        onClick = onClick,
        modifier = modifier,
        style = style,
        variant = variant,
        enabled = enabled,
        haptic = haptic,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalContentColor.current,
            )
        },
        content = content,
    )
}

@Composable
private fun buttonContentColor(variant: AwanButtonVariant, enabled: Boolean): Color {
    val colors = AwanTheme.colors
    if (!enabled) return colors.disabledContent
    return when (variant) {
        AwanButtonVariant.Primary -> colors.onFilledControl
        AwanButtonVariant.Secondary, AwanButtonVariant.Quiet -> colors.skyPressed
        AwanButtonVariant.Destructive -> colors.onDestructive
        AwanButtonVariant.Google -> colors.textPrimary
    }
}

@Preview(name = "Skyward buttons · Light", showBackground = true)
@Composable
private fun LightButtonsPreview() {
    ButtonsPreview(dark = false)
}

@Preview(name = "Skyward buttons · Dark", showBackground = true)
@Composable
private fun DarkButtonsPreview() {
    ButtonsPreview(dark = true)
}

@Composable
private fun ButtonsPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AwanButton(
                onClick = {},
                modifier = Modifier.wrapContentWidth(),
                variant = AwanButtonVariant.Primary,
            ) { AwanText("PRIMARY") }
            AwanButton(onClick = {}, variant = AwanButtonVariant.Secondary) {
                AwanText("SECONDARY")
            }
            AwanButton(onClick = {}, variant = AwanButtonVariant.Destructive) {
                AwanText("DESTRUCTIVE")
            }
            AwanButton(onClick = {}, variant = AwanButtonVariant.Quiet) {
                AwanText("QUIET")
            }
            AwanButton(onClick = {}, enabled = false) { AwanText("DISABLED") }
        }
    }
}

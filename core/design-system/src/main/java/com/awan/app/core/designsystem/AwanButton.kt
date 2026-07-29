package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


/**
 * [style] and [rimStyle] are applied last onto the variant's face and rim, so a caller can retint a
 * variant without redefining its geometry — which is how one [AwanButtonVariant.Chip] serves a whole
 * row of differently-toned attribute chips.
 */
@Composable
fun AwanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    rimStyle: Style = Style,
    variant: AwanButtonVariant = AwanButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    haptic: HapticFeedbackType? = awanButtonHaptic(variant),
    icon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val effectiveEnabled = enabled && !isLoading
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) { it.isEnabled = effectiveEnabled }
    val variantRim = when (variant) {
        AwanButtonVariant.Primary -> AwanTheme.styles.primaryButtonRim
        AwanButtonVariant.Secondary -> AwanTheme.styles.secondaryButtonRim
        AwanButtonVariant.Destructive -> AwanTheme.styles.destructiveButtonRim
        AwanButtonVariant.Quiet -> AwanTheme.styles.quietButtonRim
        AwanButtonVariant.Google -> AwanTheme.styles.socialButtonGoogleRim
        AwanButtonVariant.Chip -> AwanTheme.styles.chipButtonRim
    }
    val faceStyle = when (variant) {
        AwanButtonVariant.Primary -> AwanTheme.styles.primaryButtonFace
        AwanButtonVariant.Secondary -> AwanTheme.styles.secondaryButtonFace
        AwanButtonVariant.Destructive -> AwanTheme.styles.destructiveButtonFace
        AwanButtonVariant.Quiet -> AwanTheme.styles.quietButtonFace
        AwanButtonVariant.Google -> AwanTheme.styles.socialButtonGoogleFace
        AwanButtonVariant.Chip -> AwanTheme.styles.chipButtonFace
    }
    val contentColor by animateColorAsState(
        targetValue = buttonContentColor(variant = variant, enabled = enabled),
        animationSpec = tween(durationMillis = AwanTheme.motion.standardMillis),
        label = "AwanButtonContentColor",
    )
    val rimDepth = if (variant == AwanButtonVariant.Quiet) 0.dp else AwanButtonRimDepth
    val rimSide = if (variant == AwanButtonVariant.Quiet) 0.dp else AwanButtonRimSide
    // A chip's target is exactly the pill: face plus rim, with no dead margin around it.
    val minTouchSize = if (variant == AwanButtonVariant.Chip) AwanChipFaceHeight + AwanButtonRimDepth else 48.dp
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
            // clickable() withholds its press interaction for TapIndicationDelay inside a scrollable
            // container, so a quick tap only presses once the finger is already up. Own the press.
            .pointerInput(effectiveEnabled) {
                if (!effectiveEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    scope.launch { interactionSource.emit(press) }
                    val up = waitForUpOrCancellation()
                    scope.launch {
                        interactionSource.emit(
                            if (up == null) PressInteraction.Cancel(press) else PressInteraction.Release(press)
                        )
                    }
                }
            }
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = effectiveEnabled,
                role = Role.Button,
                onClick = {
                    haptic?.let(hapticFeedback::performHapticFeedback)
                    onClick()
                },
            )
            .focusable(enabled = effectiveEnabled, interactionSource = interactionSource)
            .styleable(styleState, AwanTheme.styles.buttonFocus)
            .defaultMinSize(minWidth = minTouchSize, minHeight = minTouchSize),
        contentAlignment = Alignment.TopCenter,
        propagateMinConstraints = true,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = rimTopInset, end = rimStartInset)
                .styleable(styleState, variantRim, rimStyle)
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AwanTheme.spacing.md),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(AwanTheme.spacing.xs))
                } else if (icon != null) {
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
    rimStyle: Style = Style,
    variant: AwanButtonVariant = AwanButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    haptic: HapticFeedbackType? = awanButtonHaptic(variant),
    icon: ImageVector,
    content: @Composable RowScope.() -> Unit,
) {
    AwanButton(
        onClick = onClick,
        modifier = modifier,
        style = style,
        rimStyle = rimStyle,
        variant = variant,
        enabled = enabled,
        isLoading = isLoading,
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
        AwanButtonVariant.Chip -> colors.textSecondary
        AwanButtonVariant.Destructive -> colors.onDestructive
        AwanButtonVariant.Google -> colors.textPrimary
    }
}

@Preview(name = "Skyward buttons · Light", showBackground = true)
@Composable
private fun LightButtonsPreview() {
    ButtonsPreview(darkTheme = false)
}

@Preview(name = "Skyward buttons · Dark", showBackground = true)
@Composable
private fun DarkButtonsPreview() {
    ButtonsPreview(darkTheme = true)
}

@Composable
private fun ButtonsPreview(darkTheme: Boolean) {
    AwanTheme(darkTheme = darkTheme) {
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

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


/**
 * [style] and [rimStyle] are applied last onto the variant's face and rim, so a caller can retint a
 * variant without redefining its geometry — which is how one [AwanButtonVariant.Chip] serves a whole
 * row of differently-toned attribute chips.
 *
 * Sizing follows the same fix as [AwanCard]: the rim used to be sized with `matchParentSize()` while
 * the outer container relied on `propagateMinConstraints` to end up the right size — which does not
 * reliably hug the face for every content width/variant combination. Here the face is measured first
 * and the rim is then forced into exactly that size, so it can never drift from the face's edges.
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
    latchedPressed: Boolean = false,
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
    val effectivePressed = styleState.isPressed || latchedPressed
    val latchedPressActive = latchedPressed && !styleState.isPressed
    val latchedTranslationX = animateDpAsState(
        targetValue = if (latchedPressActive) {
            buttonPressedTranslationX(LocalLayoutDirection.current)
        } else {
            0.dp
        },
        animationSpec = tween(
            durationMillis = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "AwanButtonLatchedTranslationX",
    ).value
    val latchedTranslationY = animateDpAsState(
        targetValue = if (latchedPressActive) AwanButtonRimDepth else 0.dp,
        animationSpec = tween(
            durationMillis = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "AwanButtonLatchedTranslationY",
    ).value
    // A chip's target is exactly the pill: face plus rim, with no dead margin around it.
    val minTouchSize = if (variant == AwanButtonVariant.Chip) AwanChipFaceHeight + AwanButtonRimDepth else 48.dp
    val rimTopInset = animateDpAsState(
        targetValue = if (effectivePressed) AwanButtonRimDepth else 0.dp,
        animationSpec = tween(
            durationMillis = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "AwanButtonRimTopInset",
    ).value
    val rimStartInset = animateDpAsState(
        targetValue = if (effectivePressed) rimSide else 0.dp,
        animationSpec = tween(
            durationMillis = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "AwanButtonRimStartInset",
    ).value

    Layout(
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
            .styleable(styleState, AwanTheme.styles.buttonFocus),
        content = {
            // Rim (index 0). No matchParentSize here — the custom measure policy below forces
            // it to exactly the face's resolved size, whatever that ends up being.
            Box(
                modifier = Modifier
                    .padding(top = rimTopInset, end = rimStartInset)
                    .styleable(styleState, variantRim, rimStyle)
            )
            // Face (index 1). Its own padding(bottom/start) reserves the strip the rim peeks
            // through, and its measured size becomes the button's true size.
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalAwanTextStyle provides AwanTheme.styles.buttonLabel,
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = rimDepth, start = rimSide)
                        .styleable(styleState, faceStyle, style)
                        .graphicsLayer {
                            translationX = latchedTranslationX.toPx()
                            translationY = latchedTranslationY.toPx()
                        },
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
        },
    ) { measurables, constraints ->
        // Same fix as AwanCard: measure the face first (honouring the touch-target minimum),
        // then force the rim into exactly that size. No reliance on matchParentSize/propagation.
        val minTouchPx = minTouchSize.roundToPx()
        val safeMaxWidth = constraints.maxWidth.coerceAtLeast(0)
        val safeMaxHeight = constraints.maxHeight.coerceAtLeast(0)

        val minW = if (safeMaxWidth > 0) maxOf(constraints.minWidth, minTouchPx).coerceAtMost(safeMaxWidth) else 0
        val minH = if (safeMaxHeight > 0) maxOf(constraints.minHeight, minTouchPx).coerceAtMost(safeMaxHeight) else 0

        val safeConstraints = Constraints(
            minWidth = minW,
            maxWidth = maxOf(safeMaxWidth, minW),
            minHeight = minH,
            maxHeight = maxOf(safeMaxHeight, minH),
        )

        val facePlaceable = measurables[1].measure(safeConstraints)
        val width = facePlaceable.width.coerceAtLeast(0)
        val height = facePlaceable.height.coerceAtLeast(0)

        // Rim gets fixed constraints equal to the face's resolved size — it can never be
        // bigger, smaller, or misaligned relative to the face, regardless of content width.
        val rimPlaceable = measurables[0].measure(Constraints.fixed(width, height))

        layout(width, height) {
            rimPlaceable.placeRelative(0, 0)
            facePlaceable.placeRelative(0, 0)
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
    AwanTheme(dark = darkTheme) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Full-width "SEND CODE"-style primary button, narrow content in a wide button —
            // this is exactly the case the old matchParentSize rim used to get wrong.
            AwanButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = AwanButtonVariant.Primary,
            ) { AwanText("SEND CODE") }
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

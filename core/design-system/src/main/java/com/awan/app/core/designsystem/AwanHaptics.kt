package com.awan.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The feel of each button variant. A primary commit lands solidly, a secondary is a light tap, a
 * destructive one is deliberately unlike the others so the hand notices before the eye does, and a
 * quiet link barely registers.
 *
 * Pass `haptic = null` to [AwanButton] or [AwanIconButton] to silence a single call site, or pass a
 * different [HapticFeedbackType] to override the variant default.
 */
fun awanButtonHaptic(variant: AwanButtonVariant): HapticFeedbackType = when (variant) {
    AwanButtonVariant.Primary -> HapticFeedbackType.Confirm
    AwanButtonVariant.Secondary -> HapticFeedbackType.ContextClick
    AwanButtonVariant.Destructive -> HapticFeedbackType.Reject
    AwanButtonVariant.Quiet -> HapticFeedbackType.SegmentTick
    AwanButtonVariant.Google -> HapticFeedbackType.Confirm
    AwanButtonVariant.Chip -> HapticFeedbackType.SegmentTick
}

@Composable
fun rememberHapticClick(
    onClick: () -> Unit,
    haptic: HapticFeedbackType? = HapticFeedbackType.ContextClick,
): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    return remember(hapticFeedback, haptic) {
        {
            haptic?.let(hapticFeedback::performHapticFeedback)
            currentOnClick()
        }
    }
}

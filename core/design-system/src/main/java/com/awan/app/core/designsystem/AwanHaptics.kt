package com.awan.app.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

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

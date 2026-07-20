package com.awan.app.core.designsystem

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleScope
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.focused
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.selected
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val StyleScope.colors: AwanColors
    get() = LocalAwanTheme.currentValue.colors

private val StyleScope.typography: AwanTypography
    get() = LocalAwanTheme.currentValue.typography

private val StyleScope.shapes: AwanShapes
    get() = LocalAwanTheme.currentValue.shapes

internal fun buttonPressedTranslationX(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Ltr) -AwanButtonRimSide else AwanButtonRimSide

private fun StyleScope.buttonPressedTransform() {
    pressed {
        animate(
            tween(
                durationMillis = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
                easing = LinearOutSlowInEasing,
            )
        ) {
            translationX(buttonPressedTranslationX(LocalLayoutDirection.currentValue).toPx())
            translationY(AwanButtonRimDepth.toPx())
        }
    }
}

object AwanStyles {
    val screen = Style {
        background(Brush.verticalGradient(listOf(colors.backgroundStart, colors.background)))
        contentColor(colors.textPrimary)
    }

    val surface = Style {
        background(colors.surface)
        border(2.dp, colors.line)
        shape(shapes.card)
        contentColor(colors.textPrimary)
        contentPadding(horizontal = 13.dp, vertical = 11.dp)
    }

    val displayText = Style {
        textStyle(typography.display)
        fontFamily(typography.display.fontFamily!!)
        contentColor(colors.textPrimary)
    }
    val titleText = Style {
        textStyle(typography.title)
        fontFamily(typography.title.fontFamily!!)
        contentColor(colors.textPrimary)
    }
    val headingText = Style {
        textStyle(typography.heading)
        fontFamily(typography.heading.fontFamily!!)
        contentColor(colors.textPrimary)
    }
    val bodyText = Style {
        textStyle(typography.body)
        fontFamily(typography.body.fontFamily!!)
        contentColor(colors.textPrimary)
    }
    val captionText = Style {
        textStyle(typography.caption)
        fontFamily(typography.caption.fontFamily!!)
        contentColor(colors.textSecondary)
    }
    val placeholderText = Style {
        textStyle(typography.body)
        fontFamily(typography.body.fontFamily!!)
        contentColor(colors.textSecondary)
    }


    val buttonFocus = Style {
        shape(androidx.compose.foundation.shape.RoundedCornerShape(23.dp))
        border(3.dp, Color.Transparent)
        contentPadding(2.dp)
        minWidth(48.dp)
        minHeight(48.dp)
        focused { borderColor(colors.sky) }
    }

    val primaryButtonRim = Style {
        background(colors.filledControlPressed)
        shape(shapes.button)
        disabled { background(colors.line) }
    }

    val primaryButtonFace = Style {
        background(colors.filledControl)
        shape(shapes.button)
        minHeight(48.dp)
        contentPadding(horizontal = 13.dp, vertical = 13.dp)
        contentColor(colors.onFilledControl)
        textStyle(typography.button)
        fontFamily(typography.button.fontFamily!!)
        buttonPressedTransform()
        disabled {
            background(colors.disabledSurface)
            contentColor(colors.disabledContent)
        }
    }

    val secondaryButtonRim = Style {
        background(colors.line)
        shape(shapes.button)
    }

    val secondaryButtonFace = Style {
        background(colors.surface)
        border(2.dp, colors.line)
        shape(shapes.button)
        minHeight(48.dp)
        contentPadding(horizontal = 12.dp, vertical = 12.dp)
        contentColor(colors.skyPressed)
        textStyle(typography.button)
        fontFamily(typography.button.fontFamily!!)
        buttonPressedTransform()
        disabled {
            background(colors.disabledSurface)
            borderColor(colors.line)
            contentColor(colors.disabledContent)
        }
    }

    val destructiveButtonRim = Style {
        background(colors.destructivePressed)
        shape(shapes.button)
        disabled { background(colors.line) }
    }

    val destructiveButtonFace = Style {
        background(colors.destructive)
        shape(shapes.button)
        minHeight(48.dp)
        contentPadding(horizontal = 13.dp, vertical = 13.dp)
        contentColor(colors.onDestructive)
        textStyle(typography.button)
        fontFamily(typography.button.fontFamily!!)
        buttonPressedTransform()
        disabled {
            background(colors.disabledSurface)
            contentColor(colors.disabledContent)
        }
    }

    val quietButtonRim = Style {
        background(Color.Transparent)
        shape(shapes.button)
    }

    val quietButtonFace = Style {
        background(Color.Transparent)
        shape(shapes.button)
        minHeight(48.dp)
        contentPadding(horizontal = 4.dp, vertical = 4.dp)
        contentColor(colors.skyPressed)
        textStyle(typography.button)
        fontFamily(typography.button.fontFamily!!)
        disabled { contentColor(colors.disabledContent) }
    }

    val navigationDivider = Style { background(colors.line); minHeight(2.dp) }

    val navigationBar = Style {
        background(colors.surface)
        contentColor(colors.meta)
        contentPadding(horizontal = 12.dp, vertical = 8.dp)
    }

    val navigationItem = Style {
        shape(shapes.pill)
        minHeight(48.dp)
        contentPadding(4.dp)
        contentColor(colors.meta)
        textStyle(typography.caption)
        fontFamily(typography.caption.fontFamily!!)
        selected { contentColor(colors.sky) }
        pressed {
            animate(tween(durationMillis = 120, easing = LinearOutSlowInEasing)) {
                translationY(2.dp.toPx())
            }
        }
    }

    val textField = Style {
        background(colors.surface)
        border(InputStrokeWidth, colors.line)
        shape(shapes.button)
        minHeight(AuthInputHeight)
        contentPadding(horizontal = 16.dp, vertical = 14.dp)
        contentColor(colors.textPrimary)
        textStyle(typography.body)
        fontFamily(typography.body.fontFamily!!)
        focused {
            border(InputStrokeWidthActive, colors.sky)
        }
        disabled {
            background(colors.disabledSurface)
            contentColor(colors.disabledContent)
        }
    }

    val textFieldError = Style {
        border(InputStrokeWidthActive, colors.destructive)
    }

    val otpCell = Style {
        background(colors.surface)
        border(InputStrokeWidth, colors.line)
        shape(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        minWidth(OtpCellWidth)
        minHeight(OtpCellSize)
        contentColor(colors.textPrimary)
        textStyle(typography.title)
        fontFamily(typography.title.fontFamily!!)
    }

    val otpCellActive = Style {
        border(InputStrokeWidthActive, colors.sky)
    }

    val otpCellError = Style {
        background(colors.destructive.copy(alpha = 0.08f))
        border(InputStrokeWidthActive, colors.destructive)
        contentColor(colors.destructive)
    }

    val otpCellDisabled = Style {
        background(colors.disabledSurface)
        border(InputStrokeWidth, colors.line)
        contentColor(colors.disabledContent)
    }

    val authDivider = Style {
        background(colors.line)
        minHeight(1.dp)
    }


    val socialButtonGoogleRim = Style {
        background(colors.line)
        shape(shapes.button)
    }

    val socialButtonGoogleFace = Style {
        background(colors.surface)
        border(InputStrokeWidth, colors.line)
        shape(shapes.button)
        minHeight(48.dp)
        contentPadding(horizontal = 16.dp, vertical = 12.dp)
        contentColor(colors.textPrimary)
        textStyle(typography.button)
        fontFamily(typography.button.fontFamily!!)
        buttonPressedTransform()
    }
}


package com.awan.app.core.designsystem

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleScope
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.focused
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.selected
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StyleScope.colors: AwanColors
    get() = LocalAwanTheme.currentValue.colors

private val StyleScope.typography: AwanTypography
    get() = LocalAwanTheme.currentValue.typography

private val StyleScope.shapes: AwanShapes
    get() = LocalAwanTheme.currentValue.shapes

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

    val displayText = Style { textStyle(typography.display); contentColor(colors.textPrimary) }
    val titleText = Style { textStyle(typography.title); contentColor(colors.textPrimary) }
    val headingText = Style { textStyle(typography.heading); contentColor(colors.textPrimary) }
    val bodyText = Style { textStyle(typography.body); contentColor(colors.textPrimary) }
    val captionText = Style { textStyle(typography.caption); contentColor(colors.textSecondary) }

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
        pressed {
            animate(tween(durationMillis = 120, easing = LinearOutSlowInEasing)) {
                translationY(3.dp.toPx())
            }
        }
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
        pressed {
            animate(tween(durationMillis = 120, easing = LinearOutSlowInEasing)) {
                translationY(3.dp.toPx())
            }
        }
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
        pressed {
            animate(tween(durationMillis = 120, easing = LinearOutSlowInEasing)) {
                translationY(3.dp.toPx())
            }
        }
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
        selected { contentColor(colors.sky) }
        pressed {
            animate(tween(durationMillis = 120, easing = LinearOutSlowInEasing)) {
                translationY(2.dp.toPx())
            }
        }
    }
}

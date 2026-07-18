package com.awan.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

internal data class AwanThemeValues(
    val colors: AwanColors,
    val typography: AwanTypography,
    val shapes: AwanShapes,
    val spacing: AwanSpacing,
)

internal val LocalAwanTheme = staticCompositionLocalOf<AwanThemeValues> {
    error("AwanTheme is not present")
}

object AwanTheme {
    val colors: AwanColors
        @Composable @ReadOnlyComposable get() = LocalAwanTheme.current.colors

    val typography: AwanTypography
        @Composable @ReadOnlyComposable get() = LocalAwanTheme.current.typography

    val shapes: AwanShapes
        @Composable @ReadOnlyComposable get() = LocalAwanTheme.current.shapes

    val spacing: AwanSpacing
        @Composable @ReadOnlyComposable get() = LocalAwanTheme.current.spacing

    val styles: AwanStyles = AwanStyles

    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        highContrast: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val colors = when {
            darkTheme -> DarkAwanColors
            highContrast -> LightHighContrastAwanColors
            else -> LightAwanColors
        }
        val colorScheme = if (darkTheme) {
            darkColorScheme(
                primary = colors.filledControl,
                onPrimary = colors.onFilledControl,
                secondary = colors.zoneViolet,
                onSecondary = colors.ink,
                background = colors.background,
                onBackground = colors.textPrimary,
                surface = colors.surface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.surface,
                onSurfaceVariant = colors.textSecondary,
                error = colors.destructive,
                onError = colors.onDestructive,
                outline = colors.line,
            )
        } else {
            lightColorScheme(
                primary = colors.filledControl,
                onPrimary = colors.onFilledControl,
                secondary = colors.zoneViolet,
                onSecondary = colors.onSky,
                background = colors.background,
                onBackground = colors.textPrimary,
                surface = colors.surface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.surface,
                onSurfaceVariant = colors.textSecondary,
                error = colors.destructive,
                onError = colors.onDestructive,
                outline = colors.line,
            )
        }
        val values = AwanThemeValues(
            colors = colors,
            typography = AwanTypographyTokens,
            shapes = AwanShapeTokens,
            spacing = AwanSpacingTokens,
        )

        androidx.compose.runtime.CompositionLocalProvider(LocalAwanTheme provides values) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography(
                    displayLarge = values.typography.display,
                    titleLarge = values.typography.title,
                    headlineSmall = values.typography.heading,
                    bodyLarge = values.typography.body,
                    labelLarge = values.typography.button,
                    labelSmall = values.typography.caption,
                ),
                shapes = Shapes(
                    small = values.shapes.chip,
                    medium = values.shapes.card,
                    large = values.shapes.button,
                ),
                content = content,
            )
        }
    }
}

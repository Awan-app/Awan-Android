package com.awan.app.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

internal data class AwanThemeValues(
    val colors: AwanColors,
    val typography: AwanTypography,
    val shapes: AwanShapes,
    val spacing: AwanSpacing,
    val motion: AwanMotion,
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

    val motion: AwanMotion
        @Composable @ReadOnlyComposable get() = LocalAwanTheme.current.motion

    val styles: AwanStyles = AwanStyles

    @Composable
    operator fun invoke(
        dark: Boolean = false,
        light: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val isDark = when {
            dark -> true
            light -> false
            else -> isSystemInDarkTheme()
        }
        val colors = if (isDark) DarkAwanColors else LightAwanColors
        val colorScheme = if (isDark) {
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
                // Material containers — pickers, menus, sheets — read these, not `surface`. Left
                // unmapped they fall back to the baseline purple and land off-theme.
                surfaceContainerLowest = colors.surface,
                surfaceContainerLow = colors.surface,
                surfaceContainer = colors.surface,
                surfaceContainerHigh = colors.surface,
                surfaceContainerHighest = colors.surface,
                // Selected segments inside Material controls — a picker's hour field, its AM/PM
                // switch — come from the container roles, which default to purple and pink.
                primaryContainer = colors.line,
                onPrimaryContainer = colors.textPrimary,
                secondaryContainer = colors.line,
                onSecondaryContainer = colors.textPrimary,
                tertiary = colors.zoneViolet,
                onTertiary = colors.onSky,
                tertiaryContainer = colors.line,
                onTertiaryContainer = colors.textPrimary,
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
                surfaceContainerLowest = colors.surface,
                surfaceContainerLow = colors.surface,
                surfaceContainer = colors.surface,
                surfaceContainerHigh = colors.surface,
                surfaceContainerHighest = colors.surface,
                // Selected segments inside Material controls — a picker's hour field, its AM/PM
                // switch — come from the container roles, which default to purple and pink.
                primaryContainer = colors.line,
                onPrimaryContainer = colors.textPrimary,
                secondaryContainer = colors.line,
                onSecondaryContainer = colors.textPrimary,
                tertiary = colors.zoneViolet,
                onTertiary = colors.onSky,
                tertiaryContainer = colors.line,
                onTertiaryContainer = colors.textPrimary,
                error = colors.destructive,
                onError = colors.onDestructive,
                outline = colors.line,
            )
        }

        val configuration = LocalConfiguration.current
        val isArabic = remember(configuration) {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                configuration.locale
            }
            locale?.language?.equals("ar", ignoreCase = true) == true
        }

        val typography = if (isArabic) AwanArabicTypographyTokens else AwanTypographyTokens

        val values = AwanThemeValues(
            colors = colors,
            typography = typography,
            shapes = AwanShapeTokens,
            spacing = AwanSpacingTokens,
            motion = AwanMotionTokens,
        )

        androidx.compose.runtime.CompositionLocalProvider(LocalAwanTheme provides values) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography(
                    displayLarge = values.typography.display,
                    displayMedium = values.typography.display,
                    displaySmall = values.typography.display,
                    headlineLarge = values.typography.heading,
                    headlineMedium = values.typography.heading,
                    headlineSmall = values.typography.heading,
                    titleLarge = values.typography.title,
                    titleMedium = values.typography.title,
                    titleSmall = values.typography.title,
                    bodyLarge = values.typography.body,
                    bodyMedium = values.typography.body,
                    bodySmall = values.typography.body,
                    labelLarge = values.typography.button,
                    labelMedium = values.typography.buttonCompact,
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

package com.awan.app.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val AWAN_BUTTON_ANIMATION_DURATION_MILLIS = 40
internal val AwanButtonRimDepth = 4.dp
internal val AwanButtonRimSide = 2.dp

/** Face only; the pill is this plus [AwanButtonRimDepth], which is also its whole touch target. */
internal val AwanChipFaceHeight = 40.dp


@Immutable
data class AwanColors(
    val backgroundStart: Color,
    val background: Color,
    val surface: Color,
    val line: Color,
    val ink: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val meta: Color,
    val sky: Color,
    val skyPressed: Color,
    val onSky: Color,
    val filledControl: Color,
    val filledControlPressed: Color,
    val onFilledControl: Color,
    val destructive: Color,
    val destructivePressed: Color,
    val onDestructive: Color,
    val success: Color,
    val disabledSurface: Color,
    val disabledContent: Color,
    val skyDawn: Color,
    val skyMorning: Color,
    val skyMidday: Color,
    val skyDusk: Color,
    val zoneMelon: Color,
    val zoneBlue: Color,
    val zonePurple: Color,
    val zonePink: Color,
    val zoneGreen: Color,
    val zoneYellow: Color,
    val zoneOrange: Color,
    val zoneRed: Color,
    val zoneCyan: Color,
    val zoneGray: Color,
    val zoneSky: Color,
    val zoneViolet: Color,
    val zoneCoral: Color,
    val zoneTangerine: Color,
    val zoneSun: Color,
    val zoneLavender: Color,
    val zoneSkyPressed: Color,
    val zoneVioletPressed: Color,
    val zoneCoralPressed: Color,
    val zoneTangerinePressed: Color,
    val zoneSunPressed: Color,
    val zoneLavenderPressed: Color,
    val streakSurface: Color,
    val streakIcon: Color,
    val zoneCardAlpha: Float,
)

internal val LightAwanColors = AwanColors(
    backgroundStart = Color(0xFFCFEEFF),
    background = Color(0xFFF4FAFF),
    surface = Color(0xFFFFFFFF),
    line = Color(0xFFDCEAF5),
    ink = Color(0xFF16455E),
    textPrimary = Color(0xFF16455E),
    textSecondary = Color(0xFF5B7A8E),
    meta = Color(0xFF8FB0C4),
    sky = Color(0xFF2EAAFF),
    skyPressed = Color(0xFF1D84CC),
    onSky = Color.White,
    filledControl = Color(0xFF2EAAFF),
    filledControlPressed = Color(0xFF1D84CC),
    onFilledControl = Color.White,
    destructive = Color(0xFFFF6F91),
    destructivePressed = Color(0xFFD94F72),
    onDestructive = Color.White,
    success = Color(0xFF22C55E),
    disabledSurface = Color(0xFFE8F1F8),
    disabledContent = Color(0xFFA9C4D6),
    skyDawn = Color(0xFFFFD9A8),
    skyMorning = Color(0xFFCFE8FF),
    skyMidday = Color(0xFFA6D2FA),
    skyDusk = Color(0xFF6E7FB8),
    /*    zoneCoral = Color(0xFFFF6F91),
    zoneViolet = Color(0xFF7A64FF),
    zoneSky = Color(0xFF2EAAFF),
    zoneTangerine = Color(0xFFFF9838),
    zoneSun = Color(0xFFFFC233),
    zoneLavender = Color(0xFF9A7BFF),
    zoneCoralPressed = Color(0xFFD94F72),
    zoneVioletPressed = Color(0xFF5A47CC),
    zoneSkyPressed = Color(0xFF1D84CC),
    zoneTangerinePressed = Color(0xFFD9771C),
    zoneSunPressed = Color(0xFFD99E14),
    zoneLavenderPressed = Color(0xFF7659D9),
    zoneCardAlpha = 0.15f,*/
    zoneMelon = Color(0xFF97DBAE),
    zoneBlue = Color(0xFF8EC5E8),
    zonePurple = Color(0xFFB7A1E8),
    zonePink = Color(0xFFF2A7B5),
    zoneGreen = Color(0xFF8FD3B0),
    zoneYellow = Color(0xFFF1D98A),
    zoneOrange = Color(0xFFF4A261),
    zoneRed = Color(0xFFE98B8B),
    zoneCyan = Color(0xFF86D5D5),
    zoneGray = Color(0xFFB8C0CC),
    zoneSky = Color(0xFF8EC5E8),
    zoneViolet = Color(0xFFB7A1E8),
    zoneCoral = Color(0xFFF2A7B5),
    zoneTangerine = Color(0xFFF4A261),
    zoneSun = Color(0xFFF1D98A),
    zoneLavender = Color(0xFFB7A1E8),
    zoneSkyPressed = Color(0xFF1D84CC),
    zoneVioletPressed = Color(0xFF5A47CC),
    zoneCoralPressed = Color(0xFFD94F72),
    zoneTangerinePressed = Color(0xFFD9771C),
    zoneSunPressed = Color(0xFFD99E14),
    zoneLavenderPressed = Color(0xFF7659D9),
    streakSurface = Color(0xFFFFE7B3),
    streakIcon = Color(0xFFB45309),
    zoneCardAlpha = 0.35f, // Increased for better visibility with pastels
)

internal val LightHighContrastAwanColors = LightAwanColors.copy(
    filledControl = Color(0xFF16455E),
    filledControlPressed = Color(0xFF0F2536),
)

internal val DarkAwanColors = AwanColors(
    backgroundStart = Color(0xFF0F2536),
    background = Color(0xFF0F2536),
    surface = Color(0xFF17364C),
    line = Color(0xFF29506D),
    ink = Color(0xFF0F2536),
    textPrimary = Color(0xFFEAF6FF),
    textSecondary = Color(0xFFA8C6DA),
    meta = Color(0xFF6F93AA),
    sky = Color(0xFF4DBAFF),
    skyPressed = Color(0xFF2D93D9),
    onSky = Color(0xFF0F2536),
    filledControl = Color(0xFF4DBAFF),
    filledControlPressed = Color(0xFF2D93D9),
    onFilledControl = Color(0xFF0F2536),
    destructive = Color(0xFFFF86A4),
    destructivePressed = Color(0xFFD94F72),
    onDestructive = Color(0xFF0F2536),
    success = Color(0xFF22C55E),
    disabledSurface = Color(0xFF17364C),
    disabledContent = Color(0xFF6F93AA),
    skyDawn = Color(0xFF6B4A2E),
    skyMorning = Color(0xFF24455F),
    skyMidday = Color(0xFF1B3A55),
    skyDusk = Color(0xFF2A2F52),
    /*    zoneCoral = Color(0xFFFF86A4),
    zoneViolet = Color(0xFF9683FF),
    zoneSky = Color(0xFF4DBAFF),
    zoneTangerine = Color(0xFFFFAB5C),
    zoneSun = Color(0xFFFFCF5C),
    zoneLavender = Color(0xFFAD93FF),
    zoneCoralPressed = Color(0xFFD94F72),
    zoneVioletPressed = Color(0xFF7659D9),
    zoneSkyPressed = Color(0xFF2D93D9),
    zoneTangerinePressed = Color(0xFFD9771C),
    zoneSunPressed = Color(0xFFD99E14),
    zoneLavenderPressed = Color(0xFF7659D9),
    zoneCardAlpha = 0.25f,*/
    zoneMelon = Color(0xFF97DBAE),
    zoneBlue = Color(0xFF8EC5E8),
    zonePurple = Color(0xFFB7A1E8),
    zonePink = Color(0xFFF2A7B5),
    zoneGreen = Color(0xFF8FD3B0),
    zoneYellow = Color(0xFFF1D98A),
    zoneOrange = Color(0xFFF4A261),
    zoneRed = Color(0xFFE98B8B),
    zoneCyan = Color(0xFF86D5D5),
    zoneGray = Color(0xFFB8C0CC),
    zoneSky = Color(0xFF8EC5E8),
    zoneViolet = Color(0xFFB7A1E8),
    zoneCoral = Color(0xFFF2A7B5),
    zoneTangerine = Color(0xFFF4A261),
    zoneSun = Color(0xFFF1D98A),
    zoneLavender = Color(0xFFB7A1E8),
    zoneSkyPressed = Color(0xFF2D93D9),
    zoneVioletPressed = Color(0xFF7659D9),
    zoneCoralPressed = Color(0xFFD94F72),
    zoneTangerinePressed = Color(0xFFD9771C),
    zoneSunPressed = Color(0xFFD99E14),
    zoneLavenderPressed = Color(0xFF7659D9),
    streakSurface = Color(0xFF3B2A20),
    streakIcon = Color(0xFFFFB84D),
    zoneCardAlpha = 0.45f,
)


@Immutable
data class AwanShapes(
    val chip: CornerBasedShape,
    val card: CornerBasedShape,
    val button: CornerBasedShape,
    val pill: CornerBasedShape,
)

internal val AwanShapeTokens = AwanShapes(
    chip = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    card = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    button = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    pill = androidx.compose.foundation.shape.RoundedCornerShape(99.dp),
)

@Immutable
data class AwanSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

internal val AwanSpacingTokens = AwanSpacing()

val AuthInputHeight = 56.dp

val OtpCellSize = 56.dp

val OtpCellWidth = 44.dp

val OtpCellSpacing = 8.dp

val InputStrokeWidth = 1.5.dp

val InputStrokeWidthActive = 2.dp

val MascotSize = 200.dp

const val OTP_SHAKE_DURATION_MILLIS = 400

const val AUTH_ENTER_DURATION_MILLIS = 300

/** Spring parameters rather than a built spec, so one token serves Float, Dp, IntOffset and IntSize call sites. */
@Immutable
data class AwanSpring(val dampingRatio: Float, val stiffness: Float) {
    fun <T> spec(): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

@Immutable
data class AwanMotion(
    val settle: AwanSpring = AwanSpring(0.85f, Spring.StiffnessMediumLow),
    val bouncy: AwanSpring = AwanSpring(0.55f, Spring.StiffnessLow),
    val playful: AwanSpring = AwanSpring(0.42f, Spring.StiffnessMediumLow),
    val fastMillis: Int = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
    val standardMillis: Int = 220,
    val emphasizedMillis: Int = 320,
    val staggerMillis: Int = 55,
)

internal val AwanMotionTokens = AwanMotion()

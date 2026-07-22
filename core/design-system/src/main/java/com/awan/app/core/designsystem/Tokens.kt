package com.awan.app.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val AWAN_BUTTON_ANIMATION_DURATION_MILLIS = 40
internal val AwanButtonRimDepth = 4.dp
internal val AwanButtonRimSide = 2.dp

private val Baloo2 = FontFamily(
    Font(R.font.baloo2_semibold, FontWeight.SemiBold),
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold)
)

private val Nunito = FontFamily(
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

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
    val disabledSurface: Color,
    val disabledContent: Color,
    val skyDawn: Color,
    val skyMorning: Color,
    val skyMidday: Color,
    val skyDusk: Color,
    val zoneCoral: Color,
    val zoneViolet: Color,
    val zoneSky: Color,
    val zoneTangerine: Color,
    val zoneSun: Color,
    val zoneLavender: Color,
    val zoneCoralPressed: Color,
    val zoneVioletPressed: Color,
    val zoneSkyPressed: Color,
    val zoneTangerinePressed: Color,
    val zoneSunPressed: Color,
    val zoneLavenderPressed: Color,
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
    disabledSurface = Color(0xFFE8F1F8),
    disabledContent = Color(0xFFA9C4D6),
    skyDawn = Color(0xFFFFD9A8),
    skyMorning = Color(0xFFCFE8FF),
    skyMidday = Color(0xFFA6D2FA),
    skyDusk = Color(0xFF6E7FB8),
    zoneCoral = Color(0xFFFF6F91),
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
    disabledSurface = Color(0xFF17364C),
    disabledContent = Color(0xFF6F93AA),
    skyDawn = Color(0xFF6B4A2E),
    skyMorning = Color(0xFF24455F),
    skyMidday = Color(0xFF1B3A55),
    skyDusk = Color(0xFF2A2F52),
    zoneCoral = Color(0xFFFF86A4),
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
)

@Immutable
data class AwanTypography(
    val display: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
    val buttonCompact: TextStyle,
    val caption: TextStyle,
)

internal val AwanTypographyTokens = AwanTypography(
    display = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 31.sp,
    ),
    title = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    heading = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    body = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
    ),
    button = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    buttonCompact = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    caption = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
    ),
)

/**
 * A fully resolved text appearance: a typography token plus the colour it renders in. An
 * [Color.Unspecified] colour means "inherit whatever `LocalContentColor` provides", which is how a
 * button's label picks up its animated content colour.
 *
 * Text appearance deliberately does **not** go through the Styles API. `Modifier.styleable` delivers
 * typography to text by *inheritance*, and in foundation 1.11.4 the inherited-style cache
 * (`StyleOuterNode.ancestorNodes`) is appended to on every resolve and never cleared — so a text
 * node that moves or is reused merges in the styles of nodes that are no longer its ancestors and
 * silently renders in the wrong family, weight or size. Handing `BasicText` a concrete [TextStyle]
 * is priority 1 in the Styles precedence table and cannot be corrupted by that cache.
 */
@Immutable
data class AwanTextStyle(val textStyle: TextStyle, val color: Color = Color.Unspecified)

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

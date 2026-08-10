package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

private val Cairo = FontFamily(
    Font(R.font.cairo_variable, FontWeight.Medium),
    Font(R.font.cairo_variable, FontWeight.SemiBold),
    Font(R.font.cairo_variable, FontWeight.Bold),
    Font(R.font.cairo_variable, FontWeight.ExtraBold)
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

internal val AwanArabicTypographyTokens = AwanTypography(
    display = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 31.sp,
    ),
    title = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    heading = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    body = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
    ),
    button = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    buttonCompact = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    caption = TextStyle(
        fontFamily = Cairo,
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

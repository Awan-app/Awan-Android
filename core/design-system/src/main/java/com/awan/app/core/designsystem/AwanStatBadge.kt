package com.awan.app.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide

private val BadgeShape = RoundedCornerShape(12.dp)

private const val PULSE_SCALE = 1.18f

/**
 * Formats a point integer value to a compact abbreviated string (e.g., 999, 1k, 1.2k, 10.5k, 1M).
 * Pure function suitable for deterministic unit testing.
 */
fun formatAbbreviatedPointsValue(
    points: Int,
    thousandFmt: String = "%1\$sk",
    millionFmt: String = "%1\$sM",
    billionFmt: String = "%1\$sB",
): String {
    val isNegative = points < 0
    val absValue = kotlin.math.abs(points.toLong())

    var divisor = when {
        absValue >= 1_000_000_000L -> 1_000_000_000.0
        absValue >= 1_000_000L -> 1_000_000.0
        absValue >= 1_000L -> 1_000.0
        else -> 1.0
    }

    var fmt = when {
        absValue >= 1_000_000_000L -> billionFmt
        absValue >= 1_000_000L -> millionFmt
        absValue >= 1_000L -> thousandFmt
        else -> null
    }

    var rounded = Math.round((absValue / divisor) * 10.0) / 10.0

    // Promote boundary overflow post-rounding (e.g., 999_950 -> 1000.0k -> 1.0M)
    if (rounded >= 1000.0 && fmt != billionFmt && fmt != null) {
        if (fmt == thousandFmt) {
            fmt = millionFmt
            rounded /= 1000.0
        } else if (fmt == millionFmt) {
            fmt = billionFmt
            rounded /= 1000.0
        }
    }

    val numberStr = if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", rounded)
    }

    val formattedPositive = if (fmt != null) {
        String.format(java.util.Locale.US, fmt, numberStr)
    } else {
        numberStr
    }

    return if (isNegative) "-$formattedPositive" else formattedPositive
}

/**
 * Composable wrapper retrieving localized points shortcut formats.
 */
@Composable
fun formatAbbreviatedPoints(points: Int): String {
    return formatAbbreviatedPointsValue(
        points = points,
        thousandFmt = stringResource(R.string.ds_points_thousand),
        millionFmt = stringResource(R.string.ds_points_million),
        billionFmt = stringResource(R.string.ds_points_billion),
    )
}

/**
 * The streak badge in the home header. Reads its colours from the theme's streak tokens when active,
 * and renders in a muted gray when today's streak is not yet completed/taken.
 */
@Composable
fun AwanStreakBadge(
    streakCount: Int,
    isStreakActive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = if (isStreakActive) AwanTheme.colors.streakSurface else AwanTheme.colors.disabledSurface
    val accentColor = if (isStreakActive) AwanTheme.colors.streakIcon else AwanTheme.colors.disabledContent

    AwanStatBadge(
        icon = Lucide.Flame,
        text = streakCount.toString(),
        surface = surfaceColor,
        accent = accentColor,
        modifier = modifier,
    )
}

/**
 * The points badge in the home header, and the target a points-award animation flies to.
 *
 * While an award is in flight the animation owns the number and the kick, via [LocalRewardAnchors] —
 * so the badge counts up star by star instead of snapping to the new total the moment the server
 * replies. The rest of the time it just shows [pointsCount].
 */
@Composable
fun AwanPointsBadge(
    pointsCount: Int,
    modifier: Modifier = Modifier,
) {
    val anchors = LocalRewardAnchors.current
    val currentPoints = anchors.animatedPoints ?: pointsCount
    val scale by animateFloatAsState(
        targetValue = if (anchors.pointsPulse) PULSE_SCALE else 1f,
        animationSpec = AwanTheme.motion.playful.spec(),
        label = "pointsBadgePulse",
    )
    AwanStatBadge(
        icon = Lucide.Coins,
        text = formatAbbreviatedPoints(currentPoints),
        surface = AwanTheme.colors.pointsSurface,
        accent = AwanTheme.colors.pointsIcon,
        modifier = modifier.scale(scale),
    )
}

@Composable
private fun AwanStatBadge(
    icon: ImageVector,
    text: String,
    surface: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(BadgeShape)
            .background(surface)
            .border(1.dp, accent.copy(alpha = 0.30f), BadgeShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            AwanText(
                text = text,
                style = AwanTheme.typography.heading.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                ),
            )
        }
    }
}

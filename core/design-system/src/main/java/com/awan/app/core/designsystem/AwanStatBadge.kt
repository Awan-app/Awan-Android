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
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide

private val BadgeShape = RoundedCornerShape(12.dp)
private const val PULSE_SCALE = 1.18f

/**
  * Formats a point integer value to a compact abbreviated string (e.g., 999, 1k, 1.2k, 10.5k, 1M).
  */
fun formatAbbreviatedPoints(points: Int): String {
    if (points < 0) return "-${formatAbbreviatedPoints(-points)}"
    return when {
        points >= 1_000_000_000 -> formatDecimal(points / 1_000_000_000.0) + "B"
        points >= 1_000_000 -> formatDecimal(points / 1_000_000.0) + "M"
        points >= 1_000 -> formatDecimal(points / 1_000.0) + "k"
        else -> points.toString()
    }
}

private fun formatDecimal(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", rounded)
    }
}

/**
 * The streak badge in the home header. Reads its colours from the theme's streak tokens so it
 * matches the streak line on the calendar.
 */
@Composable
fun AwanStreakBadge(
    streakCount: Int,
    modifier: Modifier = Modifier,
) {
    AwanStatBadge(
        icon = Lucide.Flame,
        text = streakCount.toString(),
        surface = AwanTheme.colors.streakSurface,
        accent = AwanTheme.colors.streakIcon,
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

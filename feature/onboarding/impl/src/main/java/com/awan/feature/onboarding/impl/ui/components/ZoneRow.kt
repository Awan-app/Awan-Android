package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.model.Zone
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.formatClock

private const val PRESS_SCALE = 0.98f
private val TimeIndent = 25.dp

/**
 * A compact two-line zone: name and toggle above, the tappable hour range below. Reordering is by
 * drag; the hours open in a sheet, so no time controls are shown until asked for.
 */
@Composable
fun ZoneRow(
    zone: Zone,
    overlapping: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    val zoneColor = Color(zone.colorArgb)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = reducedMotion()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !reduced) PRESS_SCALE else 1f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "zonePress",
    )

    // Opaque so the resting card shadow does not bleed through the tint.
    val tile = zoneColor
        .copy(alpha = if (zone.isEnabled) 0.28f else 0.08f)
        .compositeOver(AwanTheme.colors.surface)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(AwanTheme.shapes.card)
            .background(tile)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClickLabel = stringResource(R.string.onboarding_zone_edit_hours),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            dragHandle()
            AwanText(
                zone.name,
                style = if (zone.isEnabled) AwanTheme.styles.headingText else AwanTheme.styles.metaText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = zone.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = zoneColor, checkedThumbColor = Color.White),
            )
        }
        if (zone.isEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = TimeIndent),
            ) {
                AwanText(
                    stringResource(
                        R.string.onboarding_time_range,
                        formatClock(zone.startMinutes),
                        formatClock(zone.endMinutes),
                    ),
                    style = AwanTheme.styles.skipLink,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = zoneColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (overlapping) {
                AwanText(
                    stringResource(R.string.onboarding_zone_overlap),
                    style = AwanTheme.styles.captionText,
                    modifier = Modifier.padding(start = TimeIndent),
                )
            }
        }
    }
}

@Composable
fun ZoneDragHandle(color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(Modifier.size(3.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

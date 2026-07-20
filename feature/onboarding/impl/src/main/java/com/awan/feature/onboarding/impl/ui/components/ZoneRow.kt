package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Zone
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.formatClock

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AwanTheme.shapes.card)
            .background(zoneColor.copy(alpha = if (zone.isEnabled) 0.14f else 0.06f))
            .clickable(onClickLabel = stringResource(R.string.onboarding_zone_edit_hours), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            dragHandle()
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (zone.isEnabled) zoneColor else AwanTheme.colors.disabledContent),
            )
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
            AwanText(
                stringResource(
                    R.string.onboarding_time_range,
                    formatClock(zone.startMinutes),
                    formatClock(zone.endMinutes),
                ),
                style = AwanTheme.styles.skipLink,
                modifier = Modifier.padding(start = 34.dp),
            )
            if (overlapping) {
                AwanText(
                    stringResource(R.string.onboarding_zone_overlap),
                    style = AwanTheme.styles.captionText,
                    modifier = Modifier.padding(start = 34.dp),
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

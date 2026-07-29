package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@Composable
fun ZoneDetailItem(zone: DailyZone) {
    val context = LocalContext.current
    val zoneColor = zone.color.toColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(zoneColor.copy(alpha = 0.1f))
            .border(1.dp, zoneColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(zoneColor)
        )
        Column {
            AwanText(
                text = zone.name,
                style = AwanTheme.styles.bodyText.copy(
                    textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                )
            )
            AwanText(
                text = "${DailyZonesHelper.formatTime12h(context, zone.startTime)} - ${DailyZonesHelper.formatTime12h(context, zone.endTime)}",
                style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
            )
        }
    }
}

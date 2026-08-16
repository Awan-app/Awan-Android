package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@Composable
fun ZoneCardBody(
    zone: DailyZone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val zoneColor = zone.color.toColor()
    val backgroundColor = zoneColor
        .copy(alpha = AwanTheme.colors.zoneCardAlpha)
        .compositeOver(AwanTheme.colors.surface)

    AwanCard(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        background = backgroundColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(zoneColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = zone.name.ifBlank { stringResource(R.string.profile_zone_new_placeholder) },
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (zone.name.isBlank()) AwanTheme.colors.textSecondary else AwanTheme.colors.textPrimary
                    )
                )
                zone.category?.name?.let { categoryName ->
                    AwanText(
                        text = categoryName,
                        style = AwanTheme.styles.captionText.copy(
                            color = zoneColor,
                            textStyle = AwanTheme.styles.captionText.textStyle.copy(fontWeight = FontWeight.Medium)
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AwanTheme.colors.textSecondary
                    )
                    AwanText(
                        text = "${DailyZonesHelper.formatTime12h(context, zone.startTime)} - ${DailyZonesHelper.formatTime12h(context, zone.endTime)}",
                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                    )
                }
            }
        }
    }
}

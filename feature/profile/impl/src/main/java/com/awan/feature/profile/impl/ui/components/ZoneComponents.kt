package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@Composable
fun ZoneTimelineItem(
    zone: DailyZone,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(64.dp)
        ) {
            AwanText(
                text = DailyZonesHelper.formatTime12h(zone.startTime),
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp)
                )
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(AwanTheme.colors.line)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(zone.color.toColor())
                        .align(Alignment.TopCenter)
                )
            }

            if (isLast) {
                AwanText(
                    text = DailyZonesHelper.formatTime12h(zone.endTime),
                    style = AwanTheme.styles.captionText.copy(
                        textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp)
                    )
                )
            }
        }

        // Zone card body
        ZoneCardBody(
            zone = zone,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
fun ZoneCardBody(
    zone: DailyZone,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val zoneColor = zone.color.toColor()
    AwanCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        background = zoneColor.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ZoneDragHandle(color = zoneColor.copy(alpha = 0.5f))

            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = zone.name.ifBlank { "Zone name" },
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (zone.name.isBlank()) AwanTheme.colors.textSecondary else Color.Unspecified
                    )
                )
                AwanText(
                    text = "${DailyZonesHelper.formatTime12h(zone.startTime)} - ${DailyZonesHelper.formatTime12h(zone.endTime)}",
                    style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                )
            }

            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        tint = zoneColor,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(6.dp)
                    )
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = AwanTheme.colors.destructive,
                        modifier = Modifier.size(20.dp)
                    )
                }
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

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Transparent
    }
}

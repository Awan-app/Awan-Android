package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(64.dp)
        ) {
            AwanText(
                text = DailyZonesHelper.formatTime12h(context, zone.startTime),
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.styles.captionText.textStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(AwanTheme.colors.line)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(zone.color.toColor())
                        .align(Alignment.TopCenter)
                        .border(1.5.dp, AwanTheme.colors.background, CircleShape)
                )
            }

            if (isLast) {
                AwanText(
                    text = DailyZonesHelper.formatTime12h(context, zone.endTime),
                    style = AwanTheme.styles.captionText.copy(
                        textStyle = AwanTheme.styles.captionText.textStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                )
            }
        }

        ZoneCardBody(
            zone = zone,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            onClick = onClick
        )
    }
}

package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.rememberHapticClick
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@Composable
fun TimeInputBox(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticClick = rememberHapticClick(onClick)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AwanText(
            text = label,
            style = AwanTheme.styles.bodyText.copy(
                color = AwanTheme.colors.textSecondary,
                textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontSize = 13.sp)
            )
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = hapticClick)
                .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(12.dp)),
            color = AwanTheme.colors.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    null,
                    tint = AwanTheme.colors.sky,
                    modifier = Modifier.size(18.dp)
                )
                AwanText(
                    text = DailyZonesHelper.formatTime12h(context, time),
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontSize = 14.sp)
                    )
                )
            }
        }
    }
}

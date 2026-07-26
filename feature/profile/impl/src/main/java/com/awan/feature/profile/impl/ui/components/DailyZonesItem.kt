package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanText

@Composable
fun DailyZonesItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    AwanText(
                        text = title,
                        style = AwanTheme.styles.titleText.copy(
                            textStyle = AwanTheme.typography.title.copy(fontSize = 18.sp)
                        )
                    )
                    AwanText(
                        text = subtitle,
                        style = AwanTheme.styles.bodySecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneViolet))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneSky))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneSun))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneCoral))
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AwanTheme.colors.meta
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AwanTheme.colors.line,
                    thickness = 1.dp
                )
            }
        }
    }
}

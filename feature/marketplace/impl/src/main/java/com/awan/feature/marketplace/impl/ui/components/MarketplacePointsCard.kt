package com.awan.feature.marketplace.impl.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.marketplace.impl.R

@Composable
fun MarketplacePointsCard(
    points: Int,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = AwanTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AwanTheme.colors.line)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.pointsIcon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AwanText(
                            text = points.toString(),
                            style = AwanTheme.typography.heading.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AwanTheme.colors.pointsIcon
                            )
                        )
                        AwanText(
                            text = stringResource(R.string.marketplace_points_suffix),
                            style = AwanTheme.typography.body.copy(
                                fontSize = 18.sp,
                                color = AwanTheme.colors.pointsIcon
                            )
                        )
                    }
                    AwanText(
                        text = stringResource(R.string.marketplace_points_description),
                        style = AwanTheme.typography.caption.copy(
                            color = AwanTheme.colors.textSecondary
                        )
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onAddClick),
                color = AwanTheme.colors.pointsIcon.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AwanTheme.colors.pointsIcon,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

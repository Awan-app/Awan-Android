package com.awan.feature.home.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@Composable
internal fun InfoChip(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AwanTheme.colors.surface)
            .border(
                width = 1.dp,
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AwanText(text = icon, style = AwanTheme.typography.caption.copy(fontSize = 12.sp))
            Spacer(modifier = Modifier.width(4.dp))
            AwanText(
                text = label,
                style = AwanTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun DetailRow(
    icon: String,
    title: String,
    value: String,
    isBadge: Boolean = false,
    isSuccessBadge: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AwanText(text = icon, style = AwanTheme.typography.body.copy(fontSize = 14.sp))
            Spacer(modifier = Modifier.width(8.dp))
            AwanText(
                text = title,
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    color = AwanTheme.colors.textSecondary,
                ),
            )
        }

        if (isBadge) {
            val bg = if (isSuccessBadge) AwanTheme.colors.success.copy(alpha = 0.15f)
            else AwanTheme.colors.sky.copy(alpha = 0.12f)
            val fg = if (isSuccessBadge) AwanTheme.colors.success else AwanTheme.colors.sky

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                AwanText(
                    text = value,
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                    ),
                )
            }
        } else {
            AwanText(
                text = value,
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )
        }
    }
}

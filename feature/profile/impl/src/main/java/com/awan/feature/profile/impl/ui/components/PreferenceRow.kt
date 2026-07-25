package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanText

@Composable
fun PreferenceRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    iconColor: Color = AwanTheme.colors.sky,
    titleColor: Color = AwanTheme.colors.textPrimary,
    isExpanded: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Surface(
        onClick = { onClick?.invoke() },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
        enabled = onClick != null
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        AwanText(
                            text = title,
                            style = AwanTheme.styles.bodyText.copy(
                                color = titleColor,
                                textStyle = AwanTheme.typography.body.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        )
                        if (value != null && !isExpanded) {
                            AwanText(
                                text = value,
                                style = AwanTheme.styles.bodySecondaryText
                            )
                        }
                    }
                }

                if (content != null) {
                    content()
                } else {
                    Icon(
                        imageVector = if (onClick != null && value != null) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AwanTheme.colors.meta,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (onClick != null && value != null) rotationState else 0f)
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AwanTheme.colors.line.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
            }
        }
    }
}

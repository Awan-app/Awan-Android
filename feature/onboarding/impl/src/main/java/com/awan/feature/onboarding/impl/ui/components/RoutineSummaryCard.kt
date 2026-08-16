package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@Composable
fun RoutineSummaryCard(
    name: String,
    zoneCount: Int,
    modifier: Modifier = Modifier,
) {
    AwanCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AwanTheme.colors.zoneViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AwanTheme.colors.zoneViolet,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AwanText(
                        text = name,
                        style = AwanTheme.styles.titleText
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AwanTheme.colors.line)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        AwanText(
                            text = "Default",
                            style = AwanTheme.styles.captionText.copy(
                                textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp)
                            )
                        )
                    }
                }
                
                AwanText(
                    text = "Used on all 7 days",
                    style = AwanTheme.styles.metaText
                )
                
                AwanText(
                    text = "$zoneCount zones",
                    style = AwanTheme.styles.metaText.copy(color = AwanTheme.colors.sky)
                )
            }
        }
    }
}

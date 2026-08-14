package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.goals.impl.R
import com.composables.icons.lucide.Inbox
import com.composables.icons.lucide.Lucide

@Composable
internal fun GoalsTopBar(
    inboxTaskCount: Int,
    onInboxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AwanText(
            text = stringResource(R.string.goals_title_count),
            style = AwanTheme.typography.title.copy(
                fontSize = 28.sp,
                color = colors.textPrimary
            )
        )

        Box(
            contentAlignment = Alignment.Center
        ) {
            AwanIconButton(
                onClick = onInboxClick,
                contentDescription = stringResource(R.string.goals_inbox_content_description),
                icon = {
                    Icon(
                        imageVector = Lucide.Inbox,
                        contentDescription = null,
                        tint = colors.sky,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
            
            if (inboxTaskCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(16.dp)
                        .background(colors.zoneSun, CircleShape)
                        .border(1.5.dp, colors.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AwanText(
                        text = inboxTaskCount.toString(),
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF16455E)
                        )
                    )
                }
            }
        }
    }
}

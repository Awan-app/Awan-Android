package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.goals.impl.presentation.InboxTaskUiModel

@Composable
internal fun InboxTaskCard(
    task: InboxTaskUiModel,
    onTaskClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing

    AwanCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onTaskClick,
        contentPadding = PaddingValues(spacing.md),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AwanText(
                text = task.title,
                style = AwanTheme.typography.heading.copy(
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            
            if (!task.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(spacing.xxs))
                AwanText(
                    text = task.description,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

package com.awan.feature.addtask.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.feature.addtask.R

/** The mascot already sits in the sheet's sky header, so this is copy only. */
@Composable
fun GoalPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AwanTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CascadeItem(1) {
            AwanText(
                text = stringResource(R.string.add_task_goal_title),
                style = AwanTheme.typography.title.copy(
                    color = AwanTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        CascadeItem(2) {
            AwanText(
                text = stringResource(R.string.add_task_goal_body),
                style = AwanTheme.typography.body.copy(
                    color = AwanTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

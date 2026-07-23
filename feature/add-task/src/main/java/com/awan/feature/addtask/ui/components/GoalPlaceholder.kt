package com.awan.feature.addtask.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.addtask.R

@Composable
fun GoalPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AwanTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AwanMascot(expression = MascotExpression.Curious)
        AwanText(stringResource(R.string.add_task_goal_title), style = AwanTheme.styles.titleText)
        AwanText(stringResource(R.string.add_task_goal_body), style = AwanTheme.styles.bodySecondaryText)
    }
}

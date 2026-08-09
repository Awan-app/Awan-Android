package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.GoalsTab

/**
 * Empty-state shown when a tab has no goals.
 * Active tab → Curious mascot  |  Completed tab → Celebrate mascot
 */
@Composable
internal fun GoalsEmptyState(
    tab: GoalsTab,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val expression = when (tab) {
            GoalsTab.Active -> MascotExpression.Curious
            GoalsTab.Completed -> MascotExpression.Celebrate
        }
        AwanMascot(expression = expression, width = 100.dp)
        Spacer(modifier = Modifier.height(20.dp))
        AwanText(
            text = when (tab) {
                GoalsTab.Active -> stringResource(R.string.goals_empty_active_title)
                GoalsTab.Completed -> stringResource(R.string.goals_empty_completed_title)
            },
            style = AwanTheme.typography.heading.copy(
                fontSize = 18.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AwanText(
            text = when (tab) {
                GoalsTab.Active -> stringResource(R.string.goals_empty_active_subtitle)
                GoalsTab.Completed -> stringResource(R.string.goals_empty_completed_subtitle)
            },
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

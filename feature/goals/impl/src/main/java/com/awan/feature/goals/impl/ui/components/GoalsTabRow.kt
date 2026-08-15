package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanSegmentedControl
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.GoalsTab

/**
 * Switches between Active and Completed goals, with each tab's count folded into its label
 * (e.g. "Active  2").
 */
@Composable
internal fun GoalsTabRow(
    selectedTab: GoalsTab,
    activeCount: Int,
    completedCount: Int,
    onTabSelected: (GoalsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    AwanSegmentedControl(
        options = GoalsTab.entries,
        selected = selectedTab,
        onSelect = onTabSelected,
        label = { tab ->
            val label = when (tab) {
                GoalsTab.Active -> stringResource(R.string.goals_tab_active)
                GoalsTab.Completed -> stringResource(R.string.goals_tab_completed)
            }
            val count = when (tab) {
                GoalsTab.Active -> activeCount
                GoalsTab.Completed -> completedCount
            }
            stringResource(R.string.goals_tab_badge_format, label, count)
        },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

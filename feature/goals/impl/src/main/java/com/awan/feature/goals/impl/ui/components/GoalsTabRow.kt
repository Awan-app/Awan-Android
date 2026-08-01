package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.goals.impl.presentation.GoalsTab
import com.awan.feature.goals.impl.R

/**
 * Pill-shaped tab row that switches between Active and Completed goals.
 * Shows the count of goals in each tab next to the label (e.g. "Active  2").
 */
@Composable
internal fun GoalsTabRow(
    selectedTab: GoalsTab,
    activeCount: Int,
    completedCount: Int,
    onTabSelected: (GoalsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(99.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(Color(0xFFDDEEF9))
            .padding(4.dp),
    ) {
        GoalsTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val count = when (tab) {
                GoalsTab.Active -> activeCount
                GoalsTab.Completed -> completedCount
            }
            val label = when (tab) {
                GoalsTab.Active -> stringResource(R.string.goals_tab_active)
                GoalsTab.Completed -> stringResource(R.string.goals_tab_completed)
            }
            val tabShape = RoundedCornerShape(99.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(elevation = 2.dp, shape = tabShape, spotColor = Color(0x22000000))
                                .clip(tabShape)
                                .background(Color.White)
                        } else {
                            Modifier.clip(tabShape)
                        },
                    )
                    .selectable(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        role = Role.Tab,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = "$label  $count",
                    style = AwanTheme.typography.button.copy(
                        color = if (isSelected) AwanTheme.colors.ink else AwanTheme.colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                )
            }
        }
    }
}

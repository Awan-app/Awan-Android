package com.awan.feature.goals.impl.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.core.navigation.Route
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.GoalsScreen
import com.awan.feature.goals.impl.presentation.GoalsViewModel
import com.awan.feature.goals.impl.presentation.InboxScreen
import com.awan.feature.goals.impl.presentation.InboxViewModel

fun EntryProviderScope<Route>.goalsEntry() {
    entry<GoalsRoute> {
        GoalsRouteScreen()
    }
}

enum class GoalsTopLevelTab {
    Goals, Inbox
}

@Composable
fun GoalsRouteScreen(
    goalsViewModel: GoalsViewModel = hiltViewModel(),
    inboxViewModel: InboxViewModel = hiltViewModel(),
) {
    val goalsState by goalsViewModel.state.collectAsStateWithLifecycle()
    val inboxState by inboxViewModel.state.collectAsStateWithLifecycle()
    
    var selectedTab by rememberSaveable { mutableStateOf(GoalsTopLevelTab.Goals) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background)
            .statusBarsPadding()
    ) {
        // Shared Top Level Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            AwanText(
                text = stringResource(R.string.goals_title), // Or change to dynamically update based on tab
                style = AwanTheme.typography.display.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Level Segmented Control
        val shape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(shape)
                .background(AwanTheme.colors.surface)
                .border(1.dp, AwanTheme.colors.line, shape)
                .padding(4.dp),
        ) {
            GoalsTopLevelTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tabShape = RoundedCornerShape(8.dp)
                val text = when (tab) {
                    GoalsTopLevelTab.Goals -> stringResource(R.string.goals_title)
                    GoalsTopLevelTab.Inbox -> stringResource(R.string.inbox_title)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(tabShape)
                        .background(if (isSelected) AwanTheme.colors.sky else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            role = Role.Tab,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = text,
                        style = AwanTheme.typography.button.copy(
                            color = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                GoalsTopLevelTab.Goals -> {
                    GoalsScreen(
                        state = goalsState,
                        onAction = goalsViewModel::onAction,
                    )
                }
                GoalsTopLevelTab.Inbox -> {
                    InboxScreen(
                        state = inboxState,
                        onAction = inboxViewModel::onAction,
                    )
                }
            }
        }
    }
}

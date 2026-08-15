package com.awan.feature.goals.impl.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.app.core.designsystem.AwanSegmentedControl
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.core.navigation.Route
import com.awan.feature.goals.api.GoalsRoute
import androidx.compose.ui.graphics.Brush
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.ui.GoalsScreen
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AwanTheme.colors.backgroundStart,
                        AwanTheme.colors.background,
                        AwanTheme.colors.background,
                    ),
                ),
            )
            .statusBarsPadding()
    ) {
        // Shared Top Level Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val titleText = when (selectedTab) {
                GoalsTopLevelTab.Goals -> stringResource(R.string.goals_title)
                GoalsTopLevelTab.Inbox -> stringResource(R.string.inbox_title)
            }
            AwanText(
                text = titleText,
                style = AwanTheme.typography.title.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.ink,
                ),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AwanSegmentedControl(
            options = GoalsTopLevelTab.entries,
            selected = selectedTab,
            onSelect = { selectedTab = it },
            label = { tab ->
                stringResource(
                    when (tab) {
                        GoalsTopLevelTab.Goals -> R.string.goals_title
                        GoalsTopLevelTab.Inbox -> R.string.inbox_title
                    },
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

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

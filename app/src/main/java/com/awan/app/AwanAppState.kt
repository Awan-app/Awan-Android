package com.awan.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.awan.core.navigation.NavigationState
import com.awan.core.navigation.Route
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberAwanAppState(
    startKey: Route,
    topLevelKeys: List<Route>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): AwanAppState {
    val navigationState = remember {
        val topLevelStack = mutableStateListOf(startKey)
        val subStacks = topLevelKeys.associateWith { key ->
            mutableStateListOf(key)
        }.toMutableMap()
        if (!subStacks.containsKey(startKey)) {
            subStacks[startKey] = mutableStateListOf(startKey)
        }
        NavigationState(
            startKey = startKey,
            topLevelStack = topLevelStack,
            subStacks = subStacks
        )
    }

    return remember(navigationState, coroutineScope) {
        AwanAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope
        )
    }
}

@Stable
class AwanAppState(
    val navigationState: NavigationState,
    val coroutineScope: CoroutineScope,
) {
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries
}

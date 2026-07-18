package com.awan.core.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

@Stable
class NavigationState(
    val startKey: Route,
    val topLevelStack: MutableList<Route>,
    val subStacks: Map<Route, MutableList<Route>>,
) {
    val currentTopLevelKey: Route by derivedStateOf { topLevelStack.last() }
    val topLevelKeys get() = subStacks.keys

    val currentSubStack: MutableList<Route>
        get() = subStacks[currentTopLevelKey] ?: error("No sub-stack found for top-level key: $currentTopLevelKey")

    val currentKey: Route by derivedStateOf { currentSubStack.last() }

    val canGoBackSubStack: Boolean by derivedStateOf { currentSubStack.size > 1 }
    val canGoBackTopLevel: Boolean by derivedStateOf { topLevelStack.size > 1 }
}



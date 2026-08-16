package com.awan.core.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Stable
class NavigationState(
    val startKey: Route,
    val topLevelStack: MutableList<Route>,
    val subStacks: Map<Route, MutableList<Route>>,
) {
    // Bumped by every replaceAll. A tab root never leaves its own sub-stack, so nothing pops it and
    // nothing clears its ViewModel — this is what tells the entry decorators the stacks they are
    // holding state for belong to a flow that is over.
    var generation by mutableIntStateOf(0)
        internal set

    val currentTopLevelKey: Route by derivedStateOf { topLevelStack.last() }
    val topLevelKeys get() = subStacks.keys

    val currentSubStack: MutableList<Route>
        get() = subStacks[currentTopLevelKey] ?: error("No sub-stack found for top-level key: $currentTopLevelKey")

    val currentKey: Route by derivedStateOf { currentSubStack.last() }

    val canGoBackSubStack: Boolean by derivedStateOf { currentSubStack.size > 1 }
    val canGoBackTopLevel: Boolean by derivedStateOf { topLevelStack.size > 1 }
}



package com.awan.core.navigation

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {
    @Test
    fun goBack_keepsTheReplacementRoot() {
        val state = NavigationState(
            startKey = StartRoute,
            topLevelStack = mutableStateListOf(ReplacementRootRoute),
            subStacks = mapOf(
                ReplacementRootRoute to mutableStateListOf(ReplacementRootRoute),
            ),
        )

        Navigator(state).goBack()

        assertEquals(ReplacementRootRoute, state.currentTopLevelKey)
    }

    private data object StartRoute : Route
    private data object ReplacementRootRoute : Route
}

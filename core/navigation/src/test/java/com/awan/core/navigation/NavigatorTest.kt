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

    @Test
    fun replaceAll_resetsEverySubStackNotJustTheTarget() {
        val state = NavigationState(
            startKey = StartRoute,
            topLevelStack = mutableStateListOf(TabRoute),
            subStacks = mutableMapOf<Route, MutableList<Route>>(
                TabRoute to mutableStateListOf(TabRoute),
                OtherTabRoute to mutableStateListOf(OtherTabRoute, DeepRoute),
            ),
        )

        Navigator(state).replaceAll(ReplacementRootRoute)

        assertEquals(listOf(ReplacementRootRoute), state.topLevelStack)
        assertEquals(listOf(OtherTabRoute), state.subStacks.getValue(OtherTabRoute))
        assertEquals(listOf(ReplacementRootRoute), state.currentSubStack)
    }

    private data object StartRoute : Route
    private data object ReplacementRootRoute : Route
    private data object TabRoute : Route
    private data object OtherTabRoute : Route
    private data object DeepRoute : Route
}

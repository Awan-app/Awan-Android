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

    @Test
    fun replaceAll_bumpsGenerationSoEntryDecoratorsDrop() {
        val state = NavigationState(
            startKey = StartRoute,
            topLevelStack = mutableStateListOf(TabRoute),
            subStacks = mutableMapOf<Route, MutableList<Route>>(
                TabRoute to mutableStateListOf(TabRoute),
            ),
        )
        val navigator = Navigator(state)
        val before = state.generation

        navigator.replaceAll(ReplacementRootRoute)
        navigator.replaceAll(TabRoute)

        assertEquals(before + 2, state.generation)
    }

    @Test
    fun navigate_doesNotBumpGeneration() {
        val state = NavigationState(
            startKey = StartRoute,
            topLevelStack = mutableStateListOf(TabRoute),
            subStacks = mutableMapOf<Route, MutableList<Route>>(
                TabRoute to mutableStateListOf(TabRoute),
                OtherTabRoute to mutableStateListOf(OtherTabRoute),
            ),
        )
        val navigator = Navigator(state)
        val before = state.generation

        navigator.navigate(DeepRoute)
        navigator.navigate(OtherTabRoute)

        assertEquals(before, state.generation)
    }

    @Test
    fun resetCurrentSubStack_clearsIntermediateScreensAndKeepsRoot() {
        val state = NavigationState(
            startKey = StartRoute,
            topLevelStack = mutableStateListOf(TabRoute),
            subStacks = mutableMapOf<Route, MutableList<Route>>(
                TabRoute to mutableStateListOf(TabRoute, DeepRoute, OtherRoute),
            ),
        )
        val navigator = Navigator(state)

        navigator.resetCurrentSubStack(TabRoute)

        assertEquals(listOf(TabRoute), state.currentSubStack)
        assertEquals(TabRoute, state.currentKey)
    }

    private data object StartRoute : Route
    private data object ReplacementRootRoute : Route
    private data object TabRoute : Route
    private data object OtherTabRoute : Route
    private data object DeepRoute : Route
    private data object OtherRoute : Route
}

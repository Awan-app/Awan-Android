package com.awan.core.navigation

import androidx.compose.runtime.mutableStateListOf

class Navigator(val state: NavigationState) {

    fun navigate(key: Route) {
        when (key) {
            state.currentTopLevelKey -> clearSubStack()
            in state.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    fun replaceAll(key: Route) {
        state.topLevelStack.clear()
        state.topLevelStack.add(key)

        val subStack = state.subStacks[key]
        if (subStack != null) {
            subStack.clear()
            subStack.add(key)
        } else {
            state.subStacks as MutableMap<Route, MutableList<Route>>
            state.subStacks[key] = mutableStateListOf(key)
        }
    }

    fun resetCurrentSubStack(key: Route) {
        state.currentSubStack.clear()
        state.currentSubStack.add(key)
    }

    fun goBack() {
        when (state.currentKey) {
            state.startKey -> error("Cannot go back from start key")
            state.currentTopLevelKey -> if (state.canGoBackTopLevel) state.topLevelStack.removeAt(state.topLevelStack.lastIndex)
            else -> state.currentSubStack.removeLastOrNull()
        }
    }

    private fun goToKey(key: Route) { state.currentSubStack.apply { remove(key); add(key) } }
    private fun goToTopLevel(key: Route) { state.topLevelStack.apply { val root = firstOrNull(); clear(); if (root != null) add(root); if (key != root) add(key) } }
    private fun clearSubStack() { state.currentSubStack.run { if (size > 1) subList(1, size).clear() } }
}


package com.awan.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data object Arena : Route

    @Serializable
    data object Calender : Route

    @Serializable
    data object Settings : Route
}

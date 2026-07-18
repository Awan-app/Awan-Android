package com.awan.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.awan.core.navigation.Route
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.chat.api.ChatRoute
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.profile.api.ProfileRoute

enum class TopLevelDestination(
    val route: Route,
    val icon: ImageVector,
    val label: String,
) {
    HOME(
        route = HomeRoute,
        icon = Icons.Default.Home,
        label = "Home",
    ),
    CALENDAR(
        route = CalendarRoute,
        icon = Icons.Default.DateRange,
        label = "Calendar",
    ),
    CHAT(
        route = ChatRoute,
        icon = Icons.Default.Face,
        label = "Chat",
    ),
    GOALS(
        route = GoalsRoute,
        icon = Icons.Default.ThumbUp,
        label = "Goals",
    ),
    PROFILE(
        route = ProfileRoute,
        icon = Icons.Default.Person,
        label = "Profile",
    ),
}

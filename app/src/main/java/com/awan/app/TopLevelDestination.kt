package com.awan.app

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.awan.core.navigation.Route
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.profile.api.ProfileRoute

/**
 * The bar's items in order. `AwanBottomBar` splits this list down the middle and drops the add-task
 * button between the halves, so keeping an even count keeps the button centred.
 *
 * Chat is deliberately absent: it is still a registered route, it just has no bar item yet.
 */
enum class TopLevelDestination(
    val route: Route,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    HOME(
        route = HomeRoute,
        icon = Icons.Default.Home,
        labelRes = R.string.navigation_home,
    ),
    CALENDAR(
        route = CalendarRoute,
        icon = Icons.Default.DateRange,
        labelRes = R.string.navigation_calendar,
    ),
    GOALS(
        route = GoalsRoute,
        icon = Icons.Default.ThumbUp,
        labelRes = R.string.navigation_goals,
    ),
    PROFILE(
        route = ProfileRoute,
        icon = Icons.Default.Person,
        labelRes = R.string.navigation_profile,
    ),
}

package com.awan.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.awan.core.navigation.Route
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.marketplace.api.MarketplaceRoute
import com.awan.feature.profile.api.ProfileRoute

enum class TopLevelDestination(
    val route: Route?,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val isFab: Boolean = false,
) {
    HOME(
        route = HomeRoute(),
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        label = "Home",
    ),
    GOALS(
        route = GoalsRoute,
        selectedIcon = Icons.Rounded.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        label = "Goals",
    ),
    AI_ACTION(
        route = null,
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Rounded.AutoAwesome,
        label = "AI Action",
        isFab = true,
    ),
    MARKETPLACE(
        route = MarketplaceRoute,
        selectedIcon = Icons.Rounded.Storefront,
        unselectedIcon = Icons.Outlined.Storefront,
        label = "Market",
    ),
    PROFILE(
        route = ProfileRoute,
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person,
        label = "Profile",
    ),
}

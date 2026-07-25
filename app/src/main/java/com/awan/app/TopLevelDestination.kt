package com.awan.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.awan.core.navigation.Route
import com.awan.feature.chat.api.ChatRoute
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.profile.api.ProfileRoute

enum class TopLevelDestination(val route: Route, val icon: ImageVector, val label: String) {
    HOME(HomeRoute(), Icons.Default.Home, "Home"),
    CHAT(ChatRoute, Icons.Default.Face, "Chat"),
    GOALS(GoalsRoute, Icons.Default.ThumbUp, "Goals"),
    PROFILE(ProfileRoute, Icons.Default.Person, "Profile"),
}

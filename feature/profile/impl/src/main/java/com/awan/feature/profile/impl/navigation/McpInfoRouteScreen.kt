package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import com.awan.feature.profile.impl.ui.McpInfoScreen

@Composable
fun McpInfoRouteScreen(
    onBack: () -> Unit,
) {
    McpInfoScreen(
        onBackClick = onBack
    )
}

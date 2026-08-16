package com.awan.feature.marketplace.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.marketplace.api.MarketplaceRoute
import com.awan.feature.marketplace.impl.ui.MarketplaceScreen

fun EntryProviderScope<Route>.marketplaceEntry(
    onNavigateToHome: () -> Unit = {}
) {
    entry<MarketplaceRoute> {
        MarketplaceScreen(
            onNavigateToHome = onNavigateToHome
        )
    }
}

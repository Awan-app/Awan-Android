package com.awan.feature.inventory.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.inventory.api.InventoryRoute

fun EntryProviderScope<Route>.inventoryEntry(onBack: () -> Unit) {
    entry<InventoryRoute> {
        InventoryRouteScreen(onBack = onBack)
    }
}

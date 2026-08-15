package com.awan.feature.marketplace.impl.presentation

import androidx.annotation.StringRes

sealed interface MarketplaceEvent {
    data object PurchaseSuccess : MarketplaceEvent
    data class Error(@StringRes val message: Int) : MarketplaceEvent
}

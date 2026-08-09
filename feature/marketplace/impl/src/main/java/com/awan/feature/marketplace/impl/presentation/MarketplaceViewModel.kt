package com.awan.feature.marketplace.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.usecase.BuyItemUseCase
import com.awan.app.core.domain.marketplace.usecase.EquipItemUseCase
import com.awan.app.core.domain.marketplace.usecase.GetEquippedItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.GetInventoryUseCase
import com.awan.app.core.domain.marketplace.usecase.GetStoreItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.RefreshMarketplaceUseCase
import com.awan.app.core.domain.marketplace.usecase.UnequipItemUseCase
import com.awan.app.core.domain.profile.usecase.ObserveProfileUseCase
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.feature.marketplace.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val getStoreItemsUseCase: GetStoreItemsUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getEquippedItemsUseCase: GetEquippedItemsUseCase,
    private val buyItemUseCase: BuyItemUseCase,
    private val equipItemUseCase: EquipItemUseCase,
    private val unequipItemUseCase: UnequipItemUseCase,
    private val refreshMarketplaceUseCase: RefreshMarketplaceUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    
    val state: StateFlow<MarketplaceUiState> = combine(
        _uiState,
        getStoreItemsUseCase(),
        getInventoryUseCase(),
        getEquippedItemsUseCase(),
        observeProfileUseCase()
    ) { uiState, items, inventory, equipped, profile ->
        uiState.copy(
            items = items,
            inventory = inventory,
            equipped = equipped,
            points = profile?.points ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MarketplaceUiState()
    )

    init {
        refresh()
    }

    fun onAction(action: MarketplaceAction) {
        when (action) {
            is MarketplaceAction.SelectCategory -> _uiState.update { it.copy(selectedCategory = action.category) }
            is MarketplaceAction.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = action.query) }
            is MarketplaceAction.SelectItem -> _uiState.update { it.copy(selectedItem = action.item) }
            is MarketplaceAction.BuyItem -> buyItem(action.item)
            is MarketplaceAction.EquipItem -> equipItem(action.item)
            is MarketplaceAction.UnequipItem -> unequipItem(action.type)
            MarketplaceAction.Refresh -> refresh()
            MarketplaceAction.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            refreshMarketplaceUseCase()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun buyItem(item: StoreItem) {
        if (state.value.isBuying) return
        
        _uiState.update { it.copy(isBuying = true) }
        viewModelScope.launch {
            when (val result = buyItemUseCase(item.id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isBuying = false, selectedItem = null) }
                }
                is Result.Error -> {
                    val errorRes = when (val error = result.error) {
                        is AppError.Api -> when (error.errorCode) {
                            "INSUFFICIENT_POINTS" -> R.string.marketplace_insufficient_points
                            "ITEM_ALREADY_OWNED" -> R.string.marketplace_already_owned
                            "ITEM_NOT_FOUND" -> R.string.marketplace_item_not_found
                            else -> R.string.marketplace_error_buying
                        }
                        else -> R.string.marketplace_error_buying
                    }
                    _uiState.update { it.copy(error = errorRes, isBuying = false) }
                }
                Result.Loading -> { /* Handled by isBuying state */ }
            }
        }
    }

    private fun equipItem(item: StoreItem) {
        if (state.value.isEquipping) return
        
        _uiState.update { it.copy(isEquipping = true) }
        viewModelScope.launch {
            when (val result = equipItemUseCase(item.id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isEquipping = false, selectedItem = null) }
                }
                is Result.Error -> {
                    val errorRes = when (val error = result.error) {
                        is AppError.Api -> when (error.errorCode) {
                            "ITEM_NOT_OWNED" -> R.string.marketplace_error_equipping
                            "ITEM_NOT_FOUND" -> R.string.marketplace_item_not_found
                            else -> R.string.marketplace_error_equipping
                        }
                        else -> R.string.marketplace_error_equipping
                    }
                    _uiState.update { it.copy(error = errorRes, isEquipping = false) }
                }
                Result.Loading -> { /* Handled by isEquipping state */ }
            }
        }
    }

    private fun unequipItem(type: StoreItemType) {
        viewModelScope.launch {
            val result = unequipItemUseCase(type)
            if (result is Result.Error) {
                _uiState.update { it.copy(error = R.string.marketplace_error_unequipping) }
            }
        }
    }
}

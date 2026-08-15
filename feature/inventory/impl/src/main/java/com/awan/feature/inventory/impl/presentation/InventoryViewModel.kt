package com.awan.feature.inventory.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.usecase.EquipItemUseCase
import com.awan.app.core.domain.marketplace.usecase.GetEquippedItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.GetInventoryUseCase
import com.awan.app.core.domain.marketplace.usecase.RefreshMarketplaceUseCase
import com.awan.app.core.domain.network.usecase.ObserveNetworkConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getInventory: GetInventoryUseCase,
    private val getEquippedItems: GetEquippedItemsUseCase,
    private val refreshMarketplace: RefreshMarketplaceUseCase,
    private val equipItem: EquipItemUseCase,
    private val observeConnectivity: ObserveNetworkConnectivityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getInventory().collect { items ->
                _state.update { it.copy(items = items, isLoading = false) }
            }
        }
        viewModelScope.launch {
            getEquippedItems().collect { equipped ->
                _state.update { it.copy(equippedItemIds = equipped.map { it.item.id }.toSet()) }
            }
        }
        viewModelScope.launch {
            observeConnectivity().collect { isOnline -> 
                _state.update { it.copy(isOnline = isOnline) } 
            }
        }
        refresh()
    }

    fun onAction(action: InventoryAction) {
        when (action) {
            InventoryAction.Refresh -> refresh()
            is InventoryAction.SelectType -> _state.update { it.copy(selectedType = action.type) }
            is InventoryAction.ToggleRarity -> _state.update { state ->
                val rarities = state.selectedRarities.toMutableSet()
                if (action.rarity in rarities) {
                    rarities.remove(action.rarity)
                } else {
                    rarities.add(action.rarity)
                }
                state.copy(selectedRarities = rarities)
            }
            is InventoryAction.SetSort -> _state.update { it.copy(sort = action.sort) }
            is InventoryAction.Equip -> equip(action.itemId)
        }
    }

    private fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                refreshMarketplace()
                _state.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun equip(itemId: String) {
        if (!_state.value.isOnline || _state.value.equippingItemId != null) return
        viewModelScope.launch {
            _state.update { it.copy(equippingItemId = itemId, error = null) }
            when (val result = equipItem(itemId)) {
                is Result.Error -> _state.update { it.copy(equippingItemId = null, error = result.error.toUiText()) }
                is Result.Success -> _state.update { it.copy(equippingItemId = null) }
                Result.Loading -> _state.update { it.copy(equippingItemId = null) }
            }
        }
    }
}

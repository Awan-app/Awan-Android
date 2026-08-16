package com.awan.feature.inventory.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.usecase.EquipItemUseCase
import com.awan.app.core.domain.marketplace.usecase.GetEquippedItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.GetInventoryUseCase
import com.awan.app.core.domain.marketplace.usecase.MarkInventorySeenUseCase
import com.awan.app.core.domain.marketplace.usecase.RefreshMarketplaceUseCase
import com.awan.app.core.domain.marketplace.usecase.UnequipItemUseCase
import com.awan.app.core.domain.network.usecase.ObserveNetworkConnectivityUseCase
import com.awan.app.core.model.StoreItemType
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
    private val unequipItem: UnequipItemUseCase,
    private val markInventorySeen: MarkInventorySeenUseCase,
    private val observeConnectivity: ObserveNetworkConnectivityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    /**
     * In-memory latch: item ids that have been acknowledged (seen) during this process lifetime.
     * Once an id is added here it will never re-enter [InventoryState.unseenItemIds] even if the
     * database Flow re-emits (e.g. after the app returns from background), preventing the
     * acquisition animation from replaying on re-open.
     */
    private val seenThisSession = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            getInventory().collect { items ->
                val newUnseen = items
                    .filter { item -> !item.isSeen && item.item.id !in seenThisSession }
                    .map { item -> item.item.id }
                    .toSet()

                if (newUnseen.isNotEmpty()) {
                    seenThisSession += newUnseen
                    // Instantly persist isSeen = 1 to the database so it reflects
                    // that it has been seen immediately upon opening the screen.
                    markInventorySeen()
                }

                _state.update { current ->
                    current.copy(
                        items = items,
                        isLoading = false,
                        unseenItemIds = current.unseenItemIds + newUnseen,
                    )
                }
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
            is InventoryAction.Unequip -> unequip(action.type)
            is InventoryAction.OpenDetails -> _state.update { it.copy(detailsItemId = action.itemId) }
            InventoryAction.CloseDetails -> _state.update { it.copy(detailsItemId = null) }
            InventoryAction.MarkSeen -> markSeen()
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

    private fun unequip(type: StoreItemType) {
        if (!_state.value.isOnline || _state.value.unequippingType != null) return
        viewModelScope.launch {
            _state.update { it.copy(unequippingType = type, error = null) }
            when (val result = unequipItem(type)) {
                is Result.Error -> _state.update { it.copy(unequippingType = null, error = result.error.toUiText()) }
                is Result.Success -> {
                    _state.update { it.copy(unequippingType = null) }
                    refreshMarketplace()
                }
                Result.Loading -> _state.update { it.copy(unequippingType = null) }
            }
        }
    }

    private fun markSeen() {
        val idsToMark = _state.value.unseenItemIds
        seenThisSession += idsToMark
        _state.update { it.copy(unseenItemIds = emptySet()) }
    }
}

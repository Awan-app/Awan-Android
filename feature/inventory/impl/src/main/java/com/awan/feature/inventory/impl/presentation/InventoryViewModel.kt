package com.awan.feature.inventory.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.inventory.usecase.EquipCustomizationUseCase
import com.awan.app.core.domain.inventory.usecase.ObserveInventoryUseCase
import com.awan.app.core.domain.inventory.usecase.RefreshInventoryUseCase
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
    private val observeInventory: ObserveInventoryUseCase,
    private val refreshInventory: RefreshInventoryUseCase,
    private val equipCustomization: EquipCustomizationUseCase,
    private val observeConnectivity: ObserveNetworkConnectivityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeInventory().collect { customizations ->
                _state.update { it.copy(customizations = customizations) }
            }
        }
        viewModelScope.launch {
            observeConnectivity().collect { isOnline -> _state.update { it.copy(isOnline = isOnline) } }
        }
        refresh()
    }

    fun onAction(action: InventoryAction) {
        when (action) {
            InventoryAction.Refresh -> refresh()
            is InventoryAction.SelectType -> _state.update { it.copy(selectedType = action.type) }
            is InventoryAction.ToggleRarity -> _state.update {
                it.copy(
                    selectedRarities = it.selectedRarities.toMutableSet().apply {
                        if (!add(action.rarity)) remove(action.rarity)
                    },
                )
            }
            is InventoryAction.SetSort -> _state.update { it.copy(sort = action.sort) }
            is InventoryAction.Equip -> equip(action.itemId)
        }
    }

    private fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            when (val result = refreshInventory()) {
                is Result.Error -> _state.update { it.copy(isRefreshing = false, isLoading = false, error = result.error.toUiText()) }
                is Result.Success -> _state.update { it.copy(isRefreshing = false, isLoading = false) }
                Result.Loading -> Unit
            }
        }
    }

    private fun equip(itemId: String) {
        if (!_state.value.isOnline || _state.value.equippingItemId != null) return
        viewModelScope.launch {
            _state.update { it.copy(equippingItemId = itemId, error = null) }
            when (val result = equipCustomization(itemId)) {
                is Result.Error -> _state.update { it.copy(equippingItemId = null, error = result.error.toUiText()) }
                is Result.Success -> _state.update { it.copy(equippingItemId = null) }
                Result.Loading -> _state.update { it.copy(equippingItemId = null) }
            }
        }
    }
}

package com.awan.feature.marketplace.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.feature.marketplace.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketplaceUiState())
    val state: StateFlow<MarketplaceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                _state.update { it.copy(points = profile?.points ?: 0) }
            }
        }
        refresh()
    }

    fun onAction(action: MarketplaceAction) {
        when (action) {
            is MarketplaceAction.SelectCategory -> _state.update { it.copy(selectedCategory = action.category) }
            is MarketplaceAction.UpdateSearchQuery -> _state.update { it.copy(searchQuery = action.query) }
            is MarketplaceAction.SelectItem -> _state.update { it.copy(selectedItem = action.item) }
            is MarketplaceAction.BuyItem -> buyItem(action.item)
            is MarketplaceAction.EquipItem -> equipItem(action.item)
            is MarketplaceAction.UnequipItem -> unequipItem(action.type)
            MarketplaceAction.Refresh -> refresh()
            MarketplaceAction.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val items = storeRepository.getStoreItems().first()
            val inventory = storeRepository.getInventory().first()
            val equipped = storeRepository.getEquippedItems().first()
            
            _state.update {
                it.copy(
                    items = items,
                    inventory = inventory,
                    equipped = equipped,
                    isLoading = false
                )
            }
        }
    }

    private fun buyItem(item: StoreItem) {
        if (_state.value.isBuying) return
        
        _state.update { it.copy(isBuying = true) }
        viewModelScope.launch {
            val result = storeRepository.buyItem(item.id)
            if (result is Result.Success) {
                val inventory = storeRepository.getInventory().first()
                _state.update { it.copy(inventory = inventory, isBuying = false) }
            } else {
                _state.update { it.copy(isBuying = false, error = R.string.error_buying) }
            }
        }
    }

    private fun equipItem(item: StoreItem) {
        if (_state.value.isEquipping) return
        
        _state.update { it.copy(isEquipping = true) }
        viewModelScope.launch {
            val result = storeRepository.equipItem(item.id)
            if (result is Result.Success) {
                val equipped = storeRepository.getEquippedItems().first()
                _state.update { it.copy(equipped = equipped, isEquipping = false) }
            } else {
                _state.update { it.copy(isEquipping = false, error = R.string.error_equipping) }
            }
        }
    }

    private fun unequipItem(type: StoreItemType) {
        viewModelScope.launch {
            val result = storeRepository.unequipItem(type)
            if (result is Result.Success) {
                val equipped = storeRepository.getEquippedItems().first()
                _state.update { it.copy(equipped = equipped) }
            }
        }
    }
}

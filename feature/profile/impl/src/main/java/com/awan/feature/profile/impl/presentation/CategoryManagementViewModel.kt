package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.usecase.CreateCategoryUseCase
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.category.usecase.UpdateCategoryUseCase
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementState())
    val uiState: StateFlow<CategoryManagementState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun onAction(action: CategoryManagementAction) {
        when (action) {
            CategoryManagementAction.LoadCategories -> loadCategories()
            is CategoryManagementAction.CreateCategory -> createCategory(action.name)
            is CategoryManagementAction.UpdateCategory -> updateCategory(action.id, action.name)
            CategoryManagementAction.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getCategoriesUseCase()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, categories = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                Result.Loading -> Unit
            }
        }
    }

    private fun createCategory(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = createCategoryUseCase(name)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadCategories()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                Result.Loading -> Unit
            }
        }
    }

    private fun updateCategory(id: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = updateCategoryUseCase(id, name)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadCategories()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                Result.Loading -> Unit
            }
        }
    }
}

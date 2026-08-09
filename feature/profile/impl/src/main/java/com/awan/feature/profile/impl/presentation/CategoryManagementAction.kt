package com.awan.feature.profile.impl.presentation

sealed interface CategoryManagementAction {
    data object LoadCategories : CategoryManagementAction
    data class CreateCategory(val name: String) : CategoryManagementAction
    data class UpdateCategory(val id: String, val name: String) : CategoryManagementAction
    data class DeleteCategory(val id: String) : CategoryManagementAction
    data object ClearError : CategoryManagementAction
}

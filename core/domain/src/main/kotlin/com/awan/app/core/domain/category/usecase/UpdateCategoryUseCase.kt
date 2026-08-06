package com.awan.app.core.domain.category.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.model.Category
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: String, name: String): Result<Category> =
        categoryRepository.updateCategory(categoryId, name.trim())
}

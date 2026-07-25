package com.awan.app.core.domain.category.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.model.Category
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(): Result<List<Category>> = categoryRepository.getCategories()
}

package com.awan.app.core.domain.category.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: String): Result<Unit> {
        return repository.deleteCategory(categoryId)
    }
}

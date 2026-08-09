package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.model.Category

class FakeCategoryRepository(
    private var categories: List<Category> = DEFAULT_SEEDED,
    var failWith: AppError? = null,
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        failWith?.let { Result.Error(it) } ?: Result.Success(categories)

    override suspend fun createCategory(name: String): Result<Category> {
        val new = Category("cat-${name.lowercase()}", name)
        categories = categories + new
        return Result.Success(new)
    }

    override suspend fun getCategory(categoryId: String): Result<Category> = 
        Result.Success(categories.first { it.id == categoryId })

    override suspend fun updateCategory(categoryId: String, name: String): Result<Category> {
        val updated = Category(categoryId, name)
        categories = categories.map { if (it.id == categoryId) updated else it }
        return Result.Success(updated)
    }

    companion object {
        val DEFAULT_SEEDED = listOf(
            Category("cat-work", "Work"),
            Category("cat-personal", "Personal")
        )
    }
}

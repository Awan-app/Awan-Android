package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.model.Category

class FakeCategoryRepository(
    private val categories: List<Category> = DEFAULT_SEEDED,
    private val failWith: AppError? = null,
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        failWith?.let { Result.Error(it) } ?: Result.Success(categories)

    override suspend fun createCategory(name: String): Result<Category> = error("not used")
    override suspend fun getCategory(categoryId: String): Result<Category> = error("not used")
    override suspend fun updateCategory(categoryId: String, name: String): Result<Category> = error("not used")

    companion object {
        /** What the backend seeds a brand-new account with, in its documented order. */
        val DEFAULT_SEEDED = listOf(
            "General", "Work", "Personal", "Health", "Learning", "Finance", "Home", "Social",
        ).map { Category(id = "cat-${it.lowercase()}", name = it) }
    }
}

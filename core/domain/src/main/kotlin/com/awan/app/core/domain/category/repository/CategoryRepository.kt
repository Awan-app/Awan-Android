package com.awan.app.core.domain.category.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.Category

interface CategoryRepository {

    /** Every category the user owns. Unlike zones these are not scoped to a date. */
    suspend fun getCategories(): Result<List<Category>>

    /** Names are unique per user, case-insensitive — a clash answers `CATEGORY_NAME_TAKEN`. */
    suspend fun createCategory(name: String): Result<Category>

    suspend fun getCategory(categoryId: String): Result<Category>

    suspend fun updateCategory(categoryId: String, name: String): Result<Category>


}

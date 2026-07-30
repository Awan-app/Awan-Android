package com.awan.app.core.domain.category.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.Category

interface CategoryRepository {

    /** Every category the user owns. Unlike zones these are not scoped to a date. */
    suspend fun getCategories(): Result<List<Category>>
}

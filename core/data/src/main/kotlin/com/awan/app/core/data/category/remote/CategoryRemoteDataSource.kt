package com.awan.app.core.data.category.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.category.CategoryDto

interface CategoryRemoteDataSource {
    suspend fun getCategories(): Result<List<CategoryDto>>
}

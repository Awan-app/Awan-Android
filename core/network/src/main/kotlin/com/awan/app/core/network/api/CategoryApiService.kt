package com.awan.app.core.network.api

import com.awan.app.core.network.dto.category.CategoryDto
import retrofit2.http.GET
interface CategoryApiService {

    /** Every category the user owns, independent of any date. */
    @GET("v1/categories")
    suspend fun getCategories(): List<CategoryDto>
}

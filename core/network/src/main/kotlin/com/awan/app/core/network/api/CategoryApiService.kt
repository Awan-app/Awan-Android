package com.awan.app.core.network.api

import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.category.CategoryRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CategoryApiService {

    @POST("v1/categories")
    suspend fun createCategory(
        @Body request: CategoryRequestDto,
    ): CategoryDto

    @GET("v1/categories")
    suspend fun getCategories(): List<CategoryDto>
}

package com.awan.app.core.network.api

import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.category.CategoryRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CategoryApiService {

    /** Every category the user owns, independent of any date. */
    @GET("v1/categories")
    suspend fun getCategories(): List<CategoryDto>

    /** Names are unique per user, case-insensitive — a clash answers 409 `CATEGORY_NAME_TAKEN`. */
    @POST("v1/categories")
    suspend fun createCategory(@Body request: CategoryRequestDto): CategoryDto

    @GET("v1/categories/{categoryId}")
    suspend fun getCategory(@Path("categoryId") categoryId: String): CategoryDto

    @PUT("v1/categories/{categoryId}")
    suspend fun updateCategory(
        @Path("categoryId") categoryId: String,
        @Body request: CategoryRequestDto,
    ): CategoryDto
}

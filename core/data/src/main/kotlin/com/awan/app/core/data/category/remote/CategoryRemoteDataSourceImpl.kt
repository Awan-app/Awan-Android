package com.awan.app.core.data.category.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.CategoryApiService
import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.category.CategoryRequestDto
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CategoryRemoteDataSourceImpl @Inject constructor(
    private val categoryApiService: CategoryApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CategoryRemoteDataSource {

    override suspend fun getCategories(): Result<List<CategoryDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            categoryApiService.getCategories()
        }

    override suspend fun createCategory(name: String): Result<CategoryDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            categoryApiService.createCategory(CategoryRequestDto(name = name))
        }

    override suspend fun getCategory(categoryId: String): Result<CategoryDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            categoryApiService.getCategory(categoryId)
        }

    override suspend fun updateCategory(categoryId: String, name: String): Result<CategoryDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            categoryApiService.updateCategory(categoryId, CategoryRequestDto(name = name))
        }
}

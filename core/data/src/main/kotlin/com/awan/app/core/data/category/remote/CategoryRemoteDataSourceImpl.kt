package com.awan.app.core.data.category.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.CategoryApiService
import com.awan.app.core.network.dto.CategoryDto
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
}

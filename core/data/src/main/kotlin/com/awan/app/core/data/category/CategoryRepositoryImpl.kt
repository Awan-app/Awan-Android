package com.awan.app.core.data.category

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.category.remote.CategoryRemoteDataSource
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.model.Category
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: CategoryRemoteDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> = withContext(ioDispatcher) {
        remoteDataSource.getCategories().map { categories -> categories.map { it.toModel() } }
    }
}

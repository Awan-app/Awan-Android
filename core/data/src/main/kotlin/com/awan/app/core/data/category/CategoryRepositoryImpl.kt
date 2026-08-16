package com.awan.app.core.data.category

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.model.Category
import com.awan.app.core.network.api.CategoryApiService
import com.awan.app.core.network.dto.category.CategoryRequestDto
import com.awan.app.core.network.error.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads categories exclusively from the local Room database (SSOT).
 * Remote data is injected into Room by [OfflineSyncCoordinator]; this
 * repository never performs a remote GET for UI reads.
 *
 * Write operations (create/update) go through the API first, then
 * upsert the result into the local database.
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val categoryApiService: CategoryApiService,
    private val connectivityMonitor: NetworkConnectivityMonitor,
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> {
        val cached = categoryDao.getAllCategories()
        // Room is empty until SyncWorker lands, and onboarding needs the list before that: a zone
        // sent without a categoryId is rejected, so an empty table there costs the user their zones.
        // The network still only refills Room — the read below is what the caller gets.
        if (cached.isEmpty() && connectivityMonitor.isCurrentlyOnline()) {
            return safeApiCall {
                val entities = categoryApiService.getCategories()
                    .map { CategoryEntity(id = it.id, name = it.name) }
                categoryDao.upsertCategories(entities)
                entities.map { it.toModel() }
            }
        }
        return Result.Success(cached.map { it.toModel() })
    }

    override suspend fun createCategory(name: String): Result<Category> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return safeApiCall {
            val dto = categoryApiService.createCategory(CategoryRequestDto(name))
            val entity = CategoryEntity(id = dto.id, name = dto.name)
            categoryDao.upsertCategory(entity)
            entity.toModel()
        }
    }

    override suspend fun getCategory(categoryId: String): Result<Category> {
        val cached = categoryDao.getCategory(categoryId)
        if (cached != null) {
            return Result.Success(cached.toModel())
        }
        return Result.Error(AppError.NotFound)
    }

    override suspend fun updateCategory(categoryId: String, name: String): Result<Category> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return safeApiCall {
            val dto = categoryApiService.updateCategory(categoryId, CategoryRequestDto(name))
            val entity = CategoryEntity(id = dto.id, name = dto.name)
            categoryDao.upsertCategory(entity)
            entity.toModel()
        }
    }


}

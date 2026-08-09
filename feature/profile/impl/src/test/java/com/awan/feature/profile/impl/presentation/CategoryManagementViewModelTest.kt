package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.domain.category.usecase.CreateCategoryUseCase
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.category.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var viewModel: CategoryManagementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = FakeCategoryRepository()
        viewModel = CategoryManagementViewModel(
            getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
            createCategoryUseCase = CreateCategoryUseCase(categoryRepository),
            updateCategoryUseCase = UpdateCategoryUseCase(categoryRepository)
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state loads categories`() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals(2, state.categories.size)
    }

    @Test
    fun `loading categories with error updates error state`() = runTest(testDispatcher) {
        categoryRepository.failWith = AppError.Network
        viewModel.onAction(CategoryManagementAction.LoadCategories)
        
        val state = viewModel.uiState.value
        assertNotNull(state.error)
    }

    @Test
    fun `creating a category reloads the list`() = runTest(testDispatcher) {
        viewModel.onAction(CategoryManagementAction.CreateCategory("New Cat"))
        
        val state = viewModel.uiState.value
        assertEquals(3, state.categories.size)
        assertTrue(state.categories.any { it.name == "New Cat" })
    }

    @Test
    fun `creating a category with error updates error state`() = runTest(testDispatcher) {
        categoryRepository.failWith = AppError.Api(400, "Bad request", errorCode = "ERROR")
        viewModel.onAction(CategoryManagementAction.CreateCategory("New Cat"))
        
        val state = viewModel.uiState.value
        assertNotNull(state.error)
    }

    @Test
    fun `updating a category reloads the list`() = runTest(testDispatcher) {
        val firstCatId = FakeCategoryRepository.DEFAULT_SEEDED.first().id
        viewModel.onAction(CategoryManagementAction.UpdateCategory(firstCatId, "Updated Name"))
        
        val state = viewModel.uiState.value
        assertTrue(state.categories.any { it.name == "Updated Name" })
    }

    @Test
    fun `updating a category with error updates error state`() = runTest(testDispatcher) {
        categoryRepository.failWith = AppError.Api(400, "Bad request", errorCode = "ERROR")
        val firstCatId = FakeCategoryRepository.DEFAULT_SEEDED.first().id
        viewModel.onAction(CategoryManagementAction.UpdateCategory(firstCatId, "Updated Name"))
        
        val state = viewModel.uiState.value
        assertNotNull(state.error)
    }
}

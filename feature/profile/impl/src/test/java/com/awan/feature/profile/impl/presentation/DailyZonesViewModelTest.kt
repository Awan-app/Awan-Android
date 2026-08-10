package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.category.usecase.CreateCategoryUseCase
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplatesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateTemplateZonesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyZonesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var zonesRepository: FakeZonesRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var viewModel: DailyZonesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        zonesRepository = FakeZonesRepository()
        categoryRepository = FakeCategoryRepository()
        viewModel = DailyZonesViewModel(
            getWeeklyTemplatesUseCase = GetWeeklyTemplatesUseCase(zonesRepository),
            getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
            createCategoryUseCase = CreateCategoryUseCase(categoryRepository),
            updateTemplateZonesUseCase = UpdateTemplateZonesUseCase(zonesRepository)
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state has correct selected day`() = runTest(testDispatcher) {
        val expectedDay = com.awan.feature.profile.impl.helpers.DailyZonesHelper.getCurrentDay(LocalDate.now())
        assertEquals(expectedDay, viewModel.uiState.value.selectedDay)
    }

    @Test
    fun `loadData updates state with templates and categories`() = runTest(testDispatcher) {
        val template = WeeklyTemplate("t1", "Work", listOf(DayOfWeek.MONDAY), emptyList())
        zonesRepository.templates = listOf(template)
        
        viewModel.onAction(DailyZonesAction.LoadData)
        
        val state = viewModel.uiState.value
        assertEquals(1, state.templates.size)
        assertEquals("Work", state.templates.first().name)
        assertTrue(state.availableCategories.isNotEmpty())
    }

    @Test
    fun `selecting a day updates selectedDayZones`() = runTest(testDispatcher) {
        val zone = DailyZone("z1", "Focus", "09:00:00", "10:00:00", "#FFFFFF")
        val template = WeeklyTemplate("t1", "Work", listOf(DayOfWeek.TUESDAY), listOf(zone))
        zonesRepository.templates = listOf(template)
        
        viewModel.onAction(DailyZonesAction.LoadData)
        viewModel.onAction(DailyZonesAction.SelectDay(DayOfWeek.TUESDAY))
        
        val state = viewModel.uiState.value
        assertEquals(DayOfWeek.TUESDAY, state.selectedDay)
        assertEquals(1, state.selectedDayZones.size)
        assertEquals("Focus", state.selectedDayZones.first().name)
    }
}

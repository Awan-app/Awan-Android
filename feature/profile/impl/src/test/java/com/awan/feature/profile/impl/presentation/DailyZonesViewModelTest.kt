package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.usecase.GetTemplateOverridesUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplatesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateOverrideZonesUseCase
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DailyZonesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneId.of("UTC")) // Monday

    private lateinit var zonesRepository: FakeZonesRepository
    private lateinit var viewModel: DailyZonesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        zonesRepository = FakeZonesRepository()
        viewModel = DailyZonesViewModel(
            getWeeklyTemplatesUseCase = GetWeeklyTemplatesUseCase(zonesRepository),
            getTemplateOverridesUseCase = GetTemplateOverridesUseCase(zonesRepository),
            getCategoriesUseCase = GetCategoriesUseCase(FakeCategoryRepository()),
            updateTemplateZonesUseCase = UpdateTemplateZonesUseCase(zonesRepository),
            updateOverrideZonesUseCase = UpdateOverrideZonesUseCase(zonesRepository),
            clock = clock
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state has correct selected day based on clock`() = runTest(testDispatcher) {
        assertEquals(DayOfWeek.MONDAY, viewModel.uiState.value.selectedDay)
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

    @Test
    fun `overrides for a specific date take precedence over weekly template`() = runTest(testDispatcher) {
        // Clock is Monday 2026-08-10. Next Tuesday is 2026-08-11.
        val templateZone = DailyZone("z-t", "Template", "09:00:00", "10:00:00", "#FFFFFF")
        val overrideZone = DailyZone("z-o", "Override", "11:00:00", "12:00:00", "#FFFFFF")
        
        val template = WeeklyTemplate("t1", "Work", listOf(DayOfWeek.TUESDAY), listOf(templateZone))
        val override = com.awan.app.core.domain.zones.model.TemplateOverride("o1", "Override", "2026-08-11", listOf(overrideZone))
        
        zonesRepository.templates = listOf(template)
        zonesRepository.overrides = listOf(override)
        
        viewModel.onAction(DailyZonesAction.LoadData)
        viewModel.onAction(DailyZonesAction.SelectDay(DayOfWeek.TUESDAY))
        
        val state = viewModel.uiState.value
        assertEquals(1, state.selectedDayZones.size)
        assertEquals("Override", state.selectedDayZones.first().name)
    }
}

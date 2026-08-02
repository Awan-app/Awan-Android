package com.awan.feature.addtask.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.task.parser.TaskInputParser
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.task.usecase.ApplyTaskAttributeUseCase
import com.awan.app.core.domain.task.usecase.CreateTaskUseCase
import com.awan.app.core.domain.task.usecase.ParseTaskInputUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.zones.usecase.GetZonesForDateUseCase
import com.awan.app.core.model.Category
import com.awan.app.core.model.DayZone
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import com.awan.feature.addtask.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AddTaskViewModelTest {

    /** Wednesday 2026-07-22, 10:00 UTC. */
    private val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneId.of("UTC"))
    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeTaskRepository : TaskRepository {
        var lastDraft: TaskDraft? = null
        var lastSessions: List<SessionDraft> = emptyList()
        var failWith: AppError? = null
        val calls = mutableListOf<String>()

        override suspend fun createTask(draft: TaskDraft): Result<Task> {
            calls += "create"
            lastDraft = draft
            failWith?.let { return Result.Error(it) }
            return Result.Success(Task(id = "t-1", title = draft.title))
        }

        override suspend fun createTaskWithSessions(
            draft: TaskDraft,
            sessions: List<SessionDraft>,
        ): Result<TaskWithSessions> {
            calls += "create"
            lastDraft = draft
            lastSessions = sessions
            failWith?.let { return Result.Error(it) }
            return Result.Success(
                TaskWithSessions(
                    task = Task(id = "t-2", title = draft.title),
                    sessions = sessions.mapIndexed { index, session ->
                        com.awan.app.core.model.TaskSession(id = "s-$index", start = session.start, end = session.end)
                    },
                )
            )
        }

        override suspend fun createTasksWithSessions(drafts: List<TaskWithSessionsDraft>): Result<List<Task>> =
            error("not used")

        override suspend fun proposeTasksFromText(text: String): Result<TaskProposals> = error("not used")

        override suspend fun proposeTasksFromImage(
            image: ByteArray,
            mimeType: String,
            note: String?,
        ): Result<TaskProposals> = error("not used")

        override suspend fun scheduleTask(taskId: String) = error("not used")

        override suspend fun deleteTask(taskId: String): Result<Unit> = error("not used")
    }

    private class FakeZoneRepository(private val zones: List<DayZone>) : ZonesRepository {
        var requestedDate: LocalDate? = null

        override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> {
            requestedDate = date
            return Result.Success(zones)
        }

        override suspend fun getTemplates(): Result<List<WeeklyTemplate>> = error("not used")
        override suspend fun createTemplate(name: String, daysOfWeek: List<DayOfWeek>, zones: List<DailyZone>): Result<WeeklyTemplate> = error("not used")
        override suspend fun getTemplate(templateId: String): Result<WeeklyTemplate> = error("not used")
        override suspend fun updateTemplate(templateId: String, name: String, daysOfWeek: List<DayOfWeek>): Result<WeeklyTemplate> = error("not used")
        override suspend fun deleteTemplate(templateId: String): Result<Unit> = error("not used")
        override suspend fun addZoneToTemplate(templateId: String, zone: DailyZone): Result<DailyZone> = error("not used")
        override suspend fun getTemplateZones(templateId: String): Result<List<DailyZone>> = error("not used")
        override suspend fun updateTemplateZones(templateId: String, zones: List<DailyZone>): Result<List<DailyZone>> = error("not used")
        override suspend fun createOverride(date: String, zones: List<DailyZone>): Result<TemplateOverride> = error("not used")
        override suspend fun getOverrides(): Result<List<TemplateOverride>> = error("not used")
        override suspend fun getOverride(overrideId: String): Result<TemplateOverride> = error("not used")
        override suspend fun updateOverride(overrideId: String, name: String?, date: String): Result<TemplateOverride> = error("not used")
        override suspend fun deleteOverride(overrideId: String): Result<Unit> = error("not used")
        override suspend fun addZoneToOverride(overrideId: String, zone: DailyZone): Result<DailyZone> = error("not used")
        override suspend fun getOverrideZones(overrideId: String): Result<List<DailyZone>> = error("not used")
        override suspend fun updateOverrideZones(overrideId: String, zones: List<DailyZone>): Result<List<DailyZone>> = error("not used")
        override suspend fun getZone(zoneId: String): Result<DailyZone> = error("not used")
        override suspend fun getZoneSessions(zoneId: String): Result<List<Session>> = error("not used")
        override suspend fun getEffectiveZones(date: String): Result<List<DailyZone>> = error("not used")
        override suspend fun updateZone(zoneId: String, zone: DailyZone): Result<DailyZone> = error("not used")
        override suspend fun deleteZone(zoneId: String): Result<Unit> = error("not used")
    }

    private class FakeCategoryRepository(private val categories: List<Category>) : CategoryRepository {
        override suspend fun getCategories(): Result<List<Category>> = Result.Success(categories)
    }

    private val playCategory = Category(id = "cat-play", name = "Play")

    private val playZone = DayZone(
        id = "zone-play",
        name = "Play",
        startTime = LocalTime.of(17, 0),
        endTime = LocalTime.of(22, 0),
        category = playCategory,
    )

    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var zoneRepository: FakeZoneRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private fun viewModel(): AddTaskViewModel = AddTaskViewModel(
        parseTaskInput = ParseTaskInputUseCase(clock),
        applyTaskAttribute = ApplyTaskAttributeUseCase(clock),
        getCategories = GetCategoriesUseCase(categoryRepository),
        createTask = CreateTaskUseCase(taskRepository, GetZonesForDateUseCase(zoneRepository)),
        clock = clock,
    )

    /** The sentences asserted here are English, and the parser follows the ambient locale. */
    private val hostLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.ENGLISH)
        Dispatchers.setMain(testDispatcher)
        taskRepository = FakeTaskRepository()
        zoneRepository = FakeZoneRepository(listOf(playZone))
        categoryRepository = FakeCategoryRepository(listOf(playCategory))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(hostLocale)
    }

    @Test
    fun `typing parses the sentence into chips and a clean title`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm for 45m"))

        val state = viewModel.state.value
        assertEquals("Gym session", state.parsed.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), state.parsed.startAt)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `a blank title cannot be submitted`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("tomorrow 6pm"))

        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `goal mode cannot be submitted`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session"))
        viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))

        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `a category token resolves against the user's categories`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm @play"))

        assertEquals(playCategory, viewModel.state.value.resolvedCategory)
    }

    @Test
    fun `an unresolvable category token does not block submitting`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm for 45m @nowhere"))

        assertNull(viewModel.state.value.resolvedCategory)
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `placing a task without Awan needs a time and a length as well as a title`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
            assertFalse(viewModel.state.value.canSubmit)

            viewModel.onAction(AddTaskAction.InputChanged("Buy groceries for 45m"))
            assertFalse(viewModel.state.value.canSubmit)

            // A bare day defaults its hour rather than naming one, so it is still not a time.
            viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow for 45m"))
            assertFalse(viewModel.state.value.canSubmit)

            viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm"))
            assertFalse(viewModel.state.value.canSubmit)

            viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm for 45m"))
            assertTrue(viewModel.state.value.canSubmit)
        }

    @Test
    fun `a submit that the form would not allow creates nothing`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.Submit)

        assertNull(taskRepository.lastDraft)
        assertTrue(taskRepository.calls.isEmpty())
    }

    @Test
    fun `a scheduled task gets one session ending after the parsed duration`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm for 45m @play"))
        viewModel.onAction(AddTaskAction.Submit)

        val session = taskRepository.lastSessions.single()
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), session.start)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 45), session.end)
    }

    @Test
    fun `the session's zone is the one handing that category a window on the day`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm for 45m @play"))
        viewModel.onAction(AddTaskAction.Submit)

        assertEquals(LocalDate.of(2026, 7, 23), zoneRepository.requestedDate)
        assertEquals("cat-play", taskRepository.lastDraft?.categoryId)
        assertEquals("zone-play", taskRepository.lastSessions.single().zoneId)
    }

    @Test
    fun `a category with no window that day still schedules, without a zone`() = runTest(testDispatcher) {
        zoneRepository = FakeZoneRepository(emptyList())
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm for 45m @play"))
        viewModel.onAction(AddTaskAction.Submit)

        assertEquals("cat-play", taskRepository.lastDraft?.categoryId)
        assertNull(taskRepository.lastSessions.single().zoneId)
    }

    @Test
    fun `tasks are mandatory by default and the chip toggles it off`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm for 45m"))
        assertTrue(viewModel.state.value.mandatory)

        viewModel.onAction(AddTaskAction.MandatoryToggled)
        viewModel.onAction(AddTaskAction.Submit)

        assertFalse(taskRepository.lastDraft?.mandatory ?: true)
    }

    @Test
    fun `a successful create shows a receipt instead of closing`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm for 45m"))
        viewModel.onAction(AddTaskAction.Submit)

        val state = viewModel.state.value
        val confirmation = requireNotNull(state.confirmation)
        assertEquals("Buy groceries", confirmation.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), confirmation.firstSession)
        assertEquals(45, confirmation.durationMinutes)
        assertFalse(state.isSubmitting)
        assertFalse(state.showsAiSwitch)
        assertFalse(state.showsModeSelector)
    }

    @Test
    fun `closing the receipt emits TaskCreated without asking to discard`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm for 45m"))
        viewModel.onAction(AddTaskAction.Submit)
        viewModel.onAction(AddTaskAction.DismissRequested)

        assertFalse(viewModel.state.value.showDiscardConfirm)
        assertEquals(AddTaskEvent.TaskCreated("Buy groceries"), viewModel.events.first())
    }

    @Test
    fun `closing leaves nothing behind for the next time the sheet opens`() = runTest(testDispatcher) {
        // The ViewModel outlives the sheet's composition, so a stale receipt would reopen with it.
        val viewModel = viewModel()
        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm for 45m"))
        viewModel.onAction(AddTaskAction.Submit)
        viewModel.onAction(AddTaskAction.DismissRequested)

        val state = viewModel.state.value
        assertNull(state.confirmation)
        assertEquals("", state.input)
        assertEquals("", state.description)
        assertFalse(state.aiEnabled)
        assertFalse(state.isCelebrating)
        // Resetting wipes the loaded list, so the category menu has to be refilled on the way out.
        assertEquals(listOf(playCategory), state.availableCategories)
    }

    @Test
    fun `discarding also leaves a clean sheet behind`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))

        viewModel.onAction(AddTaskAction.DismissRequested)
        viewModel.onAction(AddTaskAction.DiscardConfirmed)

        val state = viewModel.state.value
        assertEquals("", state.input)
        assertFalse(state.aiEnabled)
        assertFalse(state.showDiscardConfirm)
        assertEquals(listOf(playCategory), state.availableCategories)
    }

    // ── Chips write back into the sentence ───────────────────────────────────

    @Test
    fun `picking a time writes it into the task name`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming"))
        viewModel.onAction(AddTaskAction.PickerOpened(AddTaskPicker.TIME))
        viewModel.onAction(AddTaskAction.TimePicked(15 * 60))

        val state = viewModel.state.value
        assertEquals("Go Swimming today at 3pm", state.input)
        assertEquals("Go Swimming", state.parsed.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 15, 0), state.parsed.startAt)
        assertNull(state.openPicker)
    }

    @Test
    fun `picking a time keeps the day the sentence already named`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming tomorrow"))
        viewModel.onAction(AddTaskAction.TimePicked(17 * 60 + 30))

        val state = viewModel.state.value
        assertEquals("Go Swimming tomorrow at 5:30pm", state.input)
        assertEquals(LocalDateTime.of(2026, 7, 23, 17, 30), state.parsed.startAt)
    }

    @Test
    fun `the when chip asks for a day first and the clock second`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming"))
        viewModel.onAction(AddTaskAction.PickerOpened(AddTaskPicker.DATE))
        viewModel.onAction(AddTaskAction.DatePicked(LocalDate.of(2026, 7, 25)))

        // The day is parked, not written — the sentence only changes once the clock is answered.
        assertEquals(AddTaskPicker.TIME, viewModel.state.value.openPicker)
        assertEquals("Go Swimming", viewModel.state.value.input)

        viewModel.onAction(AddTaskAction.TimePicked(15 * 60))

        val state = viewModel.state.value
        assertEquals("Go Swimming saturday at 3pm", state.input)
        assertEquals(LocalDateTime.of(2026, 7, 25, 15, 0), state.parsed.startAt)
        assertNull(state.openPicker)
        assertNull(state.pendingDate)
    }

    @Test
    fun `backing out of the clock keeps the day already chosen`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming"))
        viewModel.onAction(AddTaskAction.PickerOpened(AddTaskPicker.DATE))
        viewModel.onAction(AddTaskAction.DatePicked(LocalDate.of(2026, 7, 25)))
        viewModel.onAction(AddTaskAction.PickerDismissed)

        val state = viewModel.state.value
        assertEquals("Go Swimming saturday", state.input)
        assertEquals(LocalDate.of(2026, 7, 25), state.parsed.startAt?.toLocalDate())
        assertFalse(state.parsed.hasExplicitTime)
        assertNull(state.pendingDate)
    }

    @Test
    fun `moving the day alone keeps a time already in the sentence`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym tomorrow at 6pm"))
        viewModel.onAction(AddTaskAction.DatePicked(LocalDate.of(2026, 7, 25)))
        viewModel.onAction(AddTaskAction.PickerDismissed)

        val state = viewModel.state.value
        assertEquals("Gym saturday at 6pm", state.input)
        assertEquals(LocalDateTime.of(2026, 7, 25, 18, 0), state.parsed.startAt)
    }

    @Test
    fun `backing out of the day picker changes nothing`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming"))
        viewModel.onAction(AddTaskAction.PickerOpened(AddTaskPicker.DATE))
        viewModel.onAction(AddTaskAction.PickerDismissed)

        assertEquals("Go Swimming", viewModel.state.value.input)
        assertNull(viewModel.state.value.openPicker)
    }

    @Test
    fun `picking a duration writes it into the task name`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming"))
        viewModel.onAction(AddTaskAction.DurationPicked(180))

        val state = viewModel.state.value
        assertEquals("Go Swimming for 3 hours", state.input)
        assertEquals("Go Swimming", state.parsed.title)
        assertEquals(180, state.parsed.durationMinutes)
    }

    @Test
    fun `picking a category writes it into the task name and resolves it`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm"))
        viewModel.onAction(AddTaskAction.CategoryPicked("Play"))

        val state = viewModel.state.value
        assertEquals("Gym session tomorrow 6pm @Play", state.input)
        assertEquals("Gym session", state.parsed.title)
        assertEquals(playCategory, state.resolvedCategory)
    }

    @Test
    fun `the categories are offered even before a token is typed`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        assertEquals(listOf(playCategory), viewModel.state.value.availableCategories)
    }

    @Test
    fun `re-picking replaces rather than appends`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming"))
        viewModel.onAction(AddTaskAction.TimePicked(15 * 60))
        viewModel.onAction(AddTaskAction.TimePicked(9 * 60))
        viewModel.onAction(AddTaskAction.DurationPicked(60))
        viewModel.onAction(AddTaskAction.DurationPicked(30))

        val state = viewModel.state.value
        assertEquals("Go Swimming today at 9am for 30 min", state.input)
        assertEquals("Go Swimming", state.parsed.title)
        assertEquals(30, state.parsed.durationMinutes)
    }

    @Test
    fun `picking a time over a typed range replaces the whole range`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming from 3pm to 5pm"))
        assertEquals(120, viewModel.state.value.parsed.durationMinutes)

        viewModel.onAction(AddTaskAction.TimePicked(8 * 60))

        assertEquals("Go Swimming today at 8am", viewModel.state.value.input)
    }

    @Test
    fun `with nothing stated the task is simply for today with no time`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Go Swimming"))

        val state = viewModel.state.value
        assertNull(state.parsed.startAt)
        assertFalse(state.parsed.hasExplicitTime)
        assertEquals(TaskInputParser.DEFAULT_HOUR * 60, state.pickerInitialMinutes)
    }

    @Test
    fun `the mascot reacts to what has been typed`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        assertEquals(MascotExpression.Idle, viewModel.state.value.mascot)

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        assertEquals(MascotExpression.Greet, viewModel.state.value.mascot)

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm"))
        assertEquals(MascotExpression.Curious, viewModel.state.value.mascot)
    }

    @Test
    fun `the receipt arrives cheering and settles down`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm for 45m"))
        viewModel.onAction(AddTaskAction.Submit)

        val celebrating = viewModel.state.value
        assertTrue(celebrating.isCelebrating)
        assertEquals(MascotExpression.Celebrate, celebrating.mascot)

        // The sparkles are a beat, not a gate — the receipt stays up after they finish, and so does
        // the cheer: Awan going back to a greeting under its own receipt reads as losing interest.
        advanceUntilIdle()
        val settled = viewModel.state.value
        assertFalse(settled.isCelebrating)
        requireNotNull(settled.confirmation)
        assertEquals(MascotExpression.Celebrate, settled.mascot)
    }

    @Test
    fun `a failed create surfaces an error and keeps what the user typed`() = runTest(testDispatcher) {
        taskRepository.failWith = AppError.Network
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries tomorrow 6pm for 45m"))
        viewModel.onAction(AddTaskAction.Submit)

        val state = viewModel.state.value
        assertEquals("Buy groceries tomorrow 6pm for 45m", state.input)
        assertFalse(state.isSubmitting)
        assertEquals(R.string.add_task_error_create_failed, state.errorMessage)
    }

    // ── Handing off to Awan's full-screen review ─────────────────────────────

    @Test
    fun `switching Awan on keeps the text and drops every highlight`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm @play"))
        assertTrue(viewModel.state.value.parsed.tokens.isNotEmpty())

        viewModel.onAction(AddTaskAction.AiToggled)

        val state = viewModel.state.value
        assertTrue(state.aiEnabled)
        assertEquals("Gym session tomorrow 6pm @play", state.input)
        assertTrue(state.parsed.tokens.isEmpty())
        assertNull(state.parsed.startAt)
        assertNull(state.resolvedCategory)
        assertFalse(state.showsAttributeChips)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `switching Awan back off re-reads the same sentence`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm @play"))
        viewModel.onAction(AddTaskAction.AiToggled)
        viewModel.onAction(AddTaskAction.AiToggled)

        val state = viewModel.state.value
        assertFalse(state.aiEnabled)
        assertEquals("Gym session tomorrow 6pm @play", state.input)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), state.parsed.startAt)
        assertEquals(playCategory, state.resolvedCategory)
    }

    @Test
    fun `text typed while Awan is on is never parsed`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.AiToggled)
        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm @play"))

        val state = viewModel.state.value
        assertEquals("Gym session tomorrow 6pm @play", state.input)
        assertTrue(state.parsed.tokens.isEmpty())
    }

    @Test
    fun `submitting with Awan on hands off to the full screen and touches no repository`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onAction(AddTaskAction.AiToggled)
            viewModel.onAction(AddTaskAction.InputChanged("Build a login page"))
            viewModel.onAction(AddTaskAction.DescriptionChanged("with email and password"))
            viewModel.onAction(AddTaskAction.Submit)

            assertTrue(taskRepository.calls.isEmpty())
            assertEquals(
                AddTaskEvent.AiRequested(
                    text = "Build a login page",
                    note = "with email and password",
                    imageUri = null,
                ),
                viewModel.events.first(),
            )
        }

    @Test
    fun `a blank description is not sent as a note`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.AiToggled)
        viewModel.onAction(AddTaskAction.InputChanged("Build a login page"))
        viewModel.onAction(AddTaskAction.Submit)

        val event = viewModel.events.first() as AddTaskEvent.AiRequested
        assertNull(event.note)
    }

    @Test
    fun `a photo alone is enough to ask Awan`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.AiToggled)
        viewModel.onAction(AddTaskAction.ImagePicked("content://images/1"))

        assertTrue(viewModel.state.value.canSubmit)

        viewModel.onAction(AddTaskAction.Submit)

        val event = viewModel.events.first() as AddTaskEvent.AiRequested
        assertEquals("content://images/1", event.imageUri)
    }

    @Test
    fun `clearing the photo removes it from state`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.AiToggled)
        viewModel.onAction(AddTaskAction.ImagePicked("content://images/1"))
        viewModel.onAction(AddTaskAction.ImageCleared)

        assertNull(viewModel.state.value.imageUri)
    }

    @Test
    fun `submitting with Awan on resets the sheet just like a normal close`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.AiToggled)
        viewModel.onAction(AddTaskAction.InputChanged("Build a login page"))
        viewModel.onAction(AddTaskAction.Submit)

        val state = viewModel.state.value
        assertEquals("", state.input)
        assertFalse(state.aiEnabled)
        assertNull(state.imageUri)
    }

    // ── Nothing is lost by accident ──────────────────────────────────────────

    @Test
    fun `dismissing a clean sheet just closes it`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.DismissRequested)

        assertFalse(viewModel.state.value.showDiscardConfirm)
        assertEquals(AddTaskEvent.Dismissed, viewModel.events.first())
    }

    @Test
    fun `dismissing a dirty sheet asks first`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.DismissRequested)

        assertTrue(viewModel.state.value.showDiscardConfirm)
    }

    @Test
    fun `a note alone counts as something worth losing`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.DescriptionChanged("some context"))
        viewModel.onAction(AddTaskAction.DismissRequested)

        assertTrue(viewModel.state.value.showDiscardConfirm)
    }

    @Test
    fun `an attached photo counts as something worth losing`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.AiToggled)
        viewModel.onAction(AddTaskAction.ImagePicked("content://images/1"))
        viewModel.onAction(AddTaskAction.DismissRequested)

        assertTrue(viewModel.state.value.showDiscardConfirm)
    }

    @Test
    fun `the guard covers goal mode too`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Learn guitar"))
        viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
        viewModel.onAction(AddTaskAction.DismissRequested)

        assertTrue(viewModel.state.value.showDiscardConfirm)
    }

    @Test
    fun `keeping editing puts the sheet back as it was`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.DismissRequested)
        viewModel.onAction(AddTaskAction.DiscardCancelled)

        assertFalse(viewModel.state.value.showDiscardConfirm)
        assertEquals("Buy groceries", viewModel.state.value.input)
    }

    @Test
    fun `discarding before Awan has answered deletes nothing`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.DismissRequested)
        viewModel.onAction(AddTaskAction.DiscardConfirmed)

        assertTrue(taskRepository.calls.none { it == "delete" })
        assertNotNull(viewModel.events.first())
    }
}

package com.awan.feature.addtask.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.goal.usecase.ConfirmGoalDecompositionUseCase
import com.awan.app.core.domain.goal.usecase.ContinueGoalDecompositionUseCase
import com.awan.app.core.domain.task.parser.TaskInputParser
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.task.usecase.ApplyTaskAttributeUseCase
import com.awan.app.core.domain.task.usecase.CreateTaskUseCase
import com.awan.app.core.domain.task.usecase.ParseTaskInputUseCase
import com.awan.app.core.domain.profile.model.UserData
import com.awan.app.core.domain.profile.repository.UserDataRepository
import com.awan.app.core.domain.profile.usecase.GetUserDataUseCase
import com.awan.app.core.domain.profile.usecase.SetMicPermissionRequestedUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.zones.usecase.GetZonesForDateUseCase
import com.awan.app.core.model.Category
import com.awan.app.core.model.DayZone
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import com.awan.feature.addtask.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

    private class FakeGoalRepository : GoalRepository {
        var nextContinueReply: Result<GoalDecompositionReply>? = null
        var nextConfirmResult: Result<Goal>? = null

        val continueCalls = mutableListOf<Pair<String?, String>>()
        val confirmCalls = mutableListOf<String>()

        override suspend fun getGoals(): Result<List<Goal>> = Result.Success(emptyList())

        override suspend fun continueDecomposition(
            sessionId: String?,
            message: String,
        ): Result<GoalDecompositionReply> {
            continueCalls += Pair(sessionId, message)
            return nextContinueReply ?: Result.Error(AppError.Network)
        }

        override suspend fun confirmDecomposition(sessionId: String): Result<Goal> {
            confirmCalls += sessionId
            return nextConfirmResult ?: Result.Error(AppError.Network)
        }
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
        override suspend fun createCategory(name: String): Result<Category> = error("not used")
        override suspend fun getCategory(categoryId: String): Result<Category> = error("not used")
        override suspend fun updateCategory(categoryId: String, name: String): Result<Category> = error("not used")
    }

    private class FakeUserDataRepository : UserDataRepository {
        val _userData = MutableStateFlow(UserData(darkThemeEnabled = false, locale = "en", micPermissionRequested = false))
        override val userData: Flow<UserData> = _userData

        override suspend fun setDarkThemeEnabled(enabled: Boolean) {
            _userData.update { it.copy(darkThemeEnabled = enabled) }
        }

        override suspend fun setLocale(locale: String) {
            _userData.update { it.copy(locale = locale) }
        }

        override suspend fun setMicPermissionRequested(requested: Boolean) {
            _userData.update { it.copy(micPermissionRequested = requested) }
        }
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
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var zoneRepository: FakeZoneRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var userDataRepository: FakeUserDataRepository

    private fun viewModel(): AddTaskViewModel = AddTaskViewModel(
        parseTaskInput = ParseTaskInputUseCase(clock),
        applyTaskAttribute = ApplyTaskAttributeUseCase(clock),
        getCategories = GetCategoriesUseCase(categoryRepository),
        createTask = CreateTaskUseCase(taskRepository, GetZonesForDateUseCase(zoneRepository)),
        continueGoalDecomposition = ContinueGoalDecompositionUseCase(goalRepository),
        confirmGoalDecomposition = ConfirmGoalDecompositionUseCase(goalRepository),
        getUserDataUseCase = GetUserDataUseCase(userDataRepository),
        setMicPermissionRequestedUseCase = SetMicPermissionRequestedUseCase(userDataRepository),
        clock = clock,
    )

    /** The sentences asserted here are English, and the parser follows the ambient locale. */
    private val hostLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.ENGLISH)
        Dispatchers.setMain(testDispatcher)
        taskRepository = FakeTaskRepository()
        goalRepository = FakeGoalRepository()
        zoneRepository = FakeZoneRepository(listOf(playZone))
        categoryRepository = FakeCategoryRepository(listOf(playCategory))
        userDataRepository = FakeUserDataRepository()
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
    fun `initial goal mode with blank input cannot be submitted`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))

        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `initial goal mode with non-blank input can be submitted`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
        viewModel.onAction(AddTaskAction.InputChanged("Gym session"))

        assertTrue(viewModel.state.value.canSubmit)
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

    // ── AI Goal Creation Flow ────────────────────────────────────────────────

    @Test
    fun `Initial to MCQ transition stores session id options question reply blocks and clears input`() =
        runTest(testDispatcher) {
            val reply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Text("Let's break this down."),
                    GoalDecompositionBlock.Question(
                        text = "What is your deadline?",
                        options = listOf("1 month", "3 months"),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(reply)
            val viewModel = viewModel()

            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged(" Learn Spanish "))
            viewModel.onAction(AddTaskAction.Submit)

            // exact (null, trimmedMessage) call
            assertEquals(listOf(Pair(null, "Learn Spanish")), goalRepository.continueCalls)

            val state = viewModel.state.value
            assertEquals("session-123", state.goalSessionId)
            assertEquals("", state.input)
            assertEquals(reply.blocks, state.goalReplyBlocks)

            val mcq = state.goalStep as GoalStep.MultipleChoice
            assertEquals("What is your deadline?", mcq.question)
            assertEquals(listOf("1 month", "3 months"), mcq.options)
            assertNull(mcq.selectedOption)
            assertFalse(state.canSubmit)
        }

    @Test
    fun `MCQ selection does not trigger submit and submit sends selected option with session id`() =
        runTest(testDispatcher) {
            val initialReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "Choose timeframe",
                        options = listOf("1 month", "3 months"),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(initialReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Learn Spanish"))
            viewModel.onAction(AddTaskAction.Submit)

            viewModel.onAction(AddTaskAction.GoalOptionSelected("1 month"))

            // Selection alone does not call the usecase
            assertEquals(1, goalRepository.continueCalls.size)
            assertTrue(viewModel.state.value.canSubmit)

            val writingReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "What is your main focus area?",
                        options = emptyList(),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(writingReply)
            viewModel.onAction(AddTaskAction.Submit)

            assertEquals(
                listOf(
                    Pair(null, "Learn Spanish"),
                    Pair("session-123", "1 month"),
                ),
                goalRepository.continueCalls,
            )
            val writingStep = viewModel.state.value.goalStep as GoalStep.WritingQuestion
            assertEquals("What is your main focus area?", writingStep.question)
        }

    @Test
    fun `custom MCQ input clears a selected card, enables Continue, and sends the exact session plus trimmed custom value`() =
        runTest(testDispatcher) {
            val initialReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "Choose timeframe",
                        options = listOf("1 month", "3 months"),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(initialReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Learn Spanish"))
            viewModel.onAction(AddTaskAction.Submit)

            viewModel.onAction(AddTaskAction.GoalOptionSelected("1 month"))
            val mcqStep = viewModel.state.value.goalStep as GoalStep.MultipleChoice
            assertEquals("1 month", mcqStep.selectedOption)

            // Entering nonblank custom text clears selectedOption and enables submit
            viewModel.onAction(AddTaskAction.InputChanged("  2 weeks  "))
            val updatedMcq = viewModel.state.value.goalStep as GoalStep.MultipleChoice
            assertNull(updatedMcq.selectedOption)
            assertTrue(viewModel.state.value.canSubmit)

            val writingReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "Next question",
                        options = emptyList(),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(writingReply)
            viewModel.onAction(AddTaskAction.Submit)

            assertEquals(
                listOf(
                    Pair(null, "Learn Spanish"),
                    Pair("session-123", "2 weeks"),
                ),
                goalRepository.continueCalls,
            )
        }

    @Test
    fun `selecting a card clears a prior custom answer and submits that card`() =
        runTest(testDispatcher) {
            val initialReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "Choose timeframe",
                        options = listOf("1 month", "3 months"),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(initialReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Learn Spanish"))
            viewModel.onAction(AddTaskAction.Submit)

            viewModel.onAction(AddTaskAction.InputChanged("Custom text"))
            assertEquals("Custom text", viewModel.state.value.input)

            viewModel.onAction(AddTaskAction.GoalOptionSelected("3 months"))
            val mcqStep = viewModel.state.value.goalStep as GoalStep.MultipleChoice
            assertEquals("3 months", mcqStep.selectedOption)
            assertEquals("", viewModel.state.value.input)
            assertTrue(viewModel.state.value.canSubmit)

            val writingReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "Next question",
                        options = emptyList(),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(writingReply)
            viewModel.onAction(AddTaskAction.Submit)

            assertEquals(
                listOf(
                    Pair(null, "Learn Spanish"),
                    Pair("session-123", "3 months"),
                ),
                goalRepository.continueCalls,
            )
        }

    @Test
    fun `an MCQ custom answer survives a Result Error so retry remains possible`() =
        runTest(testDispatcher) {
            val initialReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "Choose timeframe",
                        options = listOf("1 month", "3 months"),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(initialReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Learn Spanish"))
            viewModel.onAction(AddTaskAction.Submit)

            viewModel.onAction(AddTaskAction.InputChanged("My custom timeframe"))
            goalRepository.nextContinueReply = Result.Error(AppError.Network)

            viewModel.onAction(AddTaskAction.Submit)

            val failedState = viewModel.state.value
            assertEquals("My custom timeframe", failedState.input)
            assertEquals(R.string.add_task_error_goal_continuation_failed, failedState.errorMessage)
            assertFalse(failedState.isSubmitting)

            val successReply = GoalDecompositionReply(
                sessionId = "session-123",
                blocks = listOf(
                    GoalDecompositionBlock.Question(
                        text = "Next question",
                        options = emptyList(),
                    ),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(successReply)

            viewModel.onAction(AddTaskAction.Submit)

            assertEquals(
                listOf(
                    Pair(null, "Learn Spanish"),
                    Pair("session-123", "My custom timeframe"),
                    Pair("session-123", "My custom timeframe"),
                ),
                goalRepository.continueCalls,
            )
        }

    @Test
    fun `Writing to Preview transition uses authoritative proposal block over hasProposal flag`() =
        runTest(testDispatcher) {
            val mcqReply = GoalDecompositionReply(
                sessionId = "sess-1",
                blocks = listOf(
                    GoalDecompositionBlock.Question("Any focus?", emptyList()),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(mcqReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Goal title"))
            viewModel.onAction(AddTaskAction.Submit)

            val proposal = GoalProposal(
                title = "Learn Spanish for travel",
                description = "Plan for trip",
                targetDate = "2026-09-01",
                tasks = listOf(ProposedTask("Study basic vocabulary", 30, 5)),
            )
            // hasProposal is false but actual Proposal block exists -> Proposal wins!
            val proposalReply = GoalDecompositionReply(
                sessionId = "sess-1",
                blocks = listOf(
                    GoalDecompositionBlock.Text("Here is your plan"),
                    GoalDecompositionBlock.Proposal(proposal),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(proposalReply)

            viewModel.onAction(AddTaskAction.InputChanged(" Grammar and vocab "))
            viewModel.onAction(AddTaskAction.Submit)

            assertEquals(
                listOf(
                    Pair(null, "Goal title"),
                    Pair("sess-1", "Grammar and vocab"),
                ),
                goalRepository.continueCalls,
            )
            val previewStep = viewModel.state.value.goalStep as GoalStep.Preview
            assertEquals(proposal, previewStep.proposal)
            assertTrue(viewModel.state.value.canAcceptGoal)
        }

    @Test
    fun `Text-only response remains usable Writing state with blocks retained`() =
        runTest(testDispatcher) {
            val textReply = GoalDecompositionReply(
                sessionId = "sess-text",
                blocks = listOf(
                    GoalDecompositionBlock.Text("Tell me more about your goal."),
                ),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(textReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Goal title"))
            viewModel.onAction(AddTaskAction.Submit)

            val state = viewModel.state.value
            val writingStep = state.goalStep as GoalStep.WritingQuestion
            assertEquals("", writingStep.question)
            assertEquals(textReply.blocks, state.goalReplyBlocks)
        }

    @Test
    fun `Preview revision sends modification prompt on same session and updates proposal`() =
        runTest(testDispatcher) {
            val initialProposal = GoalProposal(
                title = "Initial Plan",
                description = null,
                targetDate = null,
                tasks = emptyList(),
            )
            val previewReply = GoalDecompositionReply(
                sessionId = "sess-rev",
                blocks = listOf(GoalDecompositionBlock.Proposal(initialProposal)),
                hasProposal = true,
            )
            goalRepository.nextContinueReply = Result.Success(previewReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Goal"))
            viewModel.onAction(AddTaskAction.Submit)

            val updatedProposal = GoalProposal(
                title = "Revised Plan",
                description = "Updated desc",
                targetDate = "2026-10-01",
                tasks = listOf(ProposedTask("Task 1", 60, 10)),
            )
            val updatedReply = GoalDecompositionReply(
                sessionId = "sess-rev",
                blocks = listOf(GoalDecompositionBlock.Proposal(updatedProposal)),
                hasProposal = true,
            )
            goalRepository.nextContinueReply = Result.Success(updatedReply)

            viewModel.onAction(AddTaskAction.InputChanged(" Add more practice "))
            viewModel.onAction(AddTaskAction.Submit)

            assertEquals(
                listOf(
                    Pair(null, "Goal"),
                    Pair("sess-rev", "Add more practice"),
                ),
                goalRepository.continueCalls,
            )
            val previewStep = viewModel.state.value.goalStep as GoalStep.Preview
            assertEquals(updatedProposal, previewStep.proposal)
        }

    @Test
    fun `Accept success confirms exact session and emits GoalCreated once ignoring duplicate accepts`() =
        runTest(testDispatcher) {
            val proposal = GoalProposal("Goal Title", null, null, emptyList())
            val previewReply = GoalDecompositionReply(
                sessionId = "sess-confirm",
                blocks = listOf(GoalDecompositionBlock.Proposal(proposal)),
                hasProposal = true,
            )
            goalRepository.nextContinueReply = Result.Success(previewReply)

            val confirmGate = CompletableDeferred<Result<Goal>>()
            val gateRepository = object : GoalRepository by goalRepository {
                override suspend fun confirmDecomposition(sessionId: String): Result<Goal> {
                    goalRepository.confirmCalls += sessionId
                    return confirmGate.await()
                }
            }
            val customViewModel = AddTaskViewModel(
                parseTaskInput = ParseTaskInputUseCase(clock),
                applyTaskAttribute = ApplyTaskAttributeUseCase(clock),
                getCategories = GetCategoriesUseCase(categoryRepository),
                createTask = CreateTaskUseCase(taskRepository, GetZonesForDateUseCase(zoneRepository)),
                continueGoalDecomposition = ContinueGoalDecompositionUseCase(gateRepository),
                confirmGoalDecomposition = ConfirmGoalDecompositionUseCase(gateRepository),
                getUserDataUseCase = GetUserDataUseCase(userDataRepository),
                setMicPermissionRequestedUseCase = SetMicPermissionRequestedUseCase(userDataRepository),
                clock = clock,
            )

            customViewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            customViewModel.onAction(AddTaskAction.InputChanged("Goal"))
            customViewModel.onAction(AddTaskAction.Submit)

            customViewModel.onAction(AddTaskAction.AcceptGoalProposal)
            customViewModel.onAction(AddTaskAction.AcceptGoalProposal)

            assertEquals(listOf("sess-confirm"), goalRepository.confirmCalls)
            assertTrue(customViewModel.state.value.isSubmitting)

            val createdGoal = Goal(id = "g-1", title = "Goal Title", description = null, emoji = "🎯")
            confirmGate.complete(Result.Success(createdGoal))

            assertEquals(AddTaskEvent.GoalCreated("Goal Title"), customViewModel.events.first())
        }

    @Test
    fun `invalid MCQ option selection is ignored`() = runTest(testDispatcher) {
        val initialReply = GoalDecompositionReply(
            sessionId = "session-123",
            blocks = listOf(
                GoalDecompositionBlock.Question(
                    text = "Choose timeframe",
                    options = listOf("1 month", "3 months"),
                ),
            ),
            hasProposal = false,
        )
        goalRepository.nextContinueReply = Result.Success(initialReply)
        val viewModel = viewModel()
        viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
        viewModel.onAction(AddTaskAction.InputChanged("Learn Spanish"))
        viewModel.onAction(AddTaskAction.Submit)

        viewModel.onAction(AddTaskAction.GoalOptionSelected("1 year"))

        val mcqStep = viewModel.state.value.goalStep as GoalStep.MultipleChoice
        assertNull(mcqStep.selectedOption)
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `switching from task AI to goal clears AI attachment and closes pickers`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.InputChanged("Build login page"))
            viewModel.onAction(AddTaskAction.AiToggled)
            viewModel.onAction(AddTaskAction.ImagePicked("content://image"))
            viewModel.onAction(AddTaskAction.PickerOpened(AddTaskPicker.DATE))

            assertEquals(AddTaskPicker.DATE, viewModel.state.value.openPicker)

            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))

            val goalState = viewModel.state.value
            assertEquals(AddTaskMode.GOAL, goalState.mode)
            assertFalse(goalState.aiEnabled)
            assertNull(goalState.imageUri)
            assertNull(goalState.openPicker)
            assertEquals("Build login page", goalState.input)

            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.TASK))
            assertEquals(AddTaskMode.TASK, viewModel.state.value.mode)
            assertEquals("Build login page", viewModel.state.value.parsed.title)
        }

    @Test
    fun `discarding an in-flight goal continuation cancels the job and prevents late state updates`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Result<GoalDecompositionReply>>()
            val gateRepository = object : GoalRepository by goalRepository {
                override suspend fun continueDecomposition(
                    sessionId: String?,
                    message: String,
                ): Result<GoalDecompositionReply> {
                    return gate.await()
                }
            }
            val customViewModel = AddTaskViewModel(
                parseTaskInput = ParseTaskInputUseCase(clock),
                applyTaskAttribute = ApplyTaskAttributeUseCase(clock),
                getCategories = GetCategoriesUseCase(categoryRepository),
                createTask = CreateTaskUseCase(taskRepository, GetZonesForDateUseCase(zoneRepository)),
                continueGoalDecomposition = ContinueGoalDecompositionUseCase(gateRepository),
                confirmGoalDecomposition = ConfirmGoalDecompositionUseCase(gateRepository),
                getUserDataUseCase = GetUserDataUseCase(userDataRepository),
                setMicPermissionRequestedUseCase = SetMicPermissionRequestedUseCase(userDataRepository),
                clock = clock,
            )

            customViewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            customViewModel.onAction(AddTaskAction.InputChanged("In-flight Goal"))
            customViewModel.onAction(AddTaskAction.Submit)

            assertTrue(customViewModel.state.value.isSubmitting)

            customViewModel.onAction(AddTaskAction.DismissRequested)
            customViewModel.onAction(AddTaskAction.DiscardConfirmed)

            val resetState = customViewModel.state.value
            assertEquals(AddTaskMode.TASK, resetState.mode)
            assertEquals(GoalStep.Initial, resetState.goalStep)
            assertNull(resetState.goalSessionId)

            gate.complete(
                Result.Success(
                    GoalDecompositionReply(
                        sessionId = "sess-late",
                        blocks = listOf(GoalDecompositionBlock.Question("Late Q", emptyList())),
                        hasProposal = false,
                    ),
                ),
            )

            val finalState = customViewModel.state.value
            assertEquals(AddTaskMode.TASK, finalState.mode)
            assertEquals(GoalStep.Initial, finalState.goalStep)
            assertNull(finalState.goalSessionId)
            assertEquals(AddTaskEvent.Dismissed, customViewModel.events.first())
        }

    @Test
    fun `Continuation and confirm failures preserve full retryable state and input`() =
        runTest(testDispatcher) {
            goalRepository.nextContinueReply = Result.Error(AppError.Network)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("My Goal"))
            viewModel.onAction(AddTaskAction.Submit)

            val failedState = viewModel.state.value
            assertEquals("My Goal", failedState.input)
            assertEquals(GoalStep.Initial, failedState.goalStep)
            assertNull(failedState.goalSessionId)
            assertFalse(failedState.isSubmitting)
            assertEquals(R.string.add_task_error_goal_continuation_failed, failedState.errorMessage)

            val proposal = GoalProposal("Goal", null, null, emptyList())
            val previewReply = GoalDecompositionReply(
                sessionId = "sess-fail",
                blocks = listOf(GoalDecompositionBlock.Proposal(proposal)),
                hasProposal = true,
            )
            goalRepository.nextContinueReply = Result.Success(previewReply)
            viewModel.onAction(AddTaskAction.Submit)

            assertEquals(GoalStep.Preview(proposal), viewModel.state.value.goalStep)

            goalRepository.nextConfirmResult = Result.Error(AppError.Network)
            viewModel.onAction(AddTaskAction.AcceptGoalProposal)

            val confirmFailedState = viewModel.state.value
            assertEquals(GoalStep.Preview(proposal), confirmFailedState.goalStep)
            assertEquals("sess-fail", confirmFailedState.goalSessionId)
            assertFalse(confirmFailedState.isSubmitting)
            assertEquals(R.string.add_task_error_goal_confirm_failed, confirmFailedState.errorMessage)
        }

    @Test
    fun `Duplicate submit while in flight causes one continuation call`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Result<GoalDecompositionReply>>()
            val gateRepository = object : GoalRepository by goalRepository {
                override suspend fun continueDecomposition(
                    sessionId: String?,
                    message: String,
                ): Result<GoalDecompositionReply> {
                    goalRepository.continueCalls += Pair(sessionId, message)
                    return gate.await()
                }
            }
            val customViewModel = AddTaskViewModel(
                parseTaskInput = ParseTaskInputUseCase(clock),
                applyTaskAttribute = ApplyTaskAttributeUseCase(clock),
                getCategories = GetCategoriesUseCase(categoryRepository),
                createTask = CreateTaskUseCase(taskRepository, GetZonesForDateUseCase(zoneRepository)),
                continueGoalDecomposition = ContinueGoalDecompositionUseCase(gateRepository),
                confirmGoalDecomposition = ConfirmGoalDecompositionUseCase(gateRepository),
                getUserDataUseCase = GetUserDataUseCase(userDataRepository),
                setMicPermissionRequestedUseCase = SetMicPermissionRequestedUseCase(userDataRepository),
                clock = clock,
            )

            customViewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            customViewModel.onAction(AddTaskAction.InputChanged("My Goal"))
            customViewModel.onAction(AddTaskAction.Submit)
            customViewModel.onAction(AddTaskAction.Submit)

            assertEquals(1, goalRepository.continueCalls.size)

            gate.complete(
                Result.Success(
                    GoalDecompositionReply(
                        sessionId = "sess-gate",
                        blocks = listOf(GoalDecompositionBlock.Question("Q", emptyList())),
                        hasProposal = false,
                    ),
                ),
            )
        }

    @Test
    fun `Mode switching parsing before session works and switching is blocked during active session`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.InputChanged("Gym tomorrow 6pm"))
            assertEquals("Gym", viewModel.state.value.parsed.title)

            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            assertEquals(AddTaskMode.GOAL, viewModel.state.value.mode)

            viewModel.onAction(AddTaskAction.InputChanged("Gym tomorrow 6pm for 45m"))
            assertTrue(viewModel.state.value.parsed.tokens.isEmpty())

            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.TASK))
            assertEquals(AddTaskMode.TASK, viewModel.state.value.mode)
            assertEquals("Gym", viewModel.state.value.parsed.title)

            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            val reply = GoalDecompositionReply(
                sessionId = "active-sess",
                blocks = listOf(GoalDecompositionBlock.Question("Question?", emptyList())),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(reply)
            viewModel.onAction(AddTaskAction.Submit)

            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.TASK))
            assertEquals(AddTaskMode.GOAL, viewModel.state.value.mode)
            assertFalse(viewModel.state.value.showsModeSelector)
        }

    @Test
    fun `Dirty-dismiss behavior includes goal input and active empty-input goal sessions`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))

            assertFalse(viewModel.state.value.isDirty)

            viewModel.onAction(AddTaskAction.InputChanged("Learn guitar"))
            assertTrue(viewModel.state.value.isDirty)
            viewModel.onAction(AddTaskAction.DismissRequested)
            assertTrue(viewModel.state.value.showDiscardConfirm)
            viewModel.onAction(AddTaskAction.DiscardCancelled)

            viewModel.onAction(AddTaskAction.InputChanged(""))
            val reply = GoalDecompositionReply(
                sessionId = "sess-dirty",
                blocks = listOf(GoalDecompositionBlock.Question("Which genre?", emptyList())),
                hasProposal = false,
            )
            goalRepository.nextContinueReply = Result.Success(reply)
            viewModel.onAction(AddTaskAction.InputChanged("Goal"))
            viewModel.onAction(AddTaskAction.Submit)

            assertEquals("", viewModel.state.value.input)
            assertTrue(viewModel.state.value.isDirty)

            viewModel.onAction(AddTaskAction.DismissRequested)
            assertTrue(viewModel.state.value.showDiscardConfirm)

            viewModel.onAction(AddTaskAction.DiscardConfirmed)
            assertFalse(viewModel.state.value.showDiscardConfirm)
            assertEquals(AddTaskMode.TASK, viewModel.state.value.mode)
            assertEquals(GoalStep.Initial, viewModel.state.value.goalStep)
            assertNull(viewModel.state.value.goalSessionId)
        }

    @Test
    fun `InputChanged ignored during goal submission in-flight gate`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Result<GoalDecompositionReply>>()
            val gateRepository = object : GoalRepository by goalRepository {
                override suspend fun continueDecomposition(
                    sessionId: String?,
                    message: String,
                ): Result<GoalDecompositionReply> {
                    return gate.await()
                }
            }
            val customViewModel = AddTaskViewModel(
                parseTaskInput = ParseTaskInputUseCase(clock),
                applyTaskAttribute = ApplyTaskAttributeUseCase(clock),
                getCategories = GetCategoriesUseCase(categoryRepository),
                createTask = CreateTaskUseCase(taskRepository, GetZonesForDateUseCase(zoneRepository)),
                continueGoalDecomposition = ContinueGoalDecompositionUseCase(gateRepository),
                confirmGoalDecomposition = ConfirmGoalDecompositionUseCase(gateRepository),
                getUserDataUseCase = GetUserDataUseCase(userDataRepository),
                setMicPermissionRequestedUseCase = SetMicPermissionRequestedUseCase(userDataRepository),
                clock = clock,
            )

            customViewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            customViewModel.onAction(AddTaskAction.InputChanged("Captured input"))
            customViewModel.onAction(AddTaskAction.Submit)

            assertTrue(customViewModel.state.value.isSubmitting)

            // Late input action while submitting
            customViewModel.onAction(AddTaskAction.InputChanged("Late transcript text"))
            assertEquals("Captured input", customViewModel.state.value.input)

            gate.complete(
                Result.Success(
                    GoalDecompositionReply(
                        sessionId = "sess-in-flight",
                        blocks = listOf(GoalDecompositionBlock.Question("Q", emptyList())),
                        hasProposal = false,
                    ),
                ),
            )
            assertEquals("", customViewModel.state.value.input)
        }

    @Test
    fun `goalStep transitions to Preview and isDirty is true so sheet dirty-gate must permit programmatic hide`() =
        runTest(testDispatcher) {
            val proposalReply = GoalDecompositionReply(
                sessionId = "session-abc",
                blocks = listOf(
                    GoalDecompositionBlock.Proposal(
                        GoalProposal(
                            title = "Run a 10k marathon",
                            description = null,
                            targetDate = null,
                            tasks = emptyList(),
                        )
                    )
                ),
                hasProposal = true,
            )
            goalRepository.nextContinueReply = Result.Success(proposalReply)
            val viewModel = viewModel()
            viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
            viewModel.onAction(AddTaskAction.InputChanged("Run a 10k marathon"))
            viewModel.onAction(AddTaskAction.Submit)

            val state = viewModel.state.value
            assertTrue(state.goalStep is GoalStep.Preview)
            // isDirty must be true so that the confirmValueChange block would normally block dismissal
            // but the `!isPreviewStep` guard lifts it for programmatic hide
            assertTrue(state.isDirty)
        }

    @Test
    fun `SetMicPermissionRequested action updates state and user data repository`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            assertFalse(viewModel.state.value.hasRequestedMicPermission)

            viewModel.onAction(AddTaskAction.SetMicPermissionRequested(true))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.hasRequestedMicPermission)
            assertTrue(userDataRepository._userData.value.micPermissionRequested)

            viewModel.onAction(AddTaskAction.SetMicPermissionRequested(false))
            advanceUntilIdle()

            assertFalse(viewModel.state.value.hasRequestedMicPermission)
            assertFalse(userDataRepository._userData.value.micPermissionRequested)
        }
}

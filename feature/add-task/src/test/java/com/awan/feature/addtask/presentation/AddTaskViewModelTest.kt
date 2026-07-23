package com.awan.feature.addtask.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.domain.task.parser.TaskInputParser
import com.awan.app.core.domain.task.usecase.ApplyTaskAttributeUseCase
import com.awan.app.core.domain.task.usecase.CreateTaskUseCase
import com.awan.app.core.domain.task.usecase.ParseTaskInputUseCase
import com.awan.app.core.domain.zone.repository.ZoneRepository
import com.awan.app.core.domain.zone.usecase.GetZonesForDateUseCase
import com.awan.app.core.model.DayZone
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskSession
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.feature.addtask.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddTaskViewModelTest {

    /** Wednesday 2026-07-22, 10:00 UTC. */
    private val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneId.of("UTC"))
    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeTaskRepository : TaskRepository {
        var lastDraft: TaskDraft? = null
        var lastSessions: List<SessionDraft> = emptyList()
        var failWith: AppError? = null

        override suspend fun createTask(draft: TaskDraft): Result<Task> {
            lastDraft = draft
            failWith?.let { return Result.Error(it) }
            return Result.Success(Task(id = "t-1", title = draft.title))
        }

        override suspend fun createTaskWithSessions(
            draft: TaskDraft,
            sessions: List<SessionDraft>,
        ): Result<TaskWithSessions> {
            lastDraft = draft
            lastSessions = sessions
            failWith?.let { return Result.Error(it) }
            return Result.Success(
                TaskWithSessions(
                    task = Task(id = "t-2", title = draft.title),
                    sessions = sessions.mapIndexed { index, session ->
                        TaskSession(id = "s-$index", start = session.start, end = session.end)
                    },
                )
            )
        }
    }

    private class FakeZoneRepository(private val zones: List<DayZone>) : ZoneRepository {
        var requestedDate: LocalDate? = null

        override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> {
            requestedDate = date
            return Result.Success(zones)
        }
    }

    private val playZone = DayZone(
        id = "zone-play",
        name = "Play",
        startTime = LocalTime.of(17, 0),
        endTime = LocalTime.of(22, 0),
    )

    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var zoneRepository: FakeZoneRepository

    private fun viewModel(): AddTaskViewModel = AddTaskViewModel(
        parseTaskInput = ParseTaskInputUseCase(clock),
        applyTaskAttribute = ApplyTaskAttributeUseCase(clock),
        getZonesForDate = GetZonesForDateUseCase(zoneRepository),
        createTask = CreateTaskUseCase(taskRepository),
        clock = clock,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = FakeTaskRepository()
        zoneRepository = FakeZoneRepository(listOf(playZone))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `typing parses the sentence into chips and a clean title`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm"))

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
    fun `a zone token resolves against the zones of the parsed date`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm @play"))

        assertEquals(LocalDate.of(2026, 7, 23), zoneRepository.requestedDate)
        assertEquals(playZone, viewModel.state.value.resolvedZone)
        assertFalse(viewModel.state.value.hasUnknownZone)
    }

    @Test
    fun `an unresolvable zone token is flagged but does not block submitting`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm @nowhere"))

        assertNull(viewModel.state.value.resolvedZone)
        assertTrue(viewModel.state.value.hasUnknownZone)
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `an unscheduled task is created without any session`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.Submit)

        assertEquals("Buy groceries", taskRepository.lastDraft?.title)
        assertTrue(taskRepository.lastSessions.isEmpty())
    }

    @Test
    fun `a scheduled task gets one session ending after the parsed duration`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm for 45m @play"))
        viewModel.onAction(AddTaskAction.Submit)

        val session = taskRepository.lastSessions.single()
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), session.start)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 45), session.end)
        assertEquals("zone-play", session.zoneId)
    }

    @Test
    fun `a scheduled task with no duration falls back to the default length`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Gym session tomorrow 6pm"))
        viewModel.onAction(AddTaskAction.Submit)

        val session = taskRepository.lastSessions.single()
        assertEquals(TaskDraft.DEFAULT_DURATION_MINUTES.toLong(), java.time.Duration.between(session.start, session.end).toMinutes())
    }

    @Test
    fun `tasks are mandatory by default and the chip toggles it off`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        assertTrue(viewModel.state.value.mandatory)

        viewModel.onAction(AddTaskAction.MandatoryToggled)
        viewModel.onAction(AddTaskAction.Submit)

        assertFalse(taskRepository.lastDraft?.mandatory ?: true)
    }

    @Test
    fun `a successful create clears the sheet and emits TaskCreated`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val events = mutableListOf<AddTaskEvent>()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.Submit)
        events += viewModel.events.first()

        assertEquals(AddTaskEvent.TaskCreated("Buy groceries"), events.single())
        assertEquals("", viewModel.state.value.input)
        assertFalse(viewModel.state.value.isSubmitting)
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
    fun `a successful create celebrates before the sheet is dismissed`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.Submit)

        // The create has returned but the event has not fired yet — this is the cheer beat.
        val celebrating = viewModel.state.value
        assertTrue(celebrating.isCelebrating)
        assertFalse(celebrating.isSubmitting)
        assertEquals(MascotExpression.Celebrate, celebrating.mascot)

        assertEquals(AddTaskEvent.TaskCreated("Buy groceries"), viewModel.events.first())
        assertFalse(viewModel.state.value.isCelebrating)
    }

    @Test
    fun `a failed create surfaces an error and keeps what the user typed`() = runTest(testDispatcher) {
        taskRepository.failWith = AppError.Network
        val viewModel = viewModel()

        viewModel.onAction(AddTaskAction.InputChanged("Buy groceries"))
        viewModel.onAction(AddTaskAction.Submit)

        val state = viewModel.state.value
        assertEquals("Buy groceries", state.input)
        assertFalse(state.isSubmitting)
        assertEquals(R.string.add_task_error_create_failed, state.errorMessage)
    }
}

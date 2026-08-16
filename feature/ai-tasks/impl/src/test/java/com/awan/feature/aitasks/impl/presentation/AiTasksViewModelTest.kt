package com.awan.feature.aitasks.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.image.repository.ImageRepository
import com.awan.app.core.domain.image.usecase.ReadImageUseCase
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.task.usecase.CreateTasksUseCase
import com.awan.app.core.domain.task.usecase.ProposeTasksFromImageUseCase
import com.awan.app.core.domain.task.usecase.ProposeTasksFromTextUseCase
import com.awan.app.core.model.Category
import com.awan.app.core.model.ImageBytes
import com.awan.app.core.model.ProposedSession
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposal
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import com.awan.feature.aitasks.impl.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AiTasksViewModelTest {

    private val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneId.of("UTC"))
    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeTaskRepository : TaskRepository {
        var proposals: Result<TaskProposals> = Result.Success(TaskProposals())
        var imageProposals: Result<TaskProposals> = Result.Success(TaskProposals())
        var createResult: Result<List<Task>> = Result.Success(emptyList())
        var lastBulkDrafts: List<TaskWithSessionsDraft> = emptyList()
        var bulkCallCount = 0
        var lastTextRequest: String? = null
        var lastImageNote: String? = null

        override suspend fun createTask(draft: TaskDraft): Result<Task> = error("not used")

        override suspend fun createTaskWithSessions(
            draft: TaskDraft,
            sessions: List<SessionDraft>,
        ): Result<TaskWithSessions> = error("not used")

        override suspend fun createTasksWithSessions(drafts: List<TaskWithSessionsDraft>): Result<List<Task>> {
            lastBulkDrafts = drafts
            bulkCallCount++
            return createResult
        }

        override suspend fun proposeTasksFromText(text: String): Result<TaskProposals> {
            lastTextRequest = text
            return proposals
        }

        override suspend fun proposeTasksFromImage(
            image: ByteArray,
            mimeType: String,
            note: String?,
        ): Result<TaskProposals> {
            lastImageNote = note
            return imageProposals
        }

        override suspend fun scheduleTask(taskId: String) = error("not used")

        override suspend fun deleteTask(taskId: String, cascade: Boolean): Result<Unit> = error("not used")

        override suspend fun getInboxTasks(): Result<List<TaskWithSessions>> = error("not used")

        override suspend fun completeTask(taskId: String): Result<Task> = error("not used")

        override suspend fun moveTask(taskId: String, goalId: String?): Result<Task> = error("not used")
    }

    private class FakeCategoryRepository(private val categories: List<Category>) : CategoryRepository {
        override suspend fun getCategories(): Result<List<Category>> = Result.Success(categories)
        override suspend fun createCategory(name: String): Result<Category> = error("not used")
        override suspend fun getCategory(categoryId: String): Result<Category> = error("not used")
        override suspend fun updateCategory(categoryId: String, name: String): Result<Category> = error("not used")
    }

    private class FakeImageRepository(private val result: Result<ImageBytes>) : ImageRepository {
        override suspend fun read(uri: String): Result<ImageBytes> = result
    }

    private val playCategory = Category(id = "cat-play", name = "Play")
    private lateinit var taskRepository: FakeTaskRepository

    private fun viewModel(
        imageRepository: ImageRepository = FakeImageRepository(Result.Success(ImageBytes(ByteArray(1), "image/jpeg"))),
    ) = AiTasksViewModel(
        proposeFromText = ProposeTasksFromTextUseCase(taskRepository),
        proposeFromImage = ProposeTasksFromImageUseCase(taskRepository),
        createTasks = CreateTasksUseCase(taskRepository),
        getCategories = GetCategoriesUseCase(FakeCategoryRepository(listOf(playCategory))),
        readImage = ReadImageUseCase(imageRepository),
        clock = clock,
    )

    private fun proposal(title: String, minutes: Int = 60, sessions: List<ProposedSession> = emptyList()) =
        TaskProposal(
            draft = TaskDraft(title = title, durationMinutes = minutes),
            sessions = sessions,
            reason = "because",
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = FakeTaskRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loading from text populates proposals and categories`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(
            TaskProposals(tasks = listOf(proposal("Build login page"), proposal("Set up DB schema"))),
        )
        val viewModel = viewModel()

        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.proposals.size)
        assertEquals(listOf(playCategory), state.availableCategories)
    }

    @Test
    fun `a title and a note both reach the text endpoint, nothing typed is dropped`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AiTasksAction.Load(text = "Build login page", note = "with email and password", imageUri = null))

        val sent = requireNotNull(taskRepository.lastTextRequest)
        assertTrue(sent.contains("Build login page"))
        assertTrue(sent.contains("with email and password"))
    }

    @Test
    fun `loading from an image reads bytes and shows the source summary`() = runTest(testDispatcher) {
        taskRepository.imageProposals = Result.Success(
            TaskProposals(sourceSummary = "TASK 1: Buy groceries", tasks = listOf(proposal("Buy groceries"))),
        )
        val viewModel = viewModel()

        viewModel.onAction(AiTasksAction.Load(text = "focus on top item", note = null, imageUri = "content://img/1"))

        val state = viewModel.state.value
        assertEquals("TASK 1: Buy groceries", state.sourceSummary)
        assertEquals("focus on top item", taskRepository.lastImageNote)
        assertEquals(1, state.proposals.size)
    }

    @Test
    fun `a title typed before switching to a photo still reaches Awan alongside the note`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onAction(
                AiTasksAction.Load(text = "Build login page", note = "focus on top item", imageUri = "content://img/1"),
            )

            val sent = requireNotNull(taskRepository.lastImageNote)
            assertTrue(sent.contains("Build login page"))
            assertTrue(sent.contains("focus on top item"))
        }

    @Test
    fun `an image that fails to read surfaces an error without calling the propose endpoint`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(imageRepository = FakeImageRepository(Result.Error(AppError.Unknown())))

            viewModel.onAction(AiTasksAction.Load(text = "", note = null, imageUri = "content://img/1"))

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(R.string.ai_tasks_error_generic, state.errorMessage)
        }

    @Test
    fun `an empty proposal list is the empty state, not an error`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = emptyList()))
        val viewModel = viewModel()

        viewModel.onAction(AiTasksAction.Load(text = "nothing actionable here", note = null, imageUri = null))

        val state = viewModel.state.value
        assertTrue(state.isEmptyResult)
        assertNull(state.errorMessage)
    }

    @Test
    fun `a propose failure surfaces a mapped error message`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Error(AppError.Timeout)
        val viewModel = viewModel()

        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))

        assertEquals(R.string.ai_tasks_error_timeout, viewModel.state.value.errorMessage)
    }

    @Test
    fun `removing drops the card and offers it back, undo puts it in the slot it came from`() =
        runTest(testDispatcher) {
            taskRepository.proposals =
                Result.Success(TaskProposals(tasks = listOf(proposal("A"), proposal("B"), proposal("C"))))
            val viewModel = viewModel()
            viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
            val middle = viewModel.state.value.proposals[1].id

            viewModel.onAction(AiTasksAction.Removed(middle))

            val removed = viewModel.state.value
            assertEquals(listOf("A", "C"), removed.proposals.map { it.draft.title })
            assertEquals("B", removed.lastRemoved?.proposal?.draft?.title)
            assertTrue(removed.canReset)

            viewModel.onAction(AiTasksAction.UndoRemove)

            val restored = viewModel.state.value
            assertEquals(listOf("A", "B", "C"), restored.proposals.map { it.draft.title })
            assertNull(restored.lastRemoved)
            assertFalse(restored.canReset)
        }

    @Test
    fun `only the newest removal is undoable`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"), proposal("B"))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        val ids = viewModel.state.value.proposals.map { it.id }

        viewModel.onAction(AiTasksAction.Removed(ids[0]))
        viewModel.onAction(AiTasksAction.Removed(ids[1]))
        viewModel.onAction(AiTasksAction.UndoRemove)

        val state = viewModel.state.value
        assertEquals(listOf("B"), state.proposals.map { it.draft.title })
        assertNull(state.lastRemoved)
    }

    @Test
    fun `reset restores every removal and every edit, and expanding alone does not offer it`() =
        runTest(testDispatcher) {
            taskRepository.proposals =
                Result.Success(TaskProposals(tasks = listOf(proposal("A"), proposal("B"))))
            val viewModel = viewModel()
            viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
            val ids = viewModel.state.value.proposals.map { it.id }

            viewModel.onAction(AiTasksAction.ToggleExpanded(ids[0]))
            assertFalse(viewModel.state.value.canReset)

            viewModel.onAction(AiTasksAction.TitleChanged(ids[0], "Renamed"))
            viewModel.onAction(AiTasksAction.Removed(ids[1]))
            assertTrue(viewModel.state.value.canReset)

            viewModel.onAction(AiTasksAction.ResetPlan)

            val state = viewModel.state.value
            assertEquals(listOf("A", "B"), state.proposals.map { it.draft.title })
            assertNull(state.lastRemoved)
            assertFalse(state.canReset)
        }

    @Test
    fun `removing every card keeps the review screen rather than falling back to the empty state`() =
        runTest(testDispatcher) {
            taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"))))
            val viewModel = viewModel()
            viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))

            viewModel.onAction(AiTasksAction.Removed(viewModel.state.value.proposals.single().id))

            val state = viewModel.state.value
            assertTrue(state.proposals.isEmpty())
            assertFalse(state.isEmptyResult)
        }

    @Test
    fun `toggling expanded flips only the targeted card`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"), proposal("B"))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        val first = viewModel.state.value.proposals[0].id

        viewModel.onAction(AiTasksAction.ToggleExpanded(first))

        val state = viewModel.state.value
        assertTrue(state.proposals[0].isExpanded)
        assertFalse(state.proposals[1].isExpanded)
    }

    @Test
    fun `accepting sends only the tasks still on screen in one bulk request`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"), proposal("B"))))
        taskRepository.createResult = Result.Success(listOf(Task(id = "t-1", title = "A")))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        val second = viewModel.state.value.proposals[1].id
        viewModel.onAction(AiTasksAction.Removed(second))

        val events = mutableListOf<AiTasksEvent>()
        backgroundScope.launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(AiTasksAction.Accept)

        assertEquals(1, taskRepository.lastBulkDrafts.size)
        assertEquals("A", taskRepository.lastBulkDrafts.single().task.title)
        assertEquals(AiTasksEvent.TasksCreated(1), events.first())
    }

    @Test
    fun `a failed accept keeps the reviewed list on screen instead of the error page`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"))))
        taskRepository.createResult = Result.Error(AppError.Network)
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))

        viewModel.onAction(AiTasksAction.Accept)

        val state = viewModel.state.value
        assertEquals(R.string.ai_tasks_error_accept_failed, state.acceptError)
        assertNull(state.errorMessage)
        assertEquals(1, state.proposals.size)
        assertFalse(state.isAccepting)
    }

    @Test
    fun `a second accept while one is in flight does not fire a duplicate bulk create`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"))))
        taskRepository.createResult = Result.Error(AppError.Network)
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        taskRepository.createResult = Result.Loading

        viewModel.onAction(AiTasksAction.Accept)
        viewModel.onAction(AiTasksAction.Accept)

        assertEquals(1, taskRepository.bulkCallCount)
    }

    @Test
    fun `a blank title stops the accept before the atomic bulk create rejects everything`() =
        runTest(testDispatcher) {
            taskRepository.proposals =
                Result.Success(TaskProposals(tasks = listOf(proposal("A"), proposal(""))))
            val viewModel = viewModel()
            viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))

            viewModel.onAction(AiTasksAction.Accept)

            assertEquals(0, taskRepository.bulkCallCount)
            assertEquals(R.string.ai_tasks_error_blank_title, viewModel.state.value.acceptError)

            val blank = viewModel.state.value.proposals[1].id
            viewModel.onAction(AiTasksAction.TitleChanged(blank, "Now titled"))
            assertNull(viewModel.state.value.acceptError)
        }

    @Test
    fun `text past the contract limit never leaves the device`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(
            AiTasksAction.Load(text = "x".repeat(ProposeTasksFromTextUseCase.MAX_TEXT_LENGTH + 1), note = null, imageUri = null),
        )

        assertNull(taskRepository.lastTextRequest)
        assertEquals(R.string.ai_tasks_error_text_too_long, viewModel.state.value.errorMessage)
    }

    @Test
    fun `an oversized photo is rejected before the upload`() = runTest(testDispatcher) {
        val tooBig = ImageBytes(ByteArray(ProposeTasksFromImageUseCase.MAX_IMAGE_BYTES + 1), "image/jpeg")
        val viewModel = viewModel(imageRepository = FakeImageRepository(Result.Success(tooBig)))

        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = "content://photo"))

        assertNull(taskRepository.lastImageNote)
        assertEquals(R.string.ai_tasks_error_image_too_large, viewModel.state.value.errorMessage)
    }

    @Test
    fun `an unsupported image format is rejected before the upload`() = runTest(testDispatcher) {
        val bmp = ImageBytes(ByteArray(1), "image/bmp")
        val viewModel = viewModel(imageRepository = FakeImageRepository(Result.Success(bmp)))

        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = "content://photo"))

        assertNull(taskRepository.lastImageNote)
        assertEquals(R.string.ai_tasks_error_unsupported_image, viewModel.state.value.errorMessage)
    }

    @Test
    fun `editing a title and duration before accepting survives into the request`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A", minutes = 30))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        val id = viewModel.state.value.proposals.single().id

        viewModel.onAction(AiTasksAction.TitleChanged(id, "Edited title"))
        viewModel.onAction(AiTasksAction.DurationPicked(id, 90))
        viewModel.onAction(AiTasksAction.CategoryPicked(id, "cat-play"))
        viewModel.onAction(AiTasksAction.Accept)

        val sent = taskRepository.lastBulkDrafts.single().task
        assertEquals("Edited title", sent.title)
        assertEquals(90, sent.durationMinutes)
        assertEquals("cat-play", sent.categoryId)
    }

    @Test
    fun `removing a session drops it from the accepted draft`() = runTest(testDispatcher) {
        val session = ProposedSession(
            start = LocalDateTime.of(2026, 7, 23, 9, 0),
            end = LocalDateTime.of(2026, 7, 23, 10, 0),
            isAiSuggested = true,
        )
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A", sessions = listOf(session)))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        val id = viewModel.state.value.proposals.single().id

        viewModel.onAction(AiTasksAction.SessionRemoved(id, 0))
        viewModel.onAction(AiTasksAction.Accept)

        assertTrue(taskRepository.lastBulkDrafts.single().sessions.isEmpty())
    }

    @Test
    fun `adding a session after removing the last one gives the task a time again`() = runTest(testDispatcher) {
        val session = ProposedSession(
            start = LocalDateTime.of(2026, 7, 23, 9, 0),
            end = LocalDateTime.of(2026, 7, 23, 10, 0),
        )
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A", sessions = listOf(session)))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        val id = viewModel.state.value.proposals.single().id
        viewModel.onAction(AiTasksAction.SessionRemoved(id, 0))
        assertTrue(viewModel.state.value.proposals.single().sessions.isEmpty())

        viewModel.onAction(AiTasksAction.SessionAdded(id))

        val proposal = viewModel.state.value.proposals.single()
        assertEquals(1, proposal.sessions.size)
        assertFalse(proposal.sessions.single().isAiSuggested)
        assertEquals(SessionPickerTarget(proposalId = id, sessionIndex = 0), viewModel.state.value.sessionPicker)
    }

    @Test
    fun `editing a session's date then time replaces its start and keeps the duration`() = runTest(testDispatcher) {
        val session = ProposedSession(
            start = LocalDateTime.of(2026, 7, 23, 9, 0),
            end = LocalDateTime.of(2026, 7, 23, 10, 30),
            isAiSuggested = true,
        )
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A", sessions = listOf(session)))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        val id = viewModel.state.value.proposals.single().id

        viewModel.onAction(AiTasksAction.SessionTapped(id, 0))
        viewModel.onAction(AiTasksAction.SessionDatePicked(LocalDate.of(2026, 7, 25)))
        viewModel.onAction(AiTasksAction.SessionTimePicked(14 * 60))

        val updated = viewModel.state.value.proposals.single().sessions.single()
        assertEquals(LocalDateTime.of(2026, 7, 25, 14, 0), updated.start)
        assertEquals(LocalDateTime.of(2026, 7, 25, 15, 30), updated.end)
        assertFalse(updated.isAiSuggested)
        assertNull(viewModel.state.value.sessionPicker)
    }

    @Test
    fun `back with proposals on screen asks before discarding`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))

        viewModel.onAction(AiTasksAction.BackRequested)

        assertTrue(viewModel.state.value.showDiscardConfirm)
    }

    @Test
    fun `confirming discard emits Dismissed`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"))))
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        viewModel.onAction(AiTasksAction.BackRequested)

        val events = mutableListOf<AiTasksEvent>()
        backgroundScope.launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(AiTasksAction.DiscardConfirmed)

        assertEquals(AiTasksEvent.Dismissed, events.first())
    }

    @Test
    fun `retry re-runs the same request that failed`() = runTest(testDispatcher) {
        taskRepository.proposals = Result.Error(AppError.Network)
        val viewModel = viewModel()
        viewModel.onAction(AiTasksAction.Load(text = "note", note = null, imageUri = null))
        assertTrue(viewModel.state.value.errorMessage != null)

        taskRepository.proposals = Result.Success(TaskProposals(tasks = listOf(proposal("A"))))
        viewModel.onAction(AiTasksAction.Retry)

        assertNull(viewModel.state.value.errorMessage)
        assertEquals(1, viewModel.state.value.proposals.size)
    }
}

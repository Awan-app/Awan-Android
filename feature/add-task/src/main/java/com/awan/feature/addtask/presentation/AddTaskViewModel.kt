package com.awan.feature.addtask.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.goal.usecase.SaveGoalProposalUseCase
import com.awan.app.core.domain.goal.usecase.ContinueGoalDecompositionUseCase
import com.awan.app.core.domain.zones.usecase.GetZonesForDateUseCase
import com.awan.app.core.domain.profile.usecase.GetUserDataUseCase
import com.awan.app.core.domain.profile.usecase.SetMicPermissionRequestedUseCase
import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.domain.task.usecase.ApplyTaskAttributeUseCase
import com.awan.app.core.domain.task.usecase.CreateTaskUseCase
import com.awan.app.core.domain.task.usecase.ParseTaskInputUseCase
import com.awan.app.core.domain.task.usecase.TaskAttribute
import com.awan.app.core.model.Category
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.app.core.model.Task
import com.awan.feature.addtask.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val parseTaskInput: ParseTaskInputUseCase,
    private val applyTaskAttribute: ApplyTaskAttributeUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getZonesForDate: GetZonesForDateUseCase,
    private val createTask: CreateTaskUseCase,
    private val continueGoalDecomposition: ContinueGoalDecompositionUseCase,
    private val saveGoalProposal: SaveGoalProposalUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val setMicPermissionRequestedUseCase: SetMicPermissionRequestedUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        restoreStateFromSavedState(LocalDate.now(clock)) ?: AddTaskState(today = LocalDate.now(clock))
    )
    val state: StateFlow<AddTaskState> = _state.asStateFlow()

    private val _events = Channel<AddTaskEvent>(Channel.BUFFERED)
    val events: Flow<AddTaskEvent> = _events.receiveAsFlow()

    private var activeGoalJob: Job? = null
    private var celebrationJob: Job? = null
    private var categoriesJob: Job? = null
    private var userDataJob: Job? = null
    private var persistStateJob: Job? = null
    private var createJob: Job? = null
    private var initializeJob: Job? = null

    init {
        loadCategories()
        observeUserData()
        persistStateJob = viewModelScope.launch {
            _state.collect { state ->
                persistState(state)
            }
        }
    }

    private companion object {
        /** Roughly the length of SparkleBurst plus one mascot cheer cycle. */
        const val CELEBRATE_MILLIS = 900L
        const val MINUTES_PER_HOUR = 60

        const val KEY_MODE = "add_task_mode"
        const val KEY_INPUT = "add_task_input"
        const val KEY_DESCRIPTION = "add_task_description"
        const val KEY_MANDATORY = "add_task_mandatory"
        const val KEY_IMAGE_URI = "add_task_image_uri"
        const val KEY_AI_ENABLED = "add_task_ai_enabled"
        const val KEY_GOAL_SESSION_ID = "add_task_goal_session_id"
        const val KEY_GOAL_STEP_TYPE = "add_task_goal_step_type"
        const val KEY_GOAL_STEP_QUESTION = "add_task_goal_step_question"
        const val KEY_GOAL_STEP_OPTIONS = "add_task_goal_step_options"
        const val KEY_GOAL_STEP_SELECTED_OPTION = "add_task_goal_step_selected_option"
        const val KEY_GOAL_PROPOSAL_TITLE = "add_task_goal_proposal_title"
        const val KEY_GOAL_PROPOSAL_DESCRIPTION = "add_task_goal_proposal_description"
        const val KEY_GOAL_PROPOSAL_TARGET_DATE = "add_task_goal_proposal_target_date"
        const val KEY_GOAL_PROPOSAL_TASK_TITLES = "add_task_goal_proposal_task_titles"
        const val KEY_GOAL_PROPOSAL_TASK_DURATIONS = "add_task_goal_proposal_task_durations"
        const val KEY_GOAL_PROPOSAL_TASK_POINTS = "add_task_goal_proposal_task_points"
    }

    fun onAction(action: AddTaskAction) {
        when (action) {
            is AddTaskAction.ModeChanged -> onModeChanged(action.mode)
            is AddTaskAction.InputChanged -> onInputChanged(action.input)
            is AddTaskAction.DescriptionChanged -> _state.update { it.copy(description = action.description) }
            AddTaskAction.MandatoryToggled -> _state.update { it.copy(mandatory = !it.mandatory) }
            is AddTaskAction.PickerOpened -> _state.update { it.copy(openPicker = action.picker) }
            AddTaskAction.PickerDismissed -> dismissPicker()
            is AddTaskAction.DatePicked ->
                _state.update { it.copy(pendingDate = action.date, openPicker = AddTaskPicker.TIME) }

            is AddTaskAction.TimePicked -> applyPickedTime(action.minutesFromMidnight)
            is AddTaskAction.DurationPicked -> applyAttribute(TaskAttribute.Lasting(action.minutes))
            is AddTaskAction.CategoryPicked -> applyAttribute(TaskAttribute.In(action.categoryName))
            AddTaskAction.AiToggled -> toggleAi()
            is AddTaskAction.ImagePicked -> _state.update { it.copy(imageUri = action.uri) }
            AddTaskAction.ImageCleared -> _state.update { it.copy(imageUri = null) }

            AddTaskAction.Submit -> submit()
            is AddTaskAction.GoalOptionSelected -> selectGoalOption(action.option)
            is AddTaskAction.UpdateProposedTask -> updateProposedTask(action.index, action.task)
            is AddTaskAction.RemoveProposedTask -> removeProposedTask(action.index)
            AddTaskAction.AcceptGoalProposal -> acceptGoalProposal()
            AddTaskAction.SaveGoalAsDraft -> saveGoal(addTasks = false)
            AddTaskAction.AddGoalTasks -> saveGoal(addTasks = true)
            AddTaskAction.GoalSaveChoiceDismissed -> _state.update { it.copy(showGoalSaveChoice = false) }
            AddTaskAction.DismissRequested -> requestDismiss()
            AddTaskAction.DiscardConfirmed -> discard()
            AddTaskAction.DiscardCancelled -> _state.update { it.copy(showDiscardConfirm = false) }
            AddTaskAction.Dismiss -> close(AddTaskEvent.Dismissed)
            is AddTaskAction.SetMicPermissionRequested -> setMicPermissionRequested(action.requested)
            is AddTaskAction.Initialize -> initialize(action.goalId, action.zoneId, action.date)
        }
    }

    private fun initialize(goalId: String?, zoneId: String?, date: LocalDate?) {
        val today = LocalDate.now(clock)
        _state.update { 
            AddTaskState(
                today = today,
                goalId = goalId,
                zoneId = zoneId,
                pendingDate = date,
                availableCategories = it.availableCategories // Preserve categories to avoid re-fetch
            ) 
        }
        
        initializeJob?.cancel()
        initializeJob = viewModelScope.launch {
            if (date != null && date != today) {
                // Pre-select the date in the parser only when it is not today
                applyAttribute(TaskAttribute.On(date))
            }
            if (zoneId != null) {
                // Try to find the zone to pre-select category
                val targetDate = date ?: today
                val zonesResult = getZonesForDate(targetDate)
                if (zonesResult is Result.Success) {
                    val zone = zonesResult.data.find { it.id == zoneId }
                    zone?.category?.let { category ->
                        applyAttribute(TaskAttribute.In(category.name))
                    }
                }
            }
        }
    }

    private fun observeUserData() {
        userDataJob?.cancel()
        userDataJob = viewModelScope.launch {
            getUserDataUseCase().collect { userData ->
                _state.update { it.copy(hasRequestedMicPermission = userData.micPermissionRequested) }
            }
        }
    }

    private fun setMicPermissionRequested(requested: Boolean) {
        viewModelScope.launch {
            setMicPermissionRequestedUseCase(requested)
        }
    }

    private fun onModeChanged(newMode: AddTaskMode) {
        val current = _state.value
        if (current.mode == newMode) return

        val isBlocked = current.isSubmitting ||
            current.confirmation != null ||
            (current.mode == AddTaskMode.GOAL && (current.goalSessionId != null || current.goalStep != GoalStep.Initial))
        if (isBlocked) return

        if (newMode == AddTaskMode.GOAL) {
            _state.update {
                it.copy(
                    mode = AddTaskMode.GOAL,
                    aiEnabled = false,
                    imageUri = null,
                    openPicker = null,
                    parsed = ParsedTaskInput.Empty,
                    resolvedCategory = null,
                    errorMessage = null,
                )
            }
        } else {
            val parsed = parseTaskInput(current.input)
            _state.update {
                it.copy(
                    mode = AddTaskMode.TASK,
                    parsed = parsed,
                    resolvedCategory = it.availableCategories.matching(parsed.categoryToken),
                    errorMessage = null,
                )
            }
        }
    }

    private fun selectGoalOption(option: String) {
        _state.update { state ->
            if (state.mode != AddTaskMode.GOAL || state.isSubmitting) return@update state
            val currentStep = state.goalStep
            if (currentStep is GoalStep.MultipleChoice && currentStep.options.contains(option)) {
                state.copy(
                    goalStep = currentStep.copy(selectedOption = option),
                    input = "",
                    errorMessage = null,
                )
            } else {
                state
            }
        }
    }

    /**
     * Backing out of the clock step keeps the day already chosen rather than throwing the whole
     * edit away — the user answered one of the two questions and that answer still stands.
     */
    private fun dismissPicker() {
        val pending = _state.value.pendingDate
        _state.update { it.copy(openPicker = null, pendingDate = null) }
        if (pending != null) applyAttribute(TaskAttribute.On(pending))
    }

    /**
     * Falls back to the day the sentence already names, so picking a time on "Gym tomorrow" gives
     * tomorrow at that time rather than silently dragging it to today.
     */
    private fun applyPickedTime(minutesFromMidnight: Int) {
        val current = _state.value
        val day = current.pendingDate ?: current.parsed.startAt?.toLocalDate() ?: LocalDate.now(clock)
        val moment = day.atTime(minutesFromMidnight / MINUTES_PER_HOUR, minutesFromMidnight % MINUTES_PER_HOUR)
        _state.update { it.copy(pendingDate = null) }
        applyAttribute(TaskAttribute.At(moment))
    }

    /** Rewrites the sentence, then re-parses it exactly as if it had been typed. */
    private fun applyAttribute(attribute: TaskAttribute) {
        val current = _state.value
        val rewritten = applyTaskAttribute(current.input, current.parsed, attribute)
        _state.update { it.copy(openPicker = null) }
        onInputChanged(rewritten)
    }

    /**
     * The typed text is never touched by the switch — only whether it gets read. Standing the
     * parser down leaves `parsed` empty, which is what makes the chips and the highlights disappear
     * without anything having to hide them one by one.
     */
    private fun onInputChanged(input: String) {
        val current = _state.value
        if (current.mode == AddTaskMode.GOAL && current.isSubmitting) return
        if (current.mode == AddTaskMode.GOAL || current.aiEnabled) {
            _state.update { state ->
                val updatedStep = if (state.mode == AddTaskMode.GOAL && state.goalStep is GoalStep.MultipleChoice) {
                    if (input.isNotBlank()) {
                        state.goalStep.copy(selectedOption = null)
                    } else {
                        state.goalStep
                    }
                } else {
                    state.goalStep
                }
                state.copy(
                    input = input,
                    goalStep = updatedStep,
                    errorMessage = null,
                )
            }
            return
        }
        val parsed = parseTaskInput(input)
        _state.update {
            it.copy(
                input = input,
                parsed = parsed,
                resolvedCategory = it.availableCategories.matching(parsed.categoryToken),
                errorMessage = null,
            )
        }
    }

    /** Flipping the switch re-reads the same sentence, or stops reading it. The text survives both. */
    private fun toggleAi() {
        val current = _state.value
        val enabled = !current.aiEnabled
        val parsed = if (enabled) ParsedTaskInput.Empty else parseTaskInput(current.input)
        _state.update {
            it.copy(
                aiEnabled = enabled,
                parsed = parsed,
                resolvedCategory = it.availableCategories.matching(parsed.categoryToken),
                openPicker = null,
                errorMessage = null,
            )
        }
    }

    /**
     * Categories don't depend on the task's date, so one fetch in `init` serves the whole sheet — the
     * chip's menu offers this set, and a typed `@token` is matched against it. A failed lookup is not
     * an error the user sees: the chip stays unresolved and the task is created without a category.
     */
    private fun loadCategories() {
        categoriesJob?.cancel()
        _state.update { it.copy(isResolvingCategory = true) }
        categoriesJob = viewModelScope.launch {
            val categories = when (val result = getCategories()) {
                is Result.Success -> result.data
                else -> emptyList()
            }
            _state.update {
                it.copy(
                    availableCategories = categories,
                    resolvedCategory = categories.matching(it.parsed.categoryToken),
                    isResolvingCategory = false,
                )
            }
        }
    }

    /**
     * Exact name first, then prefix — the prefix arm is what lets a multi-word category survive the
     * round trip through `@token`, which cannot hold a space.
     */
    private fun List<Category>.matching(token: String?): Category? {
        token ?: return null
        return firstOrNull { it.name.equals(token, ignoreCase = true) }
            ?: firstOrNull { it.name.startsWith(token, ignoreCase = true) }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit || current.isSubmitting) return
        if (current.mode == AddTaskMode.GOAL) {
            submitGoalContinuation()
            return
        }
        if (current.aiEnabled) {
            close(
                AddTaskEvent.AiRequested(
                    text = current.input,
                    note = current.description.takeIf { it.isNotBlank() },
                    imageUri = current.imageUri,
                ),
            )
        } else {
            createDirectly()
        }
    }

    private fun submitGoalContinuation() {
        val current = _state.value
        val (sessionId, message) = when (val step = current.goalStep) {
            GoalStep.Initial -> null to current.input.trim()
            is GoalStep.MultipleChoice -> {
                val customText = current.input.trim()
                if (customText.isNotBlank()) {
                    current.goalSessionId to customText
                } else {
                    val selected = step.selectedOption ?: return
                    current.goalSessionId to selected.trim()
                }
            }

            is GoalStep.WritingQuestion -> current.goalSessionId to current.input.trim()
            is GoalStep.Preview -> current.goalSessionId to current.input.trim()
        }

        if (message.isBlank()) return

        _state.update { it.copy(isSubmitting = true, errorMessage = null) }

        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = continueGoalDecomposition(sessionId = sessionId, message = message)) {
                    is Result.Success -> {
                        val reply = result.data
                        val proposalBlock = reply.blocks.filterIsInstance<GoalDecompositionBlock.Proposal>().firstOrNull()
                        val questionBlock = reply.blocks.filterIsInstance<GoalDecompositionBlock.Question>().firstOrNull()

                        val nextStep = when {
                            proposalBlock != null -> GoalStep.Preview(proposalBlock.proposal)
                            questionBlock != null -> {
                                if (questionBlock.options.isNotEmpty()) {
                                    GoalStep.MultipleChoice(
                                        question = questionBlock.text,
                                        options = questionBlock.options,
                                        selectedOption = null,
                                    )
                                } else {
                                    GoalStep.WritingQuestion(question = questionBlock.text)
                                }
                            }

                            else -> GoalStep.WritingQuestion(question = "")
                        }

                        _state.update {
                            it.copy(
                                goalStep = nextStep,
                                goalSessionId = reply.sessionId,
                                goalReplyBlocks = reply.blocks,
                                input = "",
                                isSubmitting = false,
                                errorMessage = null,
                            )
                        }
                    }

                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = R.string.add_task_error_goal_continuation_failed,
                            )
                        }
                    }

                    Result.Loading -> Unit
                }
            } finally {
                if (activeGoalJob === coroutineContext[Job]) {
                    activeGoalJob = null
                }
            }
        }
        activeGoalJob = job
        job.start()
    }

    private fun acceptGoalProposal() {
        val current = _state.value
        if (!current.canAcceptGoal || current.isSubmitting) return
        _state.update { it.copy(showGoalSaveChoice = true, errorMessage = null) }
    }

    private fun saveGoal(addTasks: Boolean) {
        val current = _state.value
        if (!current.canAcceptGoal || current.isSubmitting) return
        val sessionId = current.goalSessionId ?: return
        val proposal = (current.goalStep as? GoalStep.Preview)?.proposal ?: return

        _state.update { it.copy(showGoalSaveChoice = false, isSubmitting = true, errorMessage = null) }

        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = saveGoalProposal(sessionId, proposal, addTasks)) {
                    is Result.Success -> {
                        if (addTasks) {
                            close(AddTaskEvent.GoalScheduleRequested(result.data.id))
                        } else {
                            close(AddTaskEvent.GoalCreated(result.data.title))
                        }
                    }
                    is Result.Error -> _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = R.string.add_task_error_goal_confirm_failed,
                        )
                    }

                    Result.Loading -> Unit
                }
            } finally {
                if (activeGoalJob === coroutineContext[Job]) {
                    activeGoalJob = null
                }
            }
        }
        activeGoalJob = job
        job.start()
    }

    private fun createDirectly() {
        val current = _state.value
        createJob?.cancel()
        createJob = viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (createTask(current.toDraft())) {
                is Result.Success -> confirm(current.plannedConfirmation())
                is Result.Error -> _state.update {
                    it.copy(isSubmitting = false, errorMessage = R.string.add_task_error_create_failed)
                }

                Result.Loading -> Unit
            }
        }
    }

    /**
     * The sheet stops being a form and becomes a receipt: Awan cheers, and the task is shown back
     * with the session it actually landed in. Closing is the user's move — a task the engine placed
     * somewhere they didn't choose is exactly the one they need to see before it disappears.
     */
    private fun confirm(confirmation: TaskConfirmation) {
        _state.update {
            it.copy(isSubmitting = false, isCelebrating = true, confirmation = confirmation)
        }
        celebrationJob?.cancel()
        celebrationJob = viewModelScope.launch {
            delay(CELEBRATE_MILLIS)
            _state.update { it.copy(isCelebrating = false) }
        }
    }

    /** What the user asked for, used when nothing came back with a placed session of its own. */
    private fun AddTaskState.plannedConfirmation() = TaskConfirmation(
        title = parsed.title,
        firstSession = parsed.startAt,
        durationMinutes = parsed.durationMinutes,
    )

    private fun requestDismiss() {
        val current = _state.value
        if (current.isDirty) {
            _state.update { it.copy(showDiscardConfirm = true) }
            return
        }
        // Closing the receipt is the create finally reporting itself to whoever opened the sheet.
        close(
            when (val created = current.confirmation) {
                null -> AddTaskEvent.Dismissed
                else -> AddTaskEvent.TaskCreated(created.title)
            },
        )
    }

    /** Nothing is ever persisted while the sheet is open, so walking away has nothing to clean up. */
    private fun discard() {
        _state.update { it.copy(showDiscardConfirm = false) }
        close(AddTaskEvent.Dismissed)
    }

    private fun updateProposedTask(index: Int, updatedTask: ProposedTask) {
        val current = _state.value
        val previewStep = current.goalStep as? GoalStep.Preview ?: return
        val currentTasks = previewStep.proposal.tasks
        if (index !in currentTasks.indices) return
        val newTasks = currentTasks.toMutableList().apply { set(index, updatedTask) }
        val newProposal = previewStep.proposal.copy(tasks = newTasks)
        _state.update { it.copy(goalStep = GoalStep.Preview(newProposal)) }
    }

    private fun removeProposedTask(index: Int) {
        val current = _state.value
        val previewStep = current.goalStep as? GoalStep.Preview ?: return
        val currentTasks = previewStep.proposal.tasks
        if (index !in currentTasks.indices) return
        val newTasks = currentTasks.toMutableList().apply { removeAt(index) }
        val newProposal = previewStep.proposal.copy(tasks = newTasks)
        _state.update { it.copy(goalStep = GoalStep.Preview(newProposal)) }
    }

    private fun restoreStateFromSavedState(today: LocalDate): AddTaskState? {
        val modeName = savedStateHandle.get<String>(KEY_MODE) ?: return null
        val mode = runCatching { AddTaskMode.valueOf(modeName) }.getOrDefault(AddTaskMode.TASK)
        val input = savedStateHandle.get<String>(KEY_INPUT).orEmpty()
        val description = savedStateHandle.get<String>(KEY_DESCRIPTION).orEmpty()
        val mandatory = savedStateHandle.get<Boolean>(KEY_MANDATORY) ?: true
        val imageUri = savedStateHandle.get<String>(KEY_IMAGE_URI)
        val aiEnabled = savedStateHandle.get<Boolean>(KEY_AI_ENABLED) ?: false
        val goalSessionId = savedStateHandle.get<String>(KEY_GOAL_SESSION_ID)

        val stepType = savedStateHandle.get<String>(KEY_GOAL_STEP_TYPE)
        val goalStep = when (stepType) {
            "MCQ" -> {
                val q = savedStateHandle.get<String>(KEY_GOAL_STEP_QUESTION).orEmpty()
                val opts = savedStateHandle.get<ArrayList<String>>(KEY_GOAL_STEP_OPTIONS) ?: arrayListOf()
                val sel = savedStateHandle.get<String>(KEY_GOAL_STEP_SELECTED_OPTION)
                GoalStep.MultipleChoice(question = q, options = opts, selectedOption = sel)
            }
            "WRITING" -> {
                val q = savedStateHandle.get<String>(KEY_GOAL_STEP_QUESTION).orEmpty()
                GoalStep.WritingQuestion(question = q)
            }
            "PREVIEW" -> {
                val title = savedStateHandle.get<String>(KEY_GOAL_PROPOSAL_TITLE).orEmpty()
                val desc = savedStateHandle.get<String>(KEY_GOAL_PROPOSAL_DESCRIPTION)
                val targetDate = savedStateHandle.get<String>(KEY_GOAL_PROPOSAL_TARGET_DATE)
                val titles = savedStateHandle.get<ArrayList<String>>(KEY_GOAL_PROPOSAL_TASK_TITLES) ?: arrayListOf()
                val durations = savedStateHandle.get<ArrayList<Int>>(KEY_GOAL_PROPOSAL_TASK_DURATIONS) ?: arrayListOf()
                val points = savedStateHandle.get<ArrayList<Int>>(KEY_GOAL_PROPOSAL_TASK_POINTS) ?: arrayListOf()
                val tasks = titles.indices.map { i ->
                    ProposedTask(
                        title = titles[i],
                        estimatedDuration = durations.getOrNull(i)?.takeIf { it > 0 },
                        estimatedPoints = points.getOrNull(i)?.takeIf { it > 0 },
                    )
                }
                GoalStep.Preview(GoalProposal(title = title, description = desc, targetDate = targetDate, tasks = tasks))
            }
            else -> GoalStep.Initial
        }

        val parsed = if (mode == AddTaskMode.TASK && !aiEnabled && input.isNotBlank()) {
            parseTaskInput(input)
        } else {
            ParsedTaskInput.Empty
        }

        return AddTaskState(
            today = today,
            mode = mode,
            input = input,
            description = description,
            parsed = parsed,
            mandatory = mandatory,
            goalStep = goalStep,
            goalSessionId = goalSessionId,
            aiEnabled = aiEnabled,
            imageUri = imageUri,
        )
    }

    private fun persistState(state: AddTaskState) {
        if (state.confirmation != null) {
            clearSavedState()
            return
        }
        savedStateHandle[KEY_MODE] = state.mode.name
        savedStateHandle[KEY_INPUT] = state.input
        savedStateHandle[KEY_DESCRIPTION] = state.description
        savedStateHandle[KEY_MANDATORY] = state.mandatory
        savedStateHandle[KEY_IMAGE_URI] = state.imageUri
        savedStateHandle[KEY_AI_ENABLED] = state.aiEnabled
        savedStateHandle[KEY_GOAL_SESSION_ID] = state.goalSessionId

        when (val step = state.goalStep) {
            is GoalStep.MultipleChoice -> {
                savedStateHandle[KEY_GOAL_STEP_TYPE] = "MCQ"
                savedStateHandle[KEY_GOAL_STEP_QUESTION] = step.question
                savedStateHandle[KEY_GOAL_STEP_OPTIONS] = ArrayList(step.options)
                savedStateHandle[KEY_GOAL_STEP_SELECTED_OPTION] = step.selectedOption
            }
            is GoalStep.WritingQuestion -> {
                savedStateHandle[KEY_GOAL_STEP_TYPE] = "WRITING"
                savedStateHandle[KEY_GOAL_STEP_QUESTION] = step.question
            }
            is GoalStep.Preview -> {
                savedStateHandle[KEY_GOAL_STEP_TYPE] = "PREVIEW"
                savedStateHandle[KEY_GOAL_PROPOSAL_TITLE] = step.proposal.title
                savedStateHandle[KEY_GOAL_PROPOSAL_DESCRIPTION] = step.proposal.description
                savedStateHandle[KEY_GOAL_PROPOSAL_TARGET_DATE] = step.proposal.targetDate
                savedStateHandle[KEY_GOAL_PROPOSAL_TASK_TITLES] = ArrayList(step.proposal.tasks.map { it.title })
                savedStateHandle[KEY_GOAL_PROPOSAL_TASK_DURATIONS] = ArrayList(step.proposal.tasks.map { it.estimatedDuration ?: 0 })
                savedStateHandle[KEY_GOAL_PROPOSAL_TASK_POINTS] = ArrayList(step.proposal.tasks.map { it.estimatedPoints ?: 0 })
            }
            GoalStep.Initial -> {
                savedStateHandle[KEY_GOAL_STEP_TYPE] = "INITIAL"
            }
        }
    }

    private fun clearSavedState() {
        savedStateHandle.remove<String>(KEY_MODE)
        savedStateHandle.remove<String>(KEY_INPUT)
        savedStateHandle.remove<String>(KEY_DESCRIPTION)
        savedStateHandle.remove<Boolean>(KEY_MANDATORY)
        savedStateHandle.remove<String>(KEY_IMAGE_URI)
        savedStateHandle.remove<Boolean>(KEY_AI_ENABLED)
        savedStateHandle.remove<String>(KEY_GOAL_SESSION_ID)
        savedStateHandle.remove<String>(KEY_GOAL_STEP_TYPE)
        savedStateHandle.remove<String>(KEY_GOAL_STEP_QUESTION)
        savedStateHandle.remove<ArrayList<String>>(KEY_GOAL_STEP_OPTIONS)
        savedStateHandle.remove<String>(KEY_GOAL_STEP_SELECTED_OPTION)
        savedStateHandle.remove<String>(KEY_GOAL_PROPOSAL_TITLE)
        savedStateHandle.remove<String>(KEY_GOAL_PROPOSAL_DESCRIPTION)
        savedStateHandle.remove<String>(KEY_GOAL_PROPOSAL_TARGET_DATE)
        savedStateHandle.remove<ArrayList<String>>(KEY_GOAL_PROPOSAL_TASK_TITLES)
        savedStateHandle.remove<ArrayList<Int>>(KEY_GOAL_PROPOSAL_TASK_DURATIONS)
        savedStateHandle.remove<ArrayList<Int>>(KEY_GOAL_PROPOSAL_TASK_POINTS)
    }

    /**
     * The only way out, and the only place the sheet is wiped. This ViewModel is scoped to the host
     * rather than to the sheet's composition, so it outlives every dismissal — without this the next
     * tap on `+` reopens whatever the last one left behind, receipt and all.
     */
    private fun close(event: AddTaskEvent) {
        activeGoalJob?.cancel()
        activeGoalJob = null
        celebrationJob?.cancel()
        celebrationJob = null
        createJob?.cancel()
        createJob = null
        initializeJob?.cancel()
        initializeJob = null
        clearSavedState()

        // Reset state but preserve the long-lived data already fetched
        _state.update { 
            AddTaskState(
                today = LocalDate.now(clock),
                availableCategories = it.availableCategories,
                hasRequestedMicPermission = it.hasRequestedMicPermission
            )
        }
        _events.trySend(event)
    }

    override fun onCleared() {
        super.onCleared()
        activeGoalJob?.cancel()
        celebrationJob?.cancel()
        categoriesJob?.cancel()
        userDataJob?.cancel()
        persistStateJob?.cancel()
        createJob?.cancel()
        initializeJob?.cancel()
    }
    
    /** Public for testing to ensure no leaking coroutines in runTest. */
    internal fun cancelAllJobsForTesting() {
        onCleared()
    }
}

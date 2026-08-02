package com.awan.feature.addtask.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.goal.usecase.ConfirmGoalDecompositionUseCase
import com.awan.app.core.domain.goal.usecase.ContinueGoalDecompositionUseCase
import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.domain.task.usecase.ApplyTaskAttributeUseCase
import com.awan.app.core.domain.task.usecase.CreateTaskUseCase
import com.awan.app.core.domain.task.usecase.DeleteTaskUseCase
import com.awan.app.core.domain.task.usecase.ParseTaskInputUseCase
import com.awan.app.core.domain.task.usecase.PreviewTaskWithAiUseCase
import com.awan.app.core.domain.task.usecase.ScheduleTaskWithAiUseCase
import com.awan.app.core.domain.task.usecase.TaskAttribute
import com.awan.app.core.model.AiTaskSuggestion
import com.awan.app.core.model.Category
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.Task
import com.awan.feature.addtask.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val parseTaskInput: ParseTaskInputUseCase,
    private val applyTaskAttribute: ApplyTaskAttributeUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val createTask: CreateTaskUseCase,
    private val previewTaskWithAi: PreviewTaskWithAiUseCase,
    private val scheduleTaskWithAi: ScheduleTaskWithAiUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val continueGoalDecomposition: ContinueGoalDecompositionUseCase,
    private val confirmGoalDecomposition: ConfirmGoalDecompositionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(AddTaskState(today = LocalDate.now(clock)))
    val state: StateFlow<AddTaskState> = _state.asStateFlow()

    private val _events = Channel<AddTaskEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var activeGoalJob: Job? = null

    init {
        loadCategories()
    }

    private companion object {
        /** Roughly the length of SparkleBurst plus one mascot cheer cycle. */
        const val CELEBRATE_MILLIS = 900L
        const val MINUTES_PER_HOUR = 60
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
            AddTaskAction.ScheduleWithAi -> scheduleWithAi()
            AddTaskAction.ScheduleManually ->
                _state.update { it.copy(aiStage = AddTaskAiStage.MANUAL, errorMessage = null) }

            AddTaskAction.Submit -> submit()
            is AddTaskAction.GoalOptionSelected -> selectGoalOption(action.option)
            AddTaskAction.AcceptGoalProposal -> acceptGoalProposal()
            AddTaskAction.DismissRequested -> requestDismiss()
            AddTaskAction.DiscardConfirmed -> discard()
            AddTaskAction.DiscardCancelled -> _state.update { it.copy(showDiscardConfirm = false) }
            AddTaskAction.Dismiss -> close(AddTaskEvent.Dismissed)
        }
    }

    private fun onModeChanged(newMode: AddTaskMode) {
        val current = _state.value
        if (current.mode == newMode) return

        val isBlocked = current.isSubmitting ||
            current.confirmation != null ||
            (current.mode == AddTaskMode.TASK && (current.aiStage.isReviewing || current.aiStage == AddTaskAiStage.WORKING)) ||
            (current.mode == AddTaskMode.GOAL && (current.goalSessionId != null || current.goalStep != GoalStep.Initial))
        if (isBlocked) return

        if (newMode == AddTaskMode.GOAL) {
            _state.update {
                it.copy(
                    mode = AddTaskMode.GOAL,
                    aiStage = AddTaskAiStage.OFF,
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
     * The typed text is never touched by the stage — only whether it gets read. Standing the parser
     * down leaves `parsed` empty, which is what makes the chips and the highlights disappear without
     * anything having to hide them one by one.
     */
    private fun onInputChanged(input: String) {
        val current = _state.value
        if (current.mode == AddTaskMode.GOAL && current.isSubmitting) return
        if (current.mode == AddTaskMode.GOAL || current.aiStage.isComposing) {
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
        if (!current.showsAiSwitch) return
        val stage = if (current.aiStage == AddTaskAiStage.OFF) {
            AddTaskAiStage.COMPOSING
        } else {
            AddTaskAiStage.OFF
        }
        val parsed = if (stage.isComposing) ParsedTaskInput.Empty else parseTaskInput(current.input)
        _state.update {
            it.copy(
                aiStage = stage,
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
        _state.update { it.copy(isResolvingCategory = true) }
        viewModelScope.launch {
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
        when (current.aiStage) {
            AddTaskAiStage.COMPOSING -> askAwan()
            // MANUAL is the same plain create as OFF: scheduleWithAi() never leaves a task behind
            // for a failed attempt to inherit, so picking a time by hand always starts from scratch.
            else -> createDirectly()
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
        val sessionId = current.goalSessionId ?: return

        _state.update { it.copy(isSubmitting = true, errorMessage = null) }

        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = confirmGoalDecomposition(sessionId)) {
                    is Result.Success -> close(AddTaskEvent.GoalCreated(result.data.title))
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
        viewModelScope.launch {
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

    private fun askAwan() {
        val current = _state.value
        viewModelScope.launch {
            _state.update {
                it.copy(aiStage = AddTaskAiStage.WORKING, isSubmitting = true, errorMessage = null)
            }
            when (val result = previewTaskWithAi(current.input, current.description)) {
                is Result.Success -> _state.update { it.intoReview(result.data) }
                is Result.Error -> _state.update {
                    it.copy(
                        aiStage = AddTaskAiStage.COMPOSING,
                        isSubmitting = false,
                        errorMessage = R.string.add_task_error_ai_failed,
                    )
                }

                Result.Loading -> Unit
            }
        }
    }

    /**
     * Awan's answer is folded back into the sentence rather than held beside it, so the review stage
     * is the ordinary form with ordinary chips — one path from text to draft, as before.
     */
    private fun AddTaskState.intoReview(suggestion: AiTaskSuggestion): AddTaskState {
        var sentence = suggestion.title.ifBlank { input }
        var parsed = parseTaskInput(sentence)
        suggestion.estimatedDurationMinutes?.let {
            sentence = applyTaskAttribute(sentence, parsed, TaskAttribute.Lasting(it))
            parsed = parseTaskInput(sentence)
        }
        // The preview may only echo a categoryId, not a name — fall back to our own list for that.
        val categoryName = suggestion.categoryName
            ?: suggestion.categoryId?.let { id -> availableCategories.firstOrNull { it.id == id }?.name }
        categoryName?.let {
            sentence = applyTaskAttribute(sentence, parsed, TaskAttribute.In(it))
            parsed = parseTaskInput(sentence)
        }
        return copy(
            aiStage = AddTaskAiStage.REVIEW,
            aiPoints = suggestion.estimatedPoints,
            aiSplittable = suggestion.allowTaskSplitting,
            input = sentence,
            parsed = parsed,
            description = suggestion.description.orEmpty(),
            mandatory = suggestion.mandatory,
            // Awan's category may not be one of ours — keep the id with whatever name we resolved,
            // but never fall back to the raw id as a display name (it's usually a UUID).
            resolvedCategory = availableCategories.matching(parsed.categoryToken)
                ?: categoryName?.let { name ->
                    suggestion.categoryId?.let { id -> Category(id = id, name = name) }
                },
            isSubmitting = false,
            errorMessage = null,
        )
    }

    /**
     * Nothing exists on the backend yet at REVIEW, so this creates the task itself — with whatever
     * the user edited through the chips — before asking the engine to place it. `startAt` is forced
     * out: the composed sentence may still carry a time phrase typed before Awan was switched on, and
     * "let Awan schedule it" has to mean the engine picks the slot, not whatever the parser found.
     * Anything short of a placed session deletes what was just created, so a retry (or switching to
     * "I'll pick a time") never has a stray task to inherit.
     */
    private fun scheduleWithAi() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            val created = when (val result = createTask(current.toDraft().copy(startAt = null))) {
                is Result.Success -> result.data
                is Result.Error -> {
                    _state.update {
                        it.copy(isSubmitting = false, errorMessage = R.string.add_task_error_create_failed)
                    }
                    return@launch
                }

                Result.Loading -> return@launch
            }
            when (val result = scheduleTaskWithAi(created.id)) {
                is Result.Success -> when {
                    // The engine chose the slot, so the receipt reports its answer, not the request.
                    result.data.isScheduled -> confirm(
                        TaskConfirmation(
                            title = current.parsed.title,
                            firstSession = result.data.sessions.minByOrNull { it.start }?.start,
                            durationMinutes = current.parsed.durationMinutes,
                        ),
                    )

                    else -> {
                        deleteTask(created.id)
                        _state.update {
                            it.copy(
                                aiStage = AddTaskAiStage.REVIEW,
                                isSubmitting = false,
                                errorMessage = R.string.add_task_error_ai_schedule_failed,
                            )
                        }
                    }
                }

                is Result.Error -> {
                    deleteTask(created.id)
                    _state.update {
                        it.copy(isSubmitting = false, errorMessage = R.string.add_task_error_ai_schedule_failed)
                    }
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
        viewModelScope.launch {
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

    /**
     * Nothing is ever persisted while the sheet is open — a preview never saves, and `scheduleWithAi`
     * cleans up after itself on anything short of a placed session — so walking away has nothing to
     * take with it.
     */
    private fun discard() {
        _state.update { it.copy(showDiscardConfirm = false) }
        close(AddTaskEvent.Dismissed)
    }

    /**
     * The only way out, and the only place the sheet is wiped. This ViewModel is scoped to the host
     * rather than to the sheet's composition, so it outlives every dismissal — without this the next
     * tap on `+` reopens whatever the last one left behind, receipt and all.
     */
    private fun close(event: AddTaskEvent) {
        activeGoalJob?.cancel()
        activeGoalJob = null
        _state.value = AddTaskState(today = LocalDate.now(clock))
        loadCategories()
        viewModelScope.launch { _events.send(event) }
    }
}

package com.awan.feature.addtask.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.domain.task.usecase.ApplyTaskAttributeUseCase
import com.awan.app.core.domain.task.usecase.CreateTaskUseCase
import com.awan.app.core.domain.task.usecase.ParseTaskInputUseCase
import com.awan.app.core.domain.task.usecase.TaskAttribute
import com.awan.app.core.model.Category
import com.awan.feature.addtask.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
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
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(AddTaskState(today = LocalDate.now(clock)))
    val state: StateFlow<AddTaskState> = _state.asStateFlow()

    private val _events = Channel<AddTaskEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
            is AddTaskAction.ModeChanged -> _state.update { it.copy(mode = action.mode, errorMessage = null) }
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
            AddTaskAction.DismissRequested -> requestDismiss()
            AddTaskAction.DiscardConfirmed -> discard()
            AddTaskAction.DiscardCancelled -> _state.update { it.copy(showDiscardConfirm = false) }
            AddTaskAction.Dismiss -> close(AddTaskEvent.Dismissed)
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
        if (_state.value.aiEnabled) {
            _state.update { it.copy(input = input, errorMessage = null) }
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
        if (!current.canSubmit) return
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

    /** Nothing is ever persisted while the sheet is open, so walking away has nothing to clean up. */
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
        _state.value = AddTaskState(today = LocalDate.now(clock))
        loadCategories()
        viewModelScope.launch { _events.send(event) }
    }
}

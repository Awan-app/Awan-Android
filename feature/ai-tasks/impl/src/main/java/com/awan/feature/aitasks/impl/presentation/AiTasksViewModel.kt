package com.awan.feature.aitasks.impl.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.ValidationReason
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.image.usecase.ReadImageUseCase
import com.awan.app.core.domain.task.usecase.CreateTasksUseCase
import com.awan.app.core.domain.task.usecase.ProposeTasksFromImageUseCase
import com.awan.app.core.domain.task.usecase.ProposeTasksFromTextUseCase
import com.awan.app.core.model.ProposedSession
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposal
import com.awan.app.core.model.TaskWithSessionsDraft
import com.awan.app.core.model.toSessionDraft
import com.awan.feature.aitasks.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AiTasksViewModel @Inject constructor(
    private val proposeFromText: ProposeTasksFromTextUseCase,
    private val proposeFromImage: ProposeTasksFromImageUseCase,
    private val createTasks: CreateTasksUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val readImage: ReadImageUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(AiTasksState())
    val state: StateFlow<AiTasksState> = _state.asStateFlow()

    private val _events = Channel<AiTasksEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nextProposalId = 0
    private var hasLoaded = false
    private var lastText = ""
    private var lastNote: String? = null

    private companion object {
        const val DEFAULT_SESSION_MINUTES = 60L
        const val DEFAULT_SESSION_HOUR = 9
    }

    fun onAction(action: AiTasksAction) {
        when (action) {
            is AiTasksAction.Load -> load(action.text, action.note, action.imageUri)
            AiTasksAction.Retry -> load(lastText, lastNote, _state.value.imageUri, force = true)
            is AiTasksAction.Removed -> removeProposal(action.id)
            AiTasksAction.UndoRemove -> undoRemove()
            AiTasksAction.ResetPlan -> resetPlan()
            is AiTasksAction.ToggleExpanded -> updateProposal(action.id) { it.copy(isExpanded = !it.isExpanded) }
            is AiTasksAction.TitleChanged -> updateDraft(action.id) { it.copy(title = action.title) }
            is AiTasksAction.DescriptionChanged ->
                updateDraft(action.id) { it.copy(description = action.description.takeIf(String::isNotBlank)) }

            is AiTasksAction.DurationPicked -> updateDraft(action.id) { it.copy(durationMinutes = action.minutes) }
            is AiTasksAction.CategoryPicked -> updateDraft(action.id) { it.copy(categoryId = action.categoryId) }
            is AiTasksAction.MandatoryToggled -> updateDraft(action.id) { it.copy(mandatory = !it.mandatory) }

            is AiTasksAction.SessionTapped -> openSessionPicker(action.id, action.sessionIndex)
            is AiTasksAction.SessionRemoved -> removeSession(action.id, action.sessionIndex)
            is AiTasksAction.SessionAdded -> addSession(action.id)
            is AiTasksAction.SessionDatePicked -> onSessionDatePicked(action.date)
            is AiTasksAction.SessionTimePicked -> onSessionTimePicked(action.minutesFromMidnight)
            AiTasksAction.SessionPickerDismissed -> _state.update { it.copy(sessionPicker = null) }

            AiTasksAction.Accept -> accept()
            AiTasksAction.BackRequested -> requestBack()
            AiTasksAction.DiscardConfirmed -> {
                _state.update { it.copy(showDiscardConfirm = false) }
                close(AiTasksEvent.Dismissed)
            }
            AiTasksAction.DiscardCancelled -> _state.update { it.copy(showDiscardConfirm = false) }
        }
    }

    /** Ignores a second call — the Root composable fires this once via `LaunchedEffect(Unit)`. */
    private fun load(text: String, note: String?, imageUri: String?, force: Boolean = false) {
        if (hasLoaded && !force) return
        hasLoaded = true
        lastText = text
        lastNote = note
        _state.update {
            it.copy(
                isLoading = true,
                imageUri = imageUri,
                errorMessage = null,
                acceptError = null,
                proposals = emptyList(),
                originalProposals = emptyList(),
                lastRemoved = null,
            )
        }
        viewModelScope.launch {
            coroutineScope {
                val categoriesDeferred = async { getCategories() }
                // Neither channel has room for both a title and a note, so whichever one isn't the
                // channel's own field rides along inside it — nothing typed is silently dropped.
                val proposalsResult = if (imageUri != null) {
                    fetchFromImage(imageUri, combineContext(text, note))
                } else {
                    proposeFromText(combineContext(text, note) ?: text)
                }
                val categories = when (val result = categoriesDeferred.await()) {
                    is Result.Success -> result.data
                    else -> emptyList()
                }

                when (proposalsResult) {
                    is Result.Success -> {
                        val proposals = proposalsResult.data.tasks.map { proposal -> proposal.toUi() }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                sourceSummary = proposalsResult.data.sourceSummary,
                                availableCategories = categories,
                                proposals = proposals,
                                originalProposals = proposals,
                            )
                        }
                    }

                    is Result.Error -> _state.update {
                        it.copy(
                            isLoading = false,
                            availableCategories = categories,
                            errorMessage = proposalsResult.error.toProposalErrorRes(),
                        )
                    }

                    Result.Loading -> Unit
                }
            }
        }
    }

    private suspend fun fetchFromImage(imageUri: String, note: String?) =
        when (val image = readImage(imageUri)) {
            is Result.Success -> proposeFromImage(image.data, note)
            is Result.Error -> Result.Error(image.error)
            Result.Loading -> Result.Loading
        }

    private fun TaskProposal.toUi(): ProposalUi =
        ProposalUi(id = nextProposalId++, draft = draft, sessions = sessions, reason = reason)

    private fun updateDraft(id: Int, transform: (TaskDraft) -> TaskDraft) =
        updateProposal(id) { it.copy(draft = transform(it.draft)) }

    /** Any edit clears the accept banner — the user is already acting on what it told them. */
    private fun updateProposal(id: Int, transform: (ProposalUi) -> ProposalUi) {
        _state.update { state ->
            state.copy(
                proposals = state.proposals.map { if (it.id == id) transform(it) else it },
                acceptError = null,
            )
        }
    }

    /**
     * Only the newest removal is held. A second X before the first is undone drops the older one:
     * the undo bar can only ever name one task, and offering "Undo" for a task the user can no
     * longer see named would restore something they'd stopped thinking about.
     */
    private fun removeProposal(id: Int) {
        val index = _state.value.proposals.indexOfFirst { it.id == id }
        if (index < 0) return
        _state.update { state ->
            state.copy(
                proposals = state.proposals.filterIndexed { i, _ -> i != index },
                lastRemoved = RemovedProposal(state.proposals[index], index),
                acceptError = null,
            )
        }
    }

    private fun undoRemove() {
        val removed = _state.value.lastRemoved ?: return
        _state.update { state ->
            val at = removed.index.coerceIn(0, state.proposals.size)
            state.copy(
                proposals = state.proposals.toMutableList().apply { add(at, removed.proposal) },
                lastRemoved = null,
            )
        }
    }

    private fun resetPlan() = _state.update {
        it.copy(proposals = it.originalProposals, lastRemoved = null, acceptError = null)
    }

    private fun removeSession(id: Int, index: Int) = updateProposal(id) { proposal ->
        proposal.copy(sessions = proposal.sessions.filterIndexed { i, _ -> i != index })
    }

    /** A fresh, user-owned session — never `isAiSuggested`, since Awan never proposed this one. */
    private fun addSession(id: Int) {
        val proposal = _state.value.proposals.firstOrNull { it.id == id } ?: return
        val start = LocalDate.now(clock).plusDays(1).atTime(DEFAULT_SESSION_HOUR, 0)
        val newIndex = proposal.sessions.size
        updateProposal(id) {
            it.copy(sessions = it.sessions + ProposedSession(start = start, end = start.plusMinutes(DEFAULT_SESSION_MINUTES)))
        }
        openSessionPicker(id, newIndex)
    }

    private fun openSessionPicker(id: Int, index: Int) {
        val exists = _state.value.proposals.firstOrNull { it.id == id }?.sessions?.getOrNull(index) != null
        if (!exists) return
        _state.update {
            it.copy(sessionPicker = SessionPickerTarget(proposalId = id, sessionIndex = index, step = PickerStep.DATE))
        }
    }

    private fun onSessionDatePicked(date: LocalDate) {
        val target = _state.value.sessionPicker ?: return
        _state.update { it.copy(sessionPicker = target.copy(step = PickerStep.TIME, pendingDate = date)) }
    }

    /**
     * Once the user has picked their own time, the badge that reads "Awan suggested this" no longer
     * applies — the choice is theirs now, not the availability-grounded guess Awan made.
     */
    private fun onSessionTimePicked(minutesFromMidnight: Int) {
        val target = _state.value.sessionPicker ?: return
        val date = target.pendingDate ?: LocalDate.now(clock)
        val newStart = date.atStartOfDay().plusMinutes(minutesFromMidnight.toLong())

        updateProposal(target.proposalId) { proposal ->
            val session = proposal.sessions.getOrNull(target.sessionIndex) ?: return@updateProposal proposal
            val duration = Duration.between(session.start, session.end)
                .takeIf { !it.isNegative && !it.isZero }
                ?: Duration.ofMinutes(DEFAULT_SESSION_MINUTES)
            val updated = session.copy(start = newStart, end = newStart.plus(duration), isAiSuggested = false)
            proposal.copy(
                sessions = proposal.sessions.mapIndexed { i, s -> if (i == target.sessionIndex) updated else s },
            )
        }
        _state.update { it.copy(sessionPicker = null) }
    }

    /**
     * The bulk create is atomic, so one blank title would reject every remaining task — it is stopped
     * here rather than sent. A proposal can arrive blank: `CreateTaskRequest.title` defaults to `""`
     * so a malformed entry degrades to one empty card instead of failing the whole decode.
     */
    private fun accept() {
        val current = _state.value
        if (current.isAccepting) return
        val selected = current.proposals
        if (selected.isEmpty()) return
        if (selected.any { it.draft.title.isBlank() }) {
            _state.update { it.copy(acceptError = R.string.ai_tasks_error_blank_title) }
            return
        }
        _state.update { it.copy(isAccepting = true, acceptError = null) }
        viewModelScope.launch {
            val drafts = selected.map { proposal ->
                TaskWithSessionsDraft(
                    task = proposal.draft,
                    sessions = proposal.sessions.map { it.toSessionDraft() },
                )
            }
            when (val result = createTasks(drafts)) {
                is Result.Success -> close(AiTasksEvent.TasksCreated(result.data.size))
                is Result.Error -> _state.update {
                    it.copy(isAccepting = false, acceptError = R.string.ai_tasks_error_accept_failed)
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun requestBack() {
        if (_state.value.isDirty) {
            _state.update { it.copy(showDiscardConfirm = true) }
        } else {
            close(AiTasksEvent.Dismissed)
        }
    }

    private fun close(event: AiTasksEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

/** Neither AI channel has room for both a headline and a note, so this folds them into one string. */
private fun combineContext(text: String, note: String?): String? =
    listOfNotNull(text.takeIf(String::isNotBlank), note?.takeIf(String::isNotBlank))
        .joinToString(separator = "\n")
        .takeIf(String::isNotBlank)

@StringRes
private fun AppError.toProposalErrorRes(): Int = when (this) {
    is AppError.Validation -> when (reason) {
        ValidationReason.TEXT_BLANK -> R.string.ai_tasks_error_text_blank
        ValidationReason.TEXT_TOO_LONG -> R.string.ai_tasks_error_text_too_long
        ValidationReason.IMAGE_TOO_LARGE -> R.string.ai_tasks_error_image_too_large
        ValidationReason.IMAGE_TYPE_UNSUPPORTED -> R.string.ai_tasks_error_unsupported_image
    }

    is AppError.Api -> when (code) {
        415 -> R.string.ai_tasks_error_unsupported_image
        400 -> R.string.ai_tasks_error_invalid_image
        422 -> R.string.ai_tasks_error_validation
        else -> R.string.ai_tasks_error_generic
    }

    AppError.Timeout -> R.string.ai_tasks_error_timeout
    is AppError.Server -> if (code == SERVICE_UNAVAILABLE) {
        R.string.ai_tasks_error_ai_unavailable
    } else {
        R.string.ai_tasks_error_generic
    }

    else -> R.string.ai_tasks_error_generic
}

private const val SERVICE_UNAVAILABLE = 503

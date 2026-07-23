package com.awan.feature.addtask.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.usecase.CreateTaskUseCase
import com.awan.app.core.domain.task.usecase.ParseTaskInputUseCase
import com.awan.app.core.domain.zone.usecase.GetZonesForDateUseCase
import com.awan.app.core.model.DayZone
import com.awan.feature.addtask.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val getZonesForDate: GetZonesForDateUseCase,
    private val createTask: CreateTaskUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(AddTaskState(today = LocalDate.now(clock)))
    val state: StateFlow<AddTaskState> = _state.asStateFlow()

    private val _events = Channel<AddTaskEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var zoneLookup: Job? = null

    private companion object {
        /** Roughly the length of SparkleBurst plus one mascot cheer cycle. */
        const val CELEBRATE_MILLIS = 900L
    }

    fun onAction(action: AddTaskAction) {
        when (action) {
            is AddTaskAction.ModeChanged -> _state.update { it.copy(mode = action.mode, errorMessage = null) }
            is AddTaskAction.InputChanged -> onInputChanged(action.input)
            is AddTaskAction.DescriptionChanged -> _state.update { it.copy(description = action.description) }
            AddTaskAction.MandatoryToggled -> _state.update { it.copy(mandatory = !it.mandatory) }
            AddTaskAction.Submit -> submit()
            AddTaskAction.Dismiss -> viewModelScope.launch { _events.send(AddTaskEvent.Dismissed) }
        }
    }

    private fun onInputChanged(input: String) {
        val parsed = parseTaskInput(input)
        _state.update { it.copy(input = input, parsed = parsed, errorMessage = null) }
        resolveZone(parsed.zoneToken, parsed.startAt?.toLocalDate())
    }

    /**
     * Zones are per-date, so the token can only be resolved once we know which day the task lands
     * on. A failed lookup is not an error the user needs to see — the chip just stays unresolved
     * and the task is created without a zone.
     */
    private fun resolveZone(token: String?, date: LocalDate?) {
        zoneLookup?.cancel()
        if (token == null) {
            _state.update { it.copy(resolvedZone = null, isResolvingZone = false) }
            return
        }
        _state.update { it.copy(isResolvingZone = true) }
        zoneLookup = viewModelScope.launch {
            val zones = when (val result = getZonesForDate(date ?: LocalDate.now(clock))) {
                is Result.Success -> result.data
                else -> emptyList()
            }
            _state.update { it.copy(resolvedZone = zones.matching(token), isResolvingZone = false) }
        }
    }

    private fun List<DayZone>.matching(token: String): DayZone? =
        firstOrNull { it.name.equals(token, ignoreCase = true) }
            ?: firstOrNull { it.name.startsWith(token, ignoreCase = true) }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (createTask(current.toDraft())) {
                // Hold the sheet open just long enough for Awan to cheer and the sparkles to fire.
                is Result.Success -> {
                    _state.update { it.copy(isSubmitting = false, isCelebrating = true) }
                    delay(CELEBRATE_MILLIS)
                    _state.value = AddTaskState(today = LocalDate.now(clock))
                    _events.send(AddTaskEvent.TaskCreated(current.parsed.title))
                }

                is Result.Error -> _state.update {
                    it.copy(isSubmitting = false, errorMessage = R.string.add_task_error_create_failed)
                }

                Result.Loading -> Unit
            }
        }
    }
}

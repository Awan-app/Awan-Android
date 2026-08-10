@file:Suppress("NewApi")

package com.awan.feature.home.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.R as DesignSystemR
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.usecase.GetWheelConfigUseCase
import com.awan.app.core.domain.gamification.usecase.ObserveGamificationProgressUseCase
import com.awan.app.core.domain.gamification.usecase.PublishWheelRewardUseCase
import com.awan.app.core.domain.gamification.usecase.RefreshGamificationProgressUseCase
import com.awan.app.core.domain.gamification.usecase.SpinWheelUseCase
import com.awan.feature.home.impl.R
import com.awan.app.core.designsystem.CategoryProgressSegment
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.designsystem.ScheduleCategory
import com.awan.app.core.designsystem.ScheduleSession
import com.awan.app.core.designsystem.ScheduleTask
import com.awan.app.core.designsystem.ScheduleZone
import com.awan.app.core.designsystem.TaskCategory
import com.awan.app.core.designsystem.TaskStatus
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.DaySession
import com.awan.app.core.domain.home.model.DayZone
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.domain.home.usecase.CompleteSessionUseCase
import com.awan.app.core.domain.home.usecase.GetDayScheduleUseCase
import com.awan.app.core.domain.home.usecase.GetUserProfileUseCase
import com.awan.app.core.domain.home.usecase.MoveSessionUseCase
import com.awan.app.core.domain.home.usecase.RefreshDayScheduleUseCase
import com.awan.app.core.domain.zones.usecase.RefreshZonesUseCase
import com.awan.app.core.domain.home.usecase.UncompleteSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import com.awan.app.core.domain.home.usecase.GetSessionDetailUseCase
import com.awan.app.core.domain.home.usecase.UpdateTaskDetailUseCase
import com.awan.app.core.domain.home.usecase.DeleteSessionUseCase
import com.awan.app.core.domain.home.usecase.DeleteTaskUseCase
import com.awan.feature.home.impl.ui.components.calculateDurationMinutes

/** Returned by a spin the user has already used up today. */
private const val DAILY_GIFT_ALREADY_CLAIMED = "DAILY_GIFT_ALREADY_CLAIMED"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDayScheduleUseCase: GetDayScheduleUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val uncompleteSessionUseCase: UncompleteSessionUseCase,
    private val moveSessionUseCase: MoveSessionUseCase,
    private val observeGamificationProgressUseCase: ObserveGamificationProgressUseCase,
    private val refreshGamificationProgressUseCase: RefreshGamificationProgressUseCase,
    private val getWheelConfigUseCase: GetWheelConfigUseCase,
    private val spinWheelUseCase: SpinWheelUseCase,
    private val publishWheelRewardUseCase: PublishWheelRewardUseCase,
    private val refreshDayScheduleUseCase: RefreshDayScheduleUseCase,
    private val refreshZonesUseCase: RefreshZonesUseCase,
    private val getSessionDetailUseCase: GetSessionDetailUseCase,
    private val updateTaskDetailUseCase: UpdateTaskDetailUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    // Still here only for session locking, which has no use case yet.
    private val homeRepository: HomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Held back from the reward bus until the wheel overlay closes. */
    private var pendingSpin: WheelSpinResult? = null

    init {
        updateCurrentTime()
        startClockTimer()
        loadUserProfile()
        observeGamificationProgress()
        loadWheelAvailability()
        loadScheduleForDate(LocalDate.now())
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            when (val result = getUserProfileUseCase()) {
                is Result.Success -> {
                    val user = result.data
                    val name = user.firstName.takeIf { it.isNotBlank() } ?: "User"
                    _uiState.update { state -> state.copy(userName = name) }
                }
                else -> Unit
            }
        }
    }

    /** Points and streak are server-owned; the badges mirror that state rather than tracking it. */
    private fun observeGamificationProgress() {
        viewModelScope.launch {
            observeGamificationProgressUseCase().collect { progress ->
                _uiState.update { state ->
                    state.copy(
                        streakCount = progress.streak,
                        pointsCount = progress.points,
                    )
                }
            }
        }
        viewModelScope.launch { refreshGamificationProgressUseCase() }
    }

    private fun loadWheelAvailability() {
        viewModelScope.launch {
            val result = getWheelConfigUseCase()
            if (result is Result.Success) {
                _uiState.update { state ->
                    state.copy(
                        hasFreeSpin = !result.data.claimedToday,
                        wheelSegments = result.data.segments,
                    )
                }
            }
        }
    }

    fun openWheel() = _uiState.update { it.copy(isWheelOpen = true) }

    fun spinWheel() {
        if (_uiState.value.isSpinning || pendingSpin != null) return
        // Clears any message left by a previous failed attempt, so a retry does not spin under the
        // error it is retrying.
        _uiState.update { it.copy(isSpinning = true, wheelResult = null) }

        viewModelScope.launch {
            when (val result = spinWheelUseCase()) {
                is Result.Success -> {
                    pendingSpin = result.data
                    _uiState.update { state ->
                        state.copy(
                            isSpinning = false,
                            hasFreeSpin = false,
                            landingSegmentId = result.data.segmentId,
                            wheelResult = result.data.toResultText(),
                        )
                    }
                }

                is Result.Error -> {
                    // A 409 is the normal answer to a double tap: correct the state, don't alarm.
                    val alreadyClaimed = (result.error as? AppError.Api)
                        ?.errorCode == DAILY_GIFT_ALREADY_CLAIMED
                    _uiState.update { state ->
                        state.copy(
                            isSpinning = false,
                            hasFreeSpin = if (alreadyClaimed) false else state.hasFreeSpin,
                            wheelResult = if (alreadyClaimed) {
                                UiText.StringResource(DesignSystemR.string.ds_wheel_claimed)
                            } else {
                                result.error.toReadableMessage()
                            },
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    /**
     * The payout is only released once the wheel is off screen — stars flying behind a full-screen
     * wheel would be celebrating something the user cannot see.
     */
    fun closeWheel() {
        _uiState.update {
            it.copy(isWheelOpen = false, landingSegmentId = null, wheelResult = null)
        }
        val spin = pendingSpin ?: return
        pendingSpin = null
        viewModelScope.launch { publishWheelRewardUseCase(spin) }
    }

    private fun WheelSpinResult.toResultText(): UiText {
        val wonItem = item
        return if (wonItem != null) {
            UiText.StringResource(DesignSystemR.string.ds_wheel_won_item, wonItem.name)
        } else {
            UiText.StringResource(DesignSystemR.string.ds_wheel_won_coins, coins)
        }
    }


    private var scheduleJob: Job? = null

    private fun loadScheduleForDate(date: LocalDate) {
        scheduleJob?.cancel()

        val today = LocalDate.now()
        val isToday = date == today
        val isPastDate = date.isBefore(today)

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                errorMessage = null,
                selectedDate = date,
                isToday = isToday,
                isPastDate = isPastDate,
                selectedDateText = formatSelectedDate(date),
            )
        }

        scheduleJob = viewModelScope.launch {
            getDayScheduleUseCase(date).collect { result ->
                when (result) {
                    is Result.Success -> applySchedule(result.data, isToday)
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, errorMessage = result.error.toReadableMessage()) }
                    is Result.Loading -> Unit
                }
            }
        }
        refreshFromServer(date)
    }

    /**
     * Room already rendered above; this only refills it. A failure is deliberately silent — the day
     * on screen is real data, and replacing it with an error because a background pull failed is
     * worse than being briefly stale. Offline is the common case here, not an incident.
     */
    private fun refreshFromServer(date: LocalDate) {
        viewModelScope.launch { refreshDayScheduleUseCase(date) }
        viewModelScope.launch { refreshZonesUseCase() }
    }

    private fun applySchedule(schedule: DaySchedule, isToday: Boolean) {
        val zones = schedule.zones.map { it.toUiZone() }.toMutableList()
        val zoneById = zones.associateBy { it.id }.toMutableMap()

        schedule.sessions
            .filter { it.zoneId != null && !zoneById.containsKey(it.zoneId) }
            .groupBy { it.zoneId!! }
            .forEach { (orphanZoneId, orphanSessions) ->
                val startMin = orphanSessions.minOf { it.startMinutes }
                val endMin   = orphanSessions.maxOf { it.startMinutes + it.durationMinutes }
                val category = resolveCategory(orphanSessions.first().categoryName ?: "")
                val synthetic = ScheduleZone(
                    id = orphanZoneId,
                    categoryId = orphanZoneId,
                    category = category,
                    startMinutes = startMin,
                    endMinutes = endMin,
                    isCollapsed = false,
                )
                zones.add(synthetic)
                zoneById[orphanZoneId] = synthetic
            }

        if (zones.isEmpty() && schedule.sessions.isNotEmpty()) {
            val startMin = schedule.sessions.minOf { it.startMinutes }
            val endMin   = schedule.sessions.maxOf { it.startMinutes + it.durationMinutes }
            val fallbackZone = ScheduleZone(
                id = "zone_default",
                categoryId = "personal",
                category = TaskCategory.Personal,
                startMinutes = startMin,
                endMinutes = endMin,
                isCollapsed = false,
            )
            zones.add(fallbackZone)
            zoneById[fallbackZone.id] = fallbackZone
        }

        val sessions = schedule.sessions.mapNotNull { it.toUiSession(zoneById) }

        val completedCount = sessions.count { it.status == TaskStatus.Completed }
        val (completedHours, totalHours) = calculateSessionHours(sessions)
        val subtitle = buildSubtitle(sessions.size, isToday)
        val progressSegments = buildProgressSegments(sessions)

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                errorMessage = null,
                zones = zones,
                sessions = sessions,
                subtitleText = subtitle,
                completedSessionsCount = completedCount,
                completedHours = completedHours,
                totalHours = totalHours,
                progressSegments = progressSegments,
                mascotExpression = MascotExpression.Idle,
            )
        }
    }





    fun previousDay() = loadScheduleForDate(_uiState.value.selectedDate.minusDays(1))
    fun nextDay()     = loadScheduleForDate(_uiState.value.selectedDate.plusDays(1))
    fun selectDate(date: LocalDate) = loadScheduleForDate(date)
    fun selectToday() = loadScheduleForDate(LocalDate.now())

    fun retryLoad() = loadScheduleForDate(_uiState.value.selectedDate)


    fun toggleZoneCollapse(zoneId: String) {
        _uiState.update { state ->
            state.copy(
                zones = state.zones.map { zone ->
                    if (zone.id == zoneId) zone.copy(isCollapsed = !zone.isCollapsed) else zone
                },
            )
        }
    }

    fun addSessionToZone(zoneId: String) {
        _uiState.update { state ->
            val matchedZone = state.zones.find { it.id == zoneId } ?: return@update state
            val zoneSessions = state.sessions.filter { it.zoneId == zoneId }
            val lastEnd = zoneSessions.maxOfOrNull { it.startMinutes + it.durationMinutes }
                ?: matchedZone.startMinutes
            val newStart = if (lastEnd < matchedZone.endMinutes) lastEnd else matchedZone.startMinutes

            val titles = listOf("New Task Session", "Practice Exercise", "Deep Focus", "Review Notes")
            val newSession = ScheduleSession(
                id = "s_${System.currentTimeMillis()}",
                zoneId = zoneId,
                taskId = "t_local_${System.currentTimeMillis()}",
                taskTitle = titles[state.sessions.size % titles.size],
                startMinutes = newStart,
                durationMinutes = 30,
                category = matchedZone.category,
                status = TaskStatus.Pending,
                points = 20,
            )
            val updated = state.sessions + newSession
            val (completedHours, totalHours) = calculateSessionHours(updated)
            state.copy(
                sessions = updated,
                subtitleText = buildSubtitle(updated.size, state.isToday),
                completedHours = completedHours,
                totalHours = totalHours,
                progressSegments = buildProgressSegments(updated),
            )
        }
    }

    fun toggleSessionStatus(sessionId: String) {
        var targetSession: ScheduleSession? = null
        var isCompleting = false

        _uiState.update { state ->
            val target = state.sessions.find { it.id == sessionId } ?: return@update state

            isCompleting = target.status != TaskStatus.Completed
            targetSession = target

            val updated = state.sessions.map { session ->
                if (session.id != sessionId) return@map session
                if (isCompleting) {
                    session.copy(status = TaskStatus.Completed)
                } else {
                    val restoredStatus = if (session.isFixed || session.status is TaskStatus.Fixed) {
                        TaskStatus.Fixed
                    } else {
                        TaskStatus.Pending
                    }
                    session.copy(status = restoredStatus)
                }
            }

            val completedCount = updated.count { it.status == TaskStatus.Completed }
            val (completedHours, totalHours) = calculateSessionHours(updated)

            state.copy(
                sessions = updated,
                completedSessionsCount = completedCount,
                completedHours = completedHours,
                totalHours = totalHours,
                progressSegments = buildProgressSegments(updated),
            )
        }

        val sessionToSync = targetSession ?: return
        val previousStatus = sessionToSync.status

        viewModelScope.launch {
            val result = if (isCompleting) {
                // The reward reaches the celebration through the data layer's bus, and carries the
                // new balance with it — re-reading progress here would race the star-by-star
                // count-up and snap the badge straight to the final total.
                completeSessionUseCase(sessionToSync.id)
            } else {
                uncompleteSessionUseCase(sessionToSync.id).also {
                    // Points are not clawed back, but the contract is silent on the streak, so the
                    // only way the badges stay honest is to ask.
                    if (it is Result.Success) refreshGamificationProgressUseCase()
                }
            }

            if (result is Result.Error) restoreSessionStatus(sessionId, previousStatus)
        }
    }

    /** Puts the optimistic flip back when the server refused it, so the tick cannot lie. */
    private fun restoreSessionStatus(sessionId: String, previousStatus: TaskStatus) {
        _uiState.update { state ->
            val updated = state.sessions.map { session ->
                if (session.id == sessionId) session.copy(status = previousStatus) else session
            }
            val (completedHours, totalHours) = calculateSessionHours(updated)
            state.copy(
                sessions = updated,
                completedSessionsCount = updated.count { it.status == TaskStatus.Completed },
                completedHours = completedHours,
                totalHours = totalHours,
                progressSegments = buildProgressSegments(updated),
            )
        }
    }

    fun moveSession(sessionId: String, newStartMinutes: Int) {
        var movedSession: ScheduleSession? = null

        _uiState.update { state ->
            val updated = state.sessions.map { session ->
                if (session.id == sessionId && !session.isFixed && session.status != TaskStatus.Fixed) {
                    val maxAllowedStart = (24 * 60 - session.durationMinutes).coerceAtLeast(0)
                    val clampedStartMinutes = newStartMinutes.coerceIn(0, maxAllowedStart)
                    val matchedZone = state.zones.find { zone ->
                        clampedStartMinutes in zone.startMinutes..zone.endMinutes
                    }
                    val updatedSession = session.copy(
                        startMinutes = clampedStartMinutes,
                        durationMinutes = session.durationMinutes,
                        zoneId = matchedZone?.id ?: session.zoneId,
                        category = session.category,
                    )
                    movedSession = updatedSession
                    updatedSession
                } else session
            }
            val sorted = updated.sortedBy { it.startMinutes }
            val (completedHours, totalHours) = calculateSessionHours(sorted)
            state.copy(
                sessions = sorted,
                completedHours = completedHours,
                totalHours = totalHours,
                progressSegments = buildProgressSegments(sorted),
            )
        }

        val sessionToSync = movedSession ?: return
        val date = _uiState.value.selectedDate
        val startTime = date.atStartOfDay().plusMinutes(sessionToSync.startMinutes.toLong())
        val endTime = startTime.plusMinutes(sessionToSync.durationMinutes.toLong())
        val dtFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val startIso = startTime.format(dtFormatter)
        val endIso = endTime.format(dtFormatter)

        viewModelScope.launch {
            // Times only. A move must never touch status — that is what the dedicated
            // complete/uncomplete endpoints are for.
            moveSessionUseCase(
                sessionId = sessionToSync.id,
                startIso = startIso,
                endIso = endIso,
            )
        }
    }

    fun reorderSessionsInZone(zoneId: String, fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value
        val zone = currentState.zones.find { it.id == zoneId } ?: return
        val zoneSessions = currentState.sessions.filter { it.zoneId == zoneId }.sortedBy { it.startMinutes }.toMutableList()
        if (fromIndex !in zoneSessions.indices || toIndex !in zoneSessions.indices || fromIndex == toIndex) {
            return
        }

        val movedItem = zoneSessions.removeAt(fromIndex)
        zoneSessions.add(toIndex, movedItem)

        var currentStart = zone.startHour * 60
        val resequencedZoneSessions = zoneSessions.map { session ->
            val updated = session.copy(startMinutes = currentStart)
            currentStart += session.durationMinutes
            updated
        }

        val date = currentState.selectedDate
        val dtFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

        viewModelScope.launch {
            for (session in resequencedZoneSessions) {
                val startTime = date.atStartOfDay().plusMinutes(session.startMinutes.toLong())
                val endTime = startTime.plusMinutes(session.durationMinutes.toLong())
                val startIso = startTime.format(dtFormatter)
                val endIso = endTime.format(dtFormatter)

                // Reordering only re-times a session; its status is left exactly as it was.
                val result = moveSessionUseCase(
                    sessionId = session.id,
                    startIso = startIso,
                    endIso = endIso,
                )
                if (result is Result.Error) {
                    _uiState.update { it.copy(errorMessage = result.error.toReadableMessage()) }
                    loadScheduleForDate(date)
                    break
                }
            }
            loadScheduleForDate(date)
        }
    }


    private fun calculateSessionHours(sessions: List<ScheduleSession>): Pair<Double, Double> {
        val completedMins = sessions.filter { it.status == TaskStatus.Completed }.sumOf { it.durationMinutes }
        val totalMins = sessions.sumOf { it.durationMinutes }
        return Pair(completedMins / 60.0, totalMins / 60.0)
    }

    fun fixConflict()     = _uiState.update { it.copy(hasConflict = false) }
    fun dismissConflict() = _uiState.update { it.copy(hasConflict = false) }


    fun onSessionClicked(sessionId: String) {
        _uiState.update { state ->
            state.copy(
                selectedSessionDetailState = SessionDetailDialogState(
                    sessionId = sessionId,
                    isLoading = true,
                )
            )
        }
        viewModelScope.launch {
            when (val result = getSessionDetailUseCase(sessionId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        val taskId = result.data.task.id
                        val taskSessions = state.sessions.filter { it.taskId == taskId }.map { s ->
                            val startMins = s.startMinutes
                            val endMins = s.startMinutes + s.durationMinutes
                            val startStr = com.awan.app.core.designsystem.formatTime(startMins)
                            val endStr = com.awan.app.core.designsystem.formatTime(endMins)
                            val isDone = s.status == com.awan.app.core.designsystem.TaskStatus.Completed
                            com.awan.app.core.model.SessionDetailInfo(
                                id = s.id,
                                start = startStr,
                                end = endStr,
                                status = if (isDone) "COMPLETED" else "SCHEDULED",
                                locked = s.isFixed,
                                zoneId = s.zoneId,
                                taskId = taskId,
                            )
                        }
                        val enrichedDetail = result.data.copy(relatedSessions = taskSessions)
                        state.copy(
                            selectedSessionDetailState = state.selectedSessionDetailState?.copy(
                                isLoading = false,
                                detail = enrichedDetail,
                                errorMessage = null,
                            )
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            selectedSessionDetailState = state.selectedSessionDetailState?.copy(
                                isLoading = false,
                                errorMessage = result.error.toReadableMessage(),
                            )
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun dismissSessionDetail() {
        _uiState.update { state ->
            state.copy(selectedSessionDetailState = null)
        }
    }

    fun retryLoadSessionDetail() {
        val currentSessionId = uiState.value.selectedSessionDetailState?.sessionId ?: return
        onSessionClicked(currentSessionId)
    }

    fun toggleSessionStatusFromDialog() {
        val currentDialogState = uiState.value.selectedSessionDetailState ?: return
        val currentDetail = currentDialogState.detail ?: return
        val sessionId = currentDialogState.sessionId

        val isCurrentlyCompleted = currentDetail.session.status.uppercase() == "COMPLETED"
        val newStatusStr = if (isCurrentlyCompleted) "SCHEDULED" else "COMPLETED"

        toggleSessionStatus(sessionId)

        _uiState.update { state ->
            val updatedDetail = state.selectedSessionDetailState?.detail?.let { detail ->
                detail.copy(
                    session = detail.session.copy(status = newStatusStr),
                    task = detail.task.copy(status = newStatusStr),
                )
            }
            state.copy(
                selectedSessionDetailState = state.selectedSessionDetailState?.copy(
                    detail = updatedDetail
                )
            )
        }
    }

    fun toggleSessionLockFromDialog() {
        val currentDialogState = uiState.value.selectedSessionDetailState ?: return
        val currentDetail = currentDialogState.detail ?: return
        val sessionId = currentDialogState.sessionId

        val newLocked = !currentDetail.session.locked

        _uiState.update { state ->
            val updatedSessions = state.sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(isFixed = newLocked)
                } else session
            }
            val updatedDetail = state.selectedSessionDetailState?.detail?.let { detail ->
                detail.copy(session = detail.session.copy(locked = newLocked))
            }
            state.copy(
                sessions = updatedSessions,
                selectedSessionDetailState = state.selectedSessionDetailState?.copy(
                    detail = updatedDetail
                )
            )
        }

        viewModelScope.launch {
            val result = homeRepository.updateSessionLock(sessionId, newLocked)
            if (result is Result.Error) {
                _uiState.update { state ->
                    val revertedSessions = state.sessions.map { session ->
                        if (session.id == sessionId) {
                            session.copy(isFixed = !newLocked)
                        } else session
                    }
                    val revertedDetail = state.selectedSessionDetailState?.detail?.let { detail ->
                        detail.copy(session = detail.session.copy(locked = !newLocked))
                    }
                    state.copy(
                        sessions = revertedSessions,
                        selectedSessionDetailState = state.selectedSessionDetailState?.copy(
                            detail = revertedDetail
                        )
                    )
                }
            }
        }
    }

    fun startEditingSessionDetail() {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            val detail = dialogState.detail ?: return@update state
            val duration = calculateDurationMinutes(detail.session.start, detail.session.end)
                ?: detail.task.estimatedDuration ?: 30

            state.copy(
                selectedSessionDetailState = dialogState.copy(
                    isEditing = true,
                    editTitle = detail.task.title,
                    editDescription = detail.task.description ?: "",
                    editDurationMinutes = duration,
                )
            )
        }
    }

    fun cancelEditingSessionDetail() {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            state.copy(
                selectedSessionDetailState = dialogState.copy(isEditing = false)
            )
        }
    }

    fun onEditTitleChanged(newTitle: String) {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            state.copy(
                selectedSessionDetailState = dialogState.copy(editTitle = newTitle)
            )
        }
    }

    fun onEditDescriptionChanged(newDesc: String) {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            state.copy(
                selectedSessionDetailState = dialogState.copy(editDescription = newDesc)
            )
        }
    }

    fun onEditDurationChanged(newDuration: Int) {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            state.copy(
                selectedSessionDetailState = dialogState.copy(editDurationMinutes = newDuration)
            )
        }
    }

    fun saveSessionDetailEdits() {
        val dialogState = uiState.value.selectedSessionDetailState ?: return
        val detail = dialogState.detail ?: return
        val sessionId = detail.session.id
        val taskId = detail.task.id
        val newDuration = dialogState.editDurationMinutes

        _uiState.update { state ->
            state.copy(
                selectedSessionDetailState = state.selectedSessionDetailState?.copy(isSaving = true)
            )
        }

        viewModelScope.launch {
            // 1. Update task title & description
            val taskResult = updateTaskDetailUseCase(
                taskId = taskId,
                title = dialogState.editTitle,
                description = dialogState.editDescription,
            )

            // 2. Update specific session end time if duration changed
            val currentDuration = calculateDurationMinutes(detail.session.start, detail.session.end) ?: 30
            var newEndIso = detail.session.end
            if (newDuration != currentDuration) {
                val calculatedEnd = com.awan.feature.home.impl.ui.components.calculateEndIso(detail.session.start, newDuration)
                if (calculatedEnd != null) {
                    newEndIso = calculatedEnd
                    // Changing the duration only moves the session's end; its status is untouched.
                    moveSessionUseCase(
                        sessionId = sessionId,
                        startIso = detail.session.start,
                        endIso = calculatedEnd,
                    )
                }
            }

            if (taskResult is Result.Success) {
                _uiState.update { state ->
                    val updatedDetail = state.selectedSessionDetailState?.detail?.let { d ->
                        val updatedSession = d.session.copy(end = newEndIso)
                        val updatedRelatedSessions = d.relatedSessions.map { s ->
                            if (s.id == sessionId) s.copy(end = newEndIso) else s
                        }
                        d.copy(
                            session = updatedSession,
                            task = d.task.copy(
                                title = dialogState.editTitle,
                                description = dialogState.editDescription,
                            ),
                            relatedSessions = updatedRelatedSessions,
                        )
                    }

                    // Update local schedule sessions: durationMinutes ONLY for this specific session!
                    val updatedSessions = state.sessions.map { s ->
                        if (s.id == sessionId) {
                            s.copy(
                                taskTitle = dialogState.editTitle,
                                durationMinutes = newDuration,
                            )
                        } else if (s.taskId == taskId) {
                            s.copy(taskTitle = dialogState.editTitle)
                        } else s
                    }

                    state.copy(
                        sessions = updatedSessions,
                        selectedSessionDetailState = state.selectedSessionDetailState?.copy(
                            isEditing = false,
                            isSaving = false,
                            detail = updatedDetail,
                        )
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        selectedSessionDetailState = state.selectedSessionDetailState?.copy(isSaving = false)
                    )
                }
            }
        }
    }

    fun requestDeleteSession() {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            state.copy(
                selectedSessionDetailState = dialogState.copy(
                    showDeleteConfirmDialog = true,
                    deleteTargetType = DeleteTargetType.SESSION,
                )
            )
        }
    }

    fun selectDeleteTargetType(type: DeleteTargetType) {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            state.copy(
                selectedSessionDetailState = dialogState.copy(deleteTargetType = type)
            )
        }
    }

    fun dismissDeleteConfirmDialog() {
        _uiState.update { state ->
            val dialogState = state.selectedSessionDetailState ?: return@update state
            state.copy(
                selectedSessionDetailState = dialogState.copy(
                    showDeleteConfirmDialog = false,
                    isDeleting = false,
                )
            )
        }
    }

    fun confirmDeleteAction() {
        val dialogState = uiState.value.selectedSessionDetailState ?: return
        val detail = dialogState.detail ?: return
        val sessionId = detail.session.id
        val taskId = detail.task.id
        val deleteType = dialogState.deleteTargetType

        _uiState.update { state ->
            state.copy(
                selectedSessionDetailState = state.selectedSessionDetailState?.copy(isDeleting = true)
            )
        }

        viewModelScope.launch {
            val result = if (deleteType == DeleteTargetType.SESSION) {
                deleteSessionUseCase(sessionId)
            } else {
                deleteTaskUseCase(taskId)
            }

            if (result is Result.Success) {
                _uiState.update { state ->
                    val updatedSessions = if (deleteType == DeleteTargetType.SESSION) {
                        state.sessions.filterNot { it.id == sessionId }
                    } else {
                        state.sessions.filterNot { it.taskId == taskId }
                    }
                    state.copy(
                        sessions = updatedSessions,
                        selectedSessionDetailState = null,
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        selectedSessionDetailState = state.selectedSessionDetailState?.copy(isDeleting = false)
                    )
                }
            }
        }
    }

    private fun updateCurrentTime() {
        val cal = Calendar.getInstance()
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val greeting = when (hour24) {
            in 4..11 -> UiText.StringResource(R.string.home_greeting_morning)
            in 12..17 -> UiText.StringResource(R.string.home_greeting_afternoon)
            else -> UiText.StringResource(R.string.home_greeting_evening)
        }
        val currentMins = hour24 * 60 + minute
        val formattedTime = com.awan.app.core.designsystem.formatTime(currentMins) + " ."
        _uiState.update { state ->
            state.copy(
                greetingPrefix = greeting,
                currentTimeFormatted = formattedTime,
                currentTimeMinutes   = currentMins,
            )
        }
    }

    private fun startClockTimer() {
        viewModelScope.launch {
            while (isActive) {
                updateCurrentTime()
                delay(30_000.milliseconds)
            }
        }
    }


    private fun buildSubtitle(sessionCount: Int, isToday: Boolean): UiText =
        UiText.StringResource(R.string.home_subtitle_scheduled, sessionCount)

    private fun com.awan.app.core.common.error.AppError.toReadableMessage(): UiText = when (this) {
        is com.awan.app.core.common.error.AppError.Network    -> UiText.StringResource(R.string.home_error_network)
        is com.awan.app.core.common.error.AppError.Timeout    -> UiText.StringResource(R.string.home_error_timeout)
        is com.awan.app.core.common.error.AppError.Unauthorized -> UiText.StringResource(R.string.home_error_unauthorized)
        is com.awan.app.core.common.error.AppError.Server     -> UiText.StringResource(R.string.home_error_server, code)
        is com.awan.app.core.common.error.AppError.Api        -> body?.takeIf { it.isNotBlank() }?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.home_error_something_went_wrong)
        is com.awan.app.core.common.error.AppError.Serialization -> UiText.StringResource(R.string.home_error_serialization)
        is com.awan.app.core.common.error.AppError.Unknown    -> UiText.StringResource(R.string.home_error_unknown)
        else -> UiText.StringResource(R.string.home_error_something_went_wrong)
    }
}

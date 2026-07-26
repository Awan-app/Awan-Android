@file:Suppress("NewApi")

package com.awan.feature.home.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.awan.app.core.common.result.Result
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
import com.awan.app.core.domain.home.model.SessionStatus
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.domain.home.usecase.GetDayScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds



@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDayScheduleUseCase: GetDayScheduleUseCase,
    private val homeRepository: HomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        updateCurrentTime()
        startClockTimer()
        loadUserProfile()
        loadScheduleForDate(LocalDate.now())
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            when (val result = homeRepository.getUserProfile()) {
                is Result.Success -> {
                    val user = result.data
                    val name = user.firstName.takeIf { it.isNotBlank() } ?: "User"
                    _uiState.update { state ->
                        state.copy(
                            userName = name,
                            streakCount = user.streak,
                            pointsCount = user.points,
                        )
                    }
                }
                else -> Unit
            }
        }
    }


    private fun loadScheduleForDate(date: LocalDate) {
        viewModelScope.launch {
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

            when (val result = getDayScheduleUseCase(date)) {
                is Result.Success -> applySchedule(result.data, isToday)
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, errorMessage = result.error.toReadableMessage()) }
                is Result.Loading -> Unit // not emitted by suspend use case
            }
        }
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
                    startHour = startMin / 60,
                    endHour = ceilHour(endMin),
                    isCollapsed = false,
                )
                zones.add(synthetic)
                zoneById[orphanZoneId] = synthetic
            }

        val sessions = schedule.sessions.mapNotNull { it.toUiSession(zoneById) }

        val completedCount = sessions.count { it.status == TaskStatus.Completed }
        val (completedHours, totalHours) = calculateSessionHours(sessions)
        val subtitle = buildString {
            append("Clear skies — ${sessions.size} sessions scheduled")
            if (isToday) append(" today")
        }
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
                ?: (matchedZone.startHour * 60)
            val newStart = if (lastEnd < matchedZone.endHour * 60) lastEnd else matchedZone.startHour * 60

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
            val sessionPoints = target.points ?: ((target.durationMinutes / 10).coerceAtLeast(1) * 10)

            isCompleting = target.status != TaskStatus.Completed
            targetSession = target

            val updated = state.sessions.map { session ->
                if (session.id != sessionId) return@map session
                if (isCompleting) {
                    session.copy(status = TaskStatus.Completed, points = sessionPoints)
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
            val newPointsCount = if (isCompleting) {
                state.pointsCount + sessionPoints
            } else {
                (state.pointsCount - sessionPoints).coerceAtLeast(0)
            }

            state.copy(
                sessions = updated,
                completedSessionsCount = completedCount,
                completedHours = completedHours,
                totalHours = totalHours,
                pointsCount = newPointsCount,
                progressSegments = buildProgressSegments(updated),
            )
        }

        val sessionToSync = targetSession ?: return
        val sessionStatusEnum = if (isCompleting) {
            SessionStatus.COMPLETED
        } else {
            SessionStatus.SCHEDULED
        }

        val date = _uiState.value.selectedDate
        val startTime = date.atStartOfDay().plusMinutes(sessionToSync.startMinutes.toLong())
        val endTime = startTime.plusMinutes(sessionToSync.durationMinutes.toLong())
        val dtFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val startIso = startTime.format(dtFormatter)
        val endIso = endTime.format(dtFormatter)

        viewModelScope.launch {
            homeRepository.updateSessionStatus(
                sessionId = sessionToSync.id,
                status = sessionStatusEnum,
                startIso = startIso,
                endIso = endIso,
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
                        clampedStartMinutes in (zone.startHour * 60)..(zone.endHour * 60)
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
            homeRepository.updateSessionStatus(
                sessionId = sessionToSync.id,
                status = if (sessionToSync.status == TaskStatus.Completed) {
                    SessionStatus.COMPLETED
                } else {
                    SessionStatus.SCHEDULED
                },
                startIso = startIso,
                endIso = endIso,
            )
        }
    }

    fun reorderSessionsInZone(zoneId: String, fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val zone = state.zones.find { it.id == zoneId } ?: return@update state
            val zoneSessions = state.sessions.filter { it.zoneId == zoneId }.sortedBy { it.startMinutes }.toMutableList()
            if (fromIndex !in zoneSessions.indices || toIndex !in zoneSessions.indices || fromIndex == toIndex) {
                return@update state
            }

            val movedItem = zoneSessions.removeAt(fromIndex)
            zoneSessions.add(toIndex, movedItem)

            var currentStart = zone.startHour * 60
            val resequencedZoneSessions = zoneSessions.map { session ->
                val updated = session.copy(startMinutes = currentStart)
                currentStart += session.durationMinutes
                updated
            }

            val resequencedMap = resequencedZoneSessions.associateBy { it.id }
            val updatedSessions = state.sessions.map { session ->
                resequencedMap[session.id] ?: session
            }.sortedBy { it.startMinutes }

            val (completedHours, totalHours) = calculateSessionHours(updatedSessions)
            state.copy(
                sessions = updatedSessions,
                completedHours = completedHours,
                totalHours = totalHours,
                progressSegments = buildProgressSegments(updatedSessions),
            )
        }
    }

    private fun calculateSessionHours(sessions: List<ScheduleSession>): Pair<Double, Double> {
        val completedMins = sessions.filter { it.status == TaskStatus.Completed }.sumOf { it.durationMinutes }
        val totalMins = sessions.sumOf { it.durationMinutes }
        return Pair(completedMins / 60.0, totalMins / 60.0)
    }

    fun fixConflict()     = _uiState.update { it.copy(hasConflict = false) }
    fun dismissConflict() = _uiState.update { it.copy(hasConflict = false) }


    private fun updateCurrentTime() {
        val cal = Calendar.getInstance()
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val amPm   = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0  -> 12
            hour24 > 12  -> hour24 - 12
            else         -> hour24
        }
        val greeting = when (hour24) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        _uiState.update { state ->
            state.copy(
                greetingPrefix = greeting,
                currentTimeFormatted = String.format(Locale.US, "%d:%02d %s .", hour12, minute, amPm),
                currentTimeMinutes   = hour24 * 60 + minute,
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


    private fun buildSubtitle(sessionCount: Int, isToday: Boolean) =
        "Clear skies — $sessionCount sessions scheduled${if (isToday) " today" else ""}"

    private fun com.awan.app.core.common.error.AppError.toReadableMessage(): String = when (this) {
        is com.awan.app.core.common.error.AppError.Network    -> "No internet connection. Check your network and retry."
        is com.awan.app.core.common.error.AppError.Timeout    -> "Request timed out. Please try again."
        is com.awan.app.core.common.error.AppError.Unauthorized -> "Session expired. Please log in again."
        is com.awan.app.core.common.error.AppError.Server     -> "Server error ($code). Please try again later."
        is com.awan.app.core.common.error.AppError.Api        -> body?.takeIf { it.isNotBlank() } ?: "Something went wrong."
        is com.awan.app.core.common.error.AppError.Serialization -> "Unexpected server response. Please update the app."
        is com.awan.app.core.common.error.AppError.Unknown    -> "An unexpected error occurred."
    }
}

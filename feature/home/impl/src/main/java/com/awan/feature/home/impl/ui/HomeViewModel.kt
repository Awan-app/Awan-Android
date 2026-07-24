package com.awan.feature.home.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.designsystem.CategoryProgressSegment
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.designsystem.ScheduleCategory
import com.awan.app.core.designsystem.ScheduleSession
import com.awan.app.core.designsystem.ScheduleTask
import com.awan.app.core.designsystem.ScheduleZone
import com.awan.app.core.designsystem.TaskCategory
import com.awan.app.core.designsystem.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class DateScheduleData(
    val zones: List<ScheduleZone>,
    val sessions: List<ScheduleSession>,
    val hasConflict: Boolean = false,
    val conflictMessage: String = "",
)

private fun formatSelectedDate(date: LocalDate): String {
    val today = LocalDate.now()
    val pattern = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    return if (date == today) {
        "Today · ${date.format(pattern)}"
    } else {
        date.format(pattern)
    }
}

data class HomeUiState(
    val userName: String = "Sobky",
    val streakCount: Int = 6,
    val pointsCount: Int = 1285,
    val mascotExpression: MascotExpression = MascotExpression.Greet,
    val subtitleText: String = "Clear skies — 8 sessions scheduled today",
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val isPastDate: Boolean = false,
    val selectedDateText: String = formatSelectedDate(LocalDate.now()),
    val dayNumber: Int = 15,
    val totalTasksCount: Int = 3,
    val completedSessionsCount: Int = 1,
    val scheduledHoursText: String = "3 tasks · 8 sessions scheduled",
    val categories: List<ScheduleCategory> = listOf(
        ScheduleCategory("cat_study", "Study", "📚", TaskCategory.Study),
        ScheduleCategory("cat_work", "Work", "💼", TaskCategory.Work),
        ScheduleCategory("cat_play", "Play", "🎮", TaskCategory.Play),
        ScheduleCategory("cat_personal", "Personal", "✨", TaskCategory.Personal),
    ),
    val zones: List<ScheduleZone> = emptyList(),
    val tasks: List<ScheduleTask> = listOf(
        ScheduleTask("task_1", "Read Chapter 5", "cat_study", defaultPoints = 25),
        ScheduleTask("task_2", "Solve Algorithms", "cat_study", defaultPoints = 30),
        ScheduleTask("task_3", "Prepare Presentation", "cat_work", defaultPoints = 40),
    ),
    val sessions: List<ScheduleSession> = emptyList(),
    val progressSegments: List<CategoryProgressSegment> = listOf(
        CategoryProgressSegment(TaskCategory.Study.color, 3f),
        CategoryProgressSegment(TaskCategory.Work.color, 2f),
        CategoryProgressSegment(androidx.compose.ui.graphics.Color(0xFFE2E8F0), 1f),
    ),
    val currentTimeFormatted: String = "",
    val currentTimeMinutes: Int = 0,
    val hasConflict: Boolean = true,
    val conflictMessage: String = "1:00 PM - 2:00 PM overlaps with Prepare Presentation",
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val schedulesByDate = mutableMapOf<LocalDate, DateScheduleData>()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        initDefaultSchedules()
        val today = LocalDate.now()
        val initialSchedule = getOrCreateScheduleForDate(today)
        _uiState.update { state ->
            state.copy(
                zones = initialSchedule.zones,
                sessions = initialSchedule.sessions,
                hasConflict = initialSchedule.hasConflict,
                conflictMessage = initialSchedule.conflictMessage,
                subtitleText = "Clear skies — ${initialSchedule.sessions.size} sessions scheduled today",
            )
        }
        updateCurrentTime()
        startClockTimer()
    }

    private fun initDefaultSchedules() {
        val today = LocalDate.now()

        // Schedule for TODAY
        schedulesByDate[today] = DateScheduleData(
            zones = listOf(
                ScheduleZone("z1_today", "cat_study", TaskCategory.Study, startHour = 8, endHour = 10, isCollapsed = false),
                ScheduleZone("z2_today", "cat_work", TaskCategory.Work, startHour = 13, endHour = 15, isCollapsed = false),
                ScheduleZone("z3_today", "cat_study", TaskCategory.Study, startHour = 18, endHour = 19, isCollapsed = false),
                ScheduleZone("z4_today", "cat_personal", TaskCategory.Personal, startHour = 22, endHour = 24, isCollapsed = false),
            ),
            sessions = listOf(
                ScheduleSession("s1", "z1_today", "task_1", "Read Chapter 5", startMinutes = 8 * 60, durationMinutes = 30, TaskCategory.Study, TaskStatus.Completed, points = 25),
                ScheduleSession("s2", "z1_today", "task_2", "Solve Algorithms", startMinutes = 8 * 60 + 30, durationMinutes = 30, TaskCategory.Study, TaskStatus.Pending, points = 30),
                ScheduleSession("s3", "z1_today", "task_1", "Read Chapter 5", startMinutes = 9 * 60, durationMinutes = 30, TaskCategory.Study, TaskStatus.Pending, points = 25),
                ScheduleSession("s4", "z1_today", "task_2", "Solve Algorithms", startMinutes = 9 * 60 + 30, durationMinutes = 30, TaskCategory.Study, TaskStatus.Pending, points = 30),
                ScheduleSession("s5", "z2_today", "task_3", "Prepare Presentation", startMinutes = 13 * 60, durationMinutes = 60, TaskCategory.Work, TaskStatus.Fixed, points = null),
                ScheduleSession("s6", "z2_today", "task_3", "Prepare Presentation", startMinutes = 14 * 60, durationMinutes = 60, TaskCategory.Work, TaskStatus.Pending, points = 40),
                ScheduleSession("s7", "z3_today", "task_1", "Read Chapter 5", startMinutes = 18 * 60, durationMinutes = 30, TaskCategory.Study, TaskStatus.Pending, points = 25),
                ScheduleSession("s8", "z3_today", "task_2", "Solve Algorithms", startMinutes = 18 * 60 + 30, durationMinutes = 30, TaskCategory.Study, TaskStatus.Pending, points = 30),
                ScheduleSession("s9", "z4_today", "task_4", "Night Wind-down & Journaling", startMinutes = 22 * 60, durationMinutes = 40, TaskCategory.Personal, TaskStatus.Pending, points = 25),
                ScheduleSession("s10", "z4_today", "task_5", "Review Tomorrow's Goals", startMinutes = 22 * 60 + 40, durationMinutes = 40, TaskCategory.Personal, TaskStatus.Pending, points = 25),
                ScheduleSession("s11", "z4_today", "task_6", "Relaxation & Sleep Prep", startMinutes = 23 * 60 + 20, durationMinutes = 39, TaskCategory.Personal, TaskStatus.Pending, points = 20),
            ),
            hasConflict = true,
            conflictMessage = "1:00 PM - 2:00 PM overlaps with Prepare Presentation",
        )

        // Schedule for YESTERDAY
        val yesterday = today.minusDays(1)
        schedulesByDate[yesterday] = DateScheduleData(
            zones = listOf(
                ScheduleZone("z1_yest", "cat_play", TaskCategory.Play, startHour = 7, endHour = 9, isCollapsed = false),
                ScheduleZone("z2_yest", "cat_study", TaskCategory.Study, startHour = 10, endHour = 12, isCollapsed = false),
                ScheduleZone("z3_yest", "cat_personal", TaskCategory.Personal, startHour = 15, endHour = 17, isCollapsed = false),
            ),
            sessions = listOf(
                ScheduleSession("sy1", "z1_yest", "ty1", "Morning Gaming Session", startMinutes = 7 * 60, durationMinutes = 45, TaskCategory.Play, TaskStatus.Completed, points = 20),
                ScheduleSession("sy2", "z1_yest", "ty2", "Strategy Game Warmup", startMinutes = 7 * 60 + 45, durationMinutes = 45, TaskCategory.Play, TaskStatus.Completed, points = 20),
                ScheduleSession("sy3", "z2_yest", "ty3", "Data Structures Review", startMinutes = 10 * 60, durationMinutes = 60, TaskCategory.Study, TaskStatus.Completed, points = 30),
                ScheduleSession("sy4", "z2_yest", "ty4", "LeetCode Practice", startMinutes = 11 * 60, durationMinutes = 60, TaskCategory.Study, TaskStatus.Completed, points = 35),
                ScheduleSession("sy5", "z3_yest", "ty5", "Meditation & Reflection", startMinutes = 15 * 60 + 30, durationMinutes = 60, TaskCategory.Personal, TaskStatus.Completed, points = 15),
            ),
            hasConflict = false,
        )

        // Schedule for TOMORROW
        val tomorrow = today.plusDays(1)
        schedulesByDate[tomorrow] = DateScheduleData(
            zones = listOf(
                ScheduleZone("z1_tom", "cat_work", TaskCategory.Work, startHour = 9, endHour = 12, isCollapsed = false),
                ScheduleZone("z2_tom", "cat_study", TaskCategory.Study, startHour = 14, endHour = 16, isCollapsed = false),
                ScheduleZone("z3_tom", "cat_personal", TaskCategory.Personal, startHour = 20, endHour = 21, isCollapsed = false),
            ),
            sessions = listOf(
                ScheduleSession("st1", "z1_tom", "tt1", "Team Standup & Planning", startMinutes = 9 * 60, durationMinutes = 60, TaskCategory.Work, TaskStatus.Pending, points = 25),
                ScheduleSession("st2", "z1_tom", "tt2", "System Architecture Spec", startMinutes = 10 * 60, durationMinutes = 90, TaskCategory.Work, TaskStatus.Pending, points = 40),
                ScheduleSession("st3", "z2_tom", "tt3", "Jetpack Compose UI Audit", startMinutes = 14 * 60, durationMinutes = 60, TaskCategory.Study, TaskStatus.Pending, points = 30),
                ScheduleSession("st4", "z2_tom", "tt4", "Component Tokens Refresh", startMinutes = 15 * 60, durationMinutes = 60, TaskCategory.Study, TaskStatus.Pending, points = 30),
                ScheduleSession("st5", "z3_tom", "tt5", "Technical Book Reading", startMinutes = 20 * 60, durationMinutes = 60, TaskCategory.Personal, TaskStatus.Pending, points = 15),
            ),
            hasConflict = false,
        )
    }

    private fun getOrCreateScheduleForDate(date: LocalDate): DateScheduleData {
        return schedulesByDate.getOrPut(date) {
            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            if (isWeekend) {
                DateScheduleData(
                    zones = listOf(
                        ScheduleZone("z_wk_1", "cat_personal", TaskCategory.Personal, startHour = 9, endHour = 11, isCollapsed = false),
                        ScheduleZone("z_wk_2", "cat_play", TaskCategory.Play, startHour = 16, endHour = 18, isCollapsed = false),
                    ),
                    sessions = listOf(
                        ScheduleSession("sw1", "z_wk_1", "tw1", "Weekend Book Reading", startMinutes = 9 * 60 + 30, durationMinutes = 60, TaskCategory.Personal, TaskStatus.Pending, points = 20),
                        ScheduleSession("sw2", "z_wk_2", "tw2", "Gaming & Relaxation", startMinutes = 16 * 60, durationMinutes = 90, TaskCategory.Play, TaskStatus.Pending, points = 30),
                    ),
                    hasConflict = false,
                )
            } else {
                DateScheduleData(
                    zones = listOf(
                        ScheduleZone("z_wd_1", "cat_study", TaskCategory.Study, startHour = 8, endHour = 10, isCollapsed = false),
                        ScheduleZone("z_wd_2", "cat_work", TaskCategory.Work, startHour = 13, endHour = 15, isCollapsed = false),
                    ),
                    sessions = listOf(
                        ScheduleSession("sd1", "z_wd_1", "td1", "Daily Focus Session", startMinutes = 8 * 60 + 30, durationMinutes = 60, TaskCategory.Study, TaskStatus.Pending, points = 25),
                        ScheduleSession("sd2", "z_wd_2", "td2", "Project Sprint Task", startMinutes = 13 * 60 + 30, durationMinutes = 60, TaskCategory.Work, TaskStatus.Pending, points = 30),
                    ),
                    hasConflict = false,
                )
            }
        }
    }

    private fun updateCurrentTime() {
        val cal = Calendar.getInstance()
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formatted = String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
        val totalMins = hour24 * 60 + minute

        _uiState.update { state ->
            state.copy(
                currentTimeFormatted = formatted,
                currentTimeMinutes = totalMins,
            )
        }
    }

    private fun startClockTimer() {
        viewModelScope.launch {
            while (isActive) {
                updateCurrentTime()
                delay(30_000)
            }
        }
    }

    private fun saveCurrentStateToRepository(state: HomeUiState) {
        schedulesByDate[state.selectedDate] = DateScheduleData(
            zones = state.zones,
            sessions = state.sessions,
            hasConflict = state.hasConflict,
            conflictMessage = state.conflictMessage,
        )
    }

    private fun navigateToDate(targetDate: LocalDate) {
        val today = LocalDate.now()
        val isToday = (targetDate == today)
        val isPastDate = targetDate.isBefore(today)

        saveCurrentStateToRepository(_uiState.value)

        val schedule = getOrCreateScheduleForDate(targetDate)

        val subtitle = if (isToday) {
            "Clear skies — ${schedule.sessions.size} sessions scheduled today"
        } else {
            "Clear skies — ${schedule.sessions.size} sessions scheduled"
        }

        val completedCount = schedule.sessions.count { it.status == TaskStatus.Completed }
        val pointsEarned = schedule.sessions.filter { it.status == TaskStatus.Completed }.sumOf { it.points ?: 0 }

        _uiState.update { state ->
            state.copy(
                selectedDate = targetDate,
                isToday = isToday,
                isPastDate = isPastDate,
                selectedDateText = formatSelectedDate(targetDate),
                zones = schedule.zones,
                sessions = schedule.sessions,
                hasConflict = schedule.hasConflict,
                conflictMessage = schedule.conflictMessage,
                subtitleText = subtitle,
                completedSessionsCount = completedCount,
                pointsCount = 1250 + pointsEarned,
            )
        }
    }

    fun previousDay() {
        navigateToDate(_uiState.value.selectedDate.minusDays(1))
    }

    fun nextDay() {
        navigateToDate(_uiState.value.selectedDate.plusDays(1))
    }

    fun selectToday() {
        navigateToDate(LocalDate.now())
    }

    fun toggleZoneCollapse(zoneId: String) {
        _uiState.update { state ->
            val updatedZones = state.zones.map { zone ->
                if (zone.id == zoneId) zone.copy(isCollapsed = !zone.isCollapsed) else zone
            }
            val newState = state.copy(zones = updatedZones)
            saveCurrentStateToRepository(newState)
            newState
        }
    }

    fun addSessionToZone(zoneId: String) {
        _uiState.update { state ->
            val matchedZone = state.zones.find { it.id == zoneId } ?: return@update state
            val zoneSessions = state.sessions.filter { it.zoneId == zoneId }
            val lastEndMinutes = zoneSessions.maxOfOrNull { it.startMinutes + it.durationMinutes }
                ?: (matchedZone.startHour * 60)

            val newStartMinutes = if (lastEndMinutes < matchedZone.endHour * 60) {
                lastEndMinutes
            } else {
                matchedZone.startHour * 60
            }

            val dummyTitles = listOf("New Task Session", "Practice Exercise", "Deep Focus", "Review Notes")
            val randomTitle = dummyTitles[(state.sessions.size) % dummyTitles.size]

            val newSession = ScheduleSession(
                id = "s_${System.currentTimeMillis()}",
                zoneId = zoneId,
                taskId = "t_dummy_${System.currentTimeMillis()}",
                taskTitle = randomTitle,
                startMinutes = newStartMinutes,
                durationMinutes = 30,
                category = matchedZone.category,
                status = TaskStatus.Pending,
                points = 20,
            )

            val updatedSessions = state.sessions + newSession
            val isToday = state.isToday
            val subtitle = if (isToday) {
                "Clear skies — ${updatedSessions.size} sessions scheduled today"
            } else {
                "Clear skies — ${updatedSessions.size} sessions scheduled"
            }

            val newState = state.copy(
                sessions = updatedSessions,
                subtitleText = subtitle,
            )
            saveCurrentStateToRepository(newState)
            newState
        }
    }

    fun toggleSessionStatus(sessionId: String) {
        _uiState.update { state ->
            val updatedSessions = state.sessions.map { session ->
                if (session.id == sessionId) {
                    when (session.status) {
                        TaskStatus.Completed -> {
                            session.copy(status = TaskStatus.Pending)
                        }
                        TaskStatus.Pending -> {
                            val earnedPoints = session.points ?: ((session.durationMinutes / 10).coerceAtLeast(1) * 10)
                            session.copy(
                                status = TaskStatus.Completed,
                                points = earnedPoints,
                            )
                        }
                        TaskStatus.Fixed -> session
                    }
                } else session
            }

            val completedCount = updatedSessions.count { it.status == TaskStatus.Completed }
            val pointsEarned = updatedSessions.filter { it.status == TaskStatus.Completed }.sumOf { it.points ?: 0 }

            val newState = state.copy(
                sessions = updatedSessions,
                completedSessionsCount = completedCount,
                pointsCount = 1250 + pointsEarned,
            )
            saveCurrentStateToRepository(newState)
            newState
        }
    }

    fun moveSession(sessionId: String, newStartMinutes: Int) {
        _uiState.update { state ->
            val updatedSessions = state.sessions.map { session ->
                if (session.id == sessionId && session.status !is TaskStatus.Fixed) {
                    val matchedZone = state.zones.find { zone ->
                        newStartMinutes in (zone.startHour * 60)..(zone.endHour * 60)
                    }
                    val newCategory = matchedZone?.category ?: session.category
                    val newZoneId = matchedZone?.id ?: session.zoneId
                    session.copy(
                        startMinutes = newStartMinutes,
                        zoneId = newZoneId,
                        category = newCategory,
                    )
                } else session
            }
            val newState = state.copy(sessions = updatedSessions)
            saveCurrentStateToRepository(newState)
            newState
        }
    }

    fun fixConflict() {
        _uiState.update { state ->
            val newState = state.copy(hasConflict = false)
            saveCurrentStateToRepository(newState)
            newState
        }
    }

    fun dismissConflict() {
        _uiState.update { state ->
            val newState = state.copy(hasConflict = false)
            saveCurrentStateToRepository(newState)
            newState
        }
    }
}

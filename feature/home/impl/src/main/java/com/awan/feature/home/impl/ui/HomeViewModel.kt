package com.awan.feature.home.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.designsystem.CategoryProgressSegment
import com.awan.app.core.designsystem.TaskCategory
import com.awan.app.core.designsystem.TaskStatus
import com.awan.app.core.designsystem.TimelineTaskItem
import com.awan.app.core.designsystem.TimelineZoneBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "Sam",
    val streakCount: Int = 6,
    val pointsCount: Int = 1285,
    val subtitleText: String = "Clear skies — 6 things floating today",
    val selectedDateText: String = "Today · Wed, Jul 15 📅",
    val dayNumber: Int = 15,
    val totalTasksCount: Int = 5,
    val completedTasksCount: Int = 2,
    val scheduledHoursText: String = "5 tasks · 6h scheduled",
    val progressSegments: List<CategoryProgressSegment> = listOf(
        CategoryProgressSegment(TaskCategory.Study.color, 1.5f),
        CategoryProgressSegment(TaskCategory.Work.color, 2f),
        CategoryProgressSegment(TaskCategory.Personal.color, 1f),
        CategoryProgressSegment(TaskCategory.Play.color, 1f),
        CategoryProgressSegment(androidx.compose.ui.graphics.Color(0xFFE2E8F0), 1.5f),
    ),
    val zones: List<TimelineZoneBlock> = listOf(
        TimelineZoneBlock("1", TaskCategory.Study, 6, 9, isCollapsed = false),
        TimelineZoneBlock("2", TaskCategory.Work, 9, 17, isCollapsed = false),
        TimelineZoneBlock("3", TaskCategory.Personal, 17, 21, isCollapsed = false),
        TimelineZoneBlock("4", TaskCategory.Play, 21, 24, isCollapsed = false),
    ),
    val tasks: List<TimelineTaskItem> = listOf(
        TimelineTaskItem("t1", "Read Clean Code", 8 * 60, 60, TaskCategory.Study, TaskStatus.Completed, 30),
        TimelineTaskItem("t2", "Design review", 10 * 60, 60, TaskCategory.Work, TaskStatus.Pending, 45),
        TimelineTaskItem("t3", "Team sync", 13 * 60, 60, TaskCategory.Work, TaskStatus.Fixed),
        TimelineTaskItem("t4", "Gym", 18 * 60, 60, TaskCategory.Personal, TaskStatus.Pending, 35),
        TimelineTaskItem("t5", "Movie night", 21 * 60 + 30, 60, TaskCategory.Play, TaskStatus.Pending, 25),
    ),
    val currentTimeFormatted: String = "",
    val currentTimeMinutes: Int = 0,
    val hasConflict: Boolean = true,
    val conflictMessage: String = "1:00 PM - 2:00 PM overlaps with Team sync",
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        updateCurrentTime()
        startClockTimer()
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

    fun toggleZoneCollapse(zoneId: String) {
        _uiState.update { state ->
            val updatedZones = state.zones.map { zone ->
                if (zone.id == zoneId) zone.copy(isCollapsed = !zone.isCollapsed) else zone
            }
            state.copy(zones = updatedZones)
        }
    }

    fun previousDay() {
        _uiState.update { state ->
            val newDay = (state.dayNumber - 1).coerceAtLeast(1)
            state.copy(
                dayNumber = newDay,
                selectedDateText = "Wed, Jul $newDay 📅",
            )
        }
    }

    fun nextDay() {
        _uiState.update { state ->
            val newDay = state.dayNumber + 1
            state.copy(
                dayNumber = newDay,
                selectedDateText = "Thu, Jul $newDay 📅",
            )
        }
    }

    fun toggleTaskStatus(taskId: String) {
        _uiState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val newStatus = when (task.status) {
                        TaskStatus.Completed -> TaskStatus.Pending
                        TaskStatus.Pending -> TaskStatus.Completed
                        TaskStatus.Fixed -> TaskStatus.Fixed
                    }
                    task.copy(status = newStatus)
                } else task
            }

            val completedCount = updatedTasks.count { it.status == TaskStatus.Completed }
            val pointsEarned = updatedTasks.filter { it.status == TaskStatus.Completed }.sumOf { it.points ?: 0 }

            state.copy(
                tasks = updatedTasks,
                completedTasksCount = completedCount,
                pointsCount = 1250 + pointsEarned,
            )
        }
    }

    fun moveTask(taskId: String, newStartMinutes: Int) {
        _uiState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId && task.status !is TaskStatus.Fixed) {
                    val matchedZone = state.zones.find { zone ->
                        newStartMinutes in (zone.startHour * 60)..(zone.endHour * 60)
                    }
                    val newCategory = matchedZone?.category ?: task.category
                    task.copy(startMinutes = newStartMinutes, category = newCategory)
                } else task
            }
            state.copy(tasks = updatedTasks)
        }
    }

    fun addTask() {
        _uiState.update { state ->
            val newTask = TimelineTaskItem(
                id = "t_${System.currentTimeMillis()}",
                title = "New Task",
                startMinutes = 9 * 60 + 30,
                durationMinutes = 45,
                category = TaskCategory.Work,
                status = TaskStatus.Pending,
                points = 20,
            )
            state.copy(
                tasks = state.tasks + newTask,
                totalTasksCount = state.totalTasksCount + 1,
            )
        }
    }

    fun addGoal() {
        _uiState.update { state ->
            state.copy(streakCount = state.streakCount + 1)
        }
    }

    fun fixConflict() {
        _uiState.update { state ->
            state.copy(hasConflict = false)
        }
    }

    fun dismissConflict() {
        _uiState.update { state ->
            state.copy(hasConflict = false)
        }
    }
}

package com.awan.feature.home.impl.ui

import com.awan.app.core.model.SessionDetailInfo
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.SessionTaskDetail
import com.awan.app.core.model.TaskDetailInfo
import com.awan.app.core.model.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SessionDetailDialogStateTest {

    private val baseDate = LocalDate.of(2026, 8, 17)
    private val startDateTime = LocalDateTime.of(2026, 8, 17, 10, 0)
    private val endDateTime = LocalDateTime.of(2026, 8, 17, 11, 0)

    private val sampleDetail = SessionTaskDetail(
        session = SessionDetailInfo(
            id = "session-1",
            start = startDateTime,
            end = endDateTime,
            status = SessionStatus.SCHEDULED,
            locked = false,
            zoneId = "zone-1",
            taskId = "task-1",
        ),
        task = TaskDetailInfo(
            id = "task-1",
            title = "Sample Task",
            description = "Task description",
            estimatedDuration = 60,
            status = TaskStatus.SCHEDULED,
            mandatory = false,
            estimatedPoints = 50,
            allowTaskSplitting = true,
            goalId = null,
            categoryName = "Work",
            dependsOnTaskIds = emptyList(),
        ),
    )

    @Test
    fun `isDirty is false when detail is null`() {
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = null,
        )
        assertFalse(state.isDirty)
    }

    @Test
    fun `isDirty is false when editable fields match original detail values`() {
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = sampleDetail,
            editTitle = "Sample Task",
            editDescription = "Task description",
            editDate = baseDate,
            editStartMinutes = 10 * 60, // 600
            editEndMinutes = 11 * 60, // 660
            editDurationMinutes = 60,
        )
        assertFalse(state.isDirty)
    }

    @Test
    fun `isDirty is true when date is changed`() {
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = sampleDetail,
            editTitle = "Sample Task",
            editDescription = "Task description",
            editDate = baseDate.plusDays(1),
            editStartMinutes = 600,
            editEndMinutes = 660,
            editDurationMinutes = 60,
        )
        assertTrue(state.isDirty)
    }

    @Test
    fun `isDirty is true when start minutes is changed`() {
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = sampleDetail,
            editTitle = "Sample Task",
            editDescription = "Task description",
            editDate = baseDate,
            editStartMinutes = 615,
            editEndMinutes = 660,
            editDurationMinutes = 45,
        )
        assertTrue(state.isDirty)
    }

    @Test
    fun `isDirty is true when end minutes is changed`() {
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = sampleDetail,
            editTitle = "Sample Task",
            editDescription = "Task description",
            editDate = baseDate,
            editStartMinutes = 600,
            editEndMinutes = 720,
            editDurationMinutes = 120,
        )
        assertTrue(state.isDirty)
    }

    @Test
    fun `isDirty is true when title is changed`() {
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = sampleDetail,
            editTitle = "Modified Task Title",
            editDescription = "Task description",
            editDate = baseDate,
            editStartMinutes = 600,
            editEndMinutes = 660,
            editDurationMinutes = 60,
        )
        assertTrue(state.isDirty)
    }

    @Test
    fun `isDirty is true when description is changed`() {
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = sampleDetail,
            editTitle = "Sample Task",
            editDescription = "Modified task description",
            editDate = baseDate,
            editStartMinutes = 600,
            editEndMinutes = 660,
            editDurationMinutes = 60,
        )
        assertTrue(state.isDirty)
    }

    @Test
    fun `isDirty returns false after restoring values to original`() {
        var state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = sampleDetail,
            editTitle = "Sample Task",
            editDescription = "Task description",
            editDate = baseDate,
            editStartMinutes = 600,
            editEndMinutes = 660,
            editDurationMinutes = 60,
        )
        assertFalse(state.isDirty)

        state = state.copy(editStartMinutes = 700)
        assertTrue(state.isDirty)

        state = state.copy(editStartMinutes = 600)
        assertFalse(state.isDirty)
    }

    @Test
    fun `isDirty is false when task description is null and editDescription is empty`() {
        val detailWithNullDesc = sampleDetail.copy(
            task = sampleDetail.task.copy(description = null)
        )
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = detailWithNullDesc,
            editTitle = "Sample Task",
            editDescription = "",
            editDate = baseDate,
            editStartMinutes = 600,
            editEndMinutes = 660,
            editDurationMinutes = 60,
        )
        assertFalse(state.isDirty)
    }

    @Test
    fun `isDirty is true when task description is null and editDescription is non-empty`() {
        val detailWithNullDesc = sampleDetail.copy(
            task = sampleDetail.task.copy(description = null)
        )
        val state = SessionDetailDialogState(
            sessionId = "session-1",
            detail = detailWithNullDesc,
            editTitle = "Sample Task",
            editDescription = "Added description",
            editDate = baseDate,
            editStartMinutes = 600,
            editEndMinutes = 660,
            editDurationMinutes = 60,
        )
        assertTrue(state.isDirty)
    }
}


@file:Suppress("NewApi")

package com.awan.feature.home.impl.ui

import com.awan.app.core.common.text.UiText
import com.awan.app.core.model.SessionTaskDetail
import com.awan.feature.home.impl.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatSelectedDate(date: LocalDate): UiText {
    val today = LocalDate.now()
    val pattern = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    val formatted = date.format(pattern)
    return when (date) {
        today -> UiText.StringResource(R.string.home_date_today_format, formatted)
        today.minusDays(1) -> UiText.StringResource(R.string.home_date_yesterday_format, formatted)
        today.plusDays(1) -> UiText.StringResource(R.string.home_date_tomorrow_format, formatted)
        else -> UiText.DynamicString(formatted)
    }
}

enum class DeleteTargetType {
    SESSION,
    TASK,
}

data class SessionDetailDialogState(
    val sessionId: String,
    val isLoading: Boolean = true,
    val detail: SessionTaskDetail? = null,
    val errorMessage: UiText? = null,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editDescription: String = "",
    val editDurationMinutes: Int = 30,
    val isSaving: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteTargetType: DeleteTargetType = DeleteTargetType.SESSION,
)

sealed interface TimelineContentState {
    data object Loading : TimelineContentState
    data class Error(val message: UiText) : TimelineContentState
    data object Ready : TimelineContentState
}

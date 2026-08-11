package com.awan.feature.goals.impl.presentation

sealed interface InboxAction {
    data class SearchQueryChanged(val query: String) : InboxAction
    data class StatusFilterToggled(val filter: InboxTaskDisplayStatus) : InboxAction
    data class SessionFilterToggled(val filter: InboxSessionFilter) : InboxAction
    data class TaskExpandToggled(val taskId: String) : InboxAction
    data object FilterClicked : InboxAction
    data object FilterDismissed : InboxAction
    data object RetryClicked : InboxAction
}

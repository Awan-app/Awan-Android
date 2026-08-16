package com.awan.feature.goals.impl.presentation

data class InboxUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /** All inbox tasks fetched from the server — unfiltered. */
    val allTasks: List<InboxTaskUiModel> = emptyList(),
    val searchQuery: String = "",
    /** Active task-status filter chips. Empty = show all. */
    val activeStatusFilters: Set<InboxTaskDisplayStatus> = emptySet(),
    /** Active session-display filter chips. Empty = show all. */
    val activeSessionFilters: Set<InboxSessionFilter> = emptySet(),
    /** The id of the task card currently expanded to show sessions. */
    val expandedTaskId: String? = null,
    /** Tasks visible after applying search and filter. */
    val visibleTasks: List<InboxTaskUiModel> = emptyList(),
    val showFilterSheet: Boolean = false,
)

package com.awan.feature.goals.impl.presentation

data class InboxUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /** All inbox tasks fetched from the server — unfiltered. */
    val allTasks: List<InboxTaskUiModel> = emptyList(),
    val searchQuery: String = "",
    /** Active task-status filter chips. Empty = show all. */
    val activeStatusFilters: Set<InboxTaskDisplayStatus> = emptySet(),
    /** Tasks visible after applying search and filter. */
    val visibleTasks: List<InboxTaskUiModel> = emptyList(),
    val showFilterSheet: Boolean = false,
)

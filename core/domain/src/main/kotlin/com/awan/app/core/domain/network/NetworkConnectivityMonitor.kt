package com.awan.app.core.domain.network

import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing interface for observing network connectivity state.
 * Reports online strictly when an internet-capable, validated network is connected.
 */
interface NetworkConnectivityMonitor {
    /**
     * Flow emitting true when internet is available and validated, false otherwise.
     */
    val isOnline: Flow<Boolean>

    /**
     * Synchronous check for current validated online status.
     */
    fun isCurrentlyOnline(): Boolean
}

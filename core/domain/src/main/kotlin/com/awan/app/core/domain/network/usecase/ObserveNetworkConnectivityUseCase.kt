package com.awan.app.core.domain.network.usecase

import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNetworkConnectivityUseCase @Inject constructor(
    private val connectivityMonitor: NetworkConnectivityMonitor,
) {
    operator fun invoke(): Flow<Boolean> = connectivityMonitor.isOnline
}

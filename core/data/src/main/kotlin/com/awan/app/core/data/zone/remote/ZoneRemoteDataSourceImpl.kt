package com.awan.app.core.data.zone.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.ZoneApiService
import com.awan.app.core.network.dto.ZoneDto
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ZoneRemoteDataSourceImpl @Inject constructor(
    private val zoneApiService: ZoneApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ZoneRemoteDataSource {

    override suspend fun getZonesByDate(date: String): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zoneApiService.getZonesByDate(date)
        }
}

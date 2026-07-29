package com.awan.app.core.data.zone

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.zone.remote.ZoneRemoteDataSource
import com.awan.app.core.domain.zone.repository.ZoneRepository
import com.awan.app.core.model.DayZone
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZoneRepositoryImpl @Inject constructor(
    private val remoteDataSource: ZoneRemoteDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ZoneRepository {

    override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> = withContext(ioDispatcher) {
        remoteDataSource.getZonesByDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .map { zones -> zones.mapNotNull { it.toModel() } }
    }
}

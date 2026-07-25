package com.awan.app.core.data.template

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.template.remote.TemplateRemoteDataSource
import com.awan.app.core.data.util.formatMinutesToTime
import com.awan.app.core.domain.template.repository.TemplateRepository
import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.Zone
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateZoneRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val remoteDataSource: TemplateRemoteDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TemplateRepository {

    override suspend fun createWeeklyTemplate(zones: List<Zone>): Result<Unit> = withContext(ioDispatcher) {
        val request = CreateTemplateRequest(
            name = TEMPLATE_NAME,
            daysOfWeek = DAYS_OF_WEEK,
            zones = zones.filter { it.isEnabled }.map(::toZoneRequest),
        )

        when (val result = remoteDataSource.createTemplate(request)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    /**
     * The API stores zone windows as `LocalTime`, which cannot wrap — a zone that runs past
     * midnight is truncated at the end of the day and the remainder is dropped.
     */
    private fun toZoneRequest(zone: Zone): TemplateZoneRequest = TemplateZoneRequest(
        name = zone.name,
        startTime = formatMinutesToTime(zone.startMinutes),
        endTime = if (zone.endMinutes >= DayBounds.MINUTES_PER_DAY) {
            END_OF_DAY
        } else {
            formatMinutesToTime(zone.endMinutes)
        },
        color = String.format(Locale.US, "#%06X", zone.colorArgb and RGB_MASK),
    )

    private companion object {
        // ponytail: fixed name — nothing names a template yet, and it is server-side data, not a rendered string.
        const val TEMPLATE_NAME = "My Week"
        const val END_OF_DAY = "23:59:59"
        const val RGB_MASK = 0xFFFFFF

        val DAYS_OF_WEEK = listOf(
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY",
        )
    }
}

package com.awan.app.core.data.template

import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.data.template.remote.TemplateRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.network.dto.zone.CreateTemplateRequest
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto as TemplateResponse
import com.awan.app.core.network.dto.zone.ZoneDto as ZoneResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateRepositoryImplTest {

    private var captured: CreateTemplateRequest? = null
    private var response = TemplateResponse(
        id = "template-1",
        name = "My Week",
        daysOfWeek = emptyList(),
        zones = emptyList()
    )

    private val remoteDataSource = object : TemplateRemoteDataSource {
        override suspend fun createTemplate(request: CreateTemplateRequest): Result<TemplateResponse> {
            captured = request
            return Result.Success(response)
        }
    }

    private val fakeTemplateDao = object : TemplateDao {
        override suspend fun upsertTemplate(template: TemplateEntity) {}
        override suspend fun upsertTemplates(templates: List<TemplateEntity>) {}
        override fun observeAllTemplates(): Flow<List<TemplateEntity>> = flowOf(emptyList())
        override fun observeTemplate(templateId: String): Flow<TemplateEntity?> = flowOf(null)
        override suspend fun getTemplate(templateId: String): TemplateEntity? = null
        override suspend fun deleteTemplate(templateId: String) {}
        override suspend fun upsertDays(days: List<TemplateDayOfWeekEntity>) {}
        override fun observeDaysForTemplate(templateId: String): Flow<List<TemplateDayOfWeekEntity>> = flowOf(emptyList())
        override suspend fun getDayAssignment(dayOfWeek: String): TemplateDayOfWeekEntity? = null
        override suspend fun deleteDaysForTemplate(templateId: String) {}
        override suspend fun getMinExpiryTime(): Long? = null
    }

    private val fakeZoneDao = object : ZoneDao {
        override suspend fun upsertZone(zone: ZoneEntity) {}
        override suspend fun upsertZones(zones: List<ZoneEntity>) {}
        override fun observeZone(zoneId: String): Flow<ZoneEntity?> = flowOf(null)
        override suspend fun getZone(zoneId: String): ZoneEntity? = null
        override fun observeZonesForTemplate(templateId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
        override fun observeZonesForOverride(overrideId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
        override suspend fun deleteZone(zoneId: String) {}
        override suspend fun deleteZonesForTemplate(templateId: String) {}
        override suspend fun deleteZonesForOverride(overrideId: String) {}
    }

    private val onlineMonitor = object : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(true)
        override fun isCurrentlyOnline(): Boolean = true
    }

    private val repository = TemplateRepositoryImpl(
        remoteDataSource = remoteDataSource,
        templateDao = fakeTemplateDao,
        zoneDao = fakeZoneDao,
        connectivityMonitor = onlineMonitor,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `sends every enabled zone with formatted times and an RGB color`() = runTest {
        val zones = listOf(
            Zone("study", "Study", 0xFF7A64FF.toInt(), startMinutes = 450, endMinutes = 675),
            Zone("work", "Work", 0xFF2EAAFF.toInt(), startMinutes = 675, endMinutes = 900),
        )

        repository.createWeeklyTemplate(zones)

        val sent = requireNotNull(captured).zones
        assertEquals(2, sent.size)
        assertEquals("Study", sent[0].name)
        assertEquals("07:30:00", sent[0].startTime)
        assertEquals("11:15:00", sent[0].endTime)
        assertEquals("#7A64FF", sent[0].color)
        assertEquals("#2EAAFF", sent[1].color)
    }

    @Test
    fun `disabled zones are not sent`() = runTest {
        val zones = listOf(
            Zone("study", "Study", 0, startMinutes = 450, endMinutes = 675),
            Zone("play", "Play", 0, startMinutes = 675, endMinutes = 900, isEnabled = false),
        )

        repository.createWeeklyTemplate(zones)

        assertEquals(listOf("Study"), requireNotNull(captured).zones.map { it.name })
    }

    @Test
    fun `a zone running past midnight is truncated at the end of the day`() = runTest {
        val zones = listOf(Zone("study", "Study", 0, startMinutes = 1410, endMinutes = 1470))

        repository.createWeeklyTemplate(zones)

        val sent = requireNotNull(captured).zones.single()
        assertEquals("23:30:00", sent.startTime)
        assertEquals("23:59:59", sent.endTime)
        assertTrue(sent.endTime > sent.startTime)
    }

    @Test
    fun `the created zones come back carrying the server ids a session refers to`() = runTest {
        response = TemplateResponse(
            id = "template-1",
            name = "My Week",
            daysOfWeek = emptyList(),
            zones = listOf(
                ZoneResponse(
                    id = "server-zone-1",
                    name = "Study",
                    startTime = "07:30:00",
                    endTime = "11:15:00",
                    color = "#7A64FF",
                ),
            ),
        )

        val result = repository.createWeeklyTemplate(Zone.defaults)

        val zone = (result as Result.Success).data.single()
        assertEquals("server-zone-1", zone.id)
        assertEquals("Study", zone.name)
        assertEquals(450, zone.startMinutes)
        assertEquals(675, zone.endMinutes)
        assertEquals(0xFF7A64FF.toInt(), zone.colorArgb)
    }

    @Test
    fun `the template covers all seven days`() = runTest {
        repository.createWeeklyTemplate(Zone.defaults)

        assertEquals(
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
            requireNotNull(captured).daysOfWeek,
        )
    }
}

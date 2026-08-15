package com.awan.app.core.data.zones

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.zones.local.ZonesLocalDataSource
import com.awan.app.core.data.zones.remote.ZonesRemoteDataSource
import com.awan.app.core.data.zones.repository.ZonesRepositoryImpl
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.zone.CreateOverrideRequest
import com.awan.app.core.network.dto.zone.CreateTemplateRequest
import com.awan.app.core.network.dto.zone.CreateZoneRequest
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.UpdateOverrideRequest
import com.awan.app.core.network.dto.zone.UpdateTemplateRequest
import com.awan.app.core.network.dto.zone.UpdateZoneRequest
import com.awan.app.core.network.dto.zone.UpdateZonesRequest
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The contract these pin: **no mutation may report success without a local write.** They are
 * parameterised over every mutation on the repository, so a thirteenth one added later fails here
 * until it refreshes too — which is the only thing keeping Home from silently going stale again.
 */
class ZonesRepositoryImplTest {

    private val zone = DailyZone(id = "z1", name = "Work", startTime = "09:00", endTime = "12:00", color = "#FFF")

    private fun mutations(repository: ZonesRepository): Map<String, suspend () -> Result<*>> = mapOf(
        "createTemplate" to { repository.createTemplate("T", listOf(DayOfWeek.MONDAY), listOf(zone)) },
        "updateTemplate" to { repository.updateTemplate("t1", "T", listOf(DayOfWeek.MONDAY)) },
        "deleteTemplate" to { repository.deleteTemplate("t1") },
        "addZoneToTemplate" to { repository.addZoneToTemplate("t1", zone) },
        "updateTemplateZones" to { repository.updateTemplateZones("t1", listOf(zone)) },
        "createOverride" to { repository.createOverride("2026-08-09", listOf(zone)) },
        "updateOverride" to { repository.updateOverride("o1", "N", "2026-08-09") },
        "deleteOverride" to { repository.deleteOverride("o1") },
        "addZoneToOverride" to { repository.addZoneToOverride("o1", zone) },
        "updateOverrideZones" to { repository.updateOverrideZones("o1", listOf(zone)) },
        "updateZone" to { repository.updateZone("z1", zone) },
        "deleteZone" to { repository.deleteZone("z1") },
    )

    @Test
    fun `every mutation replaces the local zone model`() = runTest {
        for ((name, mutate) in mutations(repository())) {
            local.replaceCount = 0

            val result = mutate()

            assertTrue("$name did not succeed", result is Result.Success)
            assertEquals("$name did not write to Room", 1, local.replaceCount)
        }
    }

    @Test
    fun `a failed mutation writes nothing`() = runTest {
        val repository = repository()
        remote.failWith = AppError.Api(code = 500, body = "boom")

        for ((name, mutate) in mutations(repository)) {
            local.replaceCount = 0

            assertTrue("$name should have failed", mutate() is Result.Error)
            assertEquals("$name wrote to Room after a failure", 0, local.replaceCount)
        }
    }

    @Test
    fun `offline mutations never reach the network`() = runTest {
        val repository = repository(online = false)

        for ((name, mutate) in mutations(repository)) {
            val result = mutate()

            assertTrue("$name should be offline-gated", result is Result.Error)
            assertEquals("$name returned the wrong error", AppError.Network, (result as Result.Error).error)
        }
        assertEquals(0, remote.callCount)
        assertEquals(0, local.replaceCount)
    }

    @Test
    fun `refreshZones writes nothing when either half fails`() = runTest {
        val repository = repository()
        remote.failOverridesOnly = true

        assertTrue(repository.refreshZones() is Result.Error)
        assertEquals(0, local.replaceCount)
    }

    private lateinit var remote: FakeZonesRemoteDataSource
    private lateinit var local: FakeZonesLocalDataSource

    private fun repository(online: Boolean = true): ZonesRepositoryImpl {
        remote = FakeZonesRemoteDataSource()
        local = FakeZonesLocalDataSource()
        return ZonesRepositoryImpl(
            zonesRemoteDataSource = remote,
            zonesLocalDataSource = local,
            connectivityMonitor = object : NetworkConnectivityMonitor {
                override val isOnline: Flow<Boolean> = flowOf(online)
                override fun isCurrentlyOnline(): Boolean = online
            },
            ioDispatcher = Dispatchers.Unconfined,
        )
    }
}

private class FakeZonesLocalDataSource : ZonesLocalDataSource {
    var replaceCount = 0

    override suspend fun replaceAll(
        templates: List<WeeklyTemplateDto>,
        overrides: List<TemplateOverrideDto>,
        expiryTime: Long,
    ) {
        replaceCount++
    }

    override fun observeEffectiveZonesForDate(date: String, dayOfWeek: String): Flow<List<ZoneEntity>> =
        flowOf(emptyList())
}

private class FakeZonesRemoteDataSource : ZonesRemoteDataSource {
    var failWith: AppError? = null
    var failOverridesOnly = false
    var callCount = 0
        private set

    private val zoneDto = ZoneDto(id = "z1", name = "Work", startTime = "09:00:00", endTime = "12:00:00")
    private val templateDto = WeeklyTemplateDto(id = "t1", name = "T", daysOfWeek = listOf("MONDAY"), zones = emptyList())
    private val overrideDto = TemplateOverrideDto(id = "o1", name = null, dateOfDay = "2026-08-09", zones = emptyList())

    private fun <T> answer(value: T): Result<T> {
        callCount++
        return failWith?.let { Result.Error(it) } ?: Result.Success(value)
    }

    override suspend fun getOverrides(): Result<List<TemplateOverrideDto>> =
        if (failOverridesOnly) Result.Error(AppError.Network) else answer(listOf(overrideDto))

    override suspend fun getZonesByDate(date: String): Result<List<ZoneDto>> = answer(listOf(zoneDto))
    override suspend fun getTemplates(): Result<List<WeeklyTemplateDto>> = answer(listOf(templateDto))
    override suspend fun createTemplate(request: CreateTemplateRequest) = answer(templateDto)
    override suspend fun getTemplate(templateId: String) = answer(templateDto)
    override suspend fun updateTemplate(templateId: String, request: UpdateTemplateRequest) = answer(templateDto)
    override suspend fun deleteTemplate(templateId: String) = answer(Unit)
    override suspend fun addZoneToTemplate(templateId: String, request: CreateZoneRequest) = answer(zoneDto)
    override suspend fun getTemplateZones(templateId: String) = answer(listOf(zoneDto))
    override suspend fun updateTemplateZones(templateId: String, request: UpdateZonesRequest) = answer(listOf(zoneDto))
    override suspend fun createOverride(request: CreateOverrideRequest) = answer(overrideDto)
    override suspend fun getOverride(overrideId: String) = answer(overrideDto)
    override suspend fun updateOverride(overrideId: String, request: UpdateOverrideRequest) = answer(overrideDto)
    override suspend fun deleteOverride(overrideId: String) = answer(Unit)
    override suspend fun addZoneToOverride(overrideId: String, request: CreateZoneRequest) = answer(zoneDto)
    override suspend fun getOverrideZones(overrideId: String) = answer(listOf(zoneDto))
    override suspend fun updateOverrideZones(overrideId: String, request: UpdateZonesRequest) = answer(listOf(zoneDto))
    override suspend fun getZone(zoneId: String) = answer(zoneDto)
    override suspend fun getZoneSessions(zoneId: String) = answer(emptyList<SessionDto>())
    override suspend fun getEffectiveZones(date: String) = answer(listOf(zoneDto))
    override suspend fun updateZone(zoneId: String, request: UpdateZoneRequest) = answer(zoneDto)
    override suspend fun deleteZone(zoneId: String) = answer(Unit)
}


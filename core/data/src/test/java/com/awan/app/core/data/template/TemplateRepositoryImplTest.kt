package com.awan.app.core.data.template

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.template.remote.TemplateRemoteDataSource
import com.awan.app.core.model.Zone
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateRepositoryImplTest {

    private var captured: CreateTemplateRequest? = null

    private val remoteDataSource = object : TemplateRemoteDataSource {
        override suspend fun createTemplate(request: CreateTemplateRequest): Result<TemplateResponse> {
            captured = request
            return Result.Success(TemplateResponse(id = "template-1"))
        }
    }

    private val repository = TemplateRepositoryImpl(remoteDataSource, UnconfinedTestDispatcher())

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
        // 23:30 -> 00:30 next day: endMinutes 1470 would format as 00:30:00 and be rejected as
        // not-after-start, so it is cut at 23:59:59 instead.
        val zones = listOf(Zone("study", "Study", 0, startMinutes = 1410, endMinutes = 1470))

        repository.createWeeklyTemplate(zones)

        val sent = requireNotNull(captured).zones.single()
        assertEquals("23:30:00", sent.startTime)
        assertEquals("23:59:59", sent.endTime)
        assertTrue(sent.endTime > sent.startTime)
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

package com.awan.app.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.buildInMemoryDb
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.TemplateOverrideEntity
import com.awan.app.core.database.model.ZoneEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZoneDaoTest {

    private lateinit var db: AwanDatabase
    private lateinit var dao: ZoneDao

    @Before
    fun setup() {
        db = buildInMemoryDb()
        dao = db.zoneDao()
    }

    @After
    fun teardown() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun insertTemplate(id: String = "tmpl1") {
        db.templateDao().upsertTemplate(TemplateEntity(id = id, name = "T"))
    }

    private suspend fun insertOverride(id: String = "ov1") {
        db.templateOverrideDao().upsertOverride(
            TemplateOverrideEntity(id = id, name = null, dateOfDay = "2026-07-21")
        )
    }

    private fun templateZone(
        id: String = "z1",
        templateId: String = "tmpl1",
        startTime: String = "09:00:00",
        endTime: String = "12:00:00",
    ) = ZoneEntity(
        id = id,
        name = "Study",
        startTime = startTime,
        endTime = endTime,
        color = null,
        templateId = templateId,
        templateOverrideId = null,
    )

    private fun overrideZone(
        id: String = "z2",
        overrideId: String = "ov1",
        startTime: String = "10:00:00",
    ) = ZoneEntity(
        id = id,
        name = "Work",
        startTime = startTime,
        endTime = "18:00:00",
        color = "#FF0000",
        templateId = null,
        templateOverrideId = overrideId,
    )

    @Test
    fun rejectsZoneWithoutParent() {
        assertThrows(IllegalArgumentException::class.java) {
            ZoneEntity(
                id = "orphan",
                name = "Study",
                startTime = "09:00:00",
                endTime = "12:00:00",
                color = null,
                templateId = null,
                templateOverrideId = null,
            )
        }
    }

    @Test
    fun rejectsZoneWithBothParents() {
        assertThrows(IllegalArgumentException::class.java) {
            ZoneEntity(
                id = "ambiguous",
                name = "Study",
                startTime = "09:00:00",
                endTime = "12:00:00",
                color = null,
                templateId = "tmpl1",
                templateOverrideId = "ov1",
            )
        }
    }


    // ── upsertZone / getZone ──────────────────────────────────────────────────

    @Test
    fun insertsAndRetrievesTemplateZone() = runTest {
        insertTemplate()
        dao.upsertZone(templateZone())
        assertEquals(templateZone(), dao.getZone("z1"))
    }

    @Test
    fun insertsAndRetrievesOverrideZone() = runTest {
        insertOverride()
        dao.upsertZone(overrideZone())
        assertEquals(overrideZone(), dao.getZone("z2"))
    }

    @Test
    fun returnsNullForMissingZone() = runTest {
        assertNull(dao.getZone("missing"))
    }

    @Test
    fun upsertUpdatesExistingZone() = runTest {
        insertTemplate()
        dao.upsertZone(templateZone(endTime = "11:00:00"))
        dao.upsertZone(templateZone(endTime = "13:00:00"))
        assertEquals("13:00:00", dao.getZone("z1")!!.endTime)
    }

    @Test
    fun upsertsBatch() = runTest {
        insertTemplate()
        dao.upsertZones(listOf(templateZone("z1"), templateZone("z2", startTime = "13:00:00")))
        assertEquals(2, dao.observeZonesForTemplate("tmpl1").first().size)
    }

    // ── observeZonesForTemplate — ordering ────────────────────────────────────

    @Test
    fun observeZonesForTemplate_orderedByStartTimeAsc() = runTest {
        insertTemplate()
        dao.upsertZones(
            listOf(
                templateZone("late", startTime = "14:00:00"),
                templateZone("early", startTime = "08:00:00"),
            )
        )
        val ids = dao.observeZonesForTemplate("tmpl1").first().map { it.id }
        assertEquals(listOf("early", "late"), ids)
    }

    @Test
    fun observeZonesForTemplate_emptyForUnknownTemplate() = runTest {
        assertTrue(dao.observeZonesForTemplate("none").first().isEmpty())
    }

    // ── observeZonesForOverride ────────────────────────────────────────────────

    @Test
    fun observeZonesForOverride_orderedByStartTimeAsc() = runTest {
        insertOverride()
        dao.upsertZones(
            listOf(
                overrideZone("late", startTime = "15:00:00"),
                overrideZone("early", startTime = "07:00:00"),
            )
        )
        val ids = dao.observeZonesForOverride("ov1").first().map { it.id }
        assertEquals(listOf("early", "late"), ids)
    }

    @Test
    fun templateAndOverrideZonesAreIsolated() = runTest {
        insertTemplate()
        insertOverride()
        dao.upsertZone(templateZone("z1"))
        dao.upsertZone(overrideZone("z2"))

        assertEquals(1, dao.observeZonesForTemplate("tmpl1").first().size)
        assertEquals(1, dao.observeZonesForOverride("ov1").first().size)
    }

    // ── deleteZone ────────────────────────────────────────────────────────────

    @Test
    fun deletesZone() = runTest {
        insertTemplate()
        dao.upsertZone(templateZone())
        dao.deleteZone("z1")
        assertNull(dao.getZone("z1"))
    }

    // ── deleteZonesForTemplate ────────────────────────────────────────────────

    @Test
    fun deleteZonesForTemplate_removesAllZones() = runTest {
        insertTemplate()
        dao.upsertZones(listOf(templateZone("z1"), templateZone("z2", startTime = "14:00:00")))
        dao.deleteZonesForTemplate("tmpl1")
        assertTrue(dao.observeZonesForTemplate("tmpl1").first().isEmpty())
    }

    @Test
    fun deleteZonesForTemplate_doesNotTouchOverrideZones() = runTest {
        insertTemplate()
        insertOverride()
        dao.upsertZone(templateZone("z1"))
        dao.upsertZone(overrideZone("z2"))
        dao.deleteZonesForTemplate("tmpl1")
        assertEquals(1, dao.observeZonesForOverride("ov1").first().size)
    }

    // ── deleteZonesForOverride ────────────────────────────────────────────────

    @Test
    fun deleteZonesForOverride_removesAllZones() = runTest {
        insertOverride()
        dao.upsertZone(overrideZone())
        dao.deleteZonesForOverride("ov1")
        assertTrue(dao.observeZonesForOverride("ov1").first().isEmpty())
    }

    // ── Cascade deletes ───────────────────────────────────────────────────────

    @Test
    fun templateDeleteCascadesZones() = runTest {
        insertTemplate()
        dao.upsertZone(templateZone())
        db.templateDao().deleteTemplate("tmpl1")
        assertNull(dao.getZone("z1"))
    }

    @Test
    fun overrideDeleteCascadesZones() = runTest {
        insertOverride()
        dao.upsertZone(overrideZone())
        db.templateOverrideDao().deleteOverride("ov1")
        assertNull(dao.getZone("z2"))
    }

    // ── observeEffectiveZonesForDate ──────────────────────────────────────────

    private suspend fun assignDay(templateId: String = "tmpl1", day: String = DAY) {
        db.templateDao().upsertDays(listOf(TemplateDayOfWeekEntity(dayOfWeek = day, templateId = templateId)))
    }

    @Test
    fun effectiveZones_useTheTemplateOwningTheDayWhenNoOverrideExists() = runTest {
        insertTemplate()
        assignDay()
        dao.upsertZone(templateZone())

        assertEquals(listOf("z1"), dao.observeEffectiveZonesForDate(DATE, DAY).first().map { it.id })
    }

    @Test
    fun effectiveZones_areEmptyWhenNoTemplateOwnsTheDay() = runTest {
        insertTemplate()
        dao.upsertZone(templateZone())

        assertTrue(dao.observeEffectiveZonesForDate(DATE, DAY).first().isEmpty())
    }

    @Test
    fun effectiveZones_preferTheOverrideOverTheTemplate() = runTest {
        insertTemplate()
        assignDay()
        dao.upsertZone(templateZone())
        insertOverride()
        dao.upsertZone(overrideZone())

        assertEquals(listOf("z2"), dao.observeEffectiveZonesForDate(DATE, DAY).first().map { it.id })
    }

    @Test
    fun effectiveZones_ignoreAnOverrideOnAnotherDate() = runTest {
        insertTemplate()
        assignDay()
        dao.upsertZone(templateZone())
        db.templateOverrideDao().upsertOverride(
            TemplateOverrideEntity(id = "other", name = null, dateOfDay = "2026-07-22")
        )
        dao.upsertZone(overrideZone(id = "z9", overrideId = "other"))

        assertEquals(listOf("z1"), dao.observeEffectiveZonesForDate(DATE, DAY).first().map { it.id })
    }

    /**
     * The assumption Home's schedule Flow rests on: one live subscription must re-emit for a change
     * to any of the three tables the query touches, not just `zones`. Runs on real time because Room
     * delivers invalidations on its own executor.
     */
    @Test
    fun effectiveZones_reEmitOnZoneEditDayReassignmentAndNewOverride() = runBlocking {
        insertTemplate()
        assignDay()
        dao.upsertZone(templateZone(startTime = "09:00:00"))

        val received = Channel<List<ZoneEntity>>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.IO) {
            dao.observeEffectiveZonesForDate(DATE, DAY).collect { received.send(it) }
        }
        suspend fun awaitZones(predicate: (List<ZoneEntity>) -> Boolean): List<ZoneEntity> =
            withTimeout(EMISSION_TIMEOUT_MS) {
                var zones = received.receive()
                while (!predicate(zones)) zones = received.receive()
                zones
            }

        try {
            assertEquals("09:00:00", awaitZones { it.isNotEmpty() }.single().startTime)

            // 1. the zone itself changes
            dao.upsertZone(templateZone(startTime = "10:00:00"))
            assertEquals("10:00:00", awaitZones { it.singleOrNull()?.startTime == "10:00:00" }.single().startTime)

            // 2. the day is reassigned to a template with no zones
            db.templateDao().upsertTemplate(TemplateEntity(id = "tmpl2", name = "T2"))
            assignDay(templateId = "tmpl2")
            assertTrue(awaitZones { it.isEmpty() }.isEmpty())

            // 3. an override appears for this date
            insertOverride()
            dao.upsertZone(overrideZone())
            assertEquals(listOf("z2"), awaitZones { it.isNotEmpty() }.map { it.id })
        } finally {
            collector.cancel()
        }
    }

    private companion object {
        const val DATE = "2026-07-21"
        const val DAY = "TUESDAY"
        const val EMISSION_TIMEOUT_MS = 5_000L
    }
}

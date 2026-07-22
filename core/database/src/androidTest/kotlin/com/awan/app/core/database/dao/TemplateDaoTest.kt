package com.awan.app.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.buildInMemoryDb
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateDaoTest {

    private lateinit var db: AwanDatabase
    private lateinit var dao: TemplateDao

    @Before
    fun setup() {
        db = buildInMemoryDb()
        dao = db.templateDao()
    }

    @After
    fun teardown() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun template(id: String = "tmpl1", name: String = "Morning") =
        TemplateEntity(id = id, name = name)

    private fun day(dayOfWeek: String, templateId: String = "tmpl1") =
        TemplateDayOfWeekEntity(dayOfWeek = dayOfWeek, templateId = templateId)

    // ── upsertTemplate / getTemplate ──────────────────────────────────────────

    @Test
    fun insertsAndRetrieves() = runTest {
        dao.upsertTemplate(template())
        assertEquals(template(), dao.getTemplate("tmpl1"))
    }

    @Test
    fun returnsNullForMissingTemplate() = runTest {
        assertNull(dao.getTemplate("missing"))
    }

    @Test
    fun upsertUpdatesExistingTemplate() = runTest {
        dao.upsertTemplate(template(name = "Old"))
        dao.upsertTemplate(template(name = "New"))
        assertEquals("New", dao.getTemplate("tmpl1")!!.name)
    }

    // ── observeAllTemplates ───────────────────────────────────────────────────

    @Test
    fun observesAllTemplates() = runTest {
        dao.upsertTemplates(listOf(template("a"), template("b")))
        assertEquals(2, dao.observeAllTemplates().first().size)
    }

    @Test
    fun emptyObservationWhenNoTemplates() = runTest {
        assertTrue(dao.observeAllTemplates().first().isEmpty())
    }

    // ── deleteTemplate ────────────────────────────────────────────────────────

    @Test
    fun deletesTemplate() = runTest {
        dao.upsertTemplate(template())
        dao.deleteTemplate("tmpl1")
        assertNull(dao.getTemplate("tmpl1"))
    }

    // ── TemplateDayOfWeekEntity — at-most-7-rows constraint ──────────────────

    @Test
    fun upsertsDays() = runTest {
        dao.upsertTemplate(template())
        dao.upsertDays(listOf(day("MONDAY"), day("WEDNESDAY")))
        assertEquals(2, dao.observeDaysForTemplate("tmpl1").first().size)
    }

    @Test
    fun getDayAssignment_returnsRow() = runTest {
        dao.upsertTemplate(template())
        dao.upsertDays(listOf(day("FRIDAY")))
        assertEquals("tmpl1", dao.getDayAssignment("FRIDAY")!!.templateId)
    }

    @Test
    fun getDayAssignment_returnsNullForUnassignedDay() = runTest {
        assertNull(dao.getDayAssignment("SUNDAY"))
    }

    @Test
    fun upsertDayReassignsTemplateOwner() = runTest {
        dao.upsertTemplates(listOf(template("t1"), template("t2")))
        dao.upsertDays(listOf(day("MONDAY", "t1")))
        dao.upsertDays(listOf(day("MONDAY", "t2")))   // re-assign
        assertEquals("t2", dao.getDayAssignment("MONDAY")!!.templateId)
        // Still only one row for MONDAY
        assertEquals(1, dao.observeDaysForTemplate("t2").first().count { it.dayOfWeek == "MONDAY" })
    }

    @Test
    fun deleteDaysForTemplate() = runTest {
        dao.upsertTemplate(template())
        dao.upsertDays(listOf(day("MONDAY"), day("TUESDAY")))
        dao.deleteDaysForTemplate("tmpl1")
        assertTrue(dao.observeDaysForTemplate("tmpl1").first().isEmpty())
    }

    @Test
    fun templateDeleteCascadesDays() = runTest {
        dao.upsertTemplate(template())
        dao.upsertDays(listOf(day("WEDNESDAY")))
        dao.deleteTemplate("tmpl1")
        // CASCADE: days should be gone
        assertNull(dao.getDayAssignment("WEDNESDAY"))
    }

    // ── upsertTemplateWithDays ────────────────────────────────────────────────

    @Test
    fun upsertTemplateWithDays_persistsBoth() = runTest {
        dao.upsertTemplateWithDays(template(), listOf(day("THURSDAY"), day("FRIDAY")))
        assertEquals(template(), dao.getTemplate("tmpl1"))
        assertEquals(2, dao.observeDaysForTemplate("tmpl1").first().size)
    }

    @Test
    fun upsertTemplateWithDays_replacesDays() = runTest {
        dao.upsertTemplateWithDays(template(), listOf(day("MONDAY"), day("TUESDAY")))
        dao.upsertTemplateWithDays(template(), listOf(day("SATURDAY")))
        val days = dao.observeDaysForTemplate("tmpl1").first()
        assertEquals(1, days.size)
        assertEquals("SATURDAY", days.first().dayOfWeek)
    }

    @Test
    fun upsertTemplateWithDays_emptyDaysClearsPreviousDays() = runTest {
        dao.upsertTemplateWithDays(template(), listOf(day("MONDAY")))
        dao.upsertTemplateWithDays(template(), emptyList())
        assertTrue(dao.observeDaysForTemplate("tmpl1").first().isEmpty())
    }
}

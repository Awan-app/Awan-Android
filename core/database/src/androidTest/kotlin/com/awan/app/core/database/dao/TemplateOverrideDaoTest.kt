package com.awan.app.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.buildInMemoryDb
import com.awan.app.core.database.model.TemplateOverrideEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateOverrideDaoTest {

    private lateinit var db: AwanDatabase
    private lateinit var dao: TemplateOverrideDao

    @Before
    fun setup() {
        db = buildInMemoryDb()
        dao = db.templateOverrideDao()
    }

    @After
    fun teardown() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun override(
        id: String = "ov1",
        dateOfDay: String = "2026-07-21",
        name: String? = "Holiday",
    ) = TemplateOverrideEntity(id = id, name = name, dateOfDay = dateOfDay)

    // ── upsertOverride / getOverride ──────────────────────────────────────────

    @Test
    fun insertsAndRetrieves() = runTest {
        dao.upsertOverride(override())
        assertEquals(override(), dao.getOverride("ov1"))
    }

    @Test
    fun returnsNullForMissingOverride() = runTest {
        assertNull(dao.getOverride("missing"))
    }

    @Test
    fun upsertUpdatesExistingOverride() = runTest {
        dao.upsertOverride(override(name = "Old"))
        dao.upsertOverride(override(name = "New"))
        assertEquals("New", dao.getOverride("ov1")!!.name)
    }

    @Test
    fun rejectsDuplicateDate() = runTest {
        dao.upsertOverride(override(id = "ov1", dateOfDay = "2026-07-21"))

        try {
            dao.upsertOverride(override(id = "ov2", dateOfDay = "2026-07-21"))
            fail("Expected a duplicate date to violate the unique index")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun nullNameIsAllowed() = runTest {
        dao.upsertOverride(override(name = null))
        assertNull(dao.getOverride("ov1")!!.name)
    }

    // ── upsertOverrides (batch) ───────────────────────────────────────────────

    @Test
    fun insertsBatch() = runTest {
        dao.upsertOverrides(listOf(override("o1", "2026-01-01"), override("o2", "2026-01-02")))
        assertEquals(2, dao.observeAllOverrides().first().size)
    }

    // ── observeAllOverrides — ordering ────────────────────────────────────────

    @Test
    fun observeAllOverridesOrderedByDateAsc() = runTest {
        dao.upsertOverrides(
            listOf(
                override("later", dateOfDay = "2026-12-31"),
                override("earlier", dateOfDay = "2026-01-01"),
            )
        )
        val ids = dao.observeAllOverrides().first().map { it.id }
        assertEquals(listOf("earlier", "later"), ids)
    }

    // ── getOverrideForDate ────────────────────────────────────────────────────

    @Test
    fun findsOverrideForExactDate() = runTest {
        dao.upsertOverride(override(id = "ov1", dateOfDay = "2026-07-21"))
        assertEquals("ov1", dao.getOverrideForDate("2026-07-21")!!.id)
    }

    @Test
    fun returnsNullForDateWithoutOverride() = runTest {
        assertNull(dao.getOverrideForDate("2026-07-21"))
    }

    @Test
    fun doesNotReturnOverrideForDifferentDate() = runTest {
        dao.upsertOverride(override(dateOfDay = "2026-07-20"))
        assertNull(dao.getOverrideForDate("2026-07-21"))
    }

    // ── deleteOverride ────────────────────────────────────────────────────────

    @Test
    fun deletesOverride() = runTest {
        dao.upsertOverride(override())
        dao.deleteOverride("ov1")
        assertNull(dao.getOverride("ov1"))
    }

    @Test
    fun deleteMissingOverrideIsNoop() = runTest {
        dao.upsertOverride(override())
        dao.deleteOverride("nonexistent")
        assertEquals(1, dao.observeAllOverrides().first().size)
    }
}

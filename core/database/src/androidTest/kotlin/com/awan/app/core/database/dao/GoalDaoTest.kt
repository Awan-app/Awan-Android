package com.awan.app.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.database.buildInMemoryDb
import com.awan.app.core.database.model.GoalEntity
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
class GoalDaoTest {

    private lateinit var db: com.awan.app.core.database.AwanDatabase
    private lateinit var dao: GoalDao

    @Before
    fun setup() {
        db = buildInMemoryDb()
        dao = db.goalDao()
    }

    @After
    fun teardown() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun goal(
        id: String = "g1",
        title: String = "Goal",
        status: String = "ACTIVE",
        createdAt: String = "2026-01-01T00:00:00Z",
        isInbox: Boolean = false,
    ) = GoalEntity(
        id = id,
        title = title,
        description = null,
        status = status,
        targetDate = null,
        createdAt = createdAt,
        isInbox = isInbox,
    )

    // ── upsertGoal / getGoal ──────────────────────────────────────────────────

    @Test
    fun insertsAndRetrieves() = runTest {
        val g = goal()
        dao.upsertGoal(g)
        assertEquals(g, dao.getGoal("g1"))
    }

    @Test
    fun returnsNullForMissingGoal() = runTest {
        assertNull(dao.getGoal("missing"))
    }

    @Test
    fun upsertUpdatesExistingRow() = runTest {
        dao.upsertGoal(goal(title = "Old"))
        dao.upsertGoal(goal(title = "New"))
        assertEquals("New", dao.getGoal("g1")!!.title)
    }

    // ── upsertGoals ───────────────────────────────────────────────────────────

    @Test
    fun insertsBatch() = runTest {
        dao.upsertGoals(listOf(goal("g1"), goal("g2")))
        assertEquals(2, dao.observeAllGoals().first().size)
    }

    @Test
    fun insertEmptyBatchIsNoop() = runTest {
        dao.upsertGoals(emptyList())
        assertTrue(dao.observeAllGoals().first().isEmpty())
    }

    // ── observeAllGoals — ordering ─────────────────────────────────────────────

    @Test
    fun observeAllGoalsOrderedByCreatedAtDesc() = runTest {
        dao.upsertGoals(
            listOf(
                goal("older", createdAt = "2026-01-01T00:00:00Z"),
                goal("newer", createdAt = "2026-06-01T00:00:00Z"),
            )
        )
        val ids = dao.observeAllGoals().first().map { it.id }
        assertEquals(listOf("newer", "older"), ids)
    }

    // ── observeGoalsByStatus ──────────────────────────────────────────────────

    @Test
    fun filtersByStatus() = runTest {
        dao.upsertGoals(listOf(goal("a", status = "ACTIVE"), goal("b", status = "ACHIEVED")))
        val active = dao.observeGoalsByStatus("ACTIVE").first()
        assertEquals(1, active.size)
        assertEquals("a", active.first().id)
    }

    @Test
    fun unknownStatusReturnsEmpty() = runTest {
        dao.upsertGoal(goal())
        assertTrue(dao.observeGoalsByStatus("DELETED").first().isEmpty())
    }

    // ── observeInboxGoal ──────────────────────────────────────────────────────

    @Test
    fun returnsNullWhenNoInbox() = runTest {
        dao.upsertGoal(goal(isInbox = false))
        assertNull(dao.observeInboxGoal().first())
    }

    @Test
    fun returnsInboxGoal() = runTest {
        dao.upsertGoals(listOf(goal("inbox", isInbox = true), goal("regular", isInbox = false)))
        assertEquals("inbox", dao.observeInboxGoal().first()!!.id)
    }

    // ── deleteGoal ────────────────────────────────────────────────────────────

    @Test
    fun deletesGoal() = runTest {
        dao.upsertGoal(goal())
        dao.deleteGoal("g1")
        assertNull(dao.getGoal("g1"))
    }

    @Test
    fun deleteMissingGoalIsNoop() = runTest {
        dao.upsertGoal(goal())
        dao.deleteGoal("nonexistent")
        assertEquals(1, dao.observeAllGoals().first().size)
    }

    // ── observeGoal ───────────────────────────────────────────────────────────

    @Test
    fun observeGoalEmitsNullForMissing() = runTest {
        assertNull(dao.observeGoal("none").first())
    }

    @Test
    fun observeGoalEmitsEntity() = runTest {
        dao.upsertGoal(goal())
        assertEquals(goal(), dao.observeGoal("g1").first())
    }
}

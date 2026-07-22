package com.awan.app.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.buildInMemoryDb
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: AwanDatabase
    private lateinit var dao: UserDao

    @Before
    fun setup() {
        db = buildInMemoryDb()
        dao = db.userDao()
    }

    @After
    fun teardown() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun user(
        id: String = "u1",
        email: String = "test@awan.com",
        points: Int = 0,
    ) = UserEntity(
        id = id,
        email = email,
        firstName = null,
        lastName = null,
        birthDate = null,
        points = points,
        streak = 0,
        maxStreak = 0,
    )

    private fun prefs(userId: String = "u1") = UserPreferencesEntity(
        userId = userId,
        timezone = "Africa/Cairo",
        preferredSessionDuration = 50,
        bufferBetweenSessions = 10,
        wakeupTime = "07:00:00",
        sleepTime = "23:00:00",
        schedulingType = "BALANCED",
    )

    // ── upsertUser / getUser ──────────────────────────────────────────────────

    @Test
    fun insertsAndRetrieves() = runTest {
        dao.upsertUser(user())
        assertEquals(user(), dao.getUser("u1"))
    }

    @Test
    fun returnsNullForMissingUser() = runTest {
        assertNull(dao.getUser("missing"))
    }

    @Test
    fun upsertUpdatesExistingUser() = runTest {
        dao.upsertUser(user(points = 0))
        dao.upsertUser(user(points = 100))
        assertEquals(100, dao.getUser("u1")!!.points)
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    fun deletesUser() = runTest {
        dao.upsertUser(user())
        dao.deleteUser("u1")
        assertNull(dao.getUser("u1"))
    }

    // ── observeUser ───────────────────────────────────────────────────────────

    @Test
    fun observeUserEmitsNullForMissing() = runTest {
        assertNull(dao.observeUser("none").first())
    }

    @Test
    fun observeUserEmitsEntity() = runTest {
        dao.upsertUser(user())
        assertEquals(user(), dao.observeUser("u1").first())
    }

    // ── UserPreferencesEntity ─────────────────────────────────────────────────

    @Test
    fun insertsAndRetrievesPreferences() = runTest {
        dao.upsertUser(user())
        dao.upsertPreferences(prefs())
        assertEquals(prefs(), dao.getPreferences("u1"))
    }

    @Test
    fun returnsNullPreferencesForMissingUser() = runTest {
        assertNull(dao.getPreferences("missing"))
    }

    @Test
    fun upsertUpdatesPreferences() = runTest {
        dao.upsertUser(user())
        dao.upsertPreferences(prefs().copy(schedulingType = "EASIEST_FIRST"))
        dao.upsertPreferences(prefs().copy(schedulingType = "HARDEST_FIRST"))
        assertEquals("HARDEST_FIRST", dao.getPreferences("u1")!!.schedulingType)
    }

    @Test
    fun observePreferencesEmitsNullForMissing() = runTest {
        assertNull(dao.observePreferences("none").first())
    }

    @Test
    fun preferencesDeleteCascadesOnUserDelete() = runTest {
        dao.upsertUser(user())
        dao.upsertPreferences(prefs())
        dao.deleteUser("u1")
        // CASCADE: prefs row should be gone
        assertNull(dao.getPreferences("u1"))
    }

    // ── upsertUserWithPreferences ─────────────────────────────────────────────

    @Test
    fun upsertUserWithPreferences_persistsBoth() = runTest {
        dao.upsertUserWithPreferences(user(), prefs())
        assertEquals(user(), dao.getUser("u1"))
        assertEquals(prefs(), dao.getPreferences("u1"))
    }

    @Test
    fun upsertUserWithPreferences_updatesExistingRows() = runTest {
        dao.upsertUserWithPreferences(user(points = 0), prefs().copy(schedulingType = "BALANCED"))
        dao.upsertUserWithPreferences(user(points = 200), prefs().copy(schedulingType = "EASIEST_FIRST"))
        assertEquals(200, dao.getUser("u1")!!.points)
        assertEquals("EASIEST_FIRST", dao.getPreferences("u1")!!.schedulingType)
    }
}

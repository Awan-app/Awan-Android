package com.awan.app.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AwanDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1)

        // Insert a goal to satisfy foreign key constraints
        db.execSQL("INSERT INTO goals (id, title, status, isInbox, createdAt, expiryTime) VALUES ('g1', 'My Goal', 'ACTIVE', 0, '2026-01-01T00:00:00Z', 0)")

        db.close()

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Insert valid draft pointing to g1
        db.execSQL("INSERT INTO schedule_drafts (goalId, state) VALUES ('g1', 'READY')")
        
        // Assert the data is there
        val cursor = db.query("SELECT * FROM schedule_drafts WHERE goalId = 'g1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("READY", cursor.getString(cursor.getColumnIndex("state")))
        cursor.close()

        // Test foreign key constraint: inserting a draft for a non-existent goal should fail
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL("INSERT INTO schedule_drafts (goalId, state) VALUES ('missing', 'READY')")
        }
    }
}

package com.awan.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration4To5InstrumentedTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AwanDatabase::class.java,
    )

    @Test
    fun migratesFrom4To5() {
        migrationHelper.createDatabase(TEST_DATABASE, 4).close()

        val migrated = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            AwanDatabase.MIGRATION_4_5,
        )
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'owned_customizations'").use {
            assertTrue(it.moveToFirst())
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}

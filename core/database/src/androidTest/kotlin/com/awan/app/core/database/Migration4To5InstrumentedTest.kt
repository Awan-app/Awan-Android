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
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'store_items'").use {
            assertTrue(it.moveToFirst())
        }
        migrated.close()
    }

    @Test
    fun migratesFrom5To6() {
        migrationHelper.createDatabase(TEST_DATABASE_V5_TO_V6, 5).close()

        val migrated = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE_V5_TO_V6,
            6,
            true,
            AwanDatabase.MIGRATION_5_6,
        )
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'equipped_items'").use {
            assertTrue(it.moveToFirst())
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
        const val TEST_DATABASE_V5_TO_V6 = "migration-5-to-6-test"
    }
}

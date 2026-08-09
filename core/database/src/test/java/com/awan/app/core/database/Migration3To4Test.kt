package com.awan.app.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Test
import java.lang.reflect.Proxy

import org.junit.Assert.assertTrue

class Migration3To4Test {

    @Test
    fun migration3To4_executesExpectedColumnAdditions() {
        val sqlExecuted = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                sqlExecuted.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AwanDatabase.MIGRATION_3_4.migrate(dbProxy)

        assertTrue(sqlExecuted.any { it.contains("ALTER TABLE `users` ADD COLUMN `profilePictureUrl` TEXT") })
        assertTrue(sqlExecuted.any { it.contains("ALTER TABLE `users` ADD COLUMN `isNew` INTEGER NOT NULL DEFAULT 0") })
        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `tasks_v4`") })
        assertTrue(sqlExecuted.any { it.contains("DROP TABLE `tasks`") })
    }
}

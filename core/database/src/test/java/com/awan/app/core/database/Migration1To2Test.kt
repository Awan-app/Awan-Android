package com.awan.app.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class Migration1To2Test {

    @Test
    fun migration1To2_executesExpectedSqlStatements() {
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

        AwanDatabase.MIGRATION_1_2.migrate(dbProxy)

        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `categories`") })
        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `tasks_new`") })
        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `sessions`") })
        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `cached_schedule_dates`") })
    }
}

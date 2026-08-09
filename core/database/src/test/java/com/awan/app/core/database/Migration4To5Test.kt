package com.awan.app.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class Migration4To5Test {

    @Test
    fun `migration creates the owned customizations table`() {
        val sqlExecuted = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args?.isNotEmpty() == true) sqlExecuted += args[0] as String
            null
        } as SupportSQLiteDatabase

        AwanDatabase.MIGRATION_4_5.migrate(database)

        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `store_items`") })
        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `owned_items`") })
        assertTrue(sqlExecuted.any { it.contains("CREATE TABLE IF NOT EXISTS `equipped_items`") })
    }
}

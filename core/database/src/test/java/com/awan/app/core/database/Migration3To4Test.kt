package com.awan.app.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Test
import java.lang.reflect.Proxy

class Migration3To4Test {

    @Test
    fun migration3To4_executesWithoutError() {
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, _, _ ->
            null
        } as SupportSQLiteDatabase

        AwanDatabase.MIGRATION_3_4.migrate(dbProxy)
    }
}

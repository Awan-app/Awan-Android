package com.awan.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** Creates an in-memory [AwanDatabase] that closes automatically when garbage-collected. */
fun buildInMemoryDb(): AwanDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        AwanDatabase::class.java,
    ).allowMainThreadQueries().build()

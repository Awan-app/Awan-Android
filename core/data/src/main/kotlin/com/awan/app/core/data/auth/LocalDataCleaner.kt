package com.awan.app.core.data.auth

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.database.AwanDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drops every cached row. Room is keyed by server ids alone, so whatever the last account cached
 * reads back as the next one's data — the backend then answers 404 for ids it does not own.
 *
 * An interface only so the auth tests can assert it was called: `AwanDatabase` is an abstract Room
 * class that a JVM unit test cannot construct.
 */
interface LocalDataCleaner {
    suspend fun clearAll()
}

@Singleton
class RoomLocalDataCleaner @Inject constructor(
    private val database: AwanDatabase,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LocalDataCleaner {
    override suspend fun clearAll() = withContext(ioDispatcher) { database.clearAllTables() }
}

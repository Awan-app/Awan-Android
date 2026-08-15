package com.awan.app.core.data.notifications.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.data.notifications.mapper.toUpcomingSessions
import com.awan.app.core.database.dao.CachedScheduleDateDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.domain.notifications.repository.SessionNotificationRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class SessionNotificationRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val cachedScheduleDateDao: CachedScheduleDateDao,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : SessionNotificationRepository {

    override fun observeUpcoming(startDate: LocalDate, endDate: LocalDate): Flow<List<UpcomingSession>> =
        sessionDao.observeUpcomingSessions(startDate.toString(), endDate.toString())
            .map { it.toUpcomingSessions() }
            .flowOn(ioDispatcher)

    override suspend fun getUpcoming(startDate: LocalDate, endDate: LocalDate): List<UpcomingSession> =
        withContext(ioDispatcher) {
            sessionDao.getUpcomingSessions(startDate.toString(), endDate.toString()).toUpcomingSessions()
        }

    override suspend fun isScheduleKnown(date: LocalDate): Boolean = withContext(ioDispatcher) {
        cachedScheduleDateDao.isDateCached(date.toString())
    }

    override fun observeScheduleKnown(date: LocalDate): Flow<Boolean> =
        cachedScheduleDateDao.observeIsDateCached(date.toString()).flowOn(ioDispatcher)
}

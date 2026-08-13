package com.awan.app.core.data.calendar.local

import androidx.room.withTransaction
import com.awan.app.core.data.calendar.asCalendarGoal
import com.awan.app.core.data.calendar.asEntity
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.TemplateOverrideEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.domain.calendar.repository.CalendarSnapshot
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.network.dto.GoalResponse
import com.awan.app.core.network.dto.UserProfileResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

interface CalendarLocalDataSource {
    fun observeCalendar(userId: String): Flow<CalendarSnapshot?>
    suspend fun upsertCalendar(profile: UserProfileResponse, goals: List<GoalResponse>)
}

@Singleton
class CalendarLocalDataSourceImpl @Inject constructor(
    private val database: AwanDatabase,
    private val userDao: UserDao,
    private val goalDao: GoalDao,
    private val templateDao: TemplateDao,
    private val templateOverrideDao: TemplateOverrideDao,
    private val zoneDao: ZoneDao,
) : CalendarLocalDataSource {
    override fun observeCalendar(userId: String): Flow<CalendarSnapshot?> = combine(
        userDao.observeUser(userId),
        userDao.observePreferences(userId),
        goalDao.observeGoalsByStatus("ACTIVE"),
        templateDao.observeAllTemplates(),
        templateDao.observeAllDayAssignments(),
        templateOverrideDao.observeAllOverrides(),
        zoneDao.observeAllZones(),
    ) { flowArray: Array<Any?> ->
        val user = flowArray[0] as? UserEntity
        val preferences = flowArray[1] as? UserPreferencesEntity
        @Suppress("UNCHECKED_CAST")
        val goals = flowArray[2] as List<GoalEntity>
        @Suppress("UNCHECKED_CAST")
        val templates = flowArray[3] as List<TemplateEntity>
        @Suppress("UNCHECKED_CAST")
        val dayAssignments = flowArray[4] as List<TemplateDayOfWeekEntity>
        @Suppress("UNCHECKED_CAST")
        val overrides = flowArray[5] as List<TemplateOverrideEntity>
        @Suppress("UNCHECKED_CAST")
        val allZones = flowArray[6] as List<ZoneEntity>

        user?.let {
            CalendarSnapshot(
                user = com.awan.app.core.model.CalendarUser(it.id, it.streak, preferences?.timezone.orEmpty()),
                goals = goals.filterNot(GoalEntity::isInbox).map(GoalEntity::asCalendarGoal),
                templates = templates.map { entity ->
                    entity.toDomain(
                        days = dayAssignments.filter { assignment -> assignment.templateId == entity.id },
                        zones = allZones.filter { zone -> zone.templateId == entity.id }
                    )
                },
                overrides = overrides.map { entity ->
                    entity.toDomain(
                        zones = allZones.filter { zone -> zone.templateOverrideId == entity.id }
                    )
                }
            )
        }
    }

    private fun TemplateEntity.toDomain(days: List<TemplateDayOfWeekEntity>, zones: List<ZoneEntity>): WeeklyTemplate =
        WeeklyTemplate(
            id = id,
            name = name,
            daysOfWeek = days.map { DayOfWeek.valueOf(it.dayOfWeek.uppercase()) },
            zones = zones.map { it.toDailyZone() }
        )

    private fun TemplateOverrideEntity.toDomain(zones: List<ZoneEntity>): TemplateOverride =
        TemplateOverride(
            id = id,
            name = name,
            dateOfDay = dateOfDay,
            zones = zones.map { it.toDailyZone() }
        )

    private fun ZoneEntity.toDailyZone(): DailyZone = DailyZone(
        id = id,
        name = name,
        startTime = startTime,
        endTime = endTime,
        color = color.orEmpty(),
    )

    override suspend fun upsertCalendar(profile: UserProfileResponse, goals: List<GoalResponse>) =
        database.withTransaction {
            val existing = userDao.getUser(profile.id)
            userDao.upsertUserWithPreferences(
                UserEntity(
                    id = profile.id,
                    email = profile.email,
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    birthDate = profile.birthDate,
                    points = profile.points,
                    streak = profile.streak,
                    maxStreak = profile.maxStreak,
                    profilePictureUrl = profile.profilePictureUrl ?: existing?.profilePictureUrl,
                    isNew = profile.isNew ?: existing?.isNew ?: false,
                ),
                UserPreferencesEntity(
                    userId = profile.id,
                    timezone = profile.preferences?.timezone.orEmpty(),
                    preferredSessionDuration = profile.preferences?.preferredSessionDuration ?: 0,
                    bufferBetweenSessions = profile.preferences?.bufferBetweenSessions ?: 0,
                    wakeupTime = profile.preferences?.wakeupTime.orEmpty(),
                    sleepTime = profile.preferences?.sleepTime.orEmpty(),
                    schedulingType = profile.preferences?.schedulingType ?: "BALANCED",
                ),
            )
            goalDao.upsertGoals(goals.map(GoalResponse::asEntity))
        }
}

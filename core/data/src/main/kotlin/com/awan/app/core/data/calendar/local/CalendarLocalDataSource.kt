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

    /**
     * Observes the full calendar snapshot by combining all required Room flows.
     *
     * Uses two nested [combine] calls so that every flow is received with its concrete
     * type — no `Array<Any?>` or unchecked casts.
     *
     * We load all zones via [ZoneDao.observeAllZones] and filter in-memory by
     * templateId / templateOverrideId. The zone table is inherently small (bounded by
     * templates × zones-per-template), and dynamically combining N per-parent flows
     * would require flatMapLatest chains that are significantly more complex without
     * a meaningful performance gain.
     */
    override fun observeCalendar(userId: String): Flow<CalendarSnapshot?> {
        // Inner combine: 5 typed flows → intermediate data holder
        val coreFlows = combine(
            userDao.observeUser(userId),
            userDao.observePreferences(userId),
            goalDao.observeGoalsByStatus("ACTIVE"),
            templateDao.observeAllTemplates(),
            templateDao.observeAllDayAssignments(),
        ) { user, preferences, goals, templates, dayAssignments ->
            CoreCalendarData(user, preferences, goals, templates, dayAssignments)
        }

        // Outer combine: core + overrides + zones → CalendarSnapshot
        return combine(
            coreFlows,
            templateOverrideDao.observeAllOverrides(),
            zoneDao.observeAllZones(),
        ) { core, overrides, allZones ->
            core.user?.let { user ->
                CalendarSnapshot(
                    user = com.awan.app.core.model.CalendarUser(
                        user.id, user.streak, core.preferences?.timezone.orEmpty(),
                    ),
                    goals = core.goals.filterNot(GoalEntity::isInbox)
                        .map(GoalEntity::asCalendarGoal),
                    templates = core.templates.map { entity ->
                        entity.toDomain(
                            days = core.dayAssignments.filter { it.templateId == entity.id },
                            zones = allZones.filter { it.templateId == entity.id },
                        )
                    },
                    overrides = overrides.map { entity ->
                        entity.toDomain(
                            zones = allZones.filter { it.templateOverrideId == entity.id },
                        )
                    },
                )
            }
        }
    }

    /** Typed holder so the inner [combine] can return all five values without an array. */
    private data class CoreCalendarData(
        val user: UserEntity?,
        val preferences: UserPreferencesEntity?,
        val goals: List<GoalEntity>,
        val templates: List<TemplateEntity>,
        val dayAssignments: List<TemplateDayOfWeekEntity>,
    )

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

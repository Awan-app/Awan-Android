package com.awan.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.TemplateOverrideEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.ZoneEntity

/**
 * Root Room database for the Awan app.
 *
 * All time/date values are stored as plain strings (`HH:mm:ss`, `YYYY-MM-DD`,
 * ISO-8601 instant) to avoid type-converter complexity and make the schema
 * self-documenting.
 *
 * Schema export is enabled (see `build.gradle.kts` → `room.schemaLocation`)
 * so that migrations can be validated with [MigrationTestHelper].
 */
@Database(
    entities = [
        UserEntity::class,
        UserPreferencesEntity::class,
        GoalEntity::class,
        TaskEntity::class,
        TaskDependencyEntity::class,
        TemplateEntity::class,
        TemplateDayOfWeekEntity::class,
        TemplateOverrideEntity::class,
        ZoneEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AwanDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun goalDao(): GoalDao

    abstract fun taskDao(): TaskDao

    abstract fun templateDao(): TemplateDao

    abstract fun templateOverrideDao(): TemplateOverrideDao

    abstract fun zoneDao(): ZoneDao
}

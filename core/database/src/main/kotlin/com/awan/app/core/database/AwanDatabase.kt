package com.awan.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.awan.app.core.database.dao.CachedScheduleDateDao
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.McpTokenDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.StoreDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.CachedScheduleDateEntity
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.EquippedItemEntity
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.McpTokenEntity
import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.StoreItemEntity
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
        CategoryEntity::class,
        SessionEntity::class,
        CachedScheduleDateEntity::class,
        StoreItemEntity::class,
        OwnedItemEntity::class,
        EquippedItemEntity::class,
        McpTokenEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AwanDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun goalDao(): GoalDao

    abstract fun taskDao(): TaskDao

    abstract fun templateDao(): TemplateDao

    abstract fun templateOverrideDao(): TemplateOverrideDao

    abstract fun zoneDao(): ZoneDao

    abstract fun categoryDao(): CategoryDao

    abstract fun sessionDao(): SessionDao

    abstract fun cachedScheduleDateDao(): CachedScheduleDateDao

    abstract fun storeDao(): StoreDao

    abstract fun mcpTokenDao(): McpTokenDao
}

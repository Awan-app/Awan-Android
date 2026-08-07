package com.awan.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.awan.app.core.database.dao.CachedScheduleDateDao
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.CachedScheduleDateEntity
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.SessionEntity
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
    ],
    version = 2,
    exportSchema = true,
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

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create categories table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT,
                        `icon` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                // 2. Recreate tasks table with nullable goalId and optional categoryId
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks_new` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `estimatedDuration` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `mandatory` INTEGER NOT NULL,
                        `estimatedPoints` INTEGER NOT NULL,
                        `allowTaskSplitting` INTEGER NOT NULL,
                        `goalId` TEXT,
                        `categoryId` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `tasks_new` (
                        `id`, `title`, `description`, `estimatedDuration`, `status`,
                        `mandatory`, `estimatedPoints`, `allowTaskSplitting`, `goalId`, `categoryId`
                    )
                    SELECT
                        `id`, `title`, `description`, `estimatedDuration`, `status`,
                        `mandatory`, `estimatedPoints`, `allowTaskSplitting`, `goalId`, NULL
                    FROM `tasks`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_goalId` ON `tasks` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")

                // 3. Create sessions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sessions` (
                        `id` TEXT NOT NULL,
                        `taskId` TEXT NOT NULL,
                        `zoneId` TEXT,
                        `date` TEXT NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `locked` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_taskId` ON `sessions` (`taskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_zoneId` ON `sessions` (`zoneId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_date` ON `sessions` (`date`)")

                // 4. Create cached_schedule_dates table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_schedule_dates` (
                        `date` TEXT NOT NULL,
                        `lastSyncedAt` TEXT NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

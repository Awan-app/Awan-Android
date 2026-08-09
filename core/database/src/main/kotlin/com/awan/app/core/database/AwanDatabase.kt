package com.awan.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.awan.app.core.database.dao.CachedScheduleDateDao
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.OwnedCustomizationDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.CachedScheduleDateEntity
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.OwnedCustomizationEntity
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
        OwnedCustomizationEntity::class,
    ],
    version = 5,
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

    abstract fun ownedCustomizationDao(): OwnedCustomizationDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existingColumns = mutableSetOf<String>()
                try {
                    val cursor = db.query("PRAGMA table_info(`users`)")
                    cursor.use {
                        val nameIndex = it.getColumnIndex("name")
                        while (it.moveToNext()) {
                            if (nameIndex != -1) {
                                existingColumns.add(it.getString(nameIndex))
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                if (!existingColumns.contains("expiryTime")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
                }
                if (!existingColumns.contains("profilePictureUrl")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `profilePictureUrl` TEXT")
                }
                if (!existingColumns.contains("isNew")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `isNew` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `goals` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `templates` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `cached_schedule_dates` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Ensure users table columns exist
                val existingColumns = mutableSetOf<String>()
                try {
                    val cursor = db.query("PRAGMA table_info(`users`)")
                    cursor.use {
                        val nameIndex = it.getColumnIndex("name")
                        while (it.moveToNext()) {
                            if (nameIndex != -1) {
                                existingColumns.add(it.getString(nameIndex))
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                if (!existingColumns.contains("expiryTime")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `expiryTime` INTEGER NOT NULL DEFAULT 0")
                }
                if (!existingColumns.contains("profilePictureUrl")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `profilePictureUrl` TEXT")
                }
                if (!existingColumns.contains("isNew")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `isNew` INTEGER NOT NULL DEFAULT 0")
                }

                // 2. Recreate tasks table without goalId FK
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks_v4` (
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
                        `expiryTime` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )

                val taskColumns = mutableSetOf<String>()
                try {
                    val cursor = db.query("PRAGMA table_info(`tasks`)")
                    cursor.use {
                        val nameIndex = it.getColumnIndex("name")
                        while (it.moveToNext()) {
                            if (nameIndex != -1) {
                                taskColumns.add(it.getString(nameIndex))
                            }
                        }
                    }
                } catch (_: Exception) {
                }

                val selectExpiry = if (taskColumns.contains("expiryTime")) "`expiryTime`" else "0 AS `expiryTime`"

                db.execSQL(
                    """
                    INSERT INTO `tasks_v4` (
                        `id`, `title`, `description`, `estimatedDuration`, `status`,
                        `mandatory`, `estimatedPoints`, `allowTaskSplitting`, `goalId`, `categoryId`, `expiryTime`
                    )
                    SELECT
                        `id`, `title`, `description`, `estimatedDuration`, `status`,
                        `mandatory`, `estimatedPoints`, `allowTaskSplitting`, `goalId`, `categoryId`, $selectExpiry
                    FROM `tasks`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_v4` RENAME TO `tasks`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_goalId` ON `tasks` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `owned_customizations` (
                        `userId` TEXT NOT NULL,
                        `inventoryId` TEXT NOT NULL,
                        `itemId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `type` TEXT NOT NULL,
                        `rarity` TEXT NOT NULL,
                        `acquiredAt` TEXT NOT NULL,
                        `isEquipped` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `itemId`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}

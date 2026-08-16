package com.awan.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `schedule_drafts` (
                `goalId` TEXT NOT NULL, 
                `state` TEXT NOT NULL, 
                PRIMARY KEY(`goalId`), 
                FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `schedule_draft_sessions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `goalId` TEXT NOT NULL, 
                `taskId` TEXT NOT NULL, 
                `taskTitle` TEXT, 
                `zoneId` TEXT, 
                `start` TEXT NOT NULL, 
                `end` TEXT NOT NULL, 
                `suggestionType` TEXT, 
                `suggestionReason` TEXT, 
                `overlapTaskTitle` TEXT, 
                `overlapStart` TEXT, 
                `overlapEnd` TEXT, 
                `overlapMandatory` INTEGER, 
                `overlapPoints` INTEGER, 
                `isSelected` INTEGER NOT NULL, 
                FOREIGN KEY(`goalId`) REFERENCES `schedule_drafts`(`goalId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_draft_sessions_goalId` ON `schedule_draft_sessions` (`goalId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `schedule_draft_unscheduled_tasks` (
                `taskId` TEXT NOT NULL, 
                `goalId` TEXT NOT NULL, 
                `taskTitle` TEXT NOT NULL, 
                `message` TEXT NOT NULL, 
                PRIMARY KEY(`taskId`), 
                FOREIGN KEY(`goalId`) REFERENCES `schedule_drafts`(`goalId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_draft_unscheduled_tasks_goalId` ON `schedule_draft_unscheduled_tasks` (`goalId`)")
    }
}

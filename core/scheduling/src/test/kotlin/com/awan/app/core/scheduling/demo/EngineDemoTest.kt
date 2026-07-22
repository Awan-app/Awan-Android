package com.awan.app.core.scheduling.demo

import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.entities.Zone
import com.awan.app.core.scheduling.services.DefaultScheduleEngine
import com.awan.app.core.scheduling.valueobjects.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class EngineDemoTest {

    @Test
    fun runDemo() {
        println("\n--- 🚀 STARTING SCHEDULING ENGINE DEMO ---")
        
        val engine = DefaultScheduleEngine()
        val timeZone = ZoneId.of("UTC")
        val planningDay = Instant.parse("2026-07-22T00:00:00Z")

        // 1. Setup a Zone (09:00 - 17:00)
        val workZone = Zone(
            id = UUID.randomUUID(),
            name = "Work",
            color = ZoneColor("#0000FF"),
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0)
        )

        // 2. Setup some Tasks
        val tasks = listOf(
            AwanTask(
                id = UUID.randomUUID(),
                title = "Review PRs",
                zoneID = workZone.id,
                duration = TaskDuration(45),
                isSplittable = false
            ),
            AwanTask(
                id = UUID.randomUUID(),
                title = "Feature Development",
                zoneID = workZone.id,
                duration = TaskDuration(180), // 3 hours
                isSplittable = true
            ),
            AwanTask(
                id = UUID.randomUUID(),
                title = "Team Sync",
                zoneID = workZone.id,
                duration = TaskDuration(30),
                isSplittable = false
            )
        )

        // 3. Create Snapshot
        val snapshot = SchedulingSnapshot(
            planningDay = planningDay,
            timeZone = timeZone,
            zones = listOf(workZone),
            goals = emptyList(),
            tasks = tasks,
            sessions = emptyList()
        )

        // 4. Run Engine
        val result = engine.makePlan(snapshot)

        // 5. Print Results
        println("✅ Planned ${result.todaySessionDrafts.size} sessions:")
        result.todaySessionDrafts.forEachIndexed { index, draft ->
            val task = tasks.find { it.id == draft.taskID }
            println("  [$index] Task: ${task?.title}")
            println("      Start: ${draft.timeRange.start}")
            println("      End:   ${draft.timeRange.end}")
            println("      Mins:  ${draft.timeRange.durationMinutes}")
        }

        if (result.issues.isNotEmpty()) {
            println("\n⚠️ Issues Found:")
            result.issues.forEach { issue ->
                println("  - Task ID: ${issue.taskID}, Reason: ${issue.reason}")
            }
        }
        
        println("--- 🏁 DEMO FINISHED ---\n")
    }
}

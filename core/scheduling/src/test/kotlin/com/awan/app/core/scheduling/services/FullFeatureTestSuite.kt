package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.entities.Session
import com.awan.app.core.scheduling.entities.Zone
import com.awan.app.core.scheduling.errors.SchedulingException
import com.awan.app.core.scheduling.valueobjects.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.*

class FullFeatureTestSuite {

    private val timeZone = ZoneId.of("UTC")
    private val planningDay = Instant.parse("2026-07-22T00:00:00Z")

    @Test
    fun `feature 1 - topological sorting and cycle detection`() {
        println("\n--- 🧩 FEATURE 1: TOPOLOGICAL SORTING ---")
        val sorter = StableTaskDependencySorter()
        val idA = UUID.randomUUID()
        val idB = UUID.randomUUID()
        
        val tasks = listOf(
            AwanTask(id = idB, title = "Task B", duration = TaskDuration(30), isSplittable = false, dependencyIDs = setOf(idA)),
            AwanTask(id = idA, title = "Task A", duration = TaskDuration(30), isSplittable = false)
        )
        
        val ordered = sorter.order(tasks)
        println("Input: [Task B (depends on A), Task A]")
        println("Result: ${ordered.map { it.title }}")
        assertEquals("Task A", ordered[0].title)
        assertEquals("Task B", ordered[1].title)

        val cyclicTasks = listOf(
            AwanTask(id = idA, dependencyIDs = setOf(idB), duration = TaskDuration(10), isSplittable = false),
            AwanTask(id = idB, dependencyIDs = setOf(idA), duration = TaskDuration(10), isSplittable = false)
        )
        println("Checking Cycle: [A -> B, B -> A]")
        assertThrows(SchedulingException.DependencyCycle::class.java) {
            sorter.order(cyclicTasks)
        }
        println("✅ Cycle detected correctly")
    }

    @Test
    fun `feature 2 - gap filling with occupied time`() {
        println("\n--- 🦷 FEATURE 2: GAP FILLING ---")
        val calculator = DefaultAvailabilityCalculator()
        val window = TimeRange(
            Instant.parse("2026-07-22T09:00:00Z"),
            Instant.parse("2026-07-22T12:00:00Z")
        )
        val busy = listOf(
            TimeRange(Instant.parse("2026-07-22T10:00:00Z"), Instant.parse("2026-07-22T11:00:00Z"))
        )
        
        val free = calculator.freeRanges(window, busy, null)
        println("Window: 09:00 - 12:00, Busy: 10:00 - 11:00")
        free.forEachIndexed { i, range -> println("  Gap $i: ${range.start} to ${range.end} (${range.durationMinutes} mins)") }
        
        assertEquals(2, free.size)
        assertEquals(60, free[0].durationMinutes)
        assertEquals(60, free[1].durationMinutes)
    }

    @Test
    fun `feature 3 - overnight zone calculation`() {
        println("\n--- 🌙 FEATURE 3: OVERNIGHT ZONES ---")
        val resolver = CalendarZoneWindowResolver()
        val sleepZone = Zone(
            id = UUID.randomUUID(),
            name = "Sleep",
            color = ZoneColor("#000000"),
            startTime = LocalTime(22, 0),
            endTime = LocalTime(6, 0)
        )
        
        val window = resolver.window(sleepZone, planningDay, timeZone)
        println("Zone: 22:00 to 06:00")
        println("Resolved Window: ${window.start} to ${window.end}")
        assertEquals(Instant.parse("2026-07-22T22:00:00Z"), window.start)
        assertEquals(Instant.parse("2026-07-23T06:00:00Z"), window.end)
    }

    @Test
    fun `feature 4 - candidate - continue past zone`() {
        println("\n--- ⏩ FEATURE 4: CONTINUE PAST ZONE ---")
        val engine = DefaultScheduleEngine()
        val workZone = Zone(
            id = UUID.randomUUID(),
            startTime = LocalTime(9, 0),
            endTime = LocalTime(10, 0), // 1h zone
            name = "Work",
            color = ZoneColor("#0000FF")
        )
        val task = AwanTask(
            id = UUID.randomUUID(),
            zoneID = workZone.id,
            duration = TaskDuration(90), // 1.5h task
            isSplittable = false
        )
        
        val result = engine.makePlan(SchedulingSnapshot(
            planningDay = planningDay,
            now = planningDay,
            timeZone = timeZone,
            zones = listOf(workZone),
            goals = emptyList(),
            tasks = listOf(task),
            sessions = emptyList()
        ))
        
        val issue = result.issues.first()
        println("Issue for task ${task.id}: ${issue.reason}")
        println("Resolution Candidates: ${issue.resolutionCandidates.map { it.kind }}")
        assertTrue(issue.resolutionCandidates.any { it.kind == ResolutionKind.CONTINUE_PAST_ZONE })
    }

    @Test
    fun `feature 5 - candidate - schedule next available day`() {
        println("\n--- 📅 FEATURE 5: NEXT AVAILABLE DAY ---")
        val engine = DefaultScheduleEngine()
        val workZone = Zone(
            id = UUID.randomUUID(),
            startTime = LocalTime(9, 0),
            endTime = LocalTime(11, 0), // 2h zone
            name = "Work",
            color = ZoneColor("#0000FF")
        )
        
        // Task fits in 2h, but today is full
        val task = AwanTask(
            id = UUID.randomUUID(),
            zoneID = workZone.id,
            duration = TaskDuration(60),
            isSplittable = false
        )
        val busyToday = TimeRange(
            Instant.parse("2026-07-22T09:00:00Z"),
            Instant.parse("2026-07-22T11:00:00Z")
        )
        
        val result = engine.makePlan(SchedulingSnapshot(
            planningDay = planningDay,
            now = planningDay,
            timeZone = timeZone,
            zones = listOf(workZone),
            goals = emptyList(),
            tasks = listOf(task),
            sessions = emptyList(),
            unavailableTime = listOf(busyToday)
        ))
        
        val issue = result.issues.first()
        println("Today is full. Resolution Candidates: ${issue.resolutionCandidates.map { it.kind }}")
        assertTrue(issue.resolutionCandidates.any { it.kind == ResolutionKind.SCHEDULE_NEXT_AVAILABLE_DAY })
    }

    @Test
    fun `feature 6 - task splitting within today gaps`() {
        println("\n--- ✂️ FEATURE 6: TASK SPLITTING ---")
        val engine = DefaultScheduleEngine()
        val workZone = Zone(
            id = UUID.randomUUID(),
            startTime = LocalTime(9, 0),
            endTime = LocalTime(12, 0),
            name = "Work",
            color = ZoneColor("#0000FF")
        )
        val busySession = TimeRange(
            Instant.parse("2026-07-22T10:00:00Z"),
            Instant.parse("2026-07-22T11:00:00Z")
        )
        val task = AwanTask(
            id = UUID.randomUUID(),
            zoneID = workZone.id,
            duration = TaskDuration(90),
            isSplittable = true
        )
        
        val result = engine.makePlan(SchedulingSnapshot(
            planningDay = planningDay,
            now = planningDay,
            timeZone = timeZone,
            zones = listOf(workZone),
            goals = emptyList(),
            tasks = listOf(task),
            sessions = emptyList(),
            unavailableTime = listOf(busySession)
        ))
        
        val splitCandidate = result.issues.first().resolutionCandidates.find { it.kind == ResolutionKind.SPLIT_WITHIN_TODAY }
        println("Task (90m) is splittable. Gaps are two 60m blocks.")
        println("Split drafts generated: ${splitCandidate?.sessionDrafts?.size}")
        splitCandidate?.sessionDrafts?.forEachIndexed { i, d -> println("  Session $i: ${d.timeRange.start} to ${d.timeRange.end}") }

        assertNotNull(splitCandidate)
        assertEquals(2, splitCandidate?.sessionDrafts?.size)
    }

    @Test
    fun `feature 7 - sub-minimum remainder absorption`() {
        println("\n--- 🧽 FEATURE 7: REMAINDER ABSORPTION ---")
        val engine = DefaultScheduleEngine()
        val workZone = Zone(
            id = UUID.randomUUID(),
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0),
            name = "Work",
            color = ZoneColor("#0000FF")
        )
        
        // Task 90m, one existing 87m session. Remainder = 3m.
        val task = AwanTask(
            id = UUID.randomUUID(),
            zoneID = workZone.id,
            duration = TaskDuration(90),
            isSplittable = true
        )
        
        val existingSession = Session(
            id = UUID.randomUUID(),
            taskID = task.id,
            zoneID = workZone.id,
            timeRange = TimeRange(
                Instant.parse("2026-07-22T09:00:00Z"),
                Instant.parse("2026-07-22T10:27:00Z") // 87 mins
            ),
            blocking = true,
            status = Session.Status.PLANNED
        )
        
        val result = engine.makePlan(SchedulingSnapshot(
            planningDay = planningDay,
            now = planningDay,
            timeZone = timeZone,
            zones = listOf(workZone),
            goals = emptyList(),
            tasks = listOf(task),
            sessions = listOf(existingSession),
            configuration = SchedulingConfiguration(minimumSessionMinutes = 15, futureSearchDayLimit = 14)
        ))
        
        println("Remainder: 3 mins. Existing: 87 mins.")
        println("Planned Drafts: ${result.todaySessionDrafts.size}")
        println("Planned Updates: ${result.sessionUpdates.size}")
        
        assertTrue("Should not create a new session for 3 mins", result.todaySessionDrafts.isEmpty())
        assertEquals(1, result.sessionUpdates.size)
        assertEquals(90, result.sessionUpdates.first().timeRange.durationMinutes)
        println("✅ Absorbed 3m remainder into session: ${result.sessionUpdates.first().id}")
    }
}

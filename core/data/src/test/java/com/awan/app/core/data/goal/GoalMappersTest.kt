package com.awan.app.core.data.goal

import com.awan.app.core.model.GoalStatus
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalMappersTest {

    @Test
    fun `maps ACTIVE and ACHIEVED statuses`() {
        val activeDto = GoalStatusDto.ACTIVE
        val achievedDto = GoalStatusDto.ACHIEVED

        assertEquals(GoalStatus.ACTIVE, activeDto.toModel())
        assertEquals(GoalStatus.ACHIEVED, achievedDto.toModel())
        assertEquals(GoalStatus.UNKNOWN, GoalStatusDto.UNKNOWN.toModel())
    }

    @Test
    fun `decodes unknown goal status to UNKNOWN`() {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val jsonString = """{"id":"g-unknown","title":"Future Goal","status":"FUTURE_STATUS_EMITTED_BY_BACKEND"}"""
        val response = json.decodeFromString<GoalInfoResponse>(jsonString)

        assertEquals(GoalStatus.UNKNOWN, response.toModel().status)
    }

    @Test
    fun `maps nested tasks using canonical current task mapper`() {
        val response = GoalInfoResponse(
            id = "goal-1",
            title = "Learn Compose",
            status = GoalStatusDto.ACTIVE,
            tasks = listOf(
                TaskInfoResponse(
                    id = "task-1",
                    title = "Read docs",
                    status = "COMPLETED",
                    estimatedDuration = 30,
                ),
            ),
        )

        val model = response.toModel()

        assertEquals(1, model.tasks.size)
        assertEquals("task-1", model.tasks[0].id)
        assertEquals("Read docs", model.tasks[0].title)
        assertEquals(TaskStatus.COMPLETED, model.tasks[0].status)
    }

    @Test
    fun `extracts leading emoji without corrupting plain or non-BMP titles`() {
        val emojiTitle = GoalInfoResponse(
            id = "g-1",
            title = "🎯 Learn Compose",
            status = GoalStatusDto.ACTIVE,
        ).toModel()

        assertEquals("🎯", emojiTitle.emoji)
        assertEquals("Learn Compose", emojiTitle.title)

        val surrogateEmojiTitle = GoalInfoResponse(
            id = "g-2",
            title = "🚀 Launch App",
            status = GoalStatusDto.ACTIVE,
        ).toModel()

        assertEquals("🚀", surrogateEmojiTitle.emoji)
        assertEquals("Launch App", surrogateEmojiTitle.title)

        val nonBmpPlainTitle = GoalInfoResponse(
            id = "g-3",
            title = "𐍈 Gothic Character Title",
            status = GoalStatusDto.ACTIVE,
        ).toModel()

        // 𐍈 is not an emoji symbol, title must not be corrupted
        assertEquals("𐍈 Gothic Character Title", nonBmpPlainTitle.title)
        assertEquals("🎯", nonBmpPlainTitle.emoji)
    }

    @Test
    fun `defaults emoji consistently when there is none`() {
        val plainTitle = GoalInfoResponse(
            id = "g-1",
            title = "Plain Text Goal",
            status = GoalStatusDto.ACTIVE,
        ).toModel()

        assertEquals("🎯", plainTitle.emoji)
        assertEquals("Plain Text Goal", plainTitle.title)
    }
}

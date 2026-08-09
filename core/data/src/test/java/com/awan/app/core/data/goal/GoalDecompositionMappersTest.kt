package com.awan.app.core.data.goal

import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.network.dto.GoalDecomposeBlockDto
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalProposalDto
import com.awan.app.core.network.dto.ProposedTaskDto
import com.awan.app.core.network.dto.toJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for request serialization (session-id null handling via JsonObject) and response decoding/mapping.
 */
class GoalDecompositionMappersTest {

    // --- A. Request serialization ---

    /** Json configured exactly like production NetworkModule.providesNetworkJson(). */
    private val productionJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    @Test
    fun `first message serializes sessionId as null with production Json configuration`() {
        val request = GoalDecomposeRequest(
            sessionId = null,
            message = "Learn Spanish well enough for a trip",
        )
        val encoded = productionJson.encodeToString(
            JsonObject.serializer(),
            request.toJsonObject(),
        )

        val expected = productionJson.parseToJsonElement(
            """{"sessionId":null,"message":"Learn Spanish well enough for a trip"}"""
        )
        val actual = productionJson.parseToJsonElement(encoded)

        assertEquals(expected, actual)
    }

    @Test
    fun `continuation message serializes session ID and exact message`() {
        val request = GoalDecomposeRequest(
            sessionId = "sess-abc-123",
            message = "Three months",
        )
        val encoded = productionJson.encodeToString(
            JsonObject.serializer(),
            request.toJsonObject(),
        )

        val expected = productionJson.parseToJsonElement(
            """{"sessionId":"sess-abc-123","message":"Three months"}"""
        )
        val actual = productionJson.parseToJsonElement(encoded)

        assertEquals(expected, actual)
    }

    // --- B. Response decoding / mapping ---

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `decodes ordered text and MCQ question blocks preserving order`() {
        val raw = """
            {
              "sessionId":"sess-1",
              "blocks":[
                {"type":"text","text":"Great goal!"},
                {"type":"question","text":"How long?","options":["1 month","3 months","6 months"]}
              ],
              "hasProposal":false,
              "timestamp":"2026-07-28T10:00:00Z"
            }
        """.trimIndent()

        val dto = lenientJson.decodeFromString<GoalDecomposeResponse>(raw)

        assertEquals("sess-1", dto.sessionId)
        assertEquals(2, dto.blocks.size)
        assertEquals("text", dto.blocks[0].type)
        assertEquals("Great goal!", dto.blocks[0].text)
        assertEquals("question", dto.blocks[1].type)
        assertEquals("How long?", dto.blocks[1].text)
        assertEquals(listOf("1 month", "3 months", "6 months"), dto.blocks[1].options)

        val model = dto.toDecompositionReply()
        assertEquals("sess-1", model.sessionId)
        assertEquals(2, model.blocks.size)
        assertTrue(model.blocks[0] is GoalDecompositionBlock.Text)
        assertEquals("Great goal!", (model.blocks[0] as GoalDecompositionBlock.Text).text)
        val question = model.blocks[1] as GoalDecompositionBlock.Question
        assertEquals("How long?", question.text)
        assertEquals(listOf("1 month", "3 months", "6 months"), question.options)
    }

    @Test
    fun `question with missing options is still decoded as Question with empty list`() {
        val raw = """
            {
              "sessionId":"sess-2",
              "blocks":[
                {"type":"question","text":"Describe your goal"}
              ],
              "hasProposal":false,
              "timestamp":"2026-07-28T10:00:00Z"
            }
        """.trimIndent()

        val dto = lenientJson.decodeFromString<GoalDecomposeResponse>(raw)
        val model = dto.toDecompositionReply()

        assertEquals(1, model.blocks.size)
        val q = model.blocks[0] as GoalDecompositionBlock.Question
        assertEquals("Describe your goal", q.text)
        assertTrue("Missing options should yield empty list", q.options.isEmpty())
    }

    @Test
    fun `question with empty options list remains a Question block`() {
        val raw = """
            {
              "sessionId":"sess-3",
              "blocks":[
                {"type":"question","text":"What is your timeline?","options":[]}
              ],
              "hasProposal":false,
              "timestamp":"2026-07-28T10:00:00Z"
            }
        """.trimIndent()

        val model = lenientJson.decodeFromString<GoalDecomposeResponse>(raw).toDecompositionReply()

        val q = model.blocks[0] as GoalDecompositionBlock.Question
        assertTrue(q.options.isEmpty())
    }

    @Test
    fun `decodes proposal block with duration and points`() {
        val raw = """
            {
              "sessionId":"sess-4",
              "blocks":[
                {
                  "type":"proposal",
                  "proposal":{
                    "title":"Build Web App",
                    "description":"React frontend + Spring Boot backend",
                    "targetDate":"2026-12-31",
                    "tasks":[
                      {"title":"Setup environment","estimatedDuration":120,"estimatedPoints":30}
                    ]
                  }
                }
              ],
              "hasProposal":true,
              "timestamp":"2026-07-28T10:00:00Z"
            }
        """.trimIndent()

        val model = lenientJson.decodeFromString<GoalDecomposeResponse>(raw).toDecompositionReply()

        assertTrue(model.hasProposal)
        assertEquals(1, model.blocks.size)
        val proposal = model.blocks[0] as GoalDecompositionBlock.Proposal
        assertEquals("Build Web App", proposal.proposal.title)
        assertEquals("React frontend + Spring Boot backend", proposal.proposal.description)
        assertEquals("2026-12-31", proposal.proposal.targetDate)
        assertEquals(1, proposal.proposal.tasks.size)
        val task = proposal.proposal.tasks[0]
        assertEquals("Setup environment", task.title)
        assertEquals(120, task.estimatedDuration)
        assertEquals(30, task.estimatedPoints)
    }

    @Test
    fun `unknown block type is silently ignored, supported blocks are preserved`() {
        val raw = """
            {
              "sessionId":"sess-5",
              "blocks":[
                {"type":"text","text":"Hello"},
                {"type":"future_unknown_block","someField":"value"},
                {"type":"question","text":"Choose?","options":["A","B"]}
              ],
              "hasProposal":false,
              "timestamp":"2026-07-28T10:00:00Z"
            }
        """.trimIndent()

        val model = lenientJson.decodeFromString<GoalDecomposeResponse>(raw).toDecompositionReply()

        assertEquals("Two supported blocks remain after ignoring unknown", 2, model.blocks.size)
        assertTrue(model.blocks[0] is GoalDecompositionBlock.Text)
        assertTrue(model.blocks[1] is GoalDecompositionBlock.Question)
    }

    @Test
    fun `missing optional fields in proposal do not crash decoding`() {
        // proposal without description, targetDate, or tasks
        val raw = """
            {
              "sessionId":"sess-6",
              "blocks":[
                {
                  "type":"proposal",
                  "proposal":{
                    "title":"Simple Goal"
                  }
                }
              ],
              "hasProposal":true,
              "timestamp":"2026-07-28T10:00:00Z"
            }
        """.trimIndent()

        val model = lenientJson.decodeFromString<GoalDecomposeResponse>(raw).toDecompositionReply()

        val p = (model.blocks[0] as GoalDecompositionBlock.Proposal).proposal
        assertEquals("Simple Goal", p.title)
        assertNull(p.description)
        assertNull(p.targetDate)
        assertTrue(p.tasks.isEmpty())
    }

    @Test
    fun `proposal block without required title is silently ignored`() {
        val raw = """
            {
              "sessionId":"sess-7",
              "blocks":[
                {"type":"proposal"}
              ],
              "hasProposal":false,
              "timestamp":"2026-07-28T10:00:00Z"
            }
        """.trimIndent()

        val model = lenientJson.decodeFromString<GoalDecomposeResponse>(raw).toDecompositionReply()

        assertTrue("Proposal block without proposal sub-object must be ignored", model.blocks.isEmpty())
    }
}

package com.awan.feature.addtask.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.R as DsR
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskAction
import com.awan.feature.addtask.presentation.AddTaskMode
import com.awan.feature.addtask.presentation.AddTaskState
import com.awan.feature.addtask.presentation.GoalStep
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoalFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 7, 28)

    private fun getString(resId: Int, vararg formatArgs: Any): String =
        targetContext.getString(resId, *formatArgs)

    @Test
    fun initial_showsInputMicAndSubmit() {
        var actionDispatched: AddTaskAction? = null

        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.Initial,
                        input = "Learn Spanish for trip",
                    ),
                    onAction = { actionDispatched = it },
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_initial_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn Spanish for trip").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(getString(DsR.string.ds_mic_idle)).assertIsDisplayed()

        val submitNode = composeTestRule.onNodeWithText(getString(R.string.add_task_goal_submit_initial))
        submitNode.assertIsDisplayed().assertIsEnabled()
        submitNode.performClick()

        assertEquals(AddTaskAction.Submit, actionDispatched)
    }

    @Test
    fun mcq_rendersQuestionOptionsCustomFieldAndMic_dispatchesSelectionTypingAndSubmit() {
        val actions = mutableListOf<AddTaskAction>()

        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.MultipleChoice(
                            question = "What is your main focus?",
                            options = listOf("Conversational speaking", "Grammar", "Vocabulary"),
                            selectedOption = "Conversational speaking",
                        ),
                    ),
                    onAction = { actions.add(it) },
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        composeTestRule.onNodeWithText("What is your main focus?").assertIsDisplayed()
        val selectedOptionDesc = getString(R.string.add_task_goal_mcq_option_description, "Conversational speaking")
        composeTestRule.onNodeWithContentDescription(selectedOptionDesc).assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithText("Grammar").assertIsDisplayed()

        // Assert custom field and idle mic are present
        val customFieldDesc = getString(R.string.add_task_goal_mcq_custom_description)
        composeTestRule.onNodeWithContentDescription(customFieldDesc).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(getString(DsR.string.ds_mic_idle)).assertIsDisplayed()

        // Typing dispatches InputChanged
        composeTestRule.onNodeWithContentDescription(customFieldDesc).performTextInput("Custom input")
        assertEquals(1, actions.size)
        assertEquals(AddTaskAction.InputChanged("Custom input"), actions[0])

        // Click non-selected option dispatches GoalOptionSelected
        composeTestRule.onNodeWithText("Grammar").performClick()
        assertEquals(2, actions.size)
        assertEquals(AddTaskAction.GoalOptionSelected("Grammar"), actions[1])

        // Continue dispatches Submit
        val continueNode = composeTestRule.onNodeWithText(getString(R.string.add_task_goal_mcq_continue))
        continueNode.assertIsDisplayed().assertIsEnabled()
        continueNode.performClick()

        assertEquals(3, actions.size)
        assertEquals(AddTaskAction.Submit, actions[2])
    }

    @Test
    fun writing_showsQuestionInputMicAndContinue() {
        var actionDispatched: AddTaskAction? = null

        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.WritingQuestion(question = "How many hours per week?"),
                        input = "3 hours",
                    ),
                    onAction = { actionDispatched = it },
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        composeTestRule.onNodeWithText("How many hours per week?").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 hours").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(getString(DsR.string.ds_mic_idle)).assertIsDisplayed()

        val continueNode = composeTestRule.onNodeWithText(getString(R.string.add_task_goal_writing_continue))
        continueNode.assertIsDisplayed().assertIsEnabled()
        continueNode.performClick()

        assertEquals(AddTaskAction.Submit, actionDispatched)
    }

    @Test
    fun writing_textOnlyFallback_remainsUsable() {
        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.WritingQuestion(question = ""),
                        input = "Additional goal details",
                    ),
                    onAction = {},
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_writing_fallback_prompt)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Additional goal details").assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_writing_continue)).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun preview_rendersTitleTasksAcceptAndRevision() {
        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.Preview(
                            proposal = GoalProposal(
                                title = "Conversational Spanish Goal",
                                description = "Trip preparation plan",
                                targetDate = "2026-09-01",
                                tasks = listOf(
                                    ProposedTask("Study vocab", estimatedDuration = 30, estimatedPoints = 10),
                                    ProposedTask("Practice speaking", estimatedDuration = 60, estimatedPoints = 20),
                                ),
                            ),
                        ),
                        goalSessionId = "session-123",
                    ),
                    onAction = {},
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Conversational Spanish Goal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trip preparation plan").assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_preview_task_item_title, 1, "Study vocab")).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_preview_task_item_title, 2, "Practice speaking")).assertIsDisplayed()

        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_preview_accept)).assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_preview_revision_submit)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(getString(DsR.string.ds_mic_idle)).assertIsDisplayed()
    }

    @Test
    fun preview_acceptDispatchesAccept_updateDispatchesSubmit() {
        val actions = mutableListOf<AddTaskAction>()

        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.Preview(
                            proposal = GoalProposal(
                                title = "Test Goal",
                                description = null,
                                targetDate = null,
                                tasks = listOf(ProposedTask("Task 1", null, null)),
                            ),
                        ),
                        goalSessionId = "session-123",
                        input = "Make it shorter",
                    ),
                    onAction = { actions.add(it) },
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_preview_accept)).performClick()
        assertEquals(1, actions.size)
        assertEquals(AddTaskAction.AcceptGoalProposal, actions[0])

        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_preview_revision_submit)).performClick()
        assertEquals(2, actions.size)
        assertEquals(AddTaskAction.Submit, actions[1])
    }

    @Test
    fun loading_disablesActionsAndMic() {
        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.Initial,
                        input = "Learn Spanish",
                        isSubmitting = true,
                    ),
                    onAction = {},
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.add_task_goal_submit_initial)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(getString(DsR.string.ds_mic_idle)).assertIsDisplayed().assertIsNotEnabled()

    }

    @Test
    fun serverReplyBlocks_renderInOrder() {
        composeTestRule.setContent {
            AwanTheme {
                GoalFormContent(
                    state = AddTaskState(
                        today = today,
                        mode = AddTaskMode.GOAL,
                        goalStep = GoalStep.WritingQuestion(question = "Middle Question"),
                        goalReplyBlocks = listOf(
                            GoalDecompositionBlock.Text("Top Assistant Comment"),
                            GoalDecompositionBlock.Question("Middle Question", emptyList()),
                            GoalDecompositionBlock.Text("Bottom Assistant Note"),
                        ),
                    ),
                    onAction = {},
                    isListening = false,
                    onToggleMic = {},
                    speechError = null,
                )
            }
        }

        val topText = composeTestRule.onNodeWithText("Top Assistant Comment")
        val middleQuestion = composeTestRule.onNodeWithText("Middle Question")
        val bottomText = composeTestRule.onNodeWithText("Bottom Assistant Note")

        topText.assertIsDisplayed()
        middleQuestion.assertIsDisplayed()
        bottomText.assertIsDisplayed()

        val topY = topText.getUnclippedBoundsInRoot().top
        val middleY = middleQuestion.getUnclippedBoundsInRoot().top
        val bottomY = bottomText.getUnclippedBoundsInRoot().top

        assertTrue(topY < middleY)
        assertTrue(middleY < bottomY)
    }
}

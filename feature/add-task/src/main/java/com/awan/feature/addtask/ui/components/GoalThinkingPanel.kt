package com.awan.feature.addtask.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.addtask.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val LineDwellMillis = 2000L

private val ThinkingLines = listOf(
    R.string.add_task_goal_thinking_1,
    R.string.add_task_goal_thinking_2,
    R.string.add_task_goal_thinking_3,
    R.string.add_task_goal_thinking_4,
)

/**
 * Stands in for the question that was just answered while Awan works, and for the beat after the
 * plan lands. The mascot is the sheet's own header one level up, so this is only the line beneath
 * it — two Awans on one sheet would be one too many.
 */
@Composable
fun GoalThinkingPanel(
    ready: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val standardMillis = AwanTheme.motion.standardMillis
    var lineIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(ready) {
        if (ready) return@LaunchedEffect
        while (true) {
            delay(LineDwellMillis.milliseconds)
            lineIndex = (lineIndex + 1) % ThinkingLines.size
        }
    }

    val text = stringResource(if (ready) R.string.add_task_goal_plan_ready else ThinkingLines[lineIndex])

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AwanTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                val spec = if (reduced) snap<Float>() else tween(standardMillis)
                fadeIn(spec) togetherWith fadeOut(spec)
            },
            label = "goalThinkingLine",
        ) { line ->
            val base = if (ready) AwanTheme.styles.headingText else AwanTheme.styles.bodySecondaryText
            AwanText(
                text = line,
                style = base.textStyle.copy(color = base.color, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "GoalThinkingPanel · Thinking", showBackground = true)
@Composable
private fun ThinkingPreview() {
    AwanTheme { GoalThinkingPanel(ready = false, modifier = Modifier.padding(16.dp)) }
}

@Preview(name = "GoalThinkingPanel · Ready", showBackground = true)
@Composable
private fun ReadyPreview() {
    AwanTheme { GoalThinkingPanel(ready = true, modifier = Modifier.padding(16.dp)) }
}

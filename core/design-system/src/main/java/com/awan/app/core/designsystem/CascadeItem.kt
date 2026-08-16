package com.awan.app.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val CascadeRise = 16.dp

/**
 * Reveals one element, delayed by [index] steps, so a screen's content arrives in sequence rather
 * than all at once. Each item fades in and rises [CascadeRise], staggered by
 * `AwanTheme.motion.staggerMillis`. Snaps into place instantly under [reducedMotion].
 *
 * Give siblings consecutive indices starting at 0 — the index *is* the delay multiplier, so gaps
 * read as pauses and duplicates make two items arrive together. Pass layout modifiers such as
 * `Modifier.fillMaxWidth()` here rather than on the content, since this wraps it in a [Box].
 *
 * The reveal replays whenever the composable enters a fresh subcomposition, which is what makes it
 * useful for wizard steps or tab bodies: each visit animates again.
 *
 * ```
 * Column {
 *     CascadeItem(0) { StepHeadline("Set your day") }
 *     CascadeItem(1, Modifier.fillMaxWidth()) { WakeSleepRow(...) }
 *     CascadeItem(2, Modifier.fillMaxWidth()) { DayTimeline(preview) }
 * }
 * ```
 */
@Composable
fun CascadeItem(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduced = reducedMotion()
    val motion = AwanTheme.motion
    var shown by remember { mutableStateOf(reduced) }

    LaunchedEffect(Unit) {
        if (!reduced) {
            delay((index.toLong() * motion.staggerMillis).milliseconds)
            shown = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reduced) snap() else motion.settle.spec(),
        label = "cascade",
    )

    Box(
        modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * CascadeRise.toPx()
        },
    ) {
        content()
    }
}

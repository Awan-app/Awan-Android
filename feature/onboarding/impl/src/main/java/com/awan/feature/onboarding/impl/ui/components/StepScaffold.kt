package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanStepProgress
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingStep
import com.awan.feature.onboarding.impl.ui.StepChrome

/**
 * The persistent onboarding chrome. Everything here is composed once for the whole flow and
 * animates its own properties in place; only [body] is swapped between steps.
 */
@Composable
fun StepScaffold(
    chrome: StepChrome,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    val skip by rememberUpdatedState(chrome.onSkip)
    val motion = AwanTheme.motion
    // Critically damped: the mascot's bounds shrink to zero when hidden, and an overshooting
    // spring would interpolate through a negative size, which Constraints rejects.
    val mascotBounds = remember(motion) {
        BoundsTransform { _, _ ->
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = motion.settle.stiffness)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
            .padding(horizontal = AwanTheme.spacing.xl)
            .padding(top = AwanTheme.spacing.sm, bottom = AwanTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackSlot(visible = chrome.showBack, onBack = onBack)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = chrome.onSkip != null, enter = fadeIn(), exit = fadeOut()) {
                AwanButton(onClick = { skip?.invoke() }, variant = AwanButtonVariant.Quiet) {
                    AwanText(stringResource(R.string.onboarding_skip), style = AwanTheme.styles.skipLink)
                }
            }
        }
        Spacer(Modifier.size(AwanTheme.spacing.sm))

        StepProgress(current = chrome.progressCurrent)

        LookaheadScope {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
                verticalArrangement = if (chrome.centeredContent) Arrangement.Center else Arrangement.Top,
            ) {
                Mascot(
                    expression = chrome.mascot,
                    width = chrome.mascotWidth,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .animateBounds(this@LookaheadScope, boundsTransform = mascotBounds),
                )
                Box(
                    if (chrome.centeredContent) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.weight(1f).fillMaxWidth()
                    },
                ) {
                    body()
                }
            }
        }

        Spacer(Modifier.size(AwanTheme.spacing.md))
        StepFooter(chrome)
    }
}

/**
 * The scrolling content region of a single step. Lives inside the animated region so each step
 * keeps its own arrangement and scroll behaviour.
 */
@Composable
fun StepBody(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    arrangement: Arrangement.Vertical = Arrangement.spacedBy(AwanTheme.spacing.sm),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = arrangement,
        content = content,
    )
}

/**
 * Kept in composition on every step, including Welcome, so the spring fill is continuous across
 * the whole flow rather than restarting when the bar first appears.
 */
@Composable
private fun StepProgress(current: Int) {
    val visible = current > 0
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "progressAlpha",
    )
    val height by animateDpAsState(
        targetValue = if (visible) AwanTheme.spacing.lg else 0.dp,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "progressGap",
    )
    AwanStepProgress(
        current = current,
        count = OnboardingStep.DOT_COUNT,
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
    )
    Spacer(Modifier.size(height))
}

/**
 * Sizing the mascot to zero rather than removing it keeps the single instance alive across every
 * step, so its float never restarts and animateBounds can carry it between the Welcome and Name
 * layouts as one travelling object.
 */
@Composable
private fun Mascot(expression: MascotExpression?, width: Dp, modifier: Modifier = Modifier) {
    val visible = width > 0.dp
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "mascotAlpha",
    )
    Box(modifier.graphicsLayer { this.alpha = alpha }) {
        Crossfade(targetState = expression ?: MascotExpression.Idle, label = "mascotExpression") {
            AwanMascot(expression = it, width = width)
        }
    }
}

@Composable
private fun StepFooter(chrome: StepChrome) {
    val reduced = reducedMotion()
    val pop = remember { Animatable(1f) }
    val spec = AwanTheme.motion.playful.spec<Float>()
    val enabled = chrome.primaryEnabled

    LaunchedEffect(enabled) {
        if (enabled && !reduced) {
            pop.animateTo(1.06f, spec)
            pop.animateTo(1f, spec)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        AwanButton(
            onClick = { chrome.primary.onClick() },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
        ) {
            Crossfade(targetState = chrome.primary.label, label = "primaryLabel") { AwanText(it) }
        }
        AnimatedContent(
            targetState = chrome.secondary,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "secondaryAction",
        ) { secondary ->
            if (secondary != null) {
                AwanButton(onClick = { secondary.onClick() }, variant = AwanButtonVariant.Quiet) {
                    AwanText(secondary.label, style = AwanTheme.styles.skipLink)
                }
            }
        }
    }
}

@Composable
fun StepHeadline(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
        AwanText(title, style = AwanTheme.styles.titleText)
        if (subtitle != null) {
            AwanText(subtitle, style = AwanTheme.styles.bodySecondaryText)
        }
    }
}

@Composable
fun CenteredHeadline(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        androidx.compose.foundation.text.BasicText(
            text = title,
            style = AwanTheme.typography.display.copy(
                color = AwanTheme.colors.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            ),
        )
        if (subtitle != null) {
            androidx.compose.foundation.text.BasicText(
                text = subtitle,
                style = AwanTheme.typography.body.copy(
                    color = AwanTheme.colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
            )
        }
    }
}

/** Fixed-width so the chevron can fade without shifting the skip link beside it. */
@Composable
private fun BackSlot(visible: Boolean, onBack: () -> Unit) {
    val label = stringResource(R.string.onboarding_back)
    Box {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            AwanButton(
                onClick = onBack,
                variant = AwanButtonVariant.Secondary,
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = label },
            ) {
                BackChevron()
            }
        }
    }
}

@Composable
private fun BackChevron() {
    Box(Modifier.size(20.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val path = Path().apply {
                moveTo(size.width * 0.62f, size.height * 0.24f)
                lineTo(size.width * 0.34f, size.height * 0.52f)
                lineTo(size.width * 0.62f, size.height * 0.8f)
            }
            drawPath(path, color = ink, style = stroke)
        }
    }
}

private val ink = Color(0xFF16455E)

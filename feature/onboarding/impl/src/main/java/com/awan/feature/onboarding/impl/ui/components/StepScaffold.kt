package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanBackButton
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
    notice: String? = null,
    body: @Composable () -> Unit,
) {
    val skip by rememberUpdatedState(chrome.onSkip)
    val motion = AwanTheme.motion
    Column(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
            .padding(top = AwanTheme.spacing.sm, bottom = AwanTheme.spacing.md),
    ) {
        // Both buttons stay composed on every step. Welcome is the only step with neither, and
        // AnimatedVisibility would drop them once their fade ended — collapsing the row to zero
        // height a beat after the transition looked finished, which reads as a late shift.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackSlot(visible = chrome.showBack, onBack = onBack)
            Spacer(Modifier.weight(1f))
            val skipVisible = chrome.onSkip != null
            AwanButton(
                onClick = { skip?.invoke() },
                enabled = skipVisible,
                variant = AwanButtonVariant.Quiet,
                modifier = Modifier.fadeSlot(skipVisible),
            ) {
                AwanText(stringResource(R.string.onboarding_skip), style = AwanTheme.styles.skipLink)
            }
        }
        Spacer(Modifier.size(AwanTheme.spacing.sm))

        StepProgress(
            current = chrome.progressCurrent,
            modifier = Modifier.padding(horizontal = AwanTheme.spacing.xl),
        )

        // The body slot's measurement rules must not change between steps. AnimatedContent
        // keeps the outgoing body composed for the whole transition, and a body that lost its
        // height bound mid-flight would report its full scroll height and blow up the region.
        val lead by animateDpAsState(
            targetValue = chrome.leadingSpace,
            animationSpec = motion.settle.spec(),
            label = "leadingSpace",
        )
        // The mascot's own width animates, so the column reflows it continuously and the drawn
        // image tracks its slot exactly. Animating the layout bounds instead (animateBounds) left
        // the image snapping to its new size while the slot sprang toward it — that was the jump.
        val mascotWidth by animateDpAsState(
            targetValue = chrome.mascotWidth,
            animationSpec = motion.settle.spec(),
            label = "mascotWidth",
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.xl)
                .clipToBounds(),
        ) {
            Spacer(Modifier.height(lead))
            Mascot(
                expression = chrome.mascot,
                width = mascotWidth,
                visible = chrome.mascotWidth > 0.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                body()
            }
        }

        Spacer(Modifier.size(AwanTheme.spacing.md))
        StepFooter(chrome, notice)
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
private fun StepProgress(current: Int, modifier: Modifier = Modifier) {
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
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
    Spacer(Modifier.size(height))
}

/**
 * Sizing the mascot to zero rather than removing it keeps the single instance alive across every
 * step, so its float never restarts and it glides between the Welcome and Name layouts.
 *
 * [visible] tracks the *target* width, not the animated one, so the fade runs alongside the resize
 * instead of only at the frame it reaches zero.
 */
@Composable
private fun Mascot(
    expression: MascotExpression?,
    width: Dp,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "mascotAlpha",
    )
    Box(modifier.graphicsLayer { this.alpha = alpha }) {
        Crossfade(targetState = expression ?: MascotExpression.Idle, label = "mascotExpression") {
            // settle is a bouncy spring; a negative width would reach Constraints and crash.
            AwanMascot(expression = it, width = width.coerceAtLeast(0.dp))
        }
    }
}

@Composable
private fun StepFooter(chrome: StepChrome, notice: String?) {
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
        if (notice != null) {
            InlineNotice(
                text = notice,
                tone = NoticeTone.Error,
                modifier = Modifier.padding(horizontal = AwanTheme.spacing.xl),
            )
        }
        AwanButton(
            onClick = { chrome.primary.onClick() },
            enabled = enabled,
            isLoading = chrome.primaryLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.xl)
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
        AwanText(
            text = title,
            style = AwanTheme.typography.display.copy(
                color = AwanTheme.colors.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            ),
        )
        if (subtitle != null) {
            AwanText(
                text = subtitle,
                style = AwanTheme.typography.body.copy(
                    color = AwanTheme.colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
            )
        }
    }
}

/**
 * Fades in place so the chevron never shifts the skip link beside it, nor the content below when
 * Welcome leaves the row with nothing in it.
 */
@Composable
private fun BackSlot(visible: Boolean, onBack: () -> Unit) {
    AwanBackButton(
        onClick = onBack,
        enabled = visible,
        modifier = Modifier.fadeSlot(visible),
    )
}

/**
 * Fades a control without letting it leave the layout. [hideFromAccessibility] plus the caller's
 * `enabled = false` keeps a faded-out control off TalkBack and out of the click path, which
 * removing it from composition used to do for free.
 */
@Composable
private fun Modifier.fadeSlot(visible: Boolean): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "fadeSlot",
    )
    return graphicsLayer { this.alpha = alpha }
        .then(if (visible) Modifier else Modifier.semantics { hideFromAccessibility() })
}


package com.awan.feature.onboarding.impl.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanStepProgress
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.onboarding.impl.presentation.OnboardingStep

/**
 * Shared onboarding chrome: sky background, a back-chevron + Skip header, the segmented step
 * progress, a scrolling [content] region, and a pinned [footer].
 */
@Composable
fun StepScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    onSkip: (() -> Unit)? = null,
    progressCurrent: Int = 0,
    scrollableContent: Boolean = true,
    contentArrangement: Arrangement.Vertical = Arrangement.spacedBy(AwanTheme.spacing.sm),
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
            .padding(horizontal = AwanTheme.spacing.xl)
            .padding(top = AwanTheme.spacing.sm, bottom = AwanTheme.spacing.md),
    ) {
        if (showBack || onSkip != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showBack) {
                    AwanIconButton(onClick = onBack, contentDescription = "Back", icon = { BackChevron() })
                } else {
                    Spacer(Modifier.size(38.dp))
                }
                Spacer(Modifier.weight(1f))
                if (onSkip != null) {
                    AwanButton(onClick = onSkip, variant = AwanButtonVariant.Quiet) {
                        AwanText("Skip", style = AwanTheme.styles.skipLink)
                    }
                }
            }
            Spacer(Modifier.size(AwanTheme.spacing.sm))
        }

        if (progressCurrent > 0) {
            AwanStepProgress(current = progressCurrent, count = OnboardingStep.DOT_COUNT)
            Spacer(Modifier.size(AwanTheme.spacing.lg))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scrollableContent) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = contentArrangement,
            content = content,
        )

        Spacer(Modifier.size(AwanTheme.spacing.md))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
            content = footer,
        )
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

@Composable
fun ChangeAnytimeChip(modifier: Modifier = Modifier) {
    com.awan.app.core.designsystem.AwanChip(
        text = "You can change this anytime",
        modifier = modifier,
        leadingIcon = { SyncGlyph() },
    )
}

@Composable
private fun BackChevron() {
    Box(Modifier.size(20.dp)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            )
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.62f, size.height * 0.24f)
                lineTo(size.width * 0.34f, size.height * 0.52f)
                lineTo(size.width * 0.62f, size.height * 0.8f)
            }
            drawPath(path, color = ink, style = stroke)
        }
    }
}

@Composable
private fun SyncGlyph() {
    androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.8.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawArc(sky, 150f, 180f, false, style = stroke)
        drawArc(sky, -30f, 180f, false, style = stroke)
    }
}

private val ink = androidx.compose.ui.graphics.Color(0xFF16455E)
private val sky = androidx.compose.ui.graphics.Color(0xFF1D84CC)

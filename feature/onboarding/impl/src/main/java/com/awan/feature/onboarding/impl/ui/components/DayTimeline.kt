package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.DayPreviewModel
import com.awan.feature.onboarding.impl.ui.formatClock

private val SkyDawn = Color(0xFFFFF5D6)
private val SkyDay = Color(0xFFDCEFFF)
private val SkyNight = Color(0xFF20344A)

private val TrackHeight = 44.dp
private val MinSegmentWidth = 2.dp
private val MinLabelWidth = 44.dp

/**
 * The waking day as one horizontal bar: dawn on the leading edge, night on the trailing one, with
 * zone segments laid along it. The full width *is* the waking window — [DayPreviewModel] already
 * expresses every fraction relative to it — so no screen height is spent on hours the user is asleep.
 */
@Composable
fun DayTimeline(
    preview: DayPreviewModel,
    modifier: Modifier = Modifier,
    showZones: Boolean = true,
) {
    AwanCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AwanText(stringResource(R.string.onboarding_day_preview_label), style = AwanTheme.styles.metaText)
            AwanText(
                stringResource(R.string.onboarding_day_preview_open_sky, wakingHours(preview)),
                style = AwanTheme.styles.metaText,
            )
        }
        Spacer(Modifier.height(AwanTheme.spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .clip(AwanTheme.shapes.chip)
                .background(Brush.horizontalGradient(0f to SkyDawn, 0.55f to SkyDay, 1f to SkyNight)),
        ) {
            if (showZones && preview.zones.any { it.enabled }) {
                ZoneSegments(preview)
            } else {
                HintPill()
            }
        }
        Spacer(Modifier.height(AwanTheme.spacing.xxs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Endpoint("☀️", formatClock(preview.wakeMinutes))
            Endpoint("🌙", formatClock(preview.sleepMinutes))
        }
        preview.task?.let { task ->
            Spacer(Modifier.height(AwanTheme.spacing.xxs))
            AwanText(
                stringResource(R.string.onboarding_day_preview_starred_task, task.label),
                style = AwanTheme.styles.captionText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ZoneSegments(preview: DayPreviewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullWidth = maxWidth
        preview.zones.forEach { block ->
            key(block.id) {
                val startFraction by animateFloatAsState(
                    targetValue = block.startFraction,
                    animationSpec = AwanTheme.motion.settle.spec(),
                    label = "segmentStart",
                )
                val endFraction by animateFloatAsState(
                    targetValue = block.endFraction,
                    animationSpec = AwanTheme.motion.settle.spec(),
                    label = "segmentEnd",
                )
                val segmentWidth = fullWidth * (endFraction - startFraction)
                if (segmentWidth > MinSegmentWidth) {
                    Box(
                        modifier = Modifier
                            .offset(x = fullWidth * startFraction)
                            .width(segmentWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 1.dp)
                            .clip(AwanTheme.shapes.chip)
                            .background(
                                Color(block.colorArgb).copy(alpha = if (block.enabled) 0.9f else 0.22f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (segmentWidth > MinLabelWidth) {
                            AwanText(
                                block.label,
                                style = AwanTheme.styles.zoneBandLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                        }
                    }
                }
            }
        }
        preview.task?.let { task ->
            Box(
                modifier = Modifier
                    .offset(x = fullWidth * task.startFraction)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(AwanTheme.colors.surface),
            )
        }
    }
}

@Composable
private fun BoxScope.HintPill() {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .clip(AwanTheme.shapes.pill)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        AwanText(stringResource(R.string.onboarding_day_preview_zones_hint), style = AwanTheme.styles.captionText)
    }
}

/** Both endpoints render through the one [AwanStyles.clockText] style so the two times can never diverge. */
@Composable
private fun Endpoint(glyph: String, time: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
    ) {
        AwanText(glyph, style = AwanTheme.styles.captionText, maxLines = 1)
        AwanText(time, style = AwanTheme.styles.clockText, maxLines = 1)
    }
}

private fun wakingHours(preview: DayPreviewModel): Int {
    val minutes = (preview.sleepMinutes - preview.wakeMinutes).mod(24 * 60)
    return (minutes / 60).coerceAtLeast(1)
}

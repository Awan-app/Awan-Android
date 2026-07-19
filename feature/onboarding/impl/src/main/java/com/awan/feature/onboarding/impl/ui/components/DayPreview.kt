package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
private val Sunrise = Color(0xFFD9771C)
private val Moonlight = Color(0xFFEAF6FF)

/** The live day: a vertical sky (dawn → night) with wake/sleep endpoints and, optionally, zone bands. */
@Composable
fun DayPreview(
    preview: DayPreviewModel,
    modifier: Modifier = Modifier,
    showZones: Boolean = true,
    panelHeight: androidx.compose.ui.unit.Dp = 200.dp,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .clip(AwanTheme.shapes.card)
                .background(Brush.verticalGradient(0f to SkyDawn, 0.55f to SkyDay, 1f to SkyNight))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            EdgeLabel(formatClock(preview.wakeMinutes), "☀️", Sunrise)
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp)) {
                if (showZones && preview.zones.any { it.enabled }) {
                    ZoneBands(preview)
                } else {
                    HintPill()
                }
            }
            EdgeLabel(formatClock(preview.sleepMinutes), "🌙", Moonlight)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ZoneBands(preview: DayPreviewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullHeight = maxHeight
        preview.zones.forEach { block ->
            val top = fullHeight * block.startFraction
            val bandHeight = fullHeight * (block.endFraction - block.startFraction)
            if (bandHeight.value > 2f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = top)
                        .height(bandHeight)
                        .padding(vertical = 1.dp)
                        .clip(AwanTheme.shapes.chip)
                        .background(Color(block.colorArgb).copy(alpha = if (block.enabled) 0.9f else 0.22f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (bandHeight.value > 22f) {
                        AwanText(
                            block.label,
                            style = AwanTheme.styles.zoneBandLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
        preview.task?.let { task ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = fullHeight * task.startFraction)
                    .padding(horizontal = 6.dp)
                    .clip(AwanTheme.shapes.chip)
                    .background(AwanTheme.colors.surface)
                    .border(2.dp, Color(task.colorArgb), AwanTheme.shapes.chip)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                AwanText(
                    stringResource(R.string.onboarding_day_preview_starred_task, task.label),
                    style = AwanTheme.styles.captionText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HintPill() {
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

@Composable
private fun EdgeLabel(time: String, glyph: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.text.BasicText(
            text = time,
            style = AwanTheme.typography.heading.copy(color = color),
        )
        AwanText(glyph, style = AwanTheme.styles.headingText)
    }
}

private fun wakingHours(preview: DayPreviewModel): Int {
    val minutes = (preview.sleepMinutes - preview.wakeMinutes).mod(24 * 60)
    return (minutes / 60).coerceAtLeast(1)
}

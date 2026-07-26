package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



internal fun formatHoursValue(hours: Double): String {
    return if (hours % 1.0 == 0.0) {
        hours.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", hours)
    }
}

@Composable
fun AwanProgressSummaryCard(
    title: String = stringResource(R.string.ds_todays_plan),
    subtitle: String = "",
    completionText: String = "",
    completedHours: Double? = null,
    totalHours: Double? = null,
    segments: List<CategoryProgressSegment>,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = cardShape,
                spotColor = AwanTheme.colors.line,
            )
            .clip(cardShape)
            .background(AwanTheme.colors.surface)
            .border(1.5.dp, AwanTheme.colors.line, cardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    AwanText(
                        text = title,
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 14.sp,
                            color = AwanTheme.colors.textPrimary,
                        ),
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        AwanText(
                            text = subtitle,
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                color = AwanTheme.colors.textSecondary,
                            ),
                        )
                    }
                }

                if (completedHours != null && totalHours != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AwanText(
                            text = formatHoursValue(completedHours),
                            style = AwanTheme.typography.heading.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AwanTheme.colors.sky,
                            ),
                        )
                        AwanText(
                            text = " / ${formatHoursValue(totalHours)} hrs",
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AwanTheme.colors.textSecondary,
                            ),
                        )
                    }
                } else if (completionText.isNotBlank()) {
                    AwanText(
                        text = completionText,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SegmentedProgressBar(
                segments = segments,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
        }
    }
}

@Composable
private fun SegmentedProgressBar(
    segments: List<CategoryProgressSegment>,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(99.dp)

    Row(
        modifier = modifier
            .clip(barShape)
            .background(AwanTheme.colors.line.copy(alpha = 0.20f)),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (segments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(AwanTheme.colors.line.copy(alpha = 0.25f)),
            )
        } else {
            val totalWeight = segments.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
            segments.forEach { segment ->
                val targetColor = if (segment.isCompleted) {
                    segment.color
                } else {
                    segment.color.copy(alpha = 0.20f)
                }

                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 350),
                    label = "segmentFillAnimation",
                )

                Box(
                    modifier = Modifier
                        .weight((segment.weight / totalWeight).coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(animatedColor),
                )
            }
        }
    }
}

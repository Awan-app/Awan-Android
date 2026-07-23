package com.awan.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CategoryProgressSegment(
    val color: Color,
    val weight: Float,
)

@Composable
fun AwanProgressSummaryCard(
    title: String = "TODAY'S PLAN",
    subtitle: String = "4 tasks · 6h scheduled",
    completionText: String = "2 of 4 complete",
    segments: List<CategoryProgressSegment>,
    onAddTaskClick: () -> Unit,
    onAddGoalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                spotColor = Color(0xFF94A3B8),
            )
            .clip(cardShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFFE2E8F0), cardShape)
            .padding(16.dp),
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
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B),
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AwanText(
                        text = subtitle,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                        ),
                    )
                }

                AwanText(
                    text = completionText,
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-colored segmented progress bar
            SegmentedProgressBar(
                segments = segments,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: + TASK & + GOAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillActionButton3D(
                    text = "+ TASK",
                    backgroundColor = Color(0xFF2563EB),
                    borderColor = Color(0xFF60A5FA),
                    onClick = onAddTaskClick,
                )

                Spacer(modifier = Modifier.width(16.dp))

                PillActionButton3D(
                    text = "+ GOAL",
                    backgroundColor = Color(0xFF8B5CF6),
                    borderColor = Color(0xFFC084FC),
                    onClick = onAddGoalClick,
                )
            }
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
            .background(Color(0xFFE2E8F0)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val totalWeight = segments.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        segments.forEach { segment ->
            if (segment.weight > 0f) {
                Box(
                    modifier = Modifier
                        .weight(segment.weight / totalWeight)
                        .fillMaxHeight()
                        .background(segment.color),
                )
            }
        }
    }
}

@Composable
private fun PillActionButton3D(
    text: String,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(99.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                spotColor = backgroundColor,
            )
            .clip(shape)
            .background(backgroundColor)
            .border(2.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        AwanText(
            text = text,
            style = AwanTheme.typography.button.copy(
                fontSize = 15.sp,
                color = Color.White,
            ),
        )
    }
}

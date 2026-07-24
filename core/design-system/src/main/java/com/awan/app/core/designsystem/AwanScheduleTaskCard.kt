package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface TaskStatus {
    data object Pending : TaskStatus
    data object Completed : TaskStatus
    data object Fixed : TaskStatus
}

@Composable
fun AwanScheduleTaskCard(
    title: String,
    timeRange: String,
    category: TaskCategory,
    status: TaskStatus,
    points: Int? = null,
    isDragging: Boolean = false,
    onStatusToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(category.containerColor)
            .border(
                width = if (isDragging) 2.dp else 1.5.dp,
                color = if (isDragging) category.color else category.borderColor,
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // 6-dot drag handle
                DragHandleDots()

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    AwanText(
                        text = title,
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B),
                        ),
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AwanText(
                            text = timeRange,
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                            ),
                        )

                        // Compact Animated celebration points pill when completed
                        AnimatedVisibility(
                            visible = status is TaskStatus.Completed && points != null,
                            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                                initialScale = 0.6f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            ),
                            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.6f),
                        ) {
                            val ptsShape = RoundedCornerShape(99.dp)
                            Box(
                                modifier = Modifier
                                    .clip(ptsShape)
                                    .background(Color(0xFFFEF9C3))
                                    .border(1.dp, Color(0xFFFDE047), ptsShape)
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AwanText(
                                        text = "🪙",
                                        style = AwanTheme.typography.caption.copy(fontSize = 9.5.sp),
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    AwanText(
                                        text = "+$points pts",
                                        style = AwanTheme.typography.caption.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF854D0E),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right side: Fixed badge + Action checkmark / Lock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (status is TaskStatus.Fixed) {
                    // "FIXED" pill badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        AwanText(
                            text = "FIXED",
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9),
                            ),
                        )
                    }
                }

                // Completion status icon
                StatusActionIcon(
                    status = status,
                    categoryColor = category.color,
                    onClick = onStatusToggle,
                )
            }
        }
    }
}

@Composable
private fun DragHandleDots() {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .background(Color(0xFF94A3B8), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .background(Color(0xFF94A3B8), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun StatusActionIcon(
    status: TaskStatus,
    categoryColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else Modifier

    when (status) {
        TaskStatus.Completed -> {
            Box(
                modifier = modifier
                    .then(clickModifier)
                    .size(30.dp)
                    .shadow(2.dp, CircleShape, spotColor = categoryColor.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(categoryColor)
                    .border(1.5.dp, categoryColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = "✓",
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
            }
        }

        TaskStatus.Pending -> {
            // Empty matching circle
            Box(
                modifier = modifier
                    .then(clickModifier)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, categoryColor, CircleShape),
            )
        }

        TaskStatus.Fixed -> {
            // Flat Purple lock icon
            Box(
                modifier = modifier
                    .then(clickModifier)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6))
                    .border(2.dp, Color(0xFFC084FC), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = "🔒",
                    style = AwanTheme.typography.body.copy(fontSize = 14.sp),
                )
            }
        }
    }
}

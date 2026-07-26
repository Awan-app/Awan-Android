package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay



@Composable
fun AwanScheduleTaskCard(
    title: String,
    timeRange: String,
    category: TaskCategory,
    status: TaskStatus,
    points: Int? = null,
    isDragging: Boolean = false,
    displayMode: SessionDisplayMode = SessionDisplayMode.Full,
    onStatusToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isCompleted = status is TaskStatus.Completed
    val cardShape = RoundedCornerShape(16.dp)

    val strokeColor = if (isCompleted)
        AwanTheme.colors.line
    else
        category.color.copy(alpha = 0.85f)

    val cardBgColor by animateColorAsState(
        targetValue = if (isCompleted)
            AwanTheme.colors.disabledSurface.copy(alpha = 0.95f)
        else
            category.color.copy(alpha = 0.06f),
        animationSpec = tween(durationMillis = 200),
        label = "cardBg",
    )

    when (displayMode) {
        SessionDisplayMode.Pill -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCompleted) AwanTheme.colors.disabledSurface
                        else category.color.copy(alpha = 0.75f)
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AwanTheme.colors.disabledSurface.copy(alpha = 0.5f)),
                    )
                }
            }
        }

        SessionDisplayMode.Compact -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = if (isDragging) 0.dp else 1.dp,
                        shape = cardShape,
                        spotColor = category.color.copy(alpha = if (isDragging) 0f else 0.06f),
                    )
                    .clip(cardShape)
                    .background(AwanTheme.colors.surface)
                    .background(cardBgColor)
                    .border(width = 1.dp, color = strokeColor, shape = cardShape),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(2.dp)
                        .fillMaxSize()
                        .background(
                            if (isCompleted) Color(0xFFCBD5E1)
                            else category.color.copy(alpha = 0.35f)
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AwanText(
                        text = title,
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted)
                                AwanTheme.colors.textSecondary
                            else
                                AwanTheme.colors.textPrimary,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (status is TaskStatus.Fixed) {
                        AwanText(
                            text = "🔒",
                            style = AwanTheme.typography.caption.copy(fontSize = 9.sp),
                        )
                    }
                }
            }
        }

        SessionDisplayMode.Full -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = if (isDragging) 0.dp else 2.dp,
                        shape = cardShape,
                        spotColor = category.color.copy(alpha = if (isDragging) 0f else 0.08f),
                        ambientColor = Color.Black.copy(alpha = 0.03f),
                    )
                    .clip(cardShape)
                    .background(AwanTheme.colors.surface)
                    .background(cardBgColor)
                    .border(
                        width = 1.5.dp,
                        color = strokeColor,
                        shape = cardShape,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(3.5.dp)
                        .fillMaxSize()
                        .background(
                            if (isCompleted)
                                Color(0xFFCBD5E1)
                            else
                                category.color.copy(alpha = 0.35f)
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AwanText(
                                text = title,
                                style = AwanTheme.typography.heading.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted)
                                        AwanTheme.colors.textSecondary
                                    else
                                        AwanTheme.colors.textPrimary,
                                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )

                            if (status is TaskStatus.Fixed) {
                                Spacer(modifier = Modifier.width(4.dp))
                                AwanText(
                                    text = "🔒",
                                    style = AwanTheme.typography.caption.copy(fontSize = 10.sp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        AwanText(
                            text = timeRange,
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCompleted)
                                    AwanTheme.colors.meta
                                else
                                    category.color.copy(alpha = 0.90f),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (points != null && points > 0) {
                            AwanText(
                                text = stringResource(R.string.ds_pts, points),
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted)
                                        AwanTheme.colors.textSecondary
                                    else
                                        category.color.copy(alpha = 0.95f),
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (onStatusToggle != null) {
                            CalendarStyleCheckbox(
                                isCompleted = isCompleted,
                                categoryColor = category.color,
                                onClick = onStatusToggle,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarStyleCheckbox(
    isCompleted: Boolean,
    categoryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "checkScale",
    )

    val greenCompletedColor = Color(0xFF22C55E)

    val bgColor by animateColorAsState(
        targetValue = if (isCompleted) greenCompletedColor else Color.Transparent,
        animationSpec = tween(180),
        label = "checkBg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isCompleted) greenCompletedColor else categoryColor,
        animationSpec = tween(180),
        label = "checkBorder",
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(24.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    haptic.performHapticFeedback(
                        if (isCompleted) HapticFeedbackType.TextHandleMove
                        else HapticFeedbackType.Confirm,
                    )
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isCompleted,
            enter = fadeIn(tween(120)) + scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.3f),
        ) {
            AwanText(
                text = "✓",
                style = AwanTheme.typography.heading.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                ),
            )
        }
    }
}

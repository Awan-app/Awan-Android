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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

@Composable
fun AwanHeaderBar(
    userName: String,
    streakCount: Int,
    subtitleText: String = "Clear skies — 6 things floating today",
    greetingPrefix: String = "Good morning",
    selectedDateText: String = "Today · Wed, Jul 15 📅",
    onPreviousDayClick: () -> Unit = {},
    onNextDayClick: () -> Unit = {},
    onDatePillClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Top Row: Cloud + Greeting + Subtitle (Left) | Streak Badge (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Cloud Mascot Icon
                AwanText(
                    text = "☁️",
                    style = AwanTheme.typography.title.copy(fontSize = 32.sp),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    AwanText(
                        text = "$greetingPrefix, $userName",
                        style = AwanTheme.typography.title.copy(
                            fontSize = 20.sp,
                            color = Color(0xFF1E293B),
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AwanText(
                        text = subtitleText,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Modern 3D Streak Pill Badge (Right)
            val streakShape = RoundedCornerShape(99.dp)
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 4.dp,
                        shape = streakShape,
                        spotColor = Color(0xFFFF9838),
                    )
                    .clip(streakShape)
                    .background(Color(0xFFF1F5F9))
                    .border(1.5.dp, Color(0xFFE2E8F0), streakShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AwanText(
                        text = "🔥",
                        style = AwanTheme.typography.body.copy(fontSize = 15.sp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    AwanText(
                        text = streakCount.toString(),
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 15.sp,
                            color = Color(0xFFD97706),
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Date Selector Bar: [<] [ Today · Wed, Jul 15 📅 ] [>]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Arrow Button (<)
            NavArrowButton3D(
                arrowText = "‹",
                onClick = onPreviousDayClick,
            )

            // Center Date Pill
            val pillShape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = pillShape,
                        spotColor = Color(0xFF94A3B8),
                    )
                    .clip(pillShape)
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFE2E8F0), pillShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDatePillClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = selectedDateText,
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B),
                    ),
                )
            }

            // Right Arrow Button (>)
            NavArrowButton3D(
                arrowText = "›",
                onClick = onNextDayClick,
            )
        }
    }
}

@Composable
private fun NavArrowButton3D(
    arrowText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(
                elevation = 4.dp,
                shape = buttonShape,
                spotColor = Color(0xFF94A3B8),
            )
            .clip(buttonShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFFE2E8F0), buttonShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AwanText(
            text = arrowText,
            style = AwanTheme.typography.title.copy(
                fontSize = 20.sp,
                color = Color(0xFF1E293B),
            ),
        )
    }
}

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AwanHeaderBar(
    userName: String,
    streakCount: Int,
    pointsCount: Int = 0,
    mascotExpression: MascotExpression = MascotExpression.Greet,
    subtitleText: String = "Clear skies — 6 things floating today",
    greetingPrefix: String = "Good afternoon",
    selectedDateText: String = "Today · Wed, Jul 15",
    onPreviousDayClick: () -> Unit = {},
    onNextDayClick: () -> Unit = {},
    onDatePillClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Top Section: Light Modern Card Container wrapping Greeting, Badges & Compact Mascot
        val cardShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, cardShape, spotColor = Color(0xFF0EA5E9).copy(alpha = 0.15f))
                .clip(cardShape)
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), cardShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Column: Greeting, Name, Dual Badges (Streak + Coins)
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    AwanText(
                        text = "$greetingPrefix,",
                        style = AwanTheme.typography.title
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    AwanText(
                        text = userName,
                        style = AwanTheme.typography.title,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dual Badges Row (Streak 🔥 | Coins 🪙)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val badgeShape = RoundedCornerShape(12.dp)

                        // Compact Translucent Streak Pill Badge (🔥)
                        Box(
                            modifier = Modifier
                                .clip(badgeShape)
                                .background(Color(0xFFEA580C).copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.25f), badgeShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AwanText(
                                    text = "🔥",
                                    style = AwanTheme.typography.body.copy(fontSize = 13.sp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                AwanText(
                                    text = streakCount.toString(),
                                    style = AwanTheme.typography.heading.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFEA580C),
                                    ),
                                )
                            }
                        }

                        // Compact Translucent Coins Pill Badge (🪙)
                        Box(
                            modifier = Modifier
                                .clip(badgeShape)
                                .background(Color(0xFFEAB308).copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFFEAB308).copy(alpha = 0.25f), badgeShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AwanText(
                                    text = "🪙",
                                    style = AwanTheme.typography.body.copy(fontSize = 13.sp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                AwanText(
                                    text = pointsCount.toString(),
                                    style = AwanTheme.typography.heading.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFCA8A04),
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right: Large Animated Awan Cloud Mascot
                AwanMascot(
                    expression = mascotExpression,
                    width = 80.dp,
                )
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

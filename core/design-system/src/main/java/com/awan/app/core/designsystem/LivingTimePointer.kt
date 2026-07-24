package com.awan.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun LivingTimePointer(
    pointerY: Dp,
    currentTimeFormatted: String,
    activePointerColor: Color,
    pulseScale: Float,
    pulseAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.zIndex(50f)) {
        Box(
            modifier = Modifier
                .offset(x = 52.dp - 12.dp, y = pointerY - 12.dp)
                .size(24.dp)
                .zIndex(50f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size((14 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(activePointerColor.copy(alpha = pulseAlpha)),
            )
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(activePointerColor)
                    .border(2.dp, Color.White, CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .offset(x = (-10).dp, y = pointerY - 11.dp)
                .zIndex(50f),
        ) {
            val badgeShape = RoundedCornerShape(99.dp)
            Box(
                modifier = Modifier
                    .shadow(3.dp, badgeShape, spotColor = activePointerColor)
                    .clip(badgeShape)
                    .background(activePointerColor)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), badgeShape)
                    .padding(horizontal = 6.dp, vertical = 2.5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AwanText(
                        text = currentTimeFormatted,
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

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

import androidx.compose.ui.res.stringResource

@Composable
fun AwanScheduleAlertCard(
    message: String,
    onFixItClick: () -> Unit,
    onLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.ds_schedule_conflict_title),
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                spotColor = Color(0xFFF59E0B),
            )
            .clip(shape)
            .background(Color(0xFFFFFBEB))
            .border(1.5.dp, Color(0xFFFDE68A), shape)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AwanText(
                    text = "⚠️",
                    style = AwanTheme.typography.body.copy(fontSize = 16.sp),
                )
                AwanText(
                    text = title,
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 14.sp,
                        color = Color(0xFFB45309),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AwanText(
                text = message,
                style = AwanTheme.typography.body.copy(
                    fontSize = 13.5.sp,
                    color = Color(0xFF78350F),
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(99.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLaterClick,
                        )
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                ) {
                    AwanText(
                        text = stringResource(R.string.ds_later),
                        style = AwanTheme.typography.button.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF92400E),
                        ),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(99.dp), spotColor = Color(0xFF2563EB))
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFF2563EB))
                        .border(1.5.dp, Color(0xFF60A5FA), RoundedCornerShape(99.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onFixItClick,
                        )
                        .padding(horizontal = 22.dp, vertical = 7.dp),
                ) {
                    AwanText(
                        text = stringResource(R.string.ds_fix_it),
                        style = AwanTheme.typography.button.copy(
                            fontSize = 13.sp,
                            color = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

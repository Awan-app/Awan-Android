package com.awan.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                elevation = 4.dp,
                shape = shape,
                spotColor = AwanTheme.colors.zoneTangerine.copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(AwanTheme.colors.skyDawn.copy(alpha = 0.35f))
            .border(1.5.dp, AwanTheme.colors.zoneTangerine.copy(alpha = 0.40f), shape)
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
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AwanText(
                text = message,
                style = AwanTheme.typography.body.copy(
                    fontSize = 13.5.sp,
                    color = AwanTheme.colors.textSecondary,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AwanButton(
                    onClick = onLaterClick,
                    variant = AwanButtonVariant.Quiet,
                ) {
                    AwanText(
                        text = stringResource(R.string.ds_later),
                        style = AwanTheme.typography.button.copy(fontSize = 13.sp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AwanButton(
                    onClick = onFixItClick,
                    variant = AwanButtonVariant.Primary,
                ) {
                    AwanText(
                        text = stringResource(R.string.ds_fix_it),
                        style = AwanTheme.typography.button.copy(fontSize = 13.sp),
                    )
                }
            }
        }
    }
}

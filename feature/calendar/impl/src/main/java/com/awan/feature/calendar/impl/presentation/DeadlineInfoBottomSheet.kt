package com.awan.feature.calendar.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanSurface
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.SoapBubbleStyle
import com.awan.feature.calendar.impl.R
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineInfoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = AwanTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("calendar_deadline_info_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.lg)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // Header with close button at top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AwanText(
                    text = stringResource(R.string.calendar_deadline_sheet_title),
                    style = AwanTheme.styles.titleText,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("calendar_deadline_sheet_close_button"),
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = stringResource(R.string.calendar_deadline_sheet_close),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AwanTheme.spacing.sm))

            // Visual banner: ~30% height showing horizontal curving shader threads (Green on right -> Red on left)
            DeadlineAirflowBanner(
                modifier = Modifier.testTag("calendar_deadline_airflow_banner"),
                isReducedMotion = isReducedMotion,
            )

            Spacer(modifier = Modifier.height(AwanTheme.spacing.md))

            // Subtitle explanation
            AwanText(
                text = stringResource(R.string.calendar_deadline_sheet_subtitle),
                style = AwanTheme.styles.bodySecondaryText,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(AwanTheme.spacing.md))

            // 3 Color Phase Descriptions
            ColorPhaseRow(
                dotColor = SoapBubbleStyle.CalmEmerald,
                title = stringResource(R.string.calendar_deadline_phase_green_title),
                description = stringResource(R.string.calendar_deadline_phase_green_desc),
            )

            Spacer(modifier = Modifier.height(AwanTheme.spacing.xs))

            ColorPhaseRow(
                dotColor = SoapBubbleStyle.MidAmber,
                title = stringResource(R.string.calendar_deadline_phase_amber_title),
                description = stringResource(R.string.calendar_deadline_phase_amber_desc),
            )

            Spacer(modifier = Modifier.height(AwanTheme.spacing.xs))

            ColorPhaseRow(
                dotColor = SoapBubbleStyle.UrgentRed,
                title = stringResource(R.string.calendar_deadline_phase_red_title),
                description = stringResource(R.string.calendar_deadline_phase_red_desc),
            )

            Spacer(modifier = Modifier.height(AwanTheme.spacing.xl))
        }
    }
}

@Composable
private fun ColorPhaseRow(
    dotColor: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    AwanSurface(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AwanTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(AwanTheme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = title,
                    style = AwanTheme.styles.bodyText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                AwanText(
                    text = description,
                    style = AwanTheme.styles.metaText,
                )
            }
        }
    }
}

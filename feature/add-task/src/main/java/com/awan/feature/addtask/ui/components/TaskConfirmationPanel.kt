package com.awan.feature.addtask.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChipDot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.TaskConfirmation
import java.time.LocalDate

/**
 * The sheet's receipt. It replaces the form once the task exists, because a task whose time the
 * engine chose is exactly the one the user has to see before it disappears — and a toast that slides
 * away is not something you can read a start time off.
 */
@Composable
fun TaskConfirmationPanel(
    confirmation: TaskConfirmation,
    today: LocalDate,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
    ) {
        CascadeItem(0, Modifier.fillMaxWidth()) {
            AwanText(stringResource(R.string.add_task_confirm_title), style = AwanTheme.styles.titleText)
        }

        CascadeItem(1, Modifier.fillMaxWidth()) {
            AwanCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                    AwanText(confirmation.title, style = AwanTheme.styles.headingText)
                    ScheduleLine(confirmation = confirmation, today = today)
                }
            }
        }

        CascadeItem(2, Modifier.fillMaxWidth()) {
            AwanButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                AwanText(stringResource(R.string.add_task_confirm_done))
            }
        }
    }
}

/** Names the first session, or says plainly that there isn't one rather than inventing a time. */
@Composable
private fun ScheduleLine(confirmation: TaskConfirmation, today: LocalDate) {
    val colors = AwanTheme.colors
    val start = confirmation.firstSession

    if (start == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        ) {
            AwanChipDot(tone = colors.meta, active = false)
            AwanText(
                stringResource(R.string.add_task_confirm_inbox),
                style = AwanTheme.styles.bodySecondaryText,
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
        AwanText(
            stringResource(R.string.add_task_confirm_first_session),
            style = AwanTheme.styles.metaText,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        ) {
            AwanChipDot(tone = colors.zoneSky)
            AwanText(
                rememberWhenLabel(startAt = start, today = today),
                style = AwanTheme.styles.bodyText,
            )
            confirmation.durationMinutes?.let {
                AwanText(durationLabel(it), style = AwanTheme.styles.metaText)
            }
        }
    }
}

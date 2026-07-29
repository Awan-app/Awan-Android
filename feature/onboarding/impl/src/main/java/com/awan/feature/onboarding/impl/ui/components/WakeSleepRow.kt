package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanTimePickerDialog
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.formatClock

/** "☀️ I usually wake up at … 7:00 AM ⌄" — a rim row that opens a native time picker. */
@Composable
fun WakeSleepRow(
    glyph: String,
    label: String,
    minutes: Int,
    selected: Boolean,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }

    AwanCard(modifier = modifier.fillMaxWidth(), selected = selected, onClick = { picking = true }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                AwanText(glyph, style = AwanTheme.styles.headingText)
                AwanText(label, style = AwanTheme.styles.bodySecondaryText)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
                AwanText(formatClock(minutes), style = AwanTheme.styles.clockText, maxLines = 1)
                AwanText("⌄", style = AwanTheme.styles.metaText, modifier = Modifier.clearAndSetSemantics {})
            }
        }
    }

    if (picking) {
        AwanTimePickerDialog(
            confirmLabel = stringResource(R.string.onboarding_time_picker_set),
            cancelLabel = stringResource(R.string.onboarding_time_picker_cancel),
            initialMinutes = minutes,
            onDismiss = { picking = false },
            onConfirm = {
                picking = false
                onPick(it)
            },
        )
    }
}

package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.designsystem.AwanTimePickerDialog
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.formatClock

private enum class Editing { None, Start, End }

/** Hours for one zone, reusing the shared [AwanTimePickerDialog] for the actual picking. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneSheet(
    zone: Zone,
    onDismiss: () -> Unit,
    onSetWindow: (start: Int, end: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var editing by remember { mutableStateOf(Editing.None) }
    val zoneColor = Color(zone.colorArgb)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.xl)
                .padding(bottom = AwanTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(zoneColor))
                AwanText(
                    stringResource(R.string.onboarding_zone_sheet_title, zone.name),
                    style = AwanTheme.styles.titleText,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
            ) {
                TimeField(
                    label = stringResource(R.string.onboarding_zone_starts),
                    minutes = zone.startMinutes,
                    onClick = { editing = Editing.Start },
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = stringResource(R.string.onboarding_zone_ends),
                    minutes = zone.endMinutes,
                    onClick = { editing = Editing.End },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    when (editing) {
        Editing.Start -> AwanTimePickerDialog(
            confirmLabel = stringResource(R.string.onboarding_time_picker_set),
            cancelLabel = stringResource(R.string.onboarding_time_picker_cancel),
            initialMinutes = zone.startMinutes,
            onDismiss = { editing = Editing.None },
            onConfirm = {
                editing = Editing.None
                onSetWindow(it, zone.endMinutes)
            },
        )

        Editing.End -> AwanTimePickerDialog(
            confirmLabel = stringResource(R.string.onboarding_time_picker_set),
            cancelLabel = stringResource(R.string.onboarding_time_picker_cancel),
            initialMinutes = zone.endMinutes,
            onDismiss = { editing = Editing.None },
            onConfirm = {
                editing = Editing.None
                onSetWindow(zone.startMinutes, it)
            },
        )

        Editing.None -> Unit
    }
}

@Composable
private fun TimeField(label: String, minutes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AwanCard(onClick = onClick, modifier = modifier) {
        AwanText(label, style = AwanTheme.styles.metaText, modifier = Modifier.fillMaxWidth())
        AwanText(
            formatClock(minutes),
            style = AwanTheme.styles.clockText,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

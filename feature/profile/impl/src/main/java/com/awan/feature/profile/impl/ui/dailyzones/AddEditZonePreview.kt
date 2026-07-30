package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.ui.components.ZoneCardBody

@Composable
fun AddEditZonePreview(
    previewZone: DailyZone,
    isEdit: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AwanText(
            text = stringResource(R.string.profile_daily_zones_zone_preview),
            style = AwanTheme.styles.bodyText.copy(
                textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
            )
        )
        ZoneCardBody(
            zone = previewZone,
            modifier = Modifier.fillMaxWidth()
        )
        AwanText(
            text = if (!isEdit) {
                stringResource(R.string.profile_daily_zones_zone_will_be_added)
            } else {
                stringResource(R.string.profile_daily_zones_zone_in_default)
            },
            style = AwanTheme.styles.captionText.copy(
                color = AwanTheme.colors.textSecondary
            )
        )
    }
}

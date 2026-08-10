package com.awan.feature.marketplace.impl.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.model.StoreItemType
import com.awan.feature.marketplace.impl.R

@Composable
fun CategoryTabs(
    selectedCategory: StoreItemType?,
    onCategorySelected: (StoreItemType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AwanChip(
            label = stringResource(R.string.marketplace_category_all),
            tone = if (selectedCategory == null) AwanChipTone.Sky else AwanChipTone.Neutral,
            active = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            leading = {}
        )

        StoreItemType.entries.forEach { type ->
            val label = when(type) {
                StoreItemType.FRAME -> stringResource(R.string.marketplace_category_frames)
                StoreItemType.SKIN -> stringResource(R.string.marketplace_category_skins)
                StoreItemType.THEME -> stringResource(R.string.marketplace_category_themes)
                StoreItemType.ICON -> stringResource(R.string.marketplace_category_icons)
            }
            AwanChip(
                label = label,
                tone = if (selectedCategory == type) AwanChipTone.Sky else AwanChipTone.Neutral,
                active = selectedCategory == type,
                onClick = { onCategorySelected(type) },
                leading = {}
            )
        }
        
        Spacer(modifier = Modifier.width(20.dp))
    }
}

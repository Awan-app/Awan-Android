package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.R

@Composable
fun CategoryChip(
    category: Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val tone = remember(category.id) {
        val tones = listOf(
            AwanChipTone.Sky,
            AwanChipTone.Violet,
            AwanChipTone.Lavender,
            AwanChipTone.Tangerine
        )
        tones[category.id.hashCode().coerceAtLeast(0) % tones.size]
    }

    AwanChip(
        label = category.name,
        tone = tone,
        onClick = onEditClick,
        leading = null,
        trailing = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.profile_category_delete),
                tint = AwanTheme.colors.textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onDeleteClick() }
            )
        }
    )
}

package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.rememberHapticClick
import com.awan.feature.goals.impl.R
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.SlidersHorizontal

@Composable
internal fun GoalsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isFilterActive: Boolean = false,
    onFilterClick: (() -> Unit)? = null,
) {
    val colors = AwanTheme.colors
    val hapticFilterClick = rememberHapticClick(onClick = { onFilterClick?.invoke() })
    AwanTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.goals_search_placeholder),
        leadingContent = {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingContent = onFilterClick?.let {
            {
                Icon(
                    imageVector = Lucide.SlidersHorizontal,
                    contentDescription = null,
                    tint = if (isFilterActive) colors.sky else colors.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = hapticFilterClick)
                )
            }
        },
        imeAction = ImeAction.Search,
        modifier = modifier
    )
}

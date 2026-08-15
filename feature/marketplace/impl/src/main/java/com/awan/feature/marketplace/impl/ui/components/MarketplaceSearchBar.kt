package com.awan.feature.marketplace.impl.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanTextField
import com.awan.feature.marketplace.impl.R

@Composable
fun MarketplaceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AwanTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.marketplace_search_placeholder),
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = AwanTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = modifier
    )
}

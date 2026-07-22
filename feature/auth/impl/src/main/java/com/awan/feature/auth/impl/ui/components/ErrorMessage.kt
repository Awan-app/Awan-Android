package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@Composable
fun ErrorMessage(
    message: UiText,
    modifier: Modifier = Modifier,
) {
    val text = message.asString()
    Row(
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Assertive
            contentDescription = text
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = AwanTheme.colors.destructive,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.size(4.dp))
        AwanText(
            text = text,
            style = AwanTheme.styles.errorText,
            modifier = Modifier.semantics { contentDescription = "" },
        )
    }
}

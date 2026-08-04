package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide

/** The app's single back-navigation button, styled after the onboarding chevron. */
@Composable
fun AwanBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = stringResource(R.string.ds_back),
) {
    AwanButton(
        onClick = onClick,
        enabled = enabled,
        variant = AwanButtonVariant.Secondary,
        modifier = modifier.semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
    ) {
        // Lucide's chevrons carry no auto-mirroring, and back points rightwards in Arabic. Left
        // untinted so the icon takes the variant's animated content colour through pressed and disabled.
        Icon(
            imageVector = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                Lucide.ChevronRight
            } else {
                Lucide.ChevronLeft
            },
            contentDescription = null,
            modifier = Modifier.size(BackChevronSize),
        )
    }
}

private val BackChevronSize = 20.dp

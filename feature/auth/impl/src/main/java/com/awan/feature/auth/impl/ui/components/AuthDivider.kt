package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@Composable
fun AuthDivider(
    modifier: Modifier = Modifier,
    label: String = "OR",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Or, alternatively" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .styleable(null, AwanTheme.styles.authDivider),
        )

        AwanText(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = AwanTheme.styles.captionText,
        )

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .styleable(null, AwanTheme.styles.authDivider),
        )
    }
}

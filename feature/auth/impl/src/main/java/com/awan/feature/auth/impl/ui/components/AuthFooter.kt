package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@Composable
fun AuthFooter(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
fun EmailDisplay(
    email: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Code sent to $email."
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AwanText(
            text = "We floated a 6-digit code to",
            style = AwanTheme.styles.bodyText,
        )
        Spacer(modifier = Modifier.size(4.dp))
        AwanText(
            text = email,
            style = AwanTheme.styles.bodyText,
        )
    }
}

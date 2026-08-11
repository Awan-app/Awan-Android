package com.awan.app.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AwanTopToast(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = Icons.Rounded.Notifications,
    durationMs: Long = 4000L,
    onDismiss: () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(key1 = message, key2 = durationMs) {
        if (durationMs > 0) {
            delay(durationMs)
            onDismiss()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable {
                            onClick()
                            onDismiss()
                        }
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(20.dp),
            color = AwanTheme.colors.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (!title.isNullOrEmpty()) {
                        AwanText(
                            text = title,
                            style = AwanTheme.styles.titleText,
                        )
                    }
                    AwanText(
                        text = message,
                        style = AwanTheme.styles.bodySecondaryText,
                    )
                }
            }
        }
    }
}

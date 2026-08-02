package com.awan.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

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
        BackChevron()
    }
}

@Composable
private fun BackChevron() {
    // AwanButton provides the variant's animated content colour; reading it keeps the chevron
    // correct in both themes and through the disabled and pressed states.
    val ink = LocalContentColor.current
    Box(Modifier.size(20.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val path = Path().apply {
                moveTo(size.width * 0.62f, size.height * 0.24f)
                lineTo(size.width * 0.34f, size.height * 0.52f)
                lineTo(size.width * 0.62f, size.height * 0.8f)
            }
            drawPath(path, color = ink, style = stroke)
        }
    }
}
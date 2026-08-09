package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import android.annotation.SuppressLint
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AwanSurface(
    modifier: Modifier = Modifier,
    style: Style = Style,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.styleable(null, AwanTheme.styles.surface, style),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Preview(name = "Skyward type and surface · Light", showBackground = true)
@Composable
private fun LightSurfacePreview() {
    SurfacePreview(darkTheme = false)
}

@Preview(name = "Skyward type and surface · Dark", showBackground = true)
@Composable
private fun DarkSurfacePreview() {
    SurfacePreview(darkTheme = true)
}

@SuppressLint("HardcodedText")
@Composable
private fun SurfacePreview(darkTheme: Boolean) {
    AwanTheme(dark = darkTheme, light = !darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AwanText(text = "Good morning, Sam", style = AwanTheme.styles.displayText)
            AwanText(text = "Your sky is ready.", style = AwanTheme.styles.bodyText)
            AwanSurface(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AwanText(text = "Clear skies!", style = AwanTheme.styles.headingText)
                    AwanText(
                        text = "Nothing scheduled yet.",
                        style = AwanTheme.styles.captionText,
                    )
                }
            }
        }
    }
}

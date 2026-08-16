package com.awan.feature.splash.impl.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextStyle
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CloudDrift
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.splash.impl.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WORDMARK_DELAY_MILLIS = 220L

/** Matches the cloud band Home settles into, so the hand-off between the two reads as one sky. */
private val CloudBandHeight = 180.dp

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val colors = AwanTheme.colors
    val motion = AwanTheme.motion

    val mascotEntry = remember { Animatable(if (reduced) 1f else 0f) }
    val wordmarkEntry = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        launch { mascotEntry.animateTo(1f, motion.bouncy.spec()) }
        delay(WORDMARK_DELAY_MILLIS)
        wordmarkEntry.animateTo(1f, motion.settle.spec())
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // CloudDrift opens on its own sky, which starts at backgroundStart. Landing the page
        // gradient on that same colour exactly where the band begins hides the seam between them.
        val bandTop = 1f - (CloudBandHeight / maxHeight).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colors.skyMidday,
                        bandTop to colors.backgroundStart,
                        1f to colors.backgroundStart,
                    ),
                ),
        )

        CloudDrift(
            height = CloudBandHeight,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
        ) {
            AwanMascot(
                expression = MascotExpression.Greet,
                width = 176.dp,
                blinkEnabled = true,
                modifier = Modifier.graphicsLayer {
                    val entry = mascotEntry.value
                    alpha = entry
                    scaleX = 0.6f + 0.4f * entry
                    scaleY = 0.6f + 0.4f * entry
                },
            )
            Spacer(Modifier.height(AwanTheme.spacing.lg))
            AwanText(
                text = stringResource(R.string.splash_app_name),
                style = AwanTextStyle(AwanTheme.typography.display, colors.textPrimary),
                modifier = Modifier.graphicsLayer {
                    val entry = wordmarkEntry.value
                    alpha = entry
                    translationY = (1f - entry) * 16.dp.toPx()
                },
            )
        }
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    AwanTheme { SplashScreen() }
}

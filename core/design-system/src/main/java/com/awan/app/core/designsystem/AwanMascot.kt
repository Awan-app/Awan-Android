package com.awan.app.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Awan the cloud mascot — the brand's biggest stage. Idle expressions gently float; [MascotExpression.Celebrate]
 * does a springy cheer. Static vector frames stand in until the APNG set is delivered.
 */


private const val MASCOT_ASPECT = 60f / 82f

@Composable
fun AwanMascot(
    expression: MascotExpression,
    modifier: Modifier = Modifier,
    width: Dp = 20.dp,
    blinkEnabled: Boolean = false,
) {
    var isBlinking by remember { mutableStateOf(false) }

    if (blinkEnabled && expression != MascotExpression.Idle) {
        LaunchedEffect(expression, blinkEnabled) {
            while (true) {
                delay((3000..6000).random().toLong().milliseconds)
                isBlinking = true
                delay(150.milliseconds)
                isBlinking = false
                if ((0..1).random() == 1) {
                    delay(100.milliseconds)
                    isBlinking = true
                    delay(120.milliseconds)
                    isBlinking = false
                }
            }
        }
    }

    val drawable = when {
        isBlinking -> R.drawable.awan_mascot_idle
        expression == MascotExpression.Greet -> R.drawable.awan_mascot_greet
        expression == MascotExpression.Curious -> R.drawable.awan_mascot_curious
        expression == MascotExpression.Celebrate -> R.drawable.awan_mascot_celebrate
        else -> R.drawable.awan_mascot_idle
    }

    val transition = rememberInfiniteTransition(label = "mascot")
    val cheering = expression == MascotExpression.Celebrate
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (cheering) 1100 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mascotPhase",
    )

    val wave = kotlin.math.sin(phase * 2f * Math.PI).toFloat()
    val translateY = if (cheering) -7f * kotlin.math.abs(wave) else -4f * ((wave + 1f) / 2f)
    val rotation = if (cheering) 3f * wave else 0f

    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = modifier
            .size(width = width, height = width * MASCOT_ASPECT)
            .graphicsLayer {
                translationY = translateY * density
                rotationZ = rotation
            },
    )
}

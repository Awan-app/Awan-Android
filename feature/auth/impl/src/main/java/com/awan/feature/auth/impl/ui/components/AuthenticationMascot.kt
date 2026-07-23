package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.awan.app.core.designsystem.MascotSize
import com.awan.app.core.designsystem.MascotState
import com.awan.app.core.designsystem.R

@Composable
fun AuthenticationMascot(
    state: MascotState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(MascotSize),
        contentAlignment = Alignment.Center,
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.mascot))

        LottieAnimation(
            composition = composition,
            modifier = Modifier.fillMaxSize(),
            iterations = LottieConstants.IterateForever,
        )

    }
}

package com.awan.feature.splash.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.splash.api.SplashRoute
import com.awan.feature.splash.impl.ui.SplashDestination
import com.awan.feature.splash.impl.ui.SplashScreen
import com.awan.feature.splash.impl.ui.SplashViewModel
import kotlinx.coroutines.delay

fun EntryProviderScope<Route>.splashEntry(
    onNavigateToNext: (destination: SplashDestination) -> Unit,
) {
    entry<SplashRoute> {
        SplashRouteScreen(onNext = onNavigateToNext)
    }
}

@Composable
fun SplashRouteScreen(
    onNext: (destination: SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        if (destination != SplashDestination.Loading) {
            delay(SPLASH_HOLD_MILLIS)
            onNext(destination)
        }
    }

    SplashScreen()
}

/** Long enough for the mascot to land and the wordmark to settle before the screen is replaced. */
private const val SPLASH_HOLD_MILLIS = 2000L

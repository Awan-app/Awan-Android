package com.awan.feature.splash.impl.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.splash.api.SplashRoute
import com.awan.feature.splash.impl.ui.SplashDestination
import com.awan.feature.splash.impl.ui.SplashViewModel

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
            kotlinx.coroutines.delay(2000)
            onNext(destination)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SplashRoute Screen")
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

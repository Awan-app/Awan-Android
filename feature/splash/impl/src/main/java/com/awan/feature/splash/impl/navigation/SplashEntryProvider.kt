package com.awan.feature.splash.impl.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.splash.api.SplashRoute
import androidx.compose.runtime.LaunchedEffect

fun EntryProviderScope<Route>.splashEntry(
    onNavigateToNext: () -> Unit
) {
    entry<SplashRoute> {
        SplashRouteScreen(onNext = onNavigateToNext)
    }
} 

@Composable
fun SplashRouteScreen(onNext: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onNext()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SplashRoute Screen")
            Spacer(modifier = Modifier.height(16.dp))
            // Loading indicator could go here
        }
    }
}

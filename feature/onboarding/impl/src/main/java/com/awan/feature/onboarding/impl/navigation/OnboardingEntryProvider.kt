package com.awan.feature.onboarding.impl.navigation

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
import com.awan.feature.onboarding.api.OnboardingRoute

fun EntryProviderScope<Route>.onboardingEntry(
    onNavigateToNext: () -> Unit
) {
    entry<OnboardingRoute> {
        OnboardingRouteScreen(onNext = onNavigateToNext)
    }
} 

@Composable
fun OnboardingRouteScreen(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("OnboardingRoute Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNext) {
                Text("Skip")
            }
        }
    }
}

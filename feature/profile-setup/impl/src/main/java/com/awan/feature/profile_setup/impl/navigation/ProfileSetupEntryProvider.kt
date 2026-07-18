package com.awan.feature.profile_setup.impl.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile_setup.api.ProfileSetupRoute

fun EntryProviderScope<Route>.profileSetupEntry(onNavigateToHome: () -> Unit) {
    entry<ProfileSetupRoute> {
        ProfileSetupRouteScreen(onNavigateToHome)
    }
}

@Composable
fun ProfileSetupRouteScreen(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Profile Setup Route Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNext) {
                Text("Finish")
            }
        }
    }
}

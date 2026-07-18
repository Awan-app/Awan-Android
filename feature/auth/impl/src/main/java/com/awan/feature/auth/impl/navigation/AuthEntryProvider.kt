package com.awan.feature.auth.impl.navigation

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
import com.awan.feature.auth.api.LoginRoute
import com.awan.feature.auth.api.OtpRoute

fun EntryProviderScope<Route>.authEntry(
    onNavigateToOtp: () -> Unit,
    onNavigateToNext: () -> Unit
) {
    entry<LoginRoute> {
        LoginRouteScreen(onNext = onNavigateToOtp)
    }
    entry<OtpRoute> {
        OtpRouteScreen(onNext = onNavigateToNext)
    }
}

@Composable
fun LoginRouteScreen(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LoginRoute Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNext) {
                Text("Login")
            }
        }
    }
}

@Composable
fun OtpRouteScreen(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("OtpRoute Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNext) {
                Text("OTP Verify")
            }
        }
    }
}

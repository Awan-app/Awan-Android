package com.awan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ComposeFoundationFlags
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.chat.api.ChatRoute
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.splash.api.SplashRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Deliberately off. foundation 1.11.4's inherited-style cache (StyleOuterNode.ancestorNodes)
        // is appended to on every resolve and never cleared, so text that moves or is reused merges
        // in styles from nodes that are no longer its ancestors. AwanText passes a concrete
        // TextStyle to BasicText instead, so nothing here needs the inherited path.
        ComposeFoundationFlags.isInheritedTextStyleEnabled = false
        enableEdgeToEdge()
        setContent {
            val appState = rememberAwanAppState(
                startKey = SplashRoute,
                topLevelKeys = listOf(
                    HomeRoute,
                    CalendarRoute,
                    ChatRoute,
                    GoalsRoute,
                    ProfileRoute
                )
            )

            AwanTheme {
                AwanApp(appState = appState)
            }
        }
    }
}

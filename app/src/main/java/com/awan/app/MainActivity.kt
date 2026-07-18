package com.awan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.awan.app.ui.theme.AwanTheme
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

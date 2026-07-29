package com.awan.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.awan.app.MainActivityUiState.*
import com.awan.app.core.designsystem.AwanTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.chat.api.ChatRoute
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.splash.api.SplashRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var uiState: MainActivityUiState by mutableStateOf(Loading)

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    uiState = state
                    if (state is Success) {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(state.language)
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
                }
            }
        }

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

            val useDarkTheme = when (val state = uiState) {
                Loading -> isSystemInDarkTheme()
                is Success -> state.useDarkTheme
            }

            AwanTheme(
                dark = useDarkTheme,
                light = !useDarkTheme
            ) {
                AwanApp(appState = appState)
            }
        }
    }
}

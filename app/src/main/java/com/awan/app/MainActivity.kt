package com.awan.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import java.util.Locale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.awan.app.MainActivityUiState.*
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.LocalRewardAnchors
import com.awan.app.core.designsystem.RewardAnchors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.chat.api.ChatRoute
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.marketplace.api.MarketplaceRoute
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.splash.api.SplashRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission response handled by system */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        var uiState: MainActivityUiState by mutableStateOf(Loading)
        var isOnline by mutableStateOf(true)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isOnline.collectLatest { online ->
                        isOnline = online
                        if (online) {
                            com.awan.app.core.data.sync.SyncWorker.schedulePeriodicSync(this@MainActivity)
                            com.awan.app.core.data.sync.SyncWorker.enqueueImmediateSync(this@MainActivity)
                        }
                    }
                }
                launch {
                    viewModel.uiState.collectLatest { state ->
                        uiState = state
                        if (state is Success) {
                            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(state.language)
                            AppCompatDelegate.setApplicationLocales(appLocale)
                        }
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
            val currentLanguage = when (val state = uiState) {
                Loading -> ""
                is Success -> state.language
            }

            val locale = remember(currentLanguage) {
                if (currentLanguage.isNotBlank()) Locale.forLanguageTag(currentLanguage) else Locale.getDefault()
            }
            val configuration = LocalConfiguration.current
            val updatedConfiguration = remember(locale, configuration) {
                Configuration(configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val layoutDirection = remember(locale) {
                if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
                    LayoutDirection.Rtl
                } else {
                    LayoutDirection.Ltr
                }
            }

            // App-scoped so a reward earned on one screen can still fly to a badge on another.
            val rewardAnchors = remember { RewardAnchors() }

            CompositionLocalProvider(
                LocalConfiguration provides updatedConfiguration,
                LocalLayoutDirection provides layoutDirection,
                LocalRewardAnchors provides rewardAnchors,
            ) {
                val appState = rememberAwanAppState(
                    startKey = SplashRoute,
                    topLevelKeys = listOf(
                        HomeRoute(),
                        GoalsRoute,
                        MarketplaceRoute,
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
                    AwanApp(
                        appState = appState,
                        isOnline = isOnline,
                        sessionExpiredEvents = viewModel.sessionExpired,
                        rewardEvents = viewModel.rewardEvents,
                    )
                }
            }
        }
    }
}


package com.awan.app

import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.awan.app.MainActivityUiState.*
import com.awan.app.core.data.sync.SyncWorker.Companion.enqueueImmediateSync
import com.awan.app.core.data.sync.SyncWorker.Companion.schedulePeriodicSync
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.LocalRewardAnchors
import com.awan.app.core.designsystem.RewardAnchors
import com.awan.app.core.model.DarkThemeConfig
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOnline.collectLatest { online ->
                    if (online) {
                        schedulePeriodicSync(this@MainActivity)
                        enqueueImmediateSync(this@MainActivity)
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
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

            val currentLanguage = when (val state = uiState) {
                Loading -> ""
                is Success -> state.language
            }

            if (uiState is Success) {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags((uiState as Success).language)
                AppCompatDelegate.setApplicationLocales(appLocale)
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

                val darkThemeConfig = when (val state = uiState) {
                    Loading -> DarkThemeConfig.FOLLOW_SYSTEM
                    is Success -> state.darkThemeConfig
                }

                val systemDark = isSystemInDarkTheme()
                val isDark = when (darkThemeConfig) {
                    DarkThemeConfig.FOLLOW_SYSTEM -> systemDark
                    DarkThemeConfig.DARK -> true
                    DarkThemeConfig.LIGHT -> false
                }

                AwanTheme(dark = isDark) {
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


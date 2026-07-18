package com.awan.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.awan.app.features.arena.ArenaScreen
import com.awan.app.features.calender.CalenderScreen
import com.awan.app.features.home.HomeScreen
import com.awan.app.features.settings.SettingsScreen
import com.awan.app.core.designsystem.AwanTheme
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Route.Home::class, Route.Home.serializer())
                    subclass(Route.Arena::class, Route.Arena.serializer())
                    subclass(Route.Calender::class, Route.Calender.serializer())
                    subclass(Route.Settings::class, Route.Settings.serializer())
                }
            }
        },
        Route.Home
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
    ) {
        NavDisplay(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = { key ->
                when (key) {
                    is Route.Home -> {
                        NavEntry(key) {
                            HomeScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                    is Route.Arena -> {
                        NavEntry(key) {
                            ArenaScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                    is Route.Calender -> {
                        NavEntry(key) {
                            CalenderScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                    is Route.Settings -> {
                        NavEntry(key) {
                            SettingsScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                    else -> error("Unknown NavKey: $key")
                }
            }
        )

        AwanBottomBar(
            backStack = backStack,
        )
    }
}

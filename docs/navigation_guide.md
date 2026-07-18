# Navigation Guide (Navigation 3)

This project uses Jetpack Navigation 3. It relies on state-driven navigation rather than a `NavController`.

## Core Concepts

### 1. NavigationState
Holds the UI state for navigation. It keeps track of:
- `startKey`: Initial route (e.g., Splash).
- `topLevelStack`: List of visited bottom-bar tabs (e.g., Home, Chat).
- `subStacks`: Backstack history for each top-level tab.

### 2. Navigator
The action engine. It mutates `NavigationState`.
- `navigate(Route)`: Pushes a new route to the current stack.
- `goBack()`: Pops the last route.
- `replaceAll(Route)`: Clears everything and sets a new root (used for Auth -> Home).

### 3. entryProvider Scope
A DSL from Navigation 3. It maps `Route` objects (keys) to `@Composable` screens.
Each feature module provides an extension function (e.g., `splashEntry`) that registers its screens into this provider.

## The Setup Flow
1. `AwanAppState` creates `NavigationState` and passes it to `AwanApp`.
2. `AwanApp` creates `Navigator`.
3. `AwanApp` sets up `entryProvider` and registers all feature entries.
4. `NavDisplay` watches `appState.navigationState.currentSubStack`. When it changes, `NavDisplay` looks up the top Route in the `entryProvider` and shows the UI.

---

## How To Add a New Screen (Sub-screen / Not Top-Level)

### Case A: Screen Without Parameters
1. **Create the Route** (in your feature's `api` module):
   ```kotlin
   import com.awan.core.navigation.Route
   import kotlinx.serialization.Serializable

   @Serializable
   object SettingsRoute : Route
   ```

2. **Register the Entry** (in your feature's `impl` module):
   ```kotlin
   import androidx.navigation3.runtime.NavEntryProviderScope
   import androidx.navigation3.runtime.entry

   fun NavEntryProviderScope.settingsEntry(onBack: () -> Unit) {
       entry<SettingsRoute> {
           SettingsScreen(onBack = onBack)
       }
   }
   ```

3. **Wire in `AwanApp.kt`**:
   Add `settingsEntry` inside the `entryProvider` block.
   ```kotlin
   settingsEntry(onBack = { navigator.goBack() })
   ```

4. **Navigate to it**:
   From another screen's callback:
   ```kotlin
   navigator.navigate(SettingsRoute)
   ```

### Case B: Screen With Parameters
1. **Create the Route** (in `api` module):
   Use a `data class` with `@Serializable`.
   ```kotlin
   import com.awan.core.navigation.Route
   import kotlinx.serialization.Serializable

   @Serializable
   data class UserDetailRoute(val userId: String, val age: Int) : Route
   ```

2. **Register the Entry** (in `impl` module):
   The `entry<T>` block gives you the typed route back!
   ```kotlin
   fun NavEntryProviderScope.userDetailEntry(onBack: () -> Unit) {
       entry<UserDetailRoute> { route ->
           // Access arguments safely!
           UserDetailScreen(
               userId = route.userId, 
               age = route.age,
               onBack = onBack
           )
       }
   }
   ```

3. **Wire in `AwanApp.kt`**:
   ```kotlin
   userDetailEntry(onBack = { navigator.goBack() })
   ```

4. **Navigate to it**:
   Pass the data directly:
   ```kotlin
   navigator.navigate(UserDetailRoute(userId = "123", age = 25))
   ```

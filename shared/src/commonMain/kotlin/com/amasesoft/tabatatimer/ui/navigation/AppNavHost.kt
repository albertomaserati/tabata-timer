package com.amasesoft.tabatatimer.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.amasesoft.tabatatimer.data.StorageJson
import com.amasesoft.tabatatimer.di.AppContainer
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.ui.editor.EditorMode
import com.amasesoft.tabatatimer.ui.editor.EditorScreen
import com.amasesoft.tabatatimer.ui.editor.EditorViewModel
import com.amasesoft.tabatatimer.ui.home.HomeScreen
import com.amasesoft.tabatatimer.ui.home.HomeViewModel
import com.amasesoft.tabatatimer.ui.run.RunScreen
import com.amasesoft.tabatatimer.ui.run.RunViewModel
import com.amasesoft.tabatatimer.ui.settings.SettingsScreen
import com.amasesoft.tabatatimer.ui.settings.SettingsViewModel
import kotlinx.serialization.Serializable

// Type-safe routes.

@Serializable
data object HomeRoute

@Serializable
data class EditorRoute(val mode: EditorMode, val workoutId: Long = NO_ID)

/** The configuration travels with the route, so quick (unsaved) workouts need no storage. */
@Serializable
data class RunRoute(val configJson: String, val title: String? = null, val workoutId: Long = NO_ID)

@Serializable
data object SettingsRoute

const val NO_ID = -1L

/**
 * Runs [block] only while this entry is RESUMED. As soon as a navigation starts the entry
 * leaves RESUMED, so double taps (or duplicated events) cannot push or pop twice, which
 * would stack two runs or pop the start destination and leave an empty NavHost.
 */
private inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    soundEnabled: Boolean,
) {
    fun NavBackStackEntry.startRun(config: WorkoutConfig, title: String?, workoutId: Long?) = ifResumed {
        navController.navigate(
            RunRoute(
                configJson = StorageJson.encodeToString(WorkoutConfig.serializer(), config),
                title = title,
                workoutId = workoutId ?: NO_ID,
            ),
        ) { launchSingleTop = true }
    }

    fun NavBackStackEntry.back() = ifResumed { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
    ) {
        composable<HomeRoute> { entry ->
            val viewModel = viewModel { HomeViewModel(container.workoutRepository, container.settingsRepository) }
            HomeScreen(
                viewModel = viewModel,
                onQuickWorkout = { entry.ifResumed { navController.navigate(EditorRoute(EditorMode.Quick)) } },
                onNewWorkout = { entry.ifResumed { navController.navigate(EditorRoute(EditorMode.New)) } },
                onEditWorkout = { id -> entry.ifResumed { navController.navigate(EditorRoute(EditorMode.Edit, id)) } },
                onStartWorkout = { workout -> entry.startRun(workout.config, workout.name, workout.id) },
                onSettings = { entry.ifResumed { navController.navigate(SettingsRoute) } },
            )
        }

        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            val viewModel = viewModel {
                EditorViewModel(
                    mode = route.mode,
                    workoutId = route.workoutId.takeIf { it != NO_ID },
                    workoutRepository = container.workoutRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
            EditorScreen(
                viewModel = viewModel,
                onBack = { entry.back() },
                onStartRun = { config, title, id -> entry.startRun(config, title, id) },
            )
        }

        composable<RunRoute> { entry ->
            val route = entry.toRoute<RunRoute>()
            val viewModel = viewModel {
                RunViewModel(
                    config = StorageJson.decodeFromString(WorkoutConfig.serializer(), route.configJson),
                    title = route.title,
                    workoutId = route.workoutId.takeIf { it != NO_ID },
                    workoutRepository = container.workoutRepository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
            RunScreen(
                viewModel = viewModel,
                soundEnabled = soundEnabled,
                onClose = { entry.back() },
            )
        }

        composable<SettingsRoute> { entry ->
            val viewModel = viewModel { SettingsViewModel(container.settingsRepository) }
            SettingsScreen(viewModel = viewModel, onBack = { entry.back() })
        }
    }
}

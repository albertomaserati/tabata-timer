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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.toRoute
import com.amasesoft.tabatatimer.data.StorageJson
import com.amasesoft.tabatatimer.di.AppContainer
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.ui.editor.EditorMode
import com.amasesoft.tabatatimer.ui.editor.EditorScreen
import com.amasesoft.tabatatimer.ui.editor.EditorViewModel
import com.amasesoft.tabatatimer.ui.home.HomeScreen
import com.amasesoft.tabatatimer.ui.home.HomeViewModel
import com.amasesoft.tabatatimer.ui.routines.ExerciseEditorScreen
import com.amasesoft.tabatatimer.ui.routines.ExerciseEditorViewModel
import com.amasesoft.tabatatimer.ui.routines.ManageRoutinesScreen
import com.amasesoft.tabatatimer.ui.routines.ManageRoutinesViewModel
import com.amasesoft.tabatatimer.ui.routines.RoutinesScreen
import com.amasesoft.tabatatimer.ui.routines.RoutinesViewModel
import com.amasesoft.tabatatimer.ui.run.RunScreen
import com.amasesoft.tabatatimer.ui.run.RunViewModel
import com.amasesoft.tabatatimer.ui.settings.SettingsScreen
import com.amasesoft.tabatatimer.ui.settings.SettingsViewModel
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable

// Type-safe routes.

@Serializable
data object HomeRoute

@Serializable
data class EditorRoute(val mode: EditorMode, val workoutId: Long = NO_ID)

/** Must be passed to `composable<EditorRoute>`: see [enumNavType]. */
internal val EditorRouteTypeMap = mapOf(typeOf<EditorMode>() to enumNavType<EditorMode>())

/** The configuration travels with the route, so quick (unsaved) workouts need no storage. */
@Serializable
data class RunRoute(val configJson: String, val title: String? = null, val workoutId: Long = NO_ID)

@Serializable
data object SettingsRoute

/** "Routines" tab (gym routines), the second top-level destination. */
@Serializable
data object RoutinesRoute

@Serializable
data object ManageRoutinesRoute

@Serializable
data class ExerciseEditorRoute(val routineId: Long, val exerciseId: Long = NO_ID)

const val NO_ID = -1L

/**
 * Runs [block] only while this entry is RESUMED. As soon as a navigation starts the entry
 * leaves RESUMED, so double taps (or duplicated events) cannot push or pop twice, which
 * would stack two runs or pop the start destination and leave an empty NavHost.
 */
private inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

/** Destinations that show the bottom bar. */
private fun NavBackStackEntry.isTopLevel(): Boolean =
    destination.hasRoute<HomeRoute>() || destination.hasRoute<RoutinesRoute>()

@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    soundEnabled: Boolean,
    tenSecondWarning: Boolean,
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

    /**
     * Standard bottom-navigation switch: the Timer tab (start destination) stays at the bottom of
     * the stack, so system back from Routines goes to Timer and then exits; each tab keeps its
     * state (ViewModel, scroll) across switches.
     */
    fun NavBackStackEntry.switchTab(tab: MainTab) = ifResumed {
        val route: Any = when (tab) {
            MainTab.Timer -> HomeRoute
            MainTab.Routines -> RoutinesRoute
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // Between tabs a plain crossfade; into/out of detail screens the usual slide.
        enterTransition = {
            if (initialState.isTopLevel() && targetState.isTopLevel()) fadeIn() else slideInHorizontally { it / 4 } + fadeIn()
        },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = {
            if (initialState.isTopLevel() && targetState.isTopLevel()) fadeOut() else slideOutHorizontally { it / 4 } + fadeOut()
        },
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
                bottomBar = { MainTabsBar(selected = MainTab.Timer, onSelect = { entry.switchTab(it) }) },
            )
        }

        composable<RoutinesRoute> { entry ->
            val viewModel = viewModel { RoutinesViewModel(container.gymRepository) }
            RoutinesScreen(
                viewModel = viewModel,
                onAddExercise = { routineId ->
                    entry.ifResumed { navController.navigate(ExerciseEditorRoute(routineId)) }
                },
                onEditExercise = { routineId, exerciseId ->
                    entry.ifResumed { navController.navigate(ExerciseEditorRoute(routineId, exerciseId)) }
                },
                onManage = { entry.ifResumed { navController.navigate(ManageRoutinesRoute) { launchSingleTop = true } } },
                onSettings = { entry.ifResumed { navController.navigate(SettingsRoute) } },
                bottomBar = { MainTabsBar(selected = MainTab.Routines, onSelect = { entry.switchTab(it) }) },
            )
        }

        composable<ManageRoutinesRoute> { entry ->
            val viewModel = viewModel { ManageRoutinesViewModel(container.gymRepository) }
            ManageRoutinesScreen(viewModel = viewModel, onBack = { entry.back() })
        }

        composable<ExerciseEditorRoute> { entry ->
            val route = entry.toRoute<ExerciseEditorRoute>()
            val viewModel = viewModel {
                ExerciseEditorViewModel(
                    routineId = route.routineId,
                    exerciseId = route.exerciseId.takeIf { it != NO_ID },
                    repository = container.gymRepository,
                )
            }
            ExerciseEditorScreen(viewModel = viewModel, onBack = { entry.back() })
        }

        composable<EditorRoute>(typeMap = EditorRouteTypeMap) { entry ->
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
                tenSecondWarning = tenSecondWarning,
                onClose = { entry.back() },
            )
        }

        composable<SettingsRoute> { entry ->
            val viewModel = viewModel { SettingsViewModel(container.settingsRepository) }
            SettingsScreen(viewModel = viewModel, appVersion = container.appVersion, onBack = { entry.back() })
        }
    }
}

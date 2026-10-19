package com.amasesoft.tabatatimer.di

import com.amasesoft.tabatatimer.data.AppSettings
import com.amasesoft.tabatatimer.data.DataStoreSettingsRepository
import com.amasesoft.tabatatimer.data.DataStoreWorkoutRepository
import com.amasesoft.tabatatimer.data.SettingsRepository
import com.amasesoft.tabatatimer.data.WorkoutRepository
import com.amasesoft.tabatatimer.data.WorkoutStore
import com.amasesoft.tabatatimer.data.createJsonDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Manual dependency injection: the app graph is tiny, so a single container is enough.
 *
 * Must be a process-wide singleton (see `appContainer(context)` on Android and
 * `MainViewController` on iOS) because each DataStore file allows one active instance.
 *
 * @param storageDirectory absolute path of a private, writable directory.
 */
class AppContainer(storageDirectory: String) {

    val workoutRepository: WorkoutRepository = DataStoreWorkoutRepository(
        createJsonDataStore(storageDirectory, "workouts.json", WorkoutStore.serializer(), WorkoutStore()),
    )

    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(
        createJsonDataStore(storageDirectory, "settings.json", AppSettings.serializer(), AppSettings()),
    )

    /** Application-lifetime scope (the container lives as long as the process). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Settings shared by the whole UI. Being hot and process-scoped, after the first load it
     * already holds a value when the Activity is re-created (no empty frame on rotation).
     * `null` only until the very first read completes.
     */
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings.stateIn(appScope, SharingStarted.Eagerly, null)
}

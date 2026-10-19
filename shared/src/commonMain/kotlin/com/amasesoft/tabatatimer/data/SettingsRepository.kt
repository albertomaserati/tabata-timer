package com.amasesoft.tabatatimer.data

import androidx.datastore.core.DataStore
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.domain.WorkoutSort
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** BCP-47 [tag] used for the in-app locale override; `null` means "follow the system". */
@Serializable
enum class AppLanguage(val tag: String?) { System(null), Italian("it"), English("en") }

@Serializable
enum class ThemeMode { System, Light, Dark }

@Serializable
data class AppSettings(
    val language: AppLanguage = AppLanguage.System,
    val theme: ThemeMode = ThemeMode.System,
    val workoutSort: WorkoutSort = WorkoutSort.Custom,
    val soundEnabled: Boolean = true,
    /** Last configuration used for a quick (unsaved) workout, restored next time. */
    val lastQuickConfig: WorkoutConfig = WorkoutConfig.Default,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

class DataStoreSettingsRepository(private val dataStore: DataStore<AppSettings>) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.orDefaultOnIoError { AppSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.updateData(transform)
    }
}

package com.amasesoft.tabatatimer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.amasesoft.tabatatimer.data.AppLanguage
import com.amasesoft.tabatatimer.data.AppSettings
import com.amasesoft.tabatatimer.data.SettingsRepository
import com.amasesoft.tabatatimer.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okio.IOException
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.getString
import tabatatimer.shared.generated.resources.storage_error
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.action_back
import tabatatimer.shared.generated.resources.ic_arrow_back
import tabatatimer.shared.generated.resources.language_english
import tabatatimer.shared.generated.resources.language_italian
import tabatatimer.shared.generated.resources.language_system
import tabatatimer.shared.generated.resources.settings_language
import tabatatimer.shared.generated.resources.settings_sound
import tabatatimer.shared.generated.resources.settings_sound_hint
import tabatatimer.shared.generated.resources.settings_theme
import tabatatimer.shared.generated.resources.settings_title
import tabatatimer.shared.generated.resources.settings_workout
import tabatatimer.shared.generated.resources.theme_dark
import tabatatimer.shared.generated.resources.theme_light
import tabatatimer.shared.generated.resources.theme_system

/** MVVM: a handful of independent preferences, written straight to the settings DataStore. */
class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }
    fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }
    fun setSoundEnabled(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }

    private val _errors = Channel<Unit>(Channel.CONFLATED)

    /** Emits when a setting could not be written. */
    val errors: Flow<Unit> = _errors.receiveAsFlow()

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.update(transform)
            } catch (_: IOException) {
                _errors.send(Unit)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.errors.collect { snackbarHostState.showSnackbar(getString(Res.string.storage_error)) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val current = settings ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChoiceSection(
                title = stringResource(Res.string.settings_language),
                options = AppLanguage.entries,
                selected = current.language,
                label = { it.label },
                onSelect = viewModel::setLanguage,
            )
            ChoiceSection(
                title = stringResource(Res.string.settings_theme),
                options = ThemeMode.entries,
                selected = current.theme,
                label = { it.label },
                onSelect = viewModel::setTheme,
            )
            SettingsCard(title = stringResource(Res.string.settings_workout)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(
                            value = current.soundEnabled,
                            role = Role.Switch,
                            onValueChange = viewModel::setSoundEnabled,
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.settings_sound), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(Res.string.settings_sound_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = current.soundEnabled, onCheckedChange = null)
                }
            }
        }
    }
}

private val AppLanguage.label: StringResource
    get() = when (this) {
        AppLanguage.System -> Res.string.language_system
        AppLanguage.Italian -> Res.string.language_italian
        AppLanguage.English -> Res.string.language_english
    }

private val ThemeMode.label: StringResource
    get() = when (this) {
        ThemeMode.System -> Res.string.theme_system
        ThemeMode.Light -> Res.string.theme_light
        ThemeMode.Dark -> Res.string.theme_dark
    }

@Composable
private fun <T> ChoiceSection(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> StringResource,
    onSelect: (T) -> Unit,
) {
    SettingsCard(title = title) {
        Column(Modifier.selectableGroup()) {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = option == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(option) },
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == selected, onClick = null)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(label(option)), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() },
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

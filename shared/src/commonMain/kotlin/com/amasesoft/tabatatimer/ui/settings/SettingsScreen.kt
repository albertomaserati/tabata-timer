package com.amasesoft.tabatatimer.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
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
import com.amasesoft.tabatatimer.di.AppVersion
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.IOException
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.about_developed_by
import tabatatimer.shared.generated.resources.about_open_website
import tabatatimer.shared.generated.resources.about_version
import tabatatimer.shared.generated.resources.action_back
import tabatatimer.shared.generated.resources.app_name
import tabatatimer.shared.generated.resources.ic_arrow_back
import tabatatimer.shared.generated.resources.ic_logo_am
import tabatatimer.shared.generated.resources.ic_logo_am_dark
import tabatatimer.shared.generated.resources.ic_open_in_new
import tabatatimer.shared.generated.resources.language_english
import tabatatimer.shared.generated.resources.language_italian
import tabatatimer.shared.generated.resources.language_system
import tabatatimer.shared.generated.resources.settings_about
import tabatatimer.shared.generated.resources.settings_language
import tabatatimer.shared.generated.resources.settings_sound
import tabatatimer.shared.generated.resources.settings_sound_hint
import tabatatimer.shared.generated.resources.settings_ten_seconds
import tabatatimer.shared.generated.resources.settings_ten_seconds_hint
import tabatatimer.shared.generated.resources.settings_theme
import tabatatimer.shared.generated.resources.settings_title
import tabatatimer.shared.generated.resources.settings_workout
import tabatatimer.shared.generated.resources.storage_error
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
    fun setTenSecondWarning(enabled: Boolean) = update { it.copy(tenSecondWarning = enabled) }

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
fun SettingsScreen(viewModel: SettingsViewModel, appVersion: AppVersion, onBack: () -> Unit) {
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
                SwitchRow(
                    title = stringResource(Res.string.settings_sound),
                    hint = stringResource(Res.string.settings_sound_hint),
                    checked = current.soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchRow(
                    title = stringResource(Res.string.settings_ten_seconds),
                    hint = stringResource(Res.string.settings_ten_seconds_hint),
                    checked = current.tenSecondWarning,
                    onCheckedChange = viewModel::setTenSecondWarning,
                )
            }
            AboutSection(appVersion)
        }
    }
}

@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Developer website opened from Settings > About. */
private const val DEVELOPER_WEBSITE = "https://www.albertomaserati.com"

@Composable
private fun AboutSection(appVersion: AppVersion) {
    val uriHandler = LocalUriHandler.current
    SettingsCard(title = stringResource(Res.string.settings_about)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(Res.string.about_version, appVersion.name, appVersion.build),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(
                    onClickLabel = stringResource(Res.string.about_open_website),
                    role = Role.Button,
                    onClick = { runCatching { uriHandler.openUri(DEVELOPER_WEBSITE) } },
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dark variant: the navy and teal shapes are lightened to stay visible on dark surfaces.
            val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            Image(
                painterResource(if (darkSurface) Res.drawable.ic_logo_am_dark else Res.drawable.ic_logo_am),
                contentDescription = null,
                modifier = Modifier.size(width = 32.dp, height = 28.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.about_developed_by), style = MaterialTheme.typography.bodyLarge)
                Text(
                    DEVELOPER_WEBSITE.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(16.dp))
            Icon(
                painterResource(Res.drawable.ic_open_in_new),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
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

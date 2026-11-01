package com.amasesoft.tabatatimer.ui.routines

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.amasesoft.tabatatimer.components.CountSelector
import com.amasesoft.tabatatimer.components.SectionCard
import com.amasesoft.tabatatimer.components.ValueEntryDialog
import com.amasesoft.tabatatimer.components.WeightEntry
import com.amasesoft.tabatatimer.components.WeightFormat
import com.amasesoft.tabatatimer.components.WeightSelector
import com.amasesoft.tabatatimer.components.rememberWeightFormat
import com.amasesoft.tabatatimer.domain.ExerciseLimits
import com.amasesoft.tabatatimer.domain.MAX_EXERCISES_PER_ROUTINE
import com.amasesoft.tabatatimer.domain.Progression
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.a11y_delete
import tabatatimer.shared.generated.resources.action_back
import tabatatimer.shared.generated.resources.action_cancel
import tabatatimer.shared.generated.resources.action_delete
import tabatatimer.shared.generated.resources.action_save
import tabatatimer.shared.generated.resources.char_counter
import tabatatimer.shared.generated.resources.editor_description
import tabatatimer.shared.generated.resources.editor_discard_confirm
import tabatatimer.shared.generated.resources.editor_discard_message
import tabatatimer.shared.generated.resources.editor_discard_title
import tabatatimer.shared.generated.resources.exercise_delete_message
import tabatatimer.shared.generated.resources.exercise_delete_title
import tabatatimer.shared.generated.resources.exercise_increment
import tabatatimer.shared.generated.resources.exercise_increment_other
import tabatatimer.shared.generated.resources.exercise_limit_reached
import tabatatimer.shared.generated.resources.exercise_name
import tabatatimer.shared.generated.resources.exercise_progression_increase_hint
import tabatatimer.shared.generated.resources.exercise_progression_no_weight_hint
import tabatatimer.shared.generated.resources.exercise_progression_repeat_hint
import tabatatimer.shared.generated.resources.exercise_progression_section
import tabatatimer.shared.generated.resources.exercise_reps
import tabatatimer.shared.generated.resources.exercise_sets
import tabatatimer.shared.generated.resources.exercise_title_edit
import tabatatimer.shared.generated.resources.exercise_title_new
import tabatatimer.shared.generated.resources.exercise_use_weight
import tabatatimer.shared.generated.resources.exercise_use_weight_hint
import tabatatimer.shared.generated.resources.exercise_volume_section
import tabatatimer.shared.generated.resources.exercise_weight
import tabatatimer.shared.generated.resources.exercise_weight_hint
import tabatatimer.shared.generated.resources.exercise_weight_section
import tabatatimer.shared.generated.resources.ic_arrow_back
import tabatatimer.shared.generated.resources.ic_delete
import tabatatimer.shared.generated.resources.progression_increase
import tabatatimer.shared.generated.resources.progression_repeat
import tabatatimer.shared.generated.resources.storage_error

/** Increment chips: 1, 2, 2.5, 5, 10 kg (half kilograms). */
private val IncrementPresets = listOf(2, 4, 5, 10, 20)
private val SetPresets = listOf(2, 3, 4, 5)
private val RepPresets = listOf(5, 6, 8, 10, 12, 15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorScreen(viewModel: ExerciseEditorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ExerciseEditorEvent.Close -> currentOnBack()
                ExerciseEditorEvent.StorageError -> snackbarHostState.showSnackbar(getString(Res.string.storage_error))
                ExerciseEditorEvent.LimitReached ->
                    snackbarHostState.showSnackbar(getString(Res.string.exercise_limit_reached, MAX_EXERCISES_PER_ROUTINE))
            }
        }
    }

    // System back/gesture goes through the same unsaved-changes check as the top-bar arrow.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.hasUnsavedChanges,
        onBackCompleted = viewModel::requestBack,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) Res.string.exercise_title_new else Res.string.exercise_title_edit))
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::requestBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = viewModel::requestDelete, enabled = !state.isLoading) {
                            Icon(
                                painterResource(Res.drawable.ic_delete),
                                stringResource(Res.string.a11y_delete, state.savedDraft?.name.orEmpty()),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            SaveBar(enabled = !state.isLoading && !state.isSaving, onSave = viewModel::save)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExerciseForm(state = state, viewModel = viewModel)
            }
        }
    }

    if (state.showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDiscard,
            title = { Text(stringResource(Res.string.editor_discard_title)) },
            text = { Text(stringResource(Res.string.editor_discard_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDiscard) { Text(stringResource(Res.string.editor_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDiscard) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(Res.string.exercise_delete_title)) },
            text = { Text(stringResource(Res.string.exercise_delete_message, state.savedDraft?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ExerciseForm(state: ExerciseEditorUiState, viewModel: ExerciseEditorViewModel) {
    val draft = state.draft
    val weightFormat = rememberWeightFormat()

    SectionCard {
        GymNameField(
            value = draft.name,
            onValueChange = viewModel::setName,
            label = stringResource(Res.string.exercise_name),
            error = state.nameError,
            maxLength = ExerciseLimits.NAME_MAX_LENGTH,
        )
        OutlinedTextField(
            value = draft.description,
            onValueChange = viewModel::setDescription,
            label = { Text(stringResource(Res.string.editor_description)) },
            supportingText = {
                Text(
                    stringResource(Res.string.char_counter, draft.description.length, ExerciseLimits.DESCRIPTION_MAX_LENGTH),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    SectionCard(title = stringResource(Res.string.exercise_volume_section)) {
        CountSelector(
            title = stringResource(Res.string.exercise_sets),
            value = draft.sets,
            onValueChange = viewModel::setSets,
            range = ExerciseLimits.sets,
            presets = SetPresets,
        )
        CountSelector(
            title = stringResource(Res.string.exercise_reps),
            value = draft.reps,
            onValueChange = viewModel::setReps,
            range = ExerciseLimits.reps,
            presets = RepPresets,
        )
    }

    SectionCard(title = stringResource(Res.string.exercise_weight_section)) {
        SwitchRow(
            title = stringResource(Res.string.exercise_use_weight),
            hint = stringResource(Res.string.exercise_use_weight_hint),
            checked = draft.useWeight,
            onCheckedChange = viewModel::setUseWeight,
        )
        AnimatedVisibility(visible = draft.useWeight) {
            WeightSelector(
                title = stringResource(Res.string.exercise_weight),
                valueHalfKg = draft.weightHalfKg,
                onValueChange = viewModel::setWeight,
                supportingText = stringResource(Res.string.exercise_weight_hint),
            )
        }
    }

    SectionCard(title = stringResource(Res.string.exercise_progression_section)) {
        ProgressionSelector(progression = draft.progression, onChange = viewModel::setProgression)
        val increase = draft.progression == Progression.Increase
        Text(
            text = stringResource(
                when {
                    !increase -> Res.string.exercise_progression_repeat_hint
                    draft.useWeight -> Res.string.exercise_progression_increase_hint
                    else -> Res.string.exercise_progression_no_weight_hint
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        AnimatedVisibility(visible = increase) {
            IncrementChips(
                valueHalfKg = draft.incrementHalfKg,
                onValueChange = viewModel::setIncrement,
                weightFormat = weightFormat,
            )
        }
    }
}

@Composable
private fun ProgressionSelector(progression: Progression, onChange: (Progression) -> Unit) {
    val options = listOf(Progression.Repeat to Res.string.progression_repeat, Progression.Increase to Res.string.progression_increase)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = progression == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(stringResource(label), maxLines = 1) }
        }
    }
}

/** Preset increments plus "Other…", which opens the half-kilo keypad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncrementChips(valueHalfKg: Int, onValueChange: (Int) -> Unit, weightFormat: WeightFormat) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(Res.string.exercise_increment)
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            IncrementPresets.forEach { preset ->
                FilterChip(
                    selected = valueHalfKg == preset,
                    onClick = { onValueChange(preset) },
                    label = { Text(weightFormat.withUnit(preset)) },
                )
            }
            val custom = valueHalfKg !in IncrementPresets
            FilterChip(
                selected = custom,
                onClick = { showDialog = true },
                label = {
                    Text(if (custom) weightFormat.withUnit(valueHalfKg) else stringResource(Res.string.exercise_increment_other))
                },
            )
        }
    }
    if (showDialog) {
        ValueEntryDialog(
            title = title,
            initial = WeightEntry.of(valueHalfKg, weightFormat.decimalSeparator),
            range = ExerciseLimits.incrementHalfKg,
            presets = IncrementPresets,
            format = weightFormat::withUnit,
            entryFor = { WeightEntry.of(it, weightFormat.decimalSeparator) },
            extraKey = weightFormat.halfKey,
            onConfirm = {
                showDialog = false
                onValueChange(it)
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Button(
                onClick = onSave,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.action_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

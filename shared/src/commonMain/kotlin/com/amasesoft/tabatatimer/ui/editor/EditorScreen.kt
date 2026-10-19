package com.amasesoft.tabatatimer.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.amasesoft.tabatatimer.components.CountSelector
import com.amasesoft.tabatatimer.components.TimeSelector
import com.amasesoft.tabatatimer.domain.MAX_SAVED_WORKOUTS
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.domain.WorkoutConfig.Limits
import com.amasesoft.tabatatimer.domain.formatSeconds
import com.amasesoft.tabatatimer.theme.AppThemeExtras
import com.amasesoft.tabatatimer.theme.TabularNumbers
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.action_back
import tabatatimer.shared.generated.resources.action_cancel
import tabatatimer.shared.generated.resources.action_save
import tabatatimer.shared.generated.resources.action_start
import tabatatimer.shared.generated.resources.editor_add_cycle_hint
import tabatatimer.shared.generated.resources.editor_add_cycle_n
import tabatatimer.shared.generated.resources.editor_cycle_count
import tabatatimer.shared.generated.resources.editor_max_cycles
import tabatatimer.shared.generated.resources.ic_add
import tabatatimer.shared.generated.resources.editor_advanced
import tabatatimer.shared.generated.resources.editor_advanced_hint
import tabatatimer.shared.generated.resources.editor_cycle_default_name
import tabatatimer.shared.generated.resources.editor_cycle_name
import tabatatimer.shared.generated.resources.editor_cycles
import tabatatimer.shared.generated.resources.editor_cycles_section
import tabatatimer.shared.generated.resources.editor_description
import tabatatimer.shared.generated.resources.editor_general_section
import tabatatimer.shared.generated.resources.editor_last_cycle_hint
import tabatatimer.shared.generated.resources.editor_discard_confirm
import tabatatimer.shared.generated.resources.editor_discard_message
import tabatatimer.shared.generated.resources.editor_discard_title
import tabatatimer.shared.generated.resources.a11y_delete
import tabatatimer.shared.generated.resources.a11y_duplicate
import tabatatimer.shared.generated.resources.a11y_move_down
import tabatatimer.shared.generated.resources.a11y_move_up
import tabatatimer.shared.generated.resources.storage_error
import tabatatimer.shared.generated.resources.editor_name
import tabatatimer.shared.generated.resources.editor_name_required
import tabatatimer.shared.generated.resources.editor_name_taken
import tabatatimer.shared.generated.resources.editor_prepare
import tabatatimer.shared.generated.resources.editor_rest
import tabatatimer.shared.generated.resources.editor_rest_between_sets
import tabatatimer.shared.generated.resources.editor_rest_between_sets_hint
import tabatatimer.shared.generated.resources.editor_rest_between_sets_single
import tabatatimer.shared.generated.resources.editor_rest_hint
import tabatatimer.shared.generated.resources.editor_save_dialog_title
import tabatatimer.shared.generated.resources.editor_saved
import tabatatimer.shared.generated.resources.editor_sets
import tabatatimer.shared.generated.resources.editor_simple
import tabatatimer.shared.generated.resources.editor_simple_hint
import tabatatimer.shared.generated.resources.editor_simplify_confirm
import tabatatimer.shared.generated.resources.editor_simplify_message
import tabatatimer.shared.generated.resources.editor_simplify_title
import tabatatimer.shared.generated.resources.editor_title_edit
import tabatatimer.shared.generated.resources.editor_title_new
import tabatatimer.shared.generated.resources.editor_title_quick
import tabatatimer.shared.generated.resources.editor_total_duration
import tabatatimer.shared.generated.resources.editor_work
import tabatatimer.shared.generated.resources.ic_arrow_back
import tabatatimer.shared.generated.resources.ic_arrow_down
import tabatatimer.shared.generated.resources.ic_arrow_up
import tabatatimer.shared.generated.resources.ic_copy
import tabatatimer.shared.generated.resources.ic_delete
import tabatatimer.shared.generated.resources.ic_play
import tabatatimer.shared.generated.resources.ic_save
import tabatatimer.shared.generated.resources.limit_reached

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onStartRun: (config: WorkoutConfig, title: String?, workoutId: Long?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnStartRun by rememberUpdatedState(onStartRun)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Close -> currentOnBack()
                EditorEvent.Saved -> snackbarHostState.showSnackbar(getString(Res.string.editor_saved))
                EditorEvent.StorageError -> snackbarHostState.showSnackbar(getString(Res.string.storage_error))
                EditorEvent.LimitReached ->
                    snackbarHostState.showSnackbar(getString(Res.string.limit_reached, MAX_SAVED_WORKOUTS))

                is EditorEvent.StartRun -> currentOnStartRun(event.config, event.title, event.workoutId)
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
                    Text(
                        stringResource(
                            when (state.mode) {
                                EditorMode.Quick -> Res.string.editor_title_quick
                                EditorMode.New -> Res.string.editor_title_new
                                EditorMode.Edit -> Res.string.editor_title_edit
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::requestBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    if (state.mode == EditorMode.Quick) {
                        IconButton(onClick = viewModel::openSaveDialog, enabled = !state.isLoading) {
                            Icon(painterResource(Res.drawable.ic_save), stringResource(Res.string.action_save))
                        }
                    }
                },
            )
        },
        bottomBar = {
            EditorBottomBar(
                totalSeconds = state.totalSeconds,
                mode = state.mode,
                enabled = !state.isLoading && !state.isSaving,
                onPrimary = if (state.mode == EditorMode.Quick) viewModel::start else viewModel::save,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EditorContent(state = state, viewModel = viewModel, contentPadding = padding)
        }
    }

    if (state.showSimplifyConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSimplify,
            title = { Text(stringResource(Res.string.editor_simplify_title)) },
            text = { Text(stringResource(Res.string.editor_simplify_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSimplify) { Text(stringResource(Res.string.editor_simplify_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSimplify) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
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

    if (state.showSaveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveDialog,
            title = { Text(stringResource(Res.string.editor_save_dialog_title)) },
            text = {
                NameDescriptionFields(
                    name = state.name,
                    description = state.description,
                    nameError = state.nameError,
                    onNameChange = viewModel::setName,
                    onDescriptionChange = viewModel::setDescription,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::save, enabled = !state.isSaving) { Text(stringResource(Res.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSaveDialog) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun EditorContent(
    state: EditorUiState,
    viewModel: EditorViewModel,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    val latestState by rememberUpdatedState(state)
    // Key of a cycle just added. The click updates the ViewModel synchronously, but the
    // collected `state` arrives one frame later: wait until it contains the new cycle, then
    // derive its index from the same key list that builds the LazyColumn.
    var scrollToCycle by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(scrollToCycle) {
        val key = scrollToCycle ?: return@LaunchedEffect
        val position = snapshotFlow { latestState.cycles.indexOfFirst { it.key == key } }.first { it >= 0 }
        listState.animateScrollToItem(leadingItemKeys(latestState).size + position)
        scrollToCycle = null
    }
    val leadingKeys = leadingItemKeys(state)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Items before the cycle list: declared from leadingItemKeys() only.
        leadingKeys.forEach { itemKey ->
            item(key = itemKey) {
                when (itemKey) {
                    KEY_INFO -> SectionCard {
                        NameDescriptionFields(
                            name = state.name,
                            description = state.description,
                            nameError = state.nameError,
                            onNameChange = viewModel::setName,
                            onDescriptionChange = viewModel::setDescription,
                        )
                    }

                    KEY_MODE -> ModeSelector(isAdvanced = state.isAdvanced, onAdvancedChange = viewModel::setAdvanced)
                    KEY_GENERAL -> GeneralSection(state, viewModel)
                    KEY_SIMPLE -> SimpleCyclesSection(state, viewModel)
                    KEY_CYCLES_HEADER -> CyclesHeader(count = state.cycles.size, modifier = Modifier.animateItem())
                }
            }
        }

        if (state.isAdvanced) {
            itemsIndexed(state.cycles, key = { _, cycle -> cycle.key }) { index, cycle ->
                CycleCard(
                    index = index,
                    cycle = cycle,
                    isLast = index == state.cycles.lastIndex,
                    canRemove = state.cycles.size > Limits.cycles.first,
                    canAdd = state.cycles.size < Limits.cycles.last,
                    onChange = { transform -> viewModel.updateCycle(cycle.key, transform) },
                    onDuplicate = { viewModel.duplicateCycle(cycle.key) },
                    onRemove = { viewModel.removeCycle(cycle.key) },
                    onMove = { offset -> viewModel.moveCycle(cycle.key, offset) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "add") {
                AddCycleCard(
                    nextNumber = state.cycles.size + 1,
                    enabled = state.cycles.size < Limits.cycles.last,
                    onClick = {
                        viewModel.addCycle()
                        scrollToCycle = viewModel.state.value.cycles.lastOrNull()?.key
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private const val KEY_INFO = "info"
private const val KEY_MODE = "mode"
private const val KEY_GENERAL = "general"
private const val KEY_SIMPLE = "simple"
private const val KEY_CYCLES_HEADER = "cyclesHeader"

/** Keys of the LazyColumn items that precede the advanced cycle list, in order. */
private fun leadingItemKeys(state: EditorUiState): List<String> = buildList {
    if (state.mode != EditorMode.Quick) add(KEY_INFO)
    add(KEY_MODE)
    add(KEY_GENERAL)
    add(if (state.isAdvanced) KEY_CYCLES_HEADER else KEY_SIMPLE)
}

@Composable
private fun GeneralSection(state: EditorUiState, viewModel: EditorViewModel) {
    SectionCard(title = stringResource(Res.string.editor_general_section)) {
        TimeSelector(
            title = stringResource(Res.string.editor_prepare),
            value = state.prepareSeconds,
            onValueChange = viewModel::setPrepare,
            range = Limits.prepareSeconds,
            presets = PreparePresets,
        )
        CountSelector(
            title = stringResource(Res.string.editor_sets),
            value = state.sets,
            onValueChange = viewModel::setSets,
            range = Limits.sets,
            presets = SetPresets,
        )
        TimeSelector(
            title = stringResource(Res.string.editor_rest_between_sets),
            value = state.restBetweenSetsSeconds,
            onValueChange = viewModel::setRestBetweenSets,
            range = Limits.restSeconds,
            presets = SetRestPresets,
            enabled = state.sets > 1,
            supportingText = stringResource(
                if (state.sets > 1) Res.string.editor_rest_between_sets_hint else Res.string.editor_rest_between_sets_single,
            ),
        )
    }
}

@Composable
private fun SimpleCyclesSection(state: EditorUiState, viewModel: EditorViewModel) {
    SectionCard(title = stringResource(Res.string.editor_cycles_section)) {
        TimeSelector(
            title = stringResource(Res.string.editor_work),
            value = state.uniform.workSeconds,
            onValueChange = viewModel::setUniformWork,
            range = Limits.workSeconds,
            presets = WorkPresets,
        )
        TimeSelector(
            title = stringResource(Res.string.editor_rest),
            value = state.uniform.restSeconds,
            onValueChange = viewModel::setUniformRest,
            range = Limits.restSeconds,
            presets = RestPresets,
            supportingText = stringResource(Res.string.editor_rest_hint),
        )
        CountSelector(
            title = stringResource(Res.string.editor_cycles),
            value = state.uniform.count,
            onValueChange = viewModel::setUniformCount,
            range = Limits.cycles,
            presets = CyclePresets,
        )
    }
}

@Composable
private fun NameDescriptionFields(
    name: String,
    description: String,
    nameError: NameError?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.editor_name)) },
            isError = nameError != null,
            supportingText = when (nameError) {
                NameError.Empty -> { { Text(stringResource(Res.string.editor_name_required)) } }
                NameError.Duplicate -> { { Text(stringResource(Res.string.editor_name_taken)) } }
                null -> null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(Res.string.editor_description)) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ModeSelector(isAdvanced: Boolean, onAdvancedChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isAdvanced,
                onClick = { onAdvancedChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(Res.string.editor_simple)) }
            SegmentedButton(
                selected = isAdvanced,
                onClick = { onAdvancedChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(Res.string.editor_advanced)) }
        }
        Text(
            text = stringResource(if (isAdvanced) Res.string.editor_advanced_hint else Res.string.editor_simple_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
    }
}

@Composable
private fun CycleCard(
    index: Int,
    cycle: EditableCycle,
    isLast: Boolean,
    canRemove: Boolean,
    canAdd: Boolean,
    onChange: ((EditableCycle) -> EditableCycle) -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val phaseColors = AppThemeExtras.phaseColors
    val defaultName = stringResource(Res.string.editor_cycle_default_name, index + 1)
    val label = cycle.name.ifBlank { defaultName }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = phaseColors.work.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onMove(-1) }, enabled = index > 0) {
                    Icon(painterResource(Res.drawable.ic_arrow_up), stringResource(Res.string.a11y_move_up, label))
                }
                IconButton(onClick = { onMove(1) }, enabled = !isLast) {
                    Icon(painterResource(Res.drawable.ic_arrow_down), stringResource(Res.string.a11y_move_down, label))
                }
                IconButton(onClick = onDuplicate, enabled = canAdd) {
                    Icon(painterResource(Res.drawable.ic_copy), stringResource(Res.string.a11y_duplicate, label))
                }
                IconButton(onClick = onRemove, enabled = canRemove) {
                    Icon(painterResource(Res.drawable.ic_delete), stringResource(Res.string.a11y_delete, label))
                }
            }
            Column(Modifier.padding(end = 8.dp)) {
                OutlinedTextField(
                    value = cycle.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = { Text(stringResource(Res.string.editor_cycle_name)) },
                    placeholder = { Text(defaultName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                TimeSelector(
                    title = stringResource(Res.string.editor_work),
                    value = cycle.workSeconds,
                    onValueChange = { value -> onChange { it.copy(workSeconds = value) } },
                    range = Limits.workSeconds,
                    presets = WorkPresets,
                    accessibilityContext = label,
                )
                // The last cycle of a set is work only: its rest is shown but disabled.
                TimeSelector(
                    title = stringResource(Res.string.editor_rest),
                    value = cycle.restSeconds,
                    onValueChange = { value -> onChange { it.copy(restSeconds = value) } },
                    range = Limits.restSeconds,
                    presets = RestPresets,
                    accessibilityContext = label,
                    enabled = !isLast,
                    supportingText = if (isLast) stringResource(Res.string.editor_last_cycle_hint) else null,
                )
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    totalSeconds: Int,
    mode: EditorMode,
    enabled: Boolean,
    onPrimary: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.editor_total_duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatSeconds(totalSeconds),
                        style = MaterialTheme.typography.headlineSmall.merge(TabularNumbers),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Button(
                    onClick = onPrimary,
                    enabled = enabled,
                    modifier = Modifier.height(56.dp),
                ) {
                    AnimatedVisibility(visible = mode == EditorMode.Quick) {
                        Row {
                            Icon(painterResource(Res.drawable.ic_play), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    Text(
                        stringResource(if (mode == EditorMode.Quick) Res.string.action_start else Res.string.action_save),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

// Presets offered in the value-entry dialogs, per field.
private val PreparePresets = listOf(5, 10, 15, 20, 30, 60)
private val WorkPresets = listOf(10, 20, 30, 45, 60, 90)
private val RestPresets = listOf(5, 10, 15, 20, 30, 60)
private val SetRestPresets = listOf(30, 60, 90, 120, 180, 300)
private val CyclePresets = listOf(4, 6, 8, 10, 12, 16)
private val SetPresets = listOf(1, 2, 3, 4, 5, 6)

@Composable
private fun CyclesHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.editor_cycles_section),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            text = pluralStringResource(Res.plurals.editor_cycle_count, count, count),
            style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * "Empty next cycle" placeholder: same shape and width as [CycleCard], dashed primary border,
 * light primaryContainer fill and a filled "+" so it reads as an action at a glance.
 */
@Composable
private fun AddCycleCard(
    nextNumber: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CardDefaults.shape
    val primary = MaterialTheme.colorScheme.primary
    val dashColor = if (enabled) primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            // Drawn before the clip so the whole stroke stays visible.
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = dashColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(dashColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (enabled) {
                    stringResource(Res.string.editor_add_cycle_n, nextNumber)
                } else {
                    stringResource(Res.string.editor_max_cycles, Limits.cycles.last)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (enabled) {
                Text(
                    text = stringResource(Res.string.editor_add_cycle_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

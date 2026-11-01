package com.amasesoft.tabatatimer.ui.routines

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amasesoft.tabatatimer.components.LoadErrorState
import com.amasesoft.tabatatimer.components.WeightFormat
import com.amasesoft.tabatatimer.components.WholeWordsText
import com.amasesoft.tabatatimer.components.rememberWeightFormat
import com.amasesoft.tabatatimer.domain.MAX_EXERCISES_PER_ROUTINE
import com.amasesoft.tabatatimer.domain.MAX_ROUTINES
import com.amasesoft.tabatatimer.domain.Progression
import com.amasesoft.tabatatimer.theme.AppTheme
import com.amasesoft.tabatatimer.theme.TabularNumbers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.a11y_apply
import tabatatimer.shared.generated.resources.a11y_more
import tabatatimer.shared.generated.resources.a11y_progression
import tabatatimer.shared.generated.resources.a11y_progression_increase_state
import tabatatimer.shared.generated.resources.a11y_progression_repeat_state
import tabatatimer.shared.generated.resources.action_apply
import tabatatimer.shared.generated.resources.action_cancel
import tabatatimer.shared.generated.resources.action_delete
import tabatatimer.shared.generated.resources.action_move_down
import tabatatimer.shared.generated.resources.action_move_up
import tabatatimer.shared.generated.resources.action_undo
import tabatatimer.shared.generated.resources.exercise_add
import tabatatimer.shared.generated.resources.exercise_delete_message
import tabatatimer.shared.generated.resources.exercise_delete_title
import tabatatimer.shared.generated.resources.exercise_limit_reached
import tabatatimer.shared.generated.resources.exercise_next_time
import tabatatimer.shared.generated.resources.exercise_sets_reps
import tabatatimer.shared.generated.resources.exercise_weight_updated
import tabatatimer.shared.generated.resources.ic_add
import tabatatimer.shared.generated.resources.ic_arrow_down
import tabatatimer.shared.generated.resources.ic_arrow_up
import tabatatimer.shared.generated.resources.ic_check
import tabatatimer.shared.generated.resources.ic_delete
import tabatatimer.shared.generated.resources.ic_equal
import tabatatimer.shared.generated.resources.ic_fitness
import tabatatimer.shared.generated.resources.ic_list
import tabatatimer.shared.generated.resources.ic_more_vert
import tabatatimer.shared.generated.resources.ic_settings
import tabatatimer.shared.generated.resources.ic_trending_up
import tabatatimer.shared.generated.resources.progression_increase_badge
import tabatatimer.shared.generated.resources.progression_repeat
import tabatatimer.shared.generated.resources.routine_empty_message
import tabatatimer.shared.generated.resources.routine_empty_title
import tabatatimer.shared.generated.resources.routine_limit_reached
import tabatatimer.shared.generated.resources.routines_empty_message
import tabatatimer.shared.generated.resources.routines_empty_title
import tabatatimer.shared.generated.resources.routines_load_error
import tabatatimer.shared.generated.resources.routines_manage
import tabatatimer.shared.generated.resources.routines_new
import tabatatimer.shared.generated.resources.routines_selector_label
import tabatatimer.shared.generated.resources.routines_title
import tabatatimer.shared.generated.resources.settings_title
import tabatatimer.shared.generated.resources.storage_error

/** Up to this many routines the selector is a segmented row, then a dropdown. */
private const val SEGMENTED_MAX = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    viewModel: RoutinesViewModel,
    onAddExercise: (routineId: Long) -> Unit,
    onEditExercise: (routineId: Long, exerciseId: Long) -> Unit,
    onManage: () -> Unit,
    onSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val weightFormat = rememberWeightFormat()
    val currentWeightFormat by rememberUpdatedState(weightFormat)
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RoutinesEvent.StorageError -> snackbarHostState.showSnackbar(getString(Res.string.storage_error))
                RoutinesEvent.RoutineLimitReached ->
                    snackbarHostState.showSnackbar(getString(Res.string.routine_limit_reached, MAX_ROUTINES))

                RoutinesEvent.ExerciseLimitReached ->
                    snackbarHostState.showSnackbar(getString(Res.string.exercise_limit_reached, MAX_EXERCISES_PER_ROUTINE))

                is RoutinesEvent.IncreaseApplied -> {
                    // Launched so that further events are not queued behind this snackbar;
                    // a newer "Apply" replaces the previous message.
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val message = getString(
                        Res.string.exercise_weight_updated,
                        currentWeightFormat.withUnit(event.applied.newWeightHalfKg),
                    )
                    val undo = getString(Res.string.action_undo)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoIncrease(event.applied)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.routines_title)) },
                actions = {
                    if (state.isListAvailable && state.routines.isNotEmpty()) {
                        IconButton(onClick = onManage) {
                            Icon(painterResource(Res.drawable.ic_list), stringResource(Res.string.routines_manage))
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(painterResource(Res.drawable.ic_settings), stringResource(Res.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            val selected = state.selected
            if (state.isListAvailable && selected != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (state.canAddExercise) {
                            onAddExercise(selected.id)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.exercise_limit_reached, MAX_EXERCISES_PER_ROUTINE),
                                )
                            }
                        }
                    },
                    icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(Res.string.exercise_add)) },
                )
            }
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.loadError -> item(key = "error") {
                    LoadErrorState(
                        message = stringResource(Res.string.routines_load_error),
                        onRetry = viewModel::reload,
                        modifier = Modifier.animateItem(),
                    )
                }

                state.routines.isEmpty() -> item(key = "noRoutines") {
                    NoRoutinesState(onCreate = viewModel::openNewRoutine, modifier = Modifier.animateItem())
                }

                else -> {
                    item(key = "selector") {
                        RoutineSelector(
                            routines = state.routines,
                            selectedId = state.selectedId,
                            onSelect = viewModel::select,
                            onManage = onManage,
                            modifier = Modifier.animateItem(),
                        )
                    }
                    val selected = state.selected
                    if (selected != null && state.exercises.isEmpty()) {
                        item(key = "noExercises") {
                            NoExercisesState(routineName = selected.name, modifier = Modifier.animateItem())
                        }
                    }
                    itemsIndexed(state.exercises, key = { _, item -> item.id }) { index, item ->
                        ExerciseCard(
                            item = item,
                            weightFormat = weightFormat,
                            isFirst = index == 0,
                            isLast = index == state.exercises.lastIndex,
                            onClick = { selected?.let { onEditExercise(it.id, item.id) } },
                            onToggleProgression = { viewModel.toggleProgression(item.id) },
                            onApply = { viewModel.applyIncrease(item.id) },
                            onMove = { offset -> viewModel.moveExercise(item.id, offset) },
                            onDelete = { pendingDelete = item.id },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    state.nameDialog?.let { dialog ->
        RoutineNameDialogView(
            dialog = dialog,
            onNameChange = viewModel::setDialogName,
            onConfirm = viewModel::submitDialog,
            onDismiss = viewModel::dismissDialog,
        )
    }

    val deleting = pendingDelete?.let { id -> state.exercises.firstOrNull { it.id == id } }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.exercise_delete_title)) },
            text = { Text(stringResource(Res.string.exercise_delete_message, deleting.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExercise(deleting.id)
                    pendingDelete = null
                }) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

/**
 * Active routine: segmented buttons up to [SEGMENTED_MAX] routines (one tap to switch), a
 * dropdown beyond, which also leads to "Manage routines".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineSelector(
    routines: List<RoutineSummary>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (routines.size <= SEGMENTED_MAX) {
        SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
            routines.forEachIndexed { index, routine ->
                SegmentedButton(
                    selected = routine.id == selectedId,
                    onClick = { onSelect(routine.id) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = routines.size),
                    // No check mark: the space goes to the name, the selection is shown by the color.
                    icon = {},
                    modifier = Modifier.weight(1f),
                ) {
                    Text(routine.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    } else {
        var expanded by remember { mutableStateOf(false) }
        val selected = routines.firstOrNull { it.id == selectedId }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selected?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(Res.string.routines_selector_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                routines.forEach { routine ->
                    DropdownMenuItem(
                        text = { Text(routine.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            expanded = false
                            onSelect(routine.id)
                        },
                        trailingIcon = if (routine.id == selectedId) {
                            { Icon(painterResource(Res.drawable.ic_check), contentDescription = null) }
                        } else {
                            null
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.routines_manage)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_list), contentDescription = null) },
                    onClick = {
                        expanded = false
                        onManage()
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    item: ExerciseItem,
    weightFormat: WeightFormat,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onToggleProgression: () -> Unit,
    onApply: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).padding(vertical = 4.dp)) {
                    WholeWordsText(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ProgressionBadge(
                    exerciseName = item.name,
                    progression = item.progression,
                    incrementHalfKg = item.incrementHalfKg,
                    weightFormat = weightFormat,
                    onToggle = onToggleProgression,
                )
                ExerciseOverflowMenu(
                    name = item.name,
                    canMoveUp = !isFirst,
                    canMoveDown = !isLast,
                    onMove = onMove,
                    onDelete = onDelete,
                )
            }
            Column(Modifier.padding(end = 12.dp)) {
                val volume = stringResource(Res.string.exercise_sets_reps, item.sets, item.reps)
                Text(
                    text = item.weightHalfKg?.let { "$volume · ${weightFormat.withUnit(it)}" } ?: volume,
                    style = MaterialTheme.typography.titleLarge.merge(TabularNumbers),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Only with something to apply: Increase *and* a weight. The last value is kept
                // while the row animates out (after Apply the exercise is back to Repeat).
                // Plain (non-observable) holder: written during composition without triggering
                // another recomposition.
                val lastNext = remember { IntArray(1) { item.nextWeightHalfKg ?: 0 } }
                item.nextWeightHalfKg?.let { lastNext[0] = it }
                AnimatedVisibility(visible = item.nextWeightHalfKg != null) {
                    lastNext[0].takeIf { it > 0 }?.let { next ->
                        NextTimeRow(
                            exerciseName = item.name,
                            nextWeight = weightFormat.withUnit(next),
                            onApply = onApply,
                        )
                    }
                }
            }
        }
    }
}

/**
 * "↑ +2 kg" (Increase, primary) or "= Repeat": a single toggle, announced with its state.
 */
@Composable
private fun ProgressionBadge(
    exerciseName: String,
    progression: Progression,
    incrementHalfKg: Int,
    weightFormat: WeightFormat,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val increase = progression == Progression.Increase
    val container by animateColorAsState(
        if (increase) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "badgeContainer",
    )
    val content by animateColorAsState(
        if (increase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "badgeContent",
    )
    val increment = weightFormat.withUnit(incrementHalfKg)
    val label = stringResource(Res.string.a11y_progression, exerciseName)
    val stateText = if (increase) {
        stringResource(Res.string.a11y_progression_increase_state, increment)
    } else {
        stringResource(Res.string.a11y_progression_repeat_state)
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .toggleable(value = increase, role = Role.Switch, onValueChange = { onToggle() })
            // On the toggle node itself: label + custom state ("Increase by 2 kg"), read with the
            // switch role. Compose exposes the description as a non-focusable, non-clickable
            // child of the toggle in the accessibility tree: TalkBack has a single focus stop.
            .semantics {
                contentDescription = label
                stateDescription = stateText
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = increase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            // The visual "+2 kg" / "Repeat" is replaced by the label and state above.
            modifier = Modifier.clearAndSetSemantics { },
            label = "badge",
        ) { isIncrease ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (isIncrease) Res.drawable.ic_trending_up else Res.drawable.ic_equal),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isIncrease) {
                        stringResource(Res.string.progression_increase_badge, increment)
                    } else {
                        stringResource(Res.string.progression_repeat)
                    },
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NextTimeRow(exerciseName: String, nextWeight: String, onApply: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.exercise_next_time, nextWeight),
                style = MaterialTheme.typography.titleSmall.merge(TabularNumbers),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            Spacer(Modifier.width(8.dp))
            val applyLabel = stringResource(Res.string.a11y_apply, nextWeight, exerciseName)
            Button(
                onClick = onApply,
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = applyLabel },
            ) {
                Text(stringResource(Res.string.action_apply), maxLines = 1)
            }
        }
    }
}

@Composable
private fun ExerciseOverflowMenu(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(Res.drawable.ic_more_vert), stringResource(Res.string.a11y_more, name))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_move_up)) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_arrow_up), contentDescription = null) },
                enabled = canMoveUp,
                onClick = {
                    expanded = false
                    onMove(-1)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_move_down)) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_arrow_down), contentDescription = null) },
                enabled = canMoveDown,
                onClick = {
                    expanded = false
                    onMove(1)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_delete)) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_delete), contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun NoRoutinesState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(Res.drawable.ic_fitness),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.routines_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.routines_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onCreate, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.routines_new))
        }
    }
}

@Composable
private fun NoExercisesState(routineName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.routine_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.routine_empty_message, routineName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun ExerciseCardPreview() {
    AppTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val format = rememberWeightFormat()
            ExerciseCard(
                item = ExerciseItem(1, "Panca manubri", "Schiena appoggiata", 4, 8, 28, Progression.Increase, 4, 32),
                weightFormat = format,
                isFirst = true,
                isLast = false,
                onClick = {}, onToggleProgression = {}, onApply = {}, onMove = {}, onDelete = {},
            )
            ExerciseCard(
                item = ExerciseItem(2, "Trazioni", "", 3, 10, null, Progression.Repeat, 4, null),
                weightFormat = format,
                isFirst = false,
                isLast = true,
                onClick = {}, onToggleProgression = {}, onApply = {}, onMove = {}, onDelete = {},
            )
        }
    }
}

package com.amasesoft.tabatatimer.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import tabatatimer.shared.generated.resources.action_retry
import tabatatimer.shared.generated.resources.home_load_error
import tabatatimer.shared.generated.resources.ic_error
import tabatatimer.shared.generated.resources.ic_replay
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amasesoft.tabatatimer.domain.MAX_SAVED_WORKOUTS
import com.amasesoft.tabatatimer.domain.SavedWorkout
import com.amasesoft.tabatatimer.domain.WorkoutSort
import com.amasesoft.tabatatimer.domain.formatSeconds
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.action_cancel
import tabatatimer.shared.generated.resources.action_delete
import tabatatimer.shared.generated.resources.action_done
import tabatatimer.shared.generated.resources.action_edit
import tabatatimer.shared.generated.resources.a11y_more
import tabatatimer.shared.generated.resources.a11y_move_down
import tabatatimer.shared.generated.resources.a11y_move_up
import tabatatimer.shared.generated.resources.a11y_start
import tabatatimer.shared.generated.resources.action_move_bottom
import tabatatimer.shared.generated.resources.action_move_top
import tabatatimer.shared.generated.resources.home_cycles
import tabatatimer.shared.generated.resources.home_sets
import tabatatimer.shared.generated.resources.storage_error
import tabatatimer.shared.generated.resources.action_reorder
import tabatatimer.shared.generated.resources.action_sort
import tabatatimer.shared.generated.resources.app_name
import tabatatimer.shared.generated.resources.home_advanced_badge
import tabatatimer.shared.generated.resources.home_delete_message
import tabatatimer.shared.generated.resources.home_delete_title
import tabatatimer.shared.generated.resources.home_empty_message
import tabatatimer.shared.generated.resources.home_empty_title
import tabatatimer.shared.generated.resources.home_new_workout
import tabatatimer.shared.generated.resources.home_quick_subtitle
import tabatatimer.shared.generated.resources.home_saved_section
import tabatatimer.shared.generated.resources.home_summary
import tabatatimer.shared.generated.resources.home_uses
import tabatatimer.shared.generated.resources.ic_add
import tabatatimer.shared.generated.resources.ic_arrow_down
import tabatatimer.shared.generated.resources.ic_arrow_up
import tabatatimer.shared.generated.resources.ic_bolt
import tabatatimer.shared.generated.resources.ic_check
import tabatatimer.shared.generated.resources.ic_delete
import tabatatimer.shared.generated.resources.ic_edit
import tabatatimer.shared.generated.resources.ic_more_vert
import tabatatimer.shared.generated.resources.ic_play
import tabatatimer.shared.generated.resources.ic_reorder
import tabatatimer.shared.generated.resources.ic_settings
import tabatatimer.shared.generated.resources.ic_sort
import tabatatimer.shared.generated.resources.limit_reached
import tabatatimer.shared.generated.resources.quick_workout
import tabatatimer.shared.generated.resources.settings_title
import tabatatimer.shared.generated.resources.sort_custom
import tabatatimer.shared.generated.resources.sort_longest
import tabatatimer.shared.generated.resources.sort_most_used
import tabatatimer.shared.generated.resources.sort_name
import tabatatimer.shared.generated.resources.sort_newest
import tabatatimer.shared.generated.resources.sort_oldest
import tabatatimer.shared.generated.resources.sort_shortest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuickWorkout: () -> Unit,
    onNewWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (SavedWorkout) -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentOnStart by rememberUpdatedState(onStartWorkout)
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.StartRun -> currentOnStart(event.workout)
                HomeEvent.StorageError -> snackbarHostState.showSnackbar(getString(Res.string.storage_error))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    if (state.isReordering) {
                        IconButton(onClick = { viewModel.setReordering(false) }) {
                            Icon(painterResource(Res.drawable.ic_check), stringResource(Res.string.action_done))
                        }
                    } else {
                        // Sorting and reordering only make sense on a list we could read.
                        if (state.isListAvailable) {
                            SortMenu(selected = state.sort, onSelect = viewModel::setSort)
                        }
                        if (state.isListAvailable && state.workouts.size > 1) {
                            IconButton(onClick = { viewModel.setReordering(true) }) {
                                Icon(painterResource(Res.drawable.ic_reorder), stringResource(Res.string.action_reorder))
                            }
                        }
                        IconButton(onClick = onSettings) {
                            Icon(painterResource(Res.drawable.ic_settings), stringResource(Res.string.settings_title))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            // Hidden while loading or after a read error: with an unknown file content we could
            // exceed the limit or overwrite data. The quick workout stays available.
            if (!state.isReordering && state.isListAvailable) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!state.isAtLimit) {
                            onNewWorkout()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(getString(Res.string.limit_reached, MAX_SAVED_WORKOUTS))
                            }
                        }
                    },
                    icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(Res.string.home_new_workout)) },
                )
            }
        },
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
            if (!state.isReordering) {
                item(key = "quick") { QuickWorkoutCard(onClick = onQuickWorkout, modifier = Modifier.animateItem()) }
            }
            when {
                state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp).animateItem(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.loadError -> item(key = "error") {
                    LoadErrorState(onRetry = viewModel::reload, modifier = Modifier.animateItem())
                }

                else -> item(key = "header") {
                    Text(
                        text = stringResource(Res.string.home_saved_section, state.workouts.size, MAX_SAVED_WORKOUTS),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp).semantics { heading() }.animateItem(),
                    )
                }
            }
            if (state.isListAvailable && state.workouts.isEmpty()) {
                item(key = "empty") { EmptyState(Modifier.animateItem()) }
            }
            itemsIndexed(state.workouts, key = { _, item -> item.id }) { index, item ->
                WorkoutCard(
                    item = item,
                    isReordering = state.isReordering,
                    isFirst = index == 0,
                    isCustomOrder = state.sort == WorkoutSort.Custom,
                    isLast = index == state.workouts.lastIndex,
                    onStart = { viewModel.start(item.id) },
                    onEdit = { onEditWorkout(item.id) },
                    onDelete = { pendingDelete = item.id },
                    onMove = { offset -> viewModel.move(item.id, offset) },
                    onMoveToEdge = { toTop -> viewModel.moveToEdge(item.id, toTop) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    val deleting = pendingDelete?.let { id -> state.workouts.firstOrNull { it.id == id } }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.home_delete_title)) },
            text = { Text(stringResource(Res.string.home_delete_message, deleting.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(deleting.id)
                    pendingDelete = null
                }) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SortMenu(selected: WorkoutSort, onSelect: (WorkoutSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(Res.drawable.ic_sort), stringResource(Res.string.action_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WorkoutSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.label)) },
                    onClick = {
                        onSelect(sort)
                        expanded = false
                    },
                    trailingIcon = if (sort == selected) {
                        { Icon(painterResource(Res.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private val WorkoutSort.label: StringResource
    get() = when (this) {
        WorkoutSort.Custom -> Res.string.sort_custom
        WorkoutSort.Name -> Res.string.sort_name
        WorkoutSort.MostUsed -> Res.string.sort_most_used
        WorkoutSort.Newest -> Res.string.sort_newest
        WorkoutSort.Oldest -> Res.string.sort_oldest
        WorkoutSort.Shortest -> Res.string.sort_shortest
        WorkoutSort.Longest -> Res.string.sort_longest
    }

@Composable
private fun QuickWorkoutCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(Res.drawable.ic_bolt), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.quick_workout), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(Res.string.home_quick_subtitle), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Read error: the list is unknown, so it is not shown as empty. */
@Composable
private fun LoadErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(Res.drawable.ic_error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.home_load_error),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            // Announced when it appears.
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(painterResource(Res.drawable.ic_replay), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.action_retry))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.home_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WorkoutCard(
    item: WorkoutItem,
    isReordering: Boolean,
    isFirst: Boolean,
    isCustomOrder: Boolean,
    isLast: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    onMoveToEdge: (toTop: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = !isReordering, onClick = onEdit)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                Spacer(Modifier.height(4.dp))
                val summary = stringResource(
                    Res.string.home_summary,
                    formatSeconds(item.totalSeconds),
                    pluralStringResource(Res.plurals.home_sets, item.sets, item.sets),
                    pluralStringResource(Res.plurals.home_cycles, item.cycles, item.cycles),
                )
                val uses = pluralStringResource(Res.plurals.home_uses, item.usageCount, item.usageCount)
                val advanced = if (item.isAdvanced) " · " + stringResource(Res.string.home_advanced_badge) else ""
                Text(
                    text = "$summary · $uses$advanced",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AnimatedContent(targetState = isReordering, label = "cardActions") { reordering ->
                if (reordering) {
                    Row {
                        IconButton(onClick = { onMove(-1) }, enabled = !isFirst) {
                            Icon(painterResource(Res.drawable.ic_arrow_up), stringResource(Res.string.a11y_move_up, item.name))
                        }
                        IconButton(onClick = { onMove(1) }, enabled = !isLast) {
                            Icon(painterResource(Res.drawable.ic_arrow_down), stringResource(Res.string.a11y_move_down, item.name))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(onClick = onStart, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(Res.drawable.ic_play), stringResource(Res.string.a11y_start, item.name))
                        }
                        WorkoutOverflowMenu(
                            name = item.name,
                            canMoveUp = !isFirst || !isCustomOrder,
                            canMoveDown = !isLast || !isCustomOrder,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onMoveToEdge = onMoveToEdge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutOverflowMenu(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveToEdge: (toTop: Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(Res.drawable.ic_more_vert), stringResource(Res.string.a11y_more, name))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Sets the custom order (and switches to it) without entering reorder mode.
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_move_top)) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_arrow_up), contentDescription = null) },
                enabled = canMoveUp,
                onClick = {
                    expanded = false
                    onMoveToEdge(true)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_move_bottom)) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_arrow_down), contentDescription = null) },
                enabled = canMoveDown,
                onClick = {
                    expanded = false
                    onMoveToEdge(false)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_edit)) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_edit), contentDescription = null) },
                onClick = {
                    expanded = false
                    onEdit()
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

package com.amasesoft.tabatatimer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.amasesoft.tabatatimer.theme.TabularNumbers
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.a11y_backspace
import tabatatimer.shared.generated.resources.action_cancel
import tabatatimer.shared.generated.resources.action_ok
import tabatatimer.shared.generated.resources.entry_out_of_range
import tabatatimer.shared.generated.resources.entry_range
import tabatatimer.shared.generated.resources.ic_backspace

/** Saves a [DigitEntry] across configuration changes / process death. */
internal val DigitEntrySaver = Saver<DigitEntry, List<Any>>(
    save = { entry ->
        when (entry) {
            is DurationEntry -> listOf("d", entry.digits, entry.isPristine, 0)
            is CountEntry -> listOf("c", entry.digits, entry.isPristine, entry.maxDigits)
        }
    },
    restore = { saved ->
        val digits = saved[1] as String
        val pristine = saved[2] as Boolean
        if (saved[0] == "d") DurationEntry(digits, pristine) else CountEntry(digits, saved[3] as Int, pristine)
    },
)

/**
 * Dialog to type a value with an on-screen numeric keypad.
 *
 * A custom keypad (instead of the system IME) is used on purpose: it is identical on Android
 * and iOS, it opens without focus/IME timing issues, and the iOS number pad has no Return key
 * to confirm. A hardware keyboard still works (digits, Backspace, Enter to confirm).
 *
 * Layout: scrollable column; when the dialog is short and wide (landscape, large font scale
 * on tablets) the keypad moves next to the value.
 *
 * @param initial entry prefilled with the current value; the first digit replaces it.
 * @param entryFor builds the entry for a preset value.
 */
@Composable
internal fun ValueEntryDialog(
    title: String,
    initial: DigitEntry,
    range: IntRange,
    presets: List<Int>,
    format: (Int) -> String,
    entryFor: (Int) -> DigitEntry,
    showDoubleZero: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var entry by rememberSaveable(stateSaver = DigitEntrySaver) { mutableStateOf(initial) }
    val isValid = entry.isValid(range)
    val confirm = { if (entry.isValid(range)) onConfirm(entry.value) }

    // Invisible focus target so a hardware keyboard reaches the dialog. Key events bubble
    // (non-preview onKeyEvent) from the focused node: when a button has focus, Enter is
    // consumed by that button and never confirms by mistake.
    val keyTarget = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { keyTarget.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        // Without the platform default width (narrow on Android) the M3 cap of 560dp applies,
        // which leaves room for the side-by-side layout in landscape.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp),
        title = { Text(title) },
        text = {
            BoxWithConstraints(
                modifier = Modifier.onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val char = event.utf16CodePoint.toChar()
                    when {
                        char in '0'..'9' -> { entry = entry.type(char - '0'); true }
                        event.key == Key.Backspace || event.key == Key.Delete -> { entry = entry.backspace(); true }
                        event.key == Key.Enter || event.key == Key.NumPadEnter -> { confirm(); true }
                        else -> false
                    }
                },
            ) {
                // Not a TalkBack stop: semantics cleared, size zero.
                Box(
                    Modifier
                        .size(0.dp)
                        .clearAndSetSemantics { }
                        .focusRequester(keyTarget)
                        .focusable(),
                )
                val sideBySide = maxWidth >= 420.dp && maxHeight < 460.dp
                val valuePart: @Composable () -> Unit = {
                    EntryHeader(
                        entry = entry,
                        isValid = isValid,
                        range = range,
                        presets = presets,
                        format = format,
                        onPreset = { entry = entryFor(it) },
                    )
                }
                val keypad: @Composable () -> Unit = {
                    Keypad(
                        columns = if (sideBySide) 4 else 3,
                        showDoubleZero = showDoubleZero,
                        onDigit = { entry = entry.type(it) },
                        onDoubleZero = { entry = entry.type(0).type(0) },
                        onBackspace = { entry = entry.backspace() },
                    )
                }
                if (sideBySide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) { valuePart() }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { keypad() }
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        valuePart()
                        keypad()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirm() }, enabled = isValid) { Text(stringResource(Res.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryHeader(
    entry: DigitEntry,
    isValid: Boolean,
    range: IntRange,
    presets: List<Int>,
    format: (Int) -> String,
    onPreset: (Int) -> Unit,
) {
    val rangeText = if (isValid) {
        stringResource(Res.string.entry_range, format(range.first), format(range.last))
    } else {
        stringResource(Res.string.entry_out_of_range, format(range.first), format(range.last))
    }
    EntryDisplay(
        entry = entry,
        spokenValue = format(entry.value),
        errorMessage = rangeText.takeUnless { isValid },
    )
    // The text itself changes on error, so the live region announces it.
    Text(
        text = rangeText,
        style = MaterialTheme.typography.bodySmall,
        color = if (isValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    val validPresets = presets.filter { it in range }
    if (validPresets.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            validPresets.forEach { preset ->
                FilterChip(
                    selected = entry.value == preset,
                    onClick = { onPreset(preset) },
                    label = { Text(format(preset), style = MaterialTheme.typography.labelLarge.merge(TabularNumbers)) },
                )
            }
        }
    }
}

@Composable
private fun EntryDisplay(entry: DigitEntry, spokenValue: String, errorMessage: String?) {
    // The prefilled value is highlighted like a selection: typing replaces it.
    val highlight by animateColorAsState(
        if (entry.isPristine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        label = "entryHighlight",
    )
    Box(
        modifier = Modifier
            .background(highlight, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp)
            // Announce the normalized value (0:90 is read as 1:30) and the error, if any.
            .clearAndSetSemantics {
                contentDescription = spokenValue
                liveRegion = LiveRegionMode.Polite
                if (errorMessage != null) error(errorMessage)
            },
    ) {
        Text(
            text = entry.display,
            style = MaterialTheme.typography.displayMedium.merge(TabularNumbers),
            color = if (errorMessage == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
    }
}

/** Narrow keys (4 columns) must not lose their label to the default button padding. */
private val KeyPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)

private sealed interface PadKey {
    data class Digit(val value: Int) : PadKey
    data object DoubleZero : PadKey
    data object Backspace : PadKey
    data object Empty : PadKey
}

/** 3 columns (phone layout) in portrait; 4 columns, i.e. only 3 rows, when space is short. */
private fun padLayout(columns: Int, showDoubleZero: Boolean): List<List<PadKey>> {
    val digits = (1..9).map { PadKey.Digit(it) }
    val extra = if (showDoubleZero) PadKey.DoubleZero else PadKey.Empty
    return if (columns == 4) {
        (digits + PadKey.Digit(0) + extra + PadKey.Backspace).chunked(4)
    } else {
        digits.chunked(3) + listOf(listOf(extra, PadKey.Digit(0), PadKey.Backspace))
    }
}

@Composable
private fun Keypad(
    columns: Int,
    showDoubleZero: Boolean,
    onDigit: (Int) -> Unit,
    onDoubleZero: () -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        padLayout(columns, showDoubleZero).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val keyModifier = Modifier.weight(1f)
                    when (key) {
                        is PadKey.Digit -> KeypadKey(key.value.toString(), { onDigit(key.value) }, keyModifier)
                        PadKey.DoubleZero -> KeypadKey("00", onDoubleZero, keyModifier)
                        PadKey.Empty -> Box(keyModifier)
                        PadKey.Backspace -> FilledTonalButton(
                            onClick = onBackspace,
                            modifier = keyModifier.heightIn(min = 52.dp),
                            contentPadding = KeyPadding,
                        ) {
                            Icon(
                                painterResource(Res.drawable.ic_backspace),
                                contentDescription = stringResource(Res.string.a11y_backspace),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), contentPadding = KeyPadding) {
        Text(text, style = MaterialTheme.typography.titleLarge.merge(TabularNumbers), maxLines = 1, softWrap = false)
    }
}

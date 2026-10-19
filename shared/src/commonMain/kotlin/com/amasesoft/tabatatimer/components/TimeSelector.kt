package com.amasesoft.tabatatimer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amasesoft.tabatatimer.domain.formatSeconds
import com.amasesoft.tabatatimer.theme.AppTheme
import com.amasesoft.tabatatimer.theme.TabularNumbers
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.a11y_decrease
import tabatatimer.shared.generated.resources.a11y_edit_value
import tabatatimer.shared.generated.resources.a11y_increase
import tabatatimer.shared.generated.resources.ic_add
import tabatatimer.shared.generated.resources.ic_remove

/**
 * Stepper for a duration in seconds, shown as `m:ss`.
 *
 * Tap +/- for single steps, press and hold for fast, accelerating changes.
 * Below one minute it steps by 1s, above by 5s so that long rests are quick to set.
 * Tapping the value opens a dialog to type it (timer-style `m:ss` digits) or pick a [presets] chip.
 */
@Composable
fun TimeSelector(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..3600,
    enabled: Boolean = true,
    supportingText: String? = null,
    presets: List<Int> = DefaultDurationPresets,
    /** Extra context for screen readers, e.g. the cycle ("Edit Work, Cycle 3"). */
    accessibilityContext: String? = null,
) {
    ValueStepper(
        title = title,
        value = value,
        onValueChange = onValueChange,
        range = range,
        format = ::formatSeconds,
        entryFor = DurationEntry::of,
        presets = presets,
        showDoubleZero = true,
        stepUp = { v -> if (v >= 60) 5 else 1 },
        stepDown = { v -> if (v > 60) 5 else 1 },
        modifier = modifier,
        enabled = enabled,
        supportingText = supportingText,
        accessibilityContext = accessibilityContext,
    )
}

/** Duration presets offered by default (filtered by range). */
val DefaultDurationPresets = listOf(10, 20, 30, 45, 60, 90)

/** Stepper for plain counters such as cycles or sets; tapping the value opens a numeric dialog. */
@Composable
fun CountSelector(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 1..99,
    enabled: Boolean = true,
    supportingText: String? = null,
    presets: List<Int> = emptyList(),
    /** Extra context for screen readers, e.g. the cycle ("Edit Work, Cycle 3"). */
    accessibilityContext: String? = null,
) {
    ValueStepper(
        title = title,
        value = value,
        onValueChange = onValueChange,
        range = range,
        format = Int::toString,
        entryFor = { CountEntry.of(it, range) },
        presets = presets,
        showDoubleZero = false,
        stepUp = { 1 },
        stepDown = { 1 },
        modifier = modifier,
        enabled = enabled,
        supportingText = supportingText,
        accessibilityContext = accessibilityContext,
    )
}

@Composable
private fun ValueStepper(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    format: (Int) -> String,
    entryFor: (Int) -> DigitEntry,
    presets: List<Int>,
    showDoubleZero: Boolean,
    stepUp: (Int) -> Int,
    stepDown: (Int) -> Int,
    modifier: Modifier,
    enabled: Boolean,
    supportingText: String?,
    accessibilityContext: String?,
) {
    // Label used by every accessibility description of this control.
    val a11yTitle = if (accessibilityContext != null) "$title, $accessibilityContext" else title
    val formatted = format(value)
    // Dialog state is internal: only the confirmed value leaves through onValueChange.
    var showEntryDialog by rememberSaveable { mutableStateOf(false) }
    val currentValue by rememberUpdatedState(value)
    val decrease = { onValueChange((currentValue - stepDown(currentValue)).coerceIn(range)) }
    val increase = { onValueChange((currentValue + stepUp(currentValue)).coerceIn(range)) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            // The row is one adjustable node for screen readers (label + value, swipe up/down
            // to change by one unit); the +/- buttons stay separately focusable. The visible
            // texts are cleared below so the title is not announced twice.
            .semantics(mergeDescendants = true) {
                contentDescription = if (supportingText != null) "$a11yTitle, $supportingText" else a11yTitle
                stateDescription = formatted
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value.toFloat(),
                    range = range.first.toFloat()..range.last.toFloat(),
                    // One step per unit: without steps, an accessibility "increment" moves by a
                    // fixed fraction of the range (minutes at a time for durations).
                    steps = (range.last - range.first - 1).coerceAtLeast(0),
                )
                if (enabled) {
                    setProgress { target ->
                        onValueChange(target.roundToInt().coerceIn(range))
                        true
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f).clearAndSetSemantics { }) {
            WholeWordsText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                maxLines = 2,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        RepeatingIconButton(
            onStep = decrease,
            enabled = enabled && value > range.first,
            icon = { Icon(painterResource(Res.drawable.ic_remove), stringResource(Res.string.a11y_decrease, a11yTitle)) },
        )

        // Tappable value: tonal rounded surface + ripple signal it can be edited.
        val editLabel = stringResource(Res.string.a11y_edit_value, a11yTitle)
        Box(
            modifier = Modifier
                .widthIn(min = 72.dp)
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                )
                .clickable(
                    enabled = enabled,
                    onClickLabel = editLabel,
                    role = Role.Button,
                    onClick = { showEntryDialog = true },
                )
                .semantics { contentDescription = "$a11yTitle, $formatted" }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = formatted,
                transitionSpec = {
                    val up = targetState.length > initialState.length || targetState > initialState
                    val dir = if (up) 1 else -1
                    (slideInVertically(tween(120)) { -it * dir / 2 } + fadeIn(tween(120)))
                        .togetherWith(slideOutVertically(tween(120)) { it * dir / 2 } + fadeOut(tween(120)))
                },
                modifier = Modifier.clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
                label = "stepperValue",
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.merge(TabularNumbers),
                    textAlign = TextAlign.Center,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        }

        RepeatingIconButton(
            onStep = increase,
            enabled = enabled && value < range.last,
            icon = { Icon(painterResource(Res.drawable.ic_add), stringResource(Res.string.a11y_increase, a11yTitle)) },
        )
    }

    if (showEntryDialog) {
        ValueEntryDialog(
            title = title,
            initial = entryFor(value),
            range = range,
            presets = presets,
            format = format,
            entryFor = entryFor,
            showDoubleZero = showDoubleZero,
            onConfirm = { confirmed ->
                showEntryDialog = false
                onValueChange(confirmed)
            },
            onDismiss = { showEntryDialog = false },
        )
    }
}

/**
 * Text that never breaks a word in the middle: if a line ends inside a word (narrow phones,
 * large font scale) the font is shrunk step by step, without drawing, until it no longer does.
 */
@Composable
private fun WholeWordsText(
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    minFontSize: TextUnit = 12.sp,
) {
    BoxWithConstraints {
        var fontSize by remember(text, style, constraints.maxWidth) { mutableStateOf(style.fontSize) }
        var fitted by remember(text, style, constraints.maxWidth) { mutableStateOf(false) }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.drawWithContent { if (fitted) drawContent() },
            onTextLayout = { layout ->
                if (!fitted) {
                    val splitsWord = (0 until layout.lineCount - 1).any { line ->
                        val end = layout.getLineEnd(line)
                        end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()
                    }
                    val smaller = fontSize * 0.92f
                    if (splitsWord && smaller >= minFontSize) fontSize = smaller else fitted = true
                }
            },
        )
    }
}

/**
 * Icon button that fires [onStep] on tap and repeatedly, with acceleration, while held.
 * The trailing click after a long press is swallowed so the value is not changed twice.
 */
@Composable
private fun RepeatingIconButton(
    onStep: () -> Unit,
    enabled: Boolean,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnStep by rememberUpdatedState(onStep)
    val repeatState = remember { BooleanArray(1) } // [0] = a repeat happened in this press

    LaunchedEffect(interactionSource) {
        var job: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    job?.cancel()
                    repeatState[0] = false
                    job = launch {
                        delay(400)
                        var interval = 150L
                        while (isActive) {
                            repeatState[0] = true
                            currentOnStep()
                            delay(interval)
                            interval = (interval * 0.85).toLong().coerceAtLeast(30L)
                        }
                    }
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> job?.cancel()
            }
        }
    }

    FilledTonalIconButton(
        onClick = { if (!repeatState[0]) currentOnStep() },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.size(48.dp),
    ) {
        icon()
    }
}

@Preview
@Composable
private fun TimeSelectorPreview() {
    AppTheme {
        Column {
            TimeSelector(title = "Work", value = 20, onValueChange = {})
            TimeSelector(title = "Rest between sets", value = 90, onValueChange = {}, supportingText = "Skipped after last set")
            CountSelector(title = "Cycles", value = 8, onValueChange = {})
            TimeSelector(title = "Rest", value = 10, onValueChange = {}, enabled = false)
        }
    }
}

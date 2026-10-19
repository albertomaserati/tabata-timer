package com.amasesoft.tabatatimer.ui.run

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.amasesoft.tabatatimer.domain.PhaseType
import com.amasesoft.tabatatimer.domain.Step
import com.amasesoft.tabatatimer.domain.formatSeconds
import com.amasesoft.tabatatimer.platform.KeepScreenOn
import com.amasesoft.tabatatimer.platform.rememberCuePlayer
import com.amasesoft.tabatatimer.theme.AppThemeExtras
import com.amasesoft.tabatatimer.theme.CountdownTextStyle
import com.amasesoft.tabatatimer.theme.TabularNumbers
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.action_cancel
import tabatatimer.shared.generated.resources.action_close
import tabatatimer.shared.generated.resources.action_pause
import tabatatimer.shared.generated.resources.action_restart
import tabatatimer.shared.generated.resources.action_resume
import tabatatimer.shared.generated.resources.action_skip
import tabatatimer.shared.generated.resources.action_stop
import tabatatimer.shared.generated.resources.ic_arrow_back
import tabatatimer.shared.generated.resources.ic_pause
import tabatatimer.shared.generated.resources.ic_play
import tabatatimer.shared.generated.resources.ic_replay
import tabatatimer.shared.generated.resources.ic_skip_next
import tabatatimer.shared.generated.resources.ic_stop
import tabatatimer.shared.generated.resources.phase_prepare
import tabatatimer.shared.generated.resources.phase_rest
import tabatatimer.shared.generated.resources.phase_set_rest
import tabatatimer.shared.generated.resources.phase_work
import tabatatimer.shared.generated.resources.quick_workout
import tabatatimer.shared.generated.resources.run_cycle_indicator
import tabatatimer.shared.generated.resources.run_finished_subtitle
import tabatatimer.shared.generated.resources.run_finished_title
import tabatatimer.shared.generated.resources.run_last_activity
import tabatatimer.shared.generated.resources.run_next
import tabatatimer.shared.generated.resources.run_paused
import tabatatimer.shared.generated.resources.run_set_indicator
import tabatatimer.shared.generated.resources.run_stop_message
import tabatatimer.shared.generated.resources.run_stop_title
import tabatatimer.shared.generated.resources.run_total_remaining

@Composable
fun RunScreen(
    viewModel: RunViewModel,
    soundEnabled: Boolean,
    onClose: () -> Unit,
) {
    val uiState = viewModel.state.collectAsStateWithLifecycle()
    val cuePlayer = rememberCuePlayer()
    val haptics = LocalHapticFeedback.current
    val currentSoundEnabled by rememberUpdatedState(soundEnabled)
    val currentOnClose by rememberUpdatedState(onClose)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RunEffect.CountdownCue -> {
                    if (currentSoundEnabled) cuePlayer.countdown()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                is RunEffect.PhaseStartCue -> {
                    if (currentSoundEnabled) cuePlayer.phaseStart()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                RunEffect.FinishedCue -> {
                    if (currentSoundEnabled) cuePlayer.finished()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                RunEffect.Close -> currentOnClose()
            }
        }
    }

    // Background policy (both platforms): pause when the screen is no longer visible (app in
    // background, screen off, another destination on top). Resuming is always manual, so the
    // user never misses part of a workout and no beeps play from the background.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onIntent(RunIntent.Pause)
    }

    RunContent(stateHolder = uiState, onIntent = viewModel::onIntent)
}

/**
 * The state changes ~20 times per second; to keep recomposition at ~1 per second, children
 * read it through [derivedStateOf] and the progress indicators read it in the draw phase.
 */
@Composable
private fun RunContent(
    stateHolder: State<RunUiState>,
    onIntent: (RunIntent) -> Unit,
) {
    val status by remember { derivedStateOf { stateHolder.value.status } }
    val currentStep by remember { derivedStateOf { stateHolder.value.currentStep } }
    val nextStep by remember { derivedStateOf { stateHolder.value.nextStep } }
    val title by remember { derivedStateOf { stateHolder.value.title } }
    var showStopDialog by remember { mutableStateOf(false) }

    KeepScreenOn(enabled = status == RunStatus.Running)

    val requestStop = {
        if (status == RunStatus.Finished) onIntent(RunIntent.Stop) else showStopDialog = true
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = status != RunStatus.Finished,
        onBackCompleted = { showStopDialog = true },
    )

    val phaseColors = AppThemeExtras.phaseColors
    val accent by animateColorAsState(
        targetValue = if (status == RunStatus.Finished) {
            MaterialTheme.colorScheme.primary
        } else {
            phaseColors.of(currentStep?.type ?: PhaseType.Prepare)
        },
        animationSpec = tween(500),
        label = "phaseAccent",
    )
    val background = MaterialTheme.colorScheme.background
    val tintedBackground = lerp(background, accent, 0.14f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tintedBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = requestStop) {
                Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.action_close))
            }
            Text(
                text = title ?: stringResource(Res.string.quick_workout),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
        }

        val totalSets = remember(stateHolder) { stateHolder.value.totalSets }
        val totalCycles = remember(stateHolder) { stateHolder.value.totalCycles }

        val ring: @Composable (Modifier) -> Unit = { modifier ->
            // The ring is a square sized from the space actually available (the shorter
            // side, with a margin), so it never overflows on short screens / landscape.
            BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
                val side = (minOf(maxWidth, maxHeight) * 0.92f).coerceIn(0.dp, MaxRingSize)
                ProgressRing(
                    progress = {
                        val s = stateHolder.value
                        if (s.status == RunStatus.Finished) 1f else 1f - s.stepProgress
                    },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f),
                    modifier = Modifier.size(side),
                ) {
                    if (status == RunStatus.Finished) {
                        FinishedContent(accent = accent, ringSize = side)
                    } else {
                        PhaseContent(
                            stateHolder = stateHolder,
                            step = currentStep,
                            accent = accent,
                            paused = status == RunStatus.Paused,
                            ringSize = side,
                        )
                    }
                }
            }
        }
        val details: @Composable () -> Unit = {
            if (status != RunStatus.Finished) {
                NextUp(nextStep = nextStep)
                Spacer(Modifier.height(16.dp))
                TotalRemaining(stateHolder = stateHolder, accent = accent)
                Spacer(Modifier.height(24.dp))
                RunControls(
                    paused = status == RunStatus.Paused,
                    accent = accent,
                    onStop = requestStop,
                    onTogglePause = { onIntent(RunIntent.TogglePause) },
                    onSkip = { onIntent(RunIntent.Skip) },
                )
            } else {
                FinishedControls(
                    onRestart = { onIntent(RunIntent.Restart) },
                    onClose = { onIntent(RunIntent.Stop) },
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth > maxHeight) {
                // Landscape: ring on the left, information and controls on the right.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ring(Modifier.weight(1f).fillMaxHeight())
                    Column(
                        // Safety net for very short screens / large font scale.
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProgressIndicators(stateHolder, totalSets, totalCycles, accent)
                        Spacer(Modifier.height(16.dp))
                        details()
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))
                    ProgressIndicators(stateHolder, totalSets, totalCycles, accent)
                    ring(Modifier.weight(1f).fillMaxWidth())
                    details()
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text(stringResource(Res.string.run_stop_title)) },
            text = { Text(stringResource(Res.string.run_stop_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    onIntent(RunIntent.Stop)
                }) { Text(stringResource(Res.string.action_stop)) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ProgressIndicators(stateHolder: State<RunUiState>, totalSets: Int, totalCycles: Int, accent: Color) {
    val setNumber by remember { derivedStateOf { stateHolder.value.currentStep?.setNumber ?: 0 } }
    val cycleNumber by remember { derivedStateOf { stateHolder.value.currentStep?.cycleNumber ?: 0 } }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IndicatorPill(stringResource(Res.string.run_set_indicator, setNumber, totalSets), accent)
        IndicatorPill(stringResource(Res.string.run_cycle_indicator, cycleNumber, totalCycles), accent)
    }
}

@Composable
private fun IndicatorPill(text: String, accent: Color) {
    Surface(
        shape = CircleShape,
        color = accent.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Circular progress drawn with [Canvas]; [progress] is read in the draw phase only. */
@Composable
private fun ProgressRing(
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.06f
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress().coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
private fun PhaseContent(
    stateHolder: State<RunUiState>,
    step: Step?,
    accent: Color,
    paused: Boolean,
    ringSize: Dp,
) {
    val seconds by remember { derivedStateOf { stateHolder.value.position.displaySecondsInStep } }
    val phaseLabel = step?.let { phaseLabel(it.type) }.orEmpty()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(ringSize * RingContentPadding),
    ) {
        AnimatedContent(
            targetState = phaseLabel,
            transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut(tween(150))) },
            label = "phaseLabel",
        ) { label ->
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.titleLarge.fitRing(ringSize, 0.075f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
        // Rest steps carry the name of the cycle just done: show it only while working
        // (during rests the "Next" card already announces the upcoming exercise).
        val cycleName = if (step?.type == PhaseType.Work) step.cycleName else ""
        if (cycleName.isNotEmpty()) {
            Text(
                text = cycleName,
                style = MaterialTheme.typography.titleMedium.fitRing(ringSize, 0.06f),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        val text = if (seconds >= 60) formatSeconds(seconds) else seconds.toString()
        // Countdown digits scale with the ring (independent of the user font scale, which
        // would otherwise push them out of the circle); m:ss gets a smaller size.
        val style = countdownStyle(ringSize, longText = text.length > 3)
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (slideInVertically(tween(200)) { -it / 3 } + fadeIn(tween(200)))
                    .togetherWith(slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(150)))
            },
            label = "countdown",
        ) { value ->
            Text(
                text = value,
                style = style,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { contentDescription = value },
            )
        }
        if (paused) {
            Text(
                text = stringResource(Res.string.run_paused),
                style = MaterialTheme.typography.labelLarge.fitRing(ringSize, 0.05f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinishedContent(accent: Color, ringSize: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(ringSize * RingContentPadding),
    ) {
        Text(
            text = stringResource(Res.string.run_finished_title),
            style = MaterialTheme.typography.headlineLarge.fitRing(ringSize, 0.1f),
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.run_finished_subtitle),
            style = MaterialTheme.typography.bodyLarge.fitRing(ringSize, 0.055f),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NextUp(nextStep: Step?) {
    val text = if (nextStep == null) {
        stringResource(Res.string.run_last_activity)
    } else {
        val label = stepLabel(nextStep)
        stringResource(Res.string.run_next, label, formatSeconds(nextStep.durationSeconds))
    }
    val nextColor = nextStep?.let { AppThemeExtras.phaseColors.of(it.type) } ?: MaterialTheme.colorScheme.outline
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(Modifier.size(12.dp).background(nextColor, CircleShape))
            Spacer(Modifier.size(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TotalRemaining(stateHolder: State<RunUiState>, accent: Color) {
    val totalSeconds by remember { derivedStateOf { stateHolder.value.position.displayTotalSeconds } }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.run_total_remaining),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatSeconds(totalSeconds),
                style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { stateHolder.value.totalProgress },
            color = accent,
            trackColor = accent.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth().height(6.dp),
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun RunControls(
    paused: Boolean,
    accent: Color,
    onStop: () -> Unit,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onStop, modifier = Modifier.size(64.dp)) {
            Icon(painterResource(Res.drawable.ic_stop), stringResource(Res.string.action_stop))
        }
        FilledIconButton(
            onClick = onTogglePause,
            modifier = Modifier.size(88.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            AnimatedContent(targetState = paused, label = "pauseIcon") { isPaused ->
                Icon(
                    painter = painterResource(if (isPaused) Res.drawable.ic_play else Res.drawable.ic_pause),
                    contentDescription = stringResource(if (isPaused) Res.string.action_resume else Res.string.action_pause),
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        FilledTonalIconButton(onClick = onSkip, modifier = Modifier.size(64.dp)) {
            Icon(painterResource(Res.drawable.ic_skip_next), stringResource(Res.string.action_skip))
        }
    }
}

@Composable
private fun FinishedControls(onRestart: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Labels stay on one line and shrink if needed (narrow phones, large font scale).
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                painterResource(Res.drawable.ic_replay),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            SingleLineButtonLabel(stringResource(Res.string.action_restart))
        }
        Button(onClick = onClose, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
            SingleLineButtonLabel(stringResource(Res.string.action_close))
        }
    }
}

@Composable
private fun SingleLineButtonLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 11.sp,
            maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
        ),
    )
}

/** Name of a step for the user: the exercise name for named work, the phase otherwise. */
@Composable
internal fun stepLabel(step: Step): String =
    if (step.type == PhaseType.Work && step.cycleName.isNotEmpty()) step.cycleName else phaseLabel(step.type)

@Composable
internal fun phaseLabel(type: PhaseType): String = stringResource(
    when (type) {
        PhaseType.Prepare -> Res.string.phase_prepare
        PhaseType.Work -> Res.string.phase_work
        PhaseType.Rest -> Res.string.phase_rest
        PhaseType.SetRest -> Res.string.phase_set_rest
    },
)

private val MaxRingSize = 360.dp

/** Inner padding of the ring content, as a fraction of the ring side (clears the stroke). */
private const val RingContentPadding = 0.12f

/**
 * Caps a text style so that its font size is at most [fraction] of the ring side: on a big
 * ring the theme size is kept, on a small one the text shrinks with the ring.
 */
@Composable
private fun TextStyle.fitRing(ringSize: Dp, fraction: Float): TextStyle {
    val maxSize = with(LocalDensity.current) { (ringSize * fraction).toSp() }
    return if (fontSize.isSp && fontSize.value > maxSize.value) {
        copy(fontSize = maxSize, lineHeight = maxSize * 1.2f)
    } else {
        this
    }
}

/** Countdown digits sized from the ring side: about 36% for "20", 24% for "1:30". */
@Composable
private fun countdownStyle(ringSize: Dp, longText: Boolean): TextStyle {
    val size = with(LocalDensity.current) { (ringSize * if (longText) 0.24f else 0.36f).toSp() }
    return CountdownTextStyle.copy(fontSize = size, lineHeight = size)
}

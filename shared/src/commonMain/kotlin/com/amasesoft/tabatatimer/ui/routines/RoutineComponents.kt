package com.amasesoft.tabatatimer.ui.routines

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.amasesoft.tabatatimer.domain.GymNameError
import com.amasesoft.tabatatimer.domain.RoutineLimits
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.action_cancel
import tabatatimer.shared.generated.resources.action_save
import tabatatimer.shared.generated.resources.char_counter
import tabatatimer.shared.generated.resources.editor_name_required
import tabatatimer.shared.generated.resources.name_too_long
import tabatatimer.shared.generated.resources.routine_name
import tabatatimer.shared.generated.resources.routine_name_taken
import tabatatimer.shared.generated.resources.routine_rename_title
import tabatatimer.shared.generated.resources.routines_new

/**
 * Single-line name field with an inline error (same style as the workout editor) and a
 * character counter. Input is not truncated: going over [maxLength] shows the error.
 */
@Composable
internal fun GymNameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: GymNameError?,
    maxLength: Int,
    modifier: Modifier = Modifier,
    duplicateMessage: StringResource = Res.string.routine_name_taken,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    val length = value.trim().length
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null || length > maxLength,
        supportingText = {
            Row {
                Text(
                    text = when (error) {
                        GymNameError.Empty -> stringResource(Res.string.editor_name_required)
                        GymNameError.TooLong -> stringResource(Res.string.name_too_long, maxLength)
                        GymNameError.Duplicate -> stringResource(duplicateMessage)
                        null -> ""
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                // The counter is visual only; the error text is what matters to screen readers.
                Text(
                    text = stringResource(Res.string.char_counter, length, maxLength),
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Create / rename dialog of a routine. */
@Composable
internal fun RoutineNameDialogView(
    dialog: RoutineNameDialog,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (dialog.target is RoutineNameTarget.Rename) Res.string.routine_rename_title else Res.string.routines_new,
                ),
            )
        },
        text = {
            GymNameField(
                value = dialog.text,
                onValueChange = onNameChange,
                label = stringResource(Res.string.routine_name),
                error = dialog.error,
                maxLength = RoutineLimits.NAME_MAX_LENGTH,
                imeAction = ImeAction.Done,
                onImeAction = onConfirm,
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !dialog.isSaving) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

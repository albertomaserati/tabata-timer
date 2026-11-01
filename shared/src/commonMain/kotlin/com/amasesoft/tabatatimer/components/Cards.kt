package com.amasesoft.tabatatimer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amasesoft.tabatatimer.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.action_retry
import tabatatimer.shared.generated.resources.ic_add
import tabatatimer.shared.generated.resources.ic_error
import tabatatimer.shared.generated.resources.ic_replay

/** Grouping card of the editors, with an optional heading. */
@Composable
fun SectionCard(
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

/**
 * "Add the next item" placeholder closing a list: same shape and width as a card, dashed
 * primary border, light primaryContainer fill and a filled "+" so it reads as an action at a
 * glance. When disabled (limit reached) [title] should explain why.
 */
@Composable
fun DashedAddCard(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (enabled && hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Read error: the content is unknown, so it is not shown as empty. Offers a retry. */
@Composable
fun LoadErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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
            text = message,
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

@Preview
@Composable
private fun DashedAddCardPreview() {
    AppTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashedAddCard(title = "Add cycle 9", hint = "Copies the times of the last cycle", enabled = true, onClick = {})
            DashedAddCard(title = "Maximum 20 routines", enabled = false, onClick = {})
        }
    }
}

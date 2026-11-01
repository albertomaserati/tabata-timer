package com.amasesoft.tabatatimer.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.amasesoft.tabatatimer.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.ic_fitness
import tabatatimer.shared.generated.resources.ic_timer
import tabatatimer.shared.generated.resources.tab_routines
import tabatatimer.shared.generated.resources.tab_timer

/** Top-level destinations, shown in the bottom bar. */
enum class MainTab(val label: StringResource, val icon: DrawableResource) {
    Timer(Res.string.tab_timer, Res.drawable.ic_timer),
    Routines(Res.string.tab_routines, Res.drawable.ic_fitness),
}

/**
 * "Timer | Routines" bar. It is part of each top-level screen's Scaffold (not of a scaffold
 * around the NavHost), so it leaves together with its screen when a detail screen is opened,
 * and those screens keep the whole height.
 */
@Composable
fun MainTabsBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { if (tab != selected) onSelect(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Preview
@Composable
private fun MainTabsBarPreview() {
    AppTheme { MainTabsBar(selected = MainTab.Routines, onSelect = {}) }
}

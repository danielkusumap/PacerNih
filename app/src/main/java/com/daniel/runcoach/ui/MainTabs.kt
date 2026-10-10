package com.daniel.runcoach.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.daniel.runcoach.R
import com.daniel.runcoach.ui.components.RcBottomBar
import com.daniel.runcoach.ui.components.RcTabItem
import com.daniel.runcoach.ui.theme.RcIcons

/** Bottom tabs (plan section 8.1, DESIGN.md section 6 BottomBar). */
enum class MainTab { WORKOUT, HISTORY, STATS, SETTINGS }

@Composable
fun MainBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val items = listOf(
        RcTabItem(RcIcons.PlayArrow, stringResource(R.string.tab_workout)),
        RcTabItem(RcIcons.History, stringResource(R.string.tab_history)),
        RcTabItem(RcIcons.BarChart, stringResource(R.string.tab_stats)),
        RcTabItem(RcIcons.Settings, stringResource(R.string.tab_settings)),
    )
    RcBottomBar(
        items = items,
        selectedIndex = selected.ordinal,
        onSelect = { index -> onSelect(MainTab.entries[index]) },
    )
}

package com.daniel.runcoach.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.daniel.runcoach.R

/** Bottom tabs (plan section 8.1). */
enum class MainTab { WORKOUT, HISTORY, STATS, SETTINGS }

@Composable
fun MainBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == MainTab.WORKOUT,
            onClick = { onSelect(MainTab.WORKOUT) },
            icon = { Text("▸") },
            label = { Text(stringResource(R.string.tab_workout)) },
        )
        NavigationBarItem(
            selected = selected == MainTab.HISTORY,
            onClick = { onSelect(MainTab.HISTORY) },
            icon = { Text("☰") },
            label = { Text(stringResource(R.string.tab_history)) },
        )
        NavigationBarItem(
            selected = selected == MainTab.STATS,
            onClick = { onSelect(MainTab.STATS) },
            icon = { Text("∑") },
            label = { Text(stringResource(R.string.tab_stats)) },
        )
        NavigationBarItem(
            selected = selected == MainTab.SETTINGS,
            onClick = { onSelect(MainTab.SETTINGS) },
            icon = { Text("⚙") },
            label = { Text(stringResource(R.string.tab_settings)) },
        )
    }
}

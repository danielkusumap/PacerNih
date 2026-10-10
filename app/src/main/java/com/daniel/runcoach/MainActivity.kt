package com.daniel.runcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.daniel.runcoach.data.AppSettings
import com.daniel.runcoach.data.AppTheme
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.ui.AppNavHost
import com.daniel.runcoach.ui.theme.RcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(this) }
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            val darkTheme = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            RcTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}

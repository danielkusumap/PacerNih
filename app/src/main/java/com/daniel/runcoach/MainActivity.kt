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
import com.daniel.runcoach.debug.MockDataSeeder
import com.daniel.runcoach.ui.AppNavHost
import com.daniel.runcoach.ui.theme.RcTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.MOCK_DATA) {
            val appContext = applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { MockDataSeeder.seedIfEmpty(appContext) }
                    .onFailure { android.util.Log.e("MockDataSeeder", "seed failed", it) }
            }
        }
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

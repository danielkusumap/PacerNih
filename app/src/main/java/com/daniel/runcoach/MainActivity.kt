package com.daniel.runcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.daniel.runcoach.ui.AppNavHost
import com.daniel.runcoach.ui.theme.RunCoachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunCoachTheme {
                AppNavHost()
            }
        }
    }
}

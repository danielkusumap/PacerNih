package com.daniel.runcoach.ui

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daniel.runcoach.ui.battery.BatteryGuideScreen
import com.daniel.runcoach.ui.debug.SimulatorScreen
import com.daniel.runcoach.ui.editor.EditorRequestHolder
import com.daniel.runcoach.ui.editor.EditorScreen
import com.daniel.runcoach.ui.home.BerandaScreen
import com.daniel.runcoach.ui.onboarding.OnboardingScreen
import com.daniel.runcoach.ui.run.RunRoute

/** Navigation routes. More screens arrive in later tasks. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val BATTERY = "battery"
    const val HOME = "home"
    const val RUN = "run"
    const val EDITOR = "editor"
    const val DEBUG = "debug"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val debugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onStart = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onOpenBatteryGuide = { navController.navigate(Routes.BATTERY) },
            )
        }
        composable(Routes.BATTERY) {
            BatteryGuideScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HOME) {
            BerandaScreen(
                debugBuild = debugBuild,
                onOpenRun = { navController.navigate(Routes.RUN) },
                onOpenEditor = { workout ->
                    EditorRequestHolder.initial = workout
                    navController.navigate(Routes.EDITOR)
                },
                onOpenSimulator = { navController.navigate(Routes.DEBUG) },
            )
        }
        composable(Routes.RUN) {
            RunRoute(onStop = { navController.popBackStack() })
        }
        composable(Routes.EDITOR) {
            EditorScreen(
                initial = EditorRequestHolder.initial,
                onSaved = { EditorRequestHolder.initial = null; navController.popBackStack() },
                onCancel = { EditorRequestHolder.initial = null; navController.popBackStack() },
            )
        }
        composable(Routes.DEBUG) {
            SimulatorScreen(onBack = { navController.popBackStack() })
        }
    }
}

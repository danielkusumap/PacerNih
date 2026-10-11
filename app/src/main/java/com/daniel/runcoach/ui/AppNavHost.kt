package com.daniel.runcoach.ui

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.ui.battery.BatteryGuideScreen
import com.daniel.runcoach.ui.components.RcCatalogScreen
import com.daniel.runcoach.ui.debug.SimulatorScreen
import com.daniel.runcoach.ui.editor.EditorRequestHolder
import com.daniel.runcoach.ui.editor.EditorScreen
import com.daniel.runcoach.ui.finish.FinishedScreen
import com.daniel.runcoach.ui.history.DetailSessionScreen
import com.daniel.runcoach.ui.history.HistoryScreen
import com.daniel.runcoach.ui.home.BerandaScreen
import com.daniel.runcoach.ui.onboarding.OnboardingScreen
import com.daniel.runcoach.ui.prestart.PraStartScreen
import com.daniel.runcoach.ui.run.RunRoute
import com.daniel.runcoach.ui.settings.BackupScreen
import com.daniel.runcoach.ui.settings.SettingsScreen
import com.daniel.runcoach.ui.stats.RecordDetailScreen
import com.daniel.runcoach.ui.stats.StatsScreen
import com.daniel.runcoach.service.FinishedSessionHolder
import kotlinx.coroutines.launch

/** Navigation routes. More screens arrive in later tasks. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val BATTERY = "battery"
    const val HOME = "home"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val DATA = "data"
    const val RECORD = "record/{key}"
    const val DETAIL = "detail/{sessionId}"
    const val SHARE = "share/{sessionId}"
    const val PRESTART = "prestart"
    const val RUN = "run"
    const val FINISHED = "finished"
    const val EDITOR = "editor"
    const val DEBUG = "debug"
    const val CATALOG = "catalog"
}

@Composable
fun AppNavHost(startDestination: String = Routes.ONBOARDING) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(context) { SettingsRepository(context) }
    val debugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    // Auto-recovery: if a finished-but-unsaved run (draft) exists, go straight to the save screen.
    LaunchedEffect(Unit) {
        val draft = SessionRepository(context).pendingDraft()
        if (draft != null) {
            FinishedSessionHolder.sessionId = draft.id
            navController.navigate(Routes.FINISHED)
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onStart = {
                    scope.launch { settingsRepository.setOnboardingDone(true) }
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
                onOpenPrestart = { workout ->
                    StartRequestHolder.workout = workout
                    navController.navigate(Routes.PRESTART)
                },
                onResumeRun = { navController.navigate(Routes.RUN) },
                onOpenEditor = { workout ->
                    EditorRequestHolder.initial = workout
                    navController.navigate(Routes.EDITOR)
                },
                onOpenSimulator = { navController.navigate(Routes.DEBUG) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onSelectTab = { tab ->
                    when (tab) {
                        MainTab.HISTORY -> navController.navigate(Routes.HISTORY)
                        MainTab.STATS -> navController.navigate(Routes.STATS)
                        MainTab.SETTINGS -> navController.navigate(Routes.SETTINGS)
                        else -> {}
                    }
                },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onOpenSession = { sessionId -> navController.navigate("detail/$sessionId") },
                onSelectTab = { tab ->
                    when (tab) {
                        MainTab.WORKOUT -> navController.popBackStack(Routes.HOME, false)
                        MainTab.STATS -> navController.navigate(Routes.STATS)
                        MainTab.SETTINGS -> navController.navigate(Routes.SETTINGS)
                        else -> {}
                    }
                },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                onOpenRecord = { key -> navController.navigate("record/$key") },
                onOpenSession = { sessionId -> navController.navigate("detail/$sessionId") },
                onSelectTab = { tab ->
                    when (tab) {
                        MainTab.WORKOUT -> navController.popBackStack(Routes.HOME, false)
                        MainTab.HISTORY -> navController.navigate(Routes.HISTORY)
                        MainTab.SETTINGS -> navController.navigate(Routes.SETTINGS)
                        else -> {}
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                debugBuild = debugBuild,
                onOpenBatteryGuide = { navController.navigate(Routes.BATTERY) },
                onOpenData = { navController.navigate(Routes.DATA) },
                onOpenSimulator = { navController.navigate(Routes.DEBUG) },
                onOpenCatalog = { navController.navigate(Routes.CATALOG) },
                onSelectTab = { tab ->
                    when (tab) {
                        MainTab.WORKOUT -> navController.popBackStack(Routes.HOME, false)
                        MainTab.HISTORY -> navController.navigate(Routes.HISTORY)
                        MainTab.STATS -> navController.navigate(Routes.STATS)
                        else -> {}
                    }
                },
            )
        }
        composable(Routes.DATA) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.RECORD,
            arguments = listOf(navArgument("key") { type = NavType.StringType }),
        ) { entry ->
            RecordDetailScreen(
                keyName = entry.arguments?.getString("key").orEmpty(),
                onOpenSession = { sessionId -> navController.navigate("detail/$sessionId") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { entry ->
            DetailSessionScreen(
                sessionId = entry.arguments?.getString("sessionId").orEmpty(),
                onOpenShare = { sessionId -> navController.navigate("share/$sessionId") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.SHARE,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { entry ->
            com.daniel.runcoach.ui.share.ShareScreen(
                sessionId = entry.arguments?.getString("sessionId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PRESTART) {
            PraStartScreen(
                workout = StartRequestHolder.workout ?: WorkoutTemplateRepository.freeRunWorkout(),
                onStarted = { navController.navigate(Routes.RUN) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.RUN) {
            RunRoute(
                onStop = {
                    navController.navigate(Routes.FINISHED) { popUpTo(Routes.HOME) }
                },
            )
        }
        composable(Routes.FINISHED) {
            FinishedScreen(onDone = { navController.popBackStack(Routes.HOME, inclusive = false) })
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
        composable(Routes.CATALOG) {
            RcCatalogScreen(onBack = { navController.popBackStack() })
        }
    }
}

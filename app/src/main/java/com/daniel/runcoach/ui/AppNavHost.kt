package com.daniel.runcoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daniel.runcoach.R
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.RunSnapshot
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.service.RunForegroundService
import com.daniel.runcoach.ui.battery.BatteryGuideScreen
import com.daniel.runcoach.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

/** Navigation routes for the screens built so far. More screens arrive in later tasks. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val BATTERY = "battery"
    const val HOME = "home"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
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
            HomePlaceholder()
        }
    }
}

/**
 * Temporary landing screen for P1.3: it can start/stop the foreground service so the notification and
 * screen-off behaviour can be verified by hand, and offers D04 resume when an unfinished run is found.
 * Replaced by S02 (Beranda) in P2.3.
 */
@Composable
private fun HomePlaceholder() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val persistence = remember(context) { RunPersistence(context) }
    var pendingSnapshot by remember { mutableStateOf<RunSnapshot?>(null) }

    LaunchedEffect(Unit) {
        pendingSnapshot = persistence.loadSnapshot()
    }

    pendingSnapshot?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { pendingSnapshot = null },
            title = { Text(stringResource(R.string.dialog_resume_title)) },
            text = { Text(stringResource(R.string.dialog_resume_message)) },
            confirmButton = {
                TextButton(onClick = {
                    RunForegroundService.resume(context, snapshot.sessionId)
                    pendingSnapshot = null
                }) { Text(stringResource(R.string.action_resume)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch { persistence.clear(snapshot.sessionId) }
                    pendingSnapshot = null
                }) { Text(stringResource(R.string.action_discard)) }
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.home_placeholder), style = MaterialTheme.typography.titleLarge)
            Button(onClick = { RunForegroundService.start(context, freeRun()) }) {
                Text(stringResource(R.string.action_start_test_run))
            }
            OutlinedButton(onClick = { RunForegroundService.stop(context) }) {
                Text(stringResource(R.string.action_stop_run))
            }
        }
    }
}

private fun freeRun() = Workout(
    id = "free",
    name = "Lari Bebas",
    items = listOf(Segment("Lari Bebas", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null)),
)

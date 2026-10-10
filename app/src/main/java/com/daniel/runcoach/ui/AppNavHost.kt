package com.daniel.runcoach.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daniel.runcoach.R
import com.daniel.runcoach.ui.battery.BatteryGuideScreen
import com.daniel.runcoach.ui.onboarding.OnboardingScreen

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

@Composable
private fun HomePlaceholder() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.home_placeholder), style = MaterialTheme.typography.titleLarge)
        }
    }
}

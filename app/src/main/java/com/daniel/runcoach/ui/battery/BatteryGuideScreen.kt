package com.daniel.runcoach.ui.battery

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.ui.onboarding.appDetailsIntent
import com.daniel.runcoach.ui.theme.RunCoachTheme

/**
 * S14 Panduan Baterai (plan section 8). Per-brand steps plus deep links to battery/app settings, and the
 * current battery-optimization exemption status when it can be detected.
 */
@Composable
fun BatteryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val exempt = remember(refreshTick) { isBatteryOptimizationExempt(context) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.battery_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.battery_intro), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(
                    if (exempt) R.string.battery_exempt_yes else R.string.battery_exempt_no,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = if (exempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            BrandStep(stringResource(R.string.battery_brand_xiaomi))
            BrandStep(stringResource(R.string.battery_brand_oppo))
            BrandStep(stringResource(R.string.battery_brand_vivo))
            BrandStep(stringResource(R.string.battery_brand_samsung))
            BrandStep(stringResource(R.string.battery_brand_other))

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    refreshTick++
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.battery_action_open_battery))
            }
            OutlinedButton(
                onClick = { context.startActivity(appDetailsIntent(context)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_open_settings))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
private fun BrandStep(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall)
}

internal fun isBatteryOptimizationExempt(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BatteryGuidePreview() {
    RunCoachTheme {
        BatteryGuideScreen(onBack = {})
    }
}

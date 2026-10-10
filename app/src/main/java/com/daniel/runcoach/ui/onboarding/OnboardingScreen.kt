package com.daniel.runcoach.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.core.content.ContextCompat
import com.daniel.runcoach.R
import com.daniel.runcoach.ui.theme.RunCoachTheme

/**
 * S01 Onboarding & Izin (plan section 8). Explains each permission, shows location/notification/GPS
 * status, and only offers Start once location is granted. Never asks for background location.
 */
@Composable
fun OnboardingScreen(onStart: () -> Unit, onOpenBatteryGuide: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshTick++ }

    val locationGranted = remember(refreshTick) { hasLocationPermission(context) }
    val notificationsGranted = remember(refreshTick) { hasNotificationPermission(context) }
    val gpsEnabled = remember(refreshTick) { isGpsEnabled(context) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyMedium)

            PermissionCard(
                title = stringResource(R.string.permission_location_title),
                description = stringResource(R.string.permission_location_desc),
                granted = locationGranted,
            )
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_grant_location))
            }

            PermissionCard(
                title = stringResource(R.string.permission_notification_title),
                description = stringResource(R.string.permission_notification_desc),
                granted = notificationsGranted,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) }) {
                        Text(stringResource(R.string.action_grant_notification))
                    }
                }
            }

            PermissionCard(
                title = stringResource(R.string.gps_title),
                description = stringResource(R.string.gps_desc),
                granted = gpsEnabled,
            )
            OutlinedButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_open_location_settings))
            }

            OutlinedButton(
                onClick = { context.startActivity(appDetailsIntent(context)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_open_settings))
            }
            OutlinedButton(onClick = onOpenBatteryGuide, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_battery_guide))
            }

            Button(
                onClick = onStart,
                enabled = locationGranted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_start))
            }
            if (!locationGranted) {
                Text(
                    stringResource(R.string.permission_location_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(title: String, description: String, granted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
            val status = if (granted) stringResource(R.string.status_granted) else stringResource(R.string.status_denied)
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

internal fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

internal fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

internal fun isGpsEnabled(context: Context): Boolean =
    context.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

internal fun appDetailsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingPreview() {
    RunCoachTheme {
        OnboardingScreen(onStart = {}, onOpenBatteryGuide = {})
    }
}

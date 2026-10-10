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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.daniel.runcoach.R
import com.daniel.runcoach.ui.components.Banner
import com.daniel.runcoach.ui.components.RcBannerVariant
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import com.daniel.runcoach.ui.theme.RcTheme

/**
 * S01 Onboarding & permission (DESIGN.md section 8): four steps, one decision each, a step indicator at the top
 * and a single primary action at the bottom. Location denial never blocks the app.
 */
@Composable
fun OnboardingScreen(onStart: () -> Unit, onOpenBatteryGuide: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var step by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshTick++ }

    val locationGranted = remember(refreshTick) { hasLocationPermission(context) }
    val notificationsGranted = remember(refreshTick) { hasNotificationPermission(context) }
    val gpsEnabled = remember(refreshTick) { isGpsEnabled(context) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    val steps = 4
    val icon = when (step) {
        0 -> RcIcons.DirectionsRun
        1 -> RcIcons.GpsFixed
        2 -> RcIcons.Notifications
        else -> RcIcons.BatteryAlert
    }
    val title = when (step) {
        0 -> stringResource(R.string.onboarding_title)
        1 -> stringResource(R.string.permission_location_title)
        2 -> stringResource(R.string.permission_notification_title)
        else -> stringResource(R.string.battery_title)
    }
    val body = when (step) {
        0 -> stringResource(R.string.onboarding_intro)
        1 -> stringResource(R.string.permission_location_desc)
        2 -> stringResource(R.string.permission_notification_desc)
        else -> stringResource(R.string.battery_intro)
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = spacing.screenH)) {
            StepIndicator(step = step, steps = steps, modifier = Modifier.padding(top = spacing.s5))

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(modifier = Modifier.size(112.dp).clip(CircleShape).background(colors.accentContainer), contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.height(spacing.s5))
                Text(title, style = type.titleL, color = colors.textPrimary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(spacing.s3))
                Text(body, style = type.body, color = colors.textSecondary, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)

                if (step == 1) PermissionStatusRow(locationGranted)
                if (step == 2) PermissionStatusRow(notificationsGranted)

                if (step == 1 && !gpsEnabled) {
                    Banner(
                        stringResource(R.string.onboarding_gps_off),
                        variant = RcBannerVariant.Warning,
                        action = stringResource(R.string.action_open_settings),
                        onAction = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                        modifier = Modifier.padding(top = spacing.s3),
                    )
                }
                if (step == 1 && !locationGranted) {
                    Banner(
                        stringResource(R.string.permission_location_required),
                        variant = RcBannerVariant.Warning,
                        modifier = Modifier.padding(top = spacing.s3),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = spacing.s5), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
                when (step) {
                    0 -> RcButton(stringResource(R.string.onboarding_next), onClick = { step = 1 }, fillWidth = true)
                    1 -> {
                        if (locationGranted) {
                            RcButton(stringResource(R.string.onboarding_next), onClick = { step = 2 }, fillWidth = true)
                        } else {
                            RcButton(
                                stringResource(R.string.action_grant_location),
                                onClick = {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                },
                                fillWidth = true,
                            )
                        }
                    }
                    2 -> {
                        if (notificationsGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            RcButton(stringResource(R.string.onboarding_next), onClick = { step = 3 }, fillWidth = true)
                        } else {
                            RcButton(
                                stringResource(R.string.action_grant_notification),
                                onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) },
                                fillWidth = true,
                            )
                        }
                        RcButton(stringResource(R.string.onboarding_skip), onClick = { step = 3 }, variant = RcButtonVariant.Tertiary, fillWidth = true)
                    }
                    else -> {
                        RcButton(stringResource(R.string.action_start), onClick = onStart, fillWidth = true)
                        RcButton(stringResource(R.string.battery_action_open_battery), onClick = onOpenBatteryGuide, variant = RcButtonVariant.Secondary, fillWidth = true)
                        RcButton(stringResource(R.string.onboarding_skip), onClick = onStart, variant = RcButtonVariant.Tertiary, fillWidth = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, steps: Int, modifier: Modifier = Modifier) {
    val colors = LocalRcColors.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(steps) { index ->
            if (index == step) {
                Box(Modifier.width(24.dp).height(6.dp).background(colors.accent, RoundedCornerShape(3.dp)))
            } else {
                Box(Modifier.size(6.dp).background(colors.outlineStrong, CircleShape))
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(granted: Boolean) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            painterResource(if (granted) RcIcons.CheckCircle else RcIcons.Cancel),
            contentDescription = null,
            tint = if (granted) colors.pass else colors.fail,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(if (granted) R.string.status_granted else R.string.status_denied),
            style = type.bodySmall,
            color = if (granted) colors.pass else colors.fail,
        )
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
    RcTheme {
        OnboardingScreen(onStart = {}, onOpenBatteryGuide = {})
    }
}

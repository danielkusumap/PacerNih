package com.daniel.runcoach.ui.battery

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.ResultChip
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.onboarding.appDetailsIntent
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcTheme

/** S14 Battery guide (DESIGN.md section 8): brand chips, numbered steps, exemption status, sticky action. */
@Composable
fun BatteryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val exempt = remember(refreshTick) { isBatteryOptimizationExempt(context) }
    var brand by remember { mutableStateOf(0) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    val brands = listOf(
        stringResource(R.string.battery_label_xiaomi) to stringResource(R.string.battery_brand_xiaomi),
        stringResource(R.string.battery_label_oppo) to stringResource(R.string.battery_brand_oppo),
        stringResource(R.string.battery_label_vivo) to stringResource(R.string.battery_brand_vivo),
        stringResource(R.string.battery_label_samsung) to stringResource(R.string.battery_brand_samsung),
        stringResource(R.string.battery_label_other) to stringResource(R.string.battery_brand_other),
    )

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = stringResource(R.string.battery_title), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            Text(stringResource(R.string.battery_intro), style = type.body, color = colors.textSecondary)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                ResultChip(
                    status = if (exempt) RcStatus.Pass else RcStatus.Warn,
                    label = if (exempt) stringResource(R.string.battery_exempt_yes) else stringResource(R.string.battery_exempt_no),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                brands.forEachIndexed { index, (label, _) ->
                    val active = index == brand
                    Text(
                        label,
                        style = type.titleS,
                        color = if (active) colors.onAccent else colors.textSecondary,
                        modifier = Modifier
                            .background(if (active) colors.accent else colors.surfaceRaised, RoundedCornerShape(spacing.s5))
                            .border(1.dp, colors.outlineStrong, RoundedCornerShape(spacing.s5))
                            .clickable { brand = index }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }

            RcCard {
                Text(brands[brand].first, style = type.titleS, color = colors.textPrimary)
                Text(brands[brand].second, style = type.body, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.s2))
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = spacing.s4))
        }

        Column(modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = spacing.screenH).padding(vertical = spacing.s3), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            RcButton(
                stringResource(R.string.battery_action_open_battery),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    refreshTick++
                },
                fillWidth = true,
            )
            RcButton(stringResource(R.string.action_open_settings), onClick = { context.startActivity(appDetailsIntent(context)) }, variant = RcButtonVariant.Secondary, fillWidth = true)
        }
    }
}

internal fun isBatteryOptimizationExempt(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BatteryGuidePreview() {
    RcTheme {
        BatteryGuideScreen(onBack = {})
    }
}

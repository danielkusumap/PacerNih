package com.daniel.runcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import com.daniel.runcoach.ui.theme.RcTheme

/**
 * Debug-only component catalogue (DESIGN.md U1). Shows every reusable component so it can be compared with
 * section 6. Reached from S12 → Pengembang (debug builds only).
 */
@Composable
fun RcCatalogScreen(onBack: () -> Unit) {
    val colors = LocalRcColors.current
    val spacing = com.daniel.runcoach.ui.theme.LocalRcSpacing.current
    var switch by remember { mutableStateOf(true) }
    var segment by remember { mutableStateOf("A") }
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(spacing.screenH),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        TopBar(title = "Catalog", onBack = onBack)

        SectionHeader("BUTTONS")
        RcButton("Primary", onClick = {}, fillWidth = true)
        RcButton("Secondary", onClick = {}, variant = RcButtonVariant.Secondary, fillWidth = true)
        RcButton("Tertiary", onClick = {}, variant = RcButtonVariant.Tertiary, fillWidth = true)
        RcButton("Destructive", onClick = {}, variant = RcButtonVariant.Destructive, fillWidth = true)
        RcButton("Loading", onClick = {}, loading = true, fillWidth = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RcButton("Small", onClick = {}, size = RcButtonSize.Small)
            RcIconButton(RcIcons.PlayArrow, "play", onClick = {}, variant = RcIconButtonVariant.Accent)
            RcIconButton(RcIcons.Pause, "pause", onClick = {}, variant = RcIconButtonVariant.Filled)
        }

        SectionHeader("STATUS")
        StatusBadge(RcStatus.Pass, "Steady", delta = "-0:02")
        StatusBadge(RcStatus.Fail, "Too slow", delta = "+0:15")
        StatusBadge(RcStatus.Fast, "Too fast")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultChip(RcStatus.Pass, "Passed")
            ResultChip(RcStatus.Fail, "Failed")
            ResultChip(RcStatus.Warn, "Borderline")
            ResultChip(RcStatus.Neutral, "No target")
        }
        Banner("Info banner", variant = RcBannerVariant.Info)
        Banner("Warning banner", variant = RcBannerVariant.Warning)
        Banner("Success banner", variant = RcBannerVariant.Success)
        Banner("Accent banner", variant = RcBannerVariant.Accent, action = "Open", onAction = {})

        SectionHeader("METRICS")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBlock("Distance", "5,03", unit = "km")
            MetricBlock("Time", "28:10")
            MetricBlock("Pace", "5:36")
        }
        HeroPace(336.0)
        HeroPace(null)

        SectionHeader("SEGMENTS")
        SegmentProgressBar(0.45f, SegmentKind.RUN)
        SegmentStrip(listOf(SegmentKind.WARMUP, SegmentKind.RUN, SegmentKind.RECOVERY, SegmentKind.COOLDOWN))
        ResultStrip(listOf(RcStatus.Pass, RcStatus.Warn, RcStatus.Fail, RcStatus.Neutral))

        SectionHeader("CONTROLS")
        SegmentedControl(listOf("A" to "Alpha", "B" to "Beta", "C" to "Gamma"), segment, { segment = it })
        RcSwitchRow("Switch row", switch, { switch = it }, subtitle = "With subtitle")
        NumberStepper(value = "420", onDecrement = {}, onIncrement = {}, stepLabel = "5 s")
        RcTextField(value = text, onValueChange = { text = it }, label = "Name", placeholder = "Lari Pagi")
        RcTextField(value = "", onValueChange = {}, label = "Error field", isError = true, errorText = "Required")

        SectionHeader("CONTAINERS & FEEDBACK")
        RcCard { Text("RcCard content", style = LocalRcTypography.current.body, color = colors.textPrimary) }
        EmptyState(title = "Nothing here", message = "No data yet.")
        Skeleton(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(64.dp))
        ProgressRing(onConfirm = {}) { RcIconButton(RcIcons.Stop, "stop", onClick = {}, tint = colors.fail) }

        SectionHeader("LIST")
        ListRow(title = "List row", subtitle = "Subtitle", trailing = { RcIconButton(RcIcons.ChevronRight, null, onClick = {}) })
        RcDivider()
    }
}

@Preview(name = "Catalog light")
@Composable
private fun CatalogLight() = RcTheme(darkTheme = false) { RcCatalogScreen(onBack = {}) }

@Preview(name = "Catalog dark")
@Composable
private fun CatalogDark() = RcTheme(darkTheme = true) { RcCatalogScreen(onBack = {}) }

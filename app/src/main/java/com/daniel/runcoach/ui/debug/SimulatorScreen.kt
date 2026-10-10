package com.daniel.runcoach.ui.debug

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.debug.SimulationController
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.sim.SimScenario
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import kotlinx.coroutines.launch

/** S15 Debug / Simulator (DESIGN.md section 8). Debug builds only; same tokens and components. */
@Composable
fun SimulatorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var scenario by remember { mutableStateOf(SimScenario.INTERVALS) }
    var scale by remember { mutableIntStateOf(60) }
    var running by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<String?>(null) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = stringResource(R.string.debug_title), onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            Text("Workout: ${intervalWorkout().name}", style = type.titleS, color = colors.textPrimary)
            Text("Scenario", style = type.label, color = colors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                SimScenario.entries.forEach { value ->
                    val active = value == scenario
                    Text(
                        value.name,
                        style = type.titleS,
                        color = if (active) colors.onAccent else colors.textSecondary,
                        modifier = Modifier
                            .background(if (active) colors.accent else colors.surfaceRaised, RoundedCornerShape(spacing.s5))
                            .border(1.dp, colors.outlineStrong, RoundedCornerShape(spacing.s5))
                            .clickable { scenario = value }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Text(stringResource(R.string.debug_speed), style = type.label, color = colors.textSecondary)
            SegmentedControl(listOf(1, 10, 60).map { it to "${it}x" }, scale, { scale = it })

            RcButton(
                text = if (running) stringResource(R.string.debug_running) else stringResource(R.string.action_run),
                onClick = {
                    scope.launch {
                        running = true
                        summary = null
                        try {
                            val result = SimulationController(intervalWorkout(), scenario, SECONDS, scale).run()
                            summary = result.summary(intervalWorkout().id, scenario, SECONDS)
                        } finally {
                            running = false
                        }
                    }
                },
                enabled = !running,
                loading = running,
                fillWidth = true,
            )

            summary?.let { text ->
                RcCard {
                    Text(text.trimEnd(), style = type.caption, color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = spacing.s4))
        }
    }
}

private const val SECONDS = 2000

/** The interval workout used for the simulator, matching the golden-test fixture. */
private fun intervalWorkout() = Workout(
    id = "interval",
    name = "Interval 200/400",
    items = listOf(
        Segment("Warm up", SegmentKind.WARMUP, Limit.Distance(1500), targetPaceSecPerKm = 480),
        Repeat(
            times = 3,
            children = listOf(
                Segment("Interval", SegmentKind.RUN, Limit.Distance(200), targetPaceSecPerKm = 390),
                Segment("Recovery", SegmentKind.RECOVERY, Limit.Distance(400), targetPaceSecPerKm = 360),
            ),
        ),
        Segment("Cooldown", SegmentKind.COOLDOWN, Limit.Distance(1000), targetPaceSecPerKm = 480),
    ),
)

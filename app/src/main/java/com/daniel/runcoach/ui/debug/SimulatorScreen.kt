package com.daniel.runcoach.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import kotlinx.coroutines.launch

/**
 * S15 Debug / Simulator (plan P1.6). Debug builds only: pick a scenario and a playback speed and run the
 * interval workout on a synthetic track. The summary mirrors the golden-test format so they can be compared.
 */
@Composable
fun SimulatorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var scenario by remember { mutableStateOf(SimScenario.INTERVALS) }
    var scale by remember { mutableIntStateOf(60) }
    var running by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineSmall)

            Text("Scenario: $scenario", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = {
                val values = SimScenario.entries
                scenario = values[(values.indexOf(scenario) + 1) % values.size]
            }) { Text(SimScenario.entries[(SimScenario.entries.indexOf(scenario) + 1) % SimScenario.entries.size].name) }

            Text(stringResource(R.string.debug_speed), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 10, 60).forEach { value ->
                    OutlinedButton(onClick = { scale = value }, enabled = scale != value) { Text("${value}x") }
                }
            }

            Button(
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
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (running) stringResource(R.string.debug_running) else stringResource(R.string.action_run)) }

            summary?.let { text ->
                Text(text.trimEnd(), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back))
            }
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

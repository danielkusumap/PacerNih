package com.daniel.runcoach

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.runner.RunnerPhase
import com.daniel.runcoach.engine.runner.RunnerState
import com.daniel.runcoach.service.RunUiState
import com.daniel.runcoach.ui.run.RunScreen
import com.daniel.runcoach.ui.theme.RunCoachTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P3.2 AC: the live map renders on S05 without crashing (display-only). */
@RunWith(AndroidJUnit4::class)
class LiveMapTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun liveMapRendersOnRunScreen() {
        val segment = Segment("Run", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null)
        val ui = RunUiState(
            phase = RunnerPhase.RUNNING,
            workoutName = "Live map",
            state = RunnerState(
                phase = RunnerPhase.RUNNING,
                stepIndex = 1,
                stepCount = 1,
                segment = segment,
                paceLiveSecPerKm = 360.0,
                totalDistanceM = 500.0,
                movingMs = 180_000,
            ),
            trackPoints = (0..10).map { -8.58 + it * 0.0001 to 116.10 },
        )
        rule.setContent { RunCoachTheme { RunScreen(ui = ui, onStop = {}, showMap = true) } }
        rule.onRoot().assertExists()
    }
}

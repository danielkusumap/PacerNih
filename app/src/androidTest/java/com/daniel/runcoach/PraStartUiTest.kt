package com.daniel.runcoach

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.ui.prestart.PraStartScreen
import com.daniel.runcoach.ui.theme.RunCoachTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P2.4 AC: S04 renders the segment summary, GPS status, and quick settings. */
@RunWith(AndroidJUnit4::class)
class PraStartUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsSummaryGpsStatusAndQuickSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val workout = Workout(
            "w",
            "Test 5k",
            listOf(Segment("Run", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 420)),
        )
        rule.setContent { RunCoachTheme { PraStartScreen(workout, onStarted = {}, onBack = {}) } }

        rule.onNodeWithText("Test 5k").assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.s04_segments)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.s04_accuracy)).assertIsDisplayed()
        // Countdown options 0/3/5/10 are offered (may be below the fold).
        rule.onNodeWithText("10").assertExists()
    }
}

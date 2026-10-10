package com.daniel.runcoach

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.RunSnapshot
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.ui.home.BerandaScreen
import com.daniel.runcoach.ui.theme.RunCoachTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P2.3 AC: empty state, and the unfinished-session banner only shows when a snapshot exists. */
@RunWith(AndroidJUnit4::class)
class BerandaUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = WorkoutTemplateRepository(context)
    private val persistence = RunPersistence(context)

    @Before
    fun clean() {
        runBlocking {
            repository.all().filterNot { it.isBuiltIn }.forEach { repository.delete(it.id) }
            persistence.loadSnapshot()?.let { persistence.clear(it.sessionId) }
        }
    }

    @Test
    fun emptyStateShownWithoutUserTemplates() {
        rule.setContent { RunCoachTheme { BerandaScreen(false, {}, {}, {}, {}) } }
        rule.onNode(hasText(context.getString(R.string.s02_create_first))).assertIsDisplayed()
    }

    @Test
    fun bannerAbsentWithoutSnapshot() {
        val title = context.getString(R.string.s02_resume_title)
        rule.setContent { RunCoachTheme { BerandaScreen(false, {}, {}, {}, {}) } }
        rule.waitForIdle()
        rule.onAllNodesWithText(title).assertCountEquals(0)
    }

    @Test
    fun bannerShownWithSnapshot() {
        val title = context.getString(R.string.s02_resume_title)
        runBlocking {
            persistence.saveSnapshot(
                RunSnapshot(
                    sessionId = "test-session",
                    workoutJson = com.daniel.runcoach.engine.model.WorkoutJson.encode(
                        Workout("free2", "Seed", listOf(Segment("Run", SegmentKind.RUN, Limit.Open, null))),
                    ),
                    startedAtElapsedMs = 0,
                    startedAtEpochMs = System.currentTimeMillis(),
                    zoneId = "UTC",
                ),
                savedAtMs = System.currentTimeMillis(),
            )
            repository.ensureBuiltIn()
        }
        rule.setContent { RunCoachTheme { BerandaScreen(false, {}, {}, {}, {}) } }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }
}


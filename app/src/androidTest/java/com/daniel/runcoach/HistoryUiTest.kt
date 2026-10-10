package com.daniel.runcoach

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.TrackPointEntity
import com.daniel.runcoach.ui.history.DetailSessionScreen
import com.daniel.runcoach.ui.history.HistoryScreen
import com.daniel.runcoach.ui.theme.RunCoachTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P2.6/P2.8 AC: history lists sessions with edit/delete, and detail shows the header and tabs. */
@RunWith(AndroidJUnit4::class)
class HistoryUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = SessionRepository(context)

    @Before
    fun clean() {
        runBlocking { repository.allSessions().forEach { repository.delete(it.id) } }
    }

    @Test
    fun historyShowsSessionWithEditAndDelete() {
        val id = "hist-${System.nanoTime()}"
        runBlocking { repository.saveSession(SessionFixtures.session(id, "History Test"), SessionFixtures.results()) }
        rule.setContent { RunCoachTheme { HistoryScreen(onOpenSession = {}, onSelectTab = {}) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("History Test").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("History Test").assertExists()
        rule.onNodeWithContentDescription(context.getString(R.string.s07_more)).performClick()
        rule.onNodeWithText(context.getString(R.string.s07_edit)).assertExists()
        rule.onNodeWithText(context.getString(R.string.s07_delete)).assertExists()
    }

    @Test
    fun detailShowsHeaderAndTabs() {
        val id = "detail-${System.nanoTime()}"
        runBlocking { repository.saveSession(SessionFixtures.session(id, "Detail Test"), SessionFixtures.results()) }
        rule.setContent { RunCoachTheme { DetailSessionScreen(sessionId = id, onBack = {}) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Detail Test").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Detail Test").assertExists()
        rule.onNodeWithText(context.getString(R.string.s08_tab_segments)).assertExists()
        rule.onNodeWithText(context.getString(R.string.s08_tab_splits)).assertExists()
        rule.onNodeWithText(context.getString(R.string.s08_tab_charts)).assertExists()
    }

    /** P3.1 AC: the Map tab renders the osmdroid route without crashing. */
    @Test
    fun detailMapTabRendersWithoutCrash() {
        val id = "map-${System.nanoTime()}"
        runBlocking {
            repository.saveSession(SessionFixtures.session(id, "Map Test"), SessionFixtures.results())
            RunPersistence(context).appendTrackPoints(
                (0..20).map { i ->
                    TrackPointEntity(
                        sessionId = id,
                        idx = i,
                        timeMs = i * 1000L,
                        lat = -8.58 + i * 0.0001,
                        lon = 116.10,
                        accuracyM = 5.0,
                        speedMps = 3.0,
                        segmentIdx = 1,
                    )
                },
            )
        }
        rule.setContent { RunCoachTheme { DetailSessionScreen(sessionId = id, onBack = {}) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Map Test").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(context.getString(R.string.s08_tab_map)).performClick()
        rule.onRoot().assertExists()
    }
}


package com.daniel.runcoach

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.ui.editor.EditorScreen
import com.daniel.runcoach.ui.theme.RunCoachTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P2.2 AC via the real editor UI: save gating and the add-segment → name → save flow persisting a template. */
@RunWith(AndroidJUnit4::class)
class EditorUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun saveIsDisabledUntilNameIsEntered() {
        rule.setContent { RunCoachTheme { EditorScreen(onSaved = {}, onCancel = {}) } }
        rule.onNodeWithTag("btn_save").assertIsNotEnabled()
    }

    @Test
    fun addSegmentEnterNameAndSavePersistsTemplate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = WorkoutTemplateRepository(context)
        val name = "UI Test ${System.nanoTime()}"
        val saved = AtomicBoolean(false)

        rule.setContent {
            RunCoachTheme { EditorScreen(onSaved = { saved.set(true) }, onCancel = {}) }
        }
        rule.onNodeWithTag("btn_add_segment").performClick()
        rule.onNodeWithTag("field_name").performTextInput(name)
        rule.onNodeWithTag("btn_save").performClick()
        rule.waitUntil(timeoutMillis = 10_000) { saved.get() }

        assertTrue(saved.get())
        val stored = repository.all().filter { it.name == name }
        assertTrue("saved template present", stored.isNotEmpty())
        stored.forEach { repository.delete(it.id) }
    }
}

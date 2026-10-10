package com.daniel.runcoach

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.ui.editor.InfoTopic
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/** P2.2 AC: every D11 topic exists and has text in both Indonesian and English, straight from strings.xml. */
@RunWith(AndroidJUnit4::class)
class EditorInfoContentTest {

    private fun contextFor(languageTag: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return base.createConfigurationContext(configuration)
    }

    @Test
    fun hasSixSegmentTypesAndSixTerms() {
        assertEquals(12, InfoTopic.entries.size)
    }

    @Test
    fun everyTopicHasIndonesianAndEnglishText() {
        val indonesian = contextFor("id")
        val english = contextFor("en")
        InfoTopic.entries.forEach { topic ->
            val idTitle = indonesian.getString(topic.titleRes)
            val idBody = indonesian.getString(topic.bodyRes)
            val enTitle = english.getString(topic.titleRes)
            val enBody = english.getString(topic.bodyRes)
            assertFalse("ID title ${topic.name}", idTitle.isBlank())
            assertFalse("ID body ${topic.name}", idBody.isBlank())
            assertFalse("EN title ${topic.name}", enTitle.isBlank())
            assertFalse("EN body ${topic.name}", enBody.isBlank())
            assertNotEquals("ID/EN body differ ${topic.name}", idBody, enBody)
        }
    }
}

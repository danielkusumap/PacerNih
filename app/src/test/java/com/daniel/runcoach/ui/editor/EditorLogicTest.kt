package com.daniel.runcoach.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Local unit tests for the pure editor helpers (P2.2 AC: save gate + pace parsing). */
class EditorLogicTest {

    @Test
    fun saveIsBlockedWithoutANameOrWithErrors() {
        assertFalse(editorCanSave("", 0))
        assertFalse(editorCanSave("   ", 0))
        assertFalse(editorCanSave("My run", 1))
        assertTrue(editorCanSave("My run", 0))
    }

    @Test
    fun paceParsingAndFormattingRoundTrip() {
        assertEquals(390, parsePace("6:30"))
        assertEquals(420, parsePace("7:00"))
        assertEquals(45, parsePace("0:45"))
        assertNull(parsePace("abc"))
        assertNull(parsePace("1:2:3"))
        assertEquals("6:30", formatPace(390))
        assertEquals("7:00", formatPace(420))
    }
}

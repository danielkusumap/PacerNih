package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.result.SegmentStatus
import com.daniel.runcoach.engine.runner.AlertEvent
import com.daniel.runcoach.service.voice.RecordingSpeechSink
import com.daniel.runcoach.service.voice.RunAnnouncer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** B2: the announcer speaks A07 (joined with A03) and the pace corrections A08-A10. */
@RunWith(AndroidJUnit4::class)
class RunAnnouncerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val segment = Segment("Lari", SegmentKind.RUN, Limit.Distance(400), 390)
    private val next = Segment("Pemulihan", SegmentKind.RECOVERY, Limit.Distance(200), null)

    @Test
    fun segmentEndAndNextStartAreJoined() {
        val sink = RecordingSpeechSink()
        val announcer = RunAnnouncer(context, speechSink = sink)
        announcer.onEvent(AlertEvent.SegmentEnd(1, segment, 400.0, SegmentStatus.PASS))
        announcer.onEvent(AlertEvent.SegmentStart(2, 5, next))
        assertEquals(1, sink.utterances.size)
        val text = sink.utterances[0].text
        assertTrue("carries the average pace", text.contains("6 menit 40"))
        assertTrue("carries the next segment", text.contains("Segmen 2 dari 5"))
    }

    @Test
    fun paceCorrectionsAreSpoken() {
        val sink = RecordingSpeechSink()
        val announcer = RunAnnouncer(context, speechSink = sink)
        announcer.onEvent(AlertEvent.TooSlow(435.0, 420, 410.0))
        announcer.onEvent(AlertEvent.TooFast(390.0, 420))
        assertTrue(sink.utterances.any { it.text.contains("Terlalu lambat", ignoreCase = true) })
        assertTrue(sink.utterances.any { it.text.contains("Terlalu cepat", ignoreCase = true) })
    }

    @Test
    fun skippedSegmentIsSilent() {
        val sink = RecordingSpeechSink()
        val announcer = RunAnnouncer(context, speechSink = sink)
        announcer.onEvent(AlertEvent.SegmentEnd(1, segment, null, SegmentStatus.SKIPPED))
        assertEquals(0, sink.utterances.size)
    }
}

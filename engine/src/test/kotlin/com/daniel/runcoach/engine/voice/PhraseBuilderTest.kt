package com.daniel.runcoach.engine.voice

import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.PaceTargetBound
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.result.SegmentStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhraseBuilderTest {

    /** V20: pace wording, ID and EN. */
    @Test
    fun v20_paceWording() {
        assertEquals("7 menit pas", PhraseBuilder.pace(420.0, VoiceLanguage.ID))
        assertEquals("7 menit 2 detik", PhraseBuilder.pace(422.0, VoiceLanguage.ID))
        assertEquals("6 menit 30", PhraseBuilder.pace(390.0, VoiceLanguage.ID))
        assertEquals("7 minutes flat", PhraseBuilder.pace(420.0, VoiceLanguage.EN))
        assertEquals("7 minutes 2 seconds", PhraseBuilder.pace(422.0, VoiceLanguage.EN))
        assertEquals("6 minutes 30 seconds", PhraseBuilder.pace(390.0, VoiceLanguage.EN))
    }

    @Test
    fun `workout started and segment start phrases`() {
        assertEquals("Mulai.", PhraseBuilder.workoutStarted(VoiceLanguage.ID))
        assertEquals("Go.", PhraseBuilder.workoutStarted(VoiceLanguage.EN))

        val segment = Segment("Interval", SegmentKind.RUN, Limit.Distance(200), targetPaceSecPerKm = 390)
        assertEquals(
            "Segmen 2 dari 9. Lari 200 meter. Target pace 6 menit 30.",
            PhraseBuilder.segmentStart(2, 9, segment, VoiceLanguage.ID),
        )
        assertEquals(
            "Segment 2 of 9. Run 200 meters. Target pace 6 minutes 30 seconds.",
            PhraseBuilder.segmentStart(2, 9, segment, VoiceLanguage.EN),
        )
    }

    @Test
    fun `too slow includes the required pace when realistic`() {
        assertEquals(
            "Terlalu lambat. Pace sekarang 7 menit 15, target 7 menit pas. Butuh 6 menit 50 untuk sisa jarak ini.",
            PhraseBuilder.tooSlow(435.0, 420, 410.0, VoiceLanguage.ID),
        )
        assertEquals(
            "Too slow. Current pace 7 minutes 15 seconds, target 7 minutes flat. " +
                "You need 6 minutes 50 seconds for the rest of this stretch.",
            PhraseBuilder.tooSlow(435.0, 420, 410.0, VoiceLanguage.EN),
        )
        assertEquals(
            "Terlalu lambat. Pace sekarang 7 menit 15, target 7 menit pas.",
            PhraseBuilder.tooSlow(435.0, 420, null, VoiceLanguage.ID),
        )
    }

    @Test
    fun `too fast and back to stable phrases`() {
        assertEquals(
            "Terlalu cepat. Pace sekarang 6 menit 30, target 7 menit pas.",
            PhraseBuilder.tooFast(390.0, 420, VoiceLanguage.ID),
        )
        assertEquals(
            "Pace stabil. Sekarang 7 menit 2 detik, target 7 menit pas.",
            PhraseBuilder.backToStable(422.0, 420, VoiceLanguage.ID),
        )
    }

    @Test
    fun `segment end phrase carries the status word`() {
        assertEquals(
            "Segmen selesai. Rata-rata 6 menit 28. Lulus.",
            PhraseBuilder.segmentEnd(388.0, SegmentStatus.PASS, VoiceLanguage.ID),
        )
        assertEquals(
            "Segment done. Average 6 minutes 28 seconds. Failed.",
            PhraseBuilder.segmentEnd(388.0, SegmentStatus.FAIL, VoiceLanguage.EN),
        )
    }

    @Test
    fun `km marker phrase`() {
        assertEquals(
            "1 kilometer. Waktu 7 menit 1 detik. Pace 7 menit 1 detik.",
            PhraseBuilder.kmMarker(1, 421.0, 421.0, VoiceLanguage.ID),
        )
    }

    @Test
    fun `goal plan completed and cue phrases`() {
        assertEquals(
            "Target jarak tercapai. Lanjut atau berhenti kapan saja.",
            PhraseBuilder.goalDistanceReached(VoiceLanguage.ID),
        )
        assertEquals("Rencana workout selesai.", PhraseBuilder.workoutPlanCompleted(VoiceLanguage.ID))
        assertEquals(
            "10 detik lagi ganti ke jalan.",
            PhraseBuilder.preSegmentWarning(10, SegmentKind.WALK, VoiceLanguage.ID),
        )
        assertEquals(
            "Get ready. Switching to walk soon.",
            PhraseBuilder.distanceCue(SegmentKind.WALK, VoiceLanguage.EN),
        )
    }

    @Test
    fun `distance wording`() {
        assertEquals("200 meter", PhraseBuilder.distance(200.0, VoiceLanguage.ID))
        assertEquals("1 kilometer", PhraseBuilder.distance(1000.0, VoiceLanguage.ID))
        assertEquals("1 koma 5 kilometer", PhraseBuilder.distance(1500.0, VoiceLanguage.ID))
        assertEquals("1 point 5 kilometers", PhraseBuilder.distance(1500.0, VoiceLanguage.EN))
    }

    @Test
    fun `spoken time switches to hours past one hour`() {
        assertEquals("1 jam 2 menit", PhraseBuilder.spokenTime(3721.0, VoiceLanguage.ID))
        assertEquals("1 hours 2 minutes", PhraseBuilder.spokenTime(3721.0, VoiceLanguage.EN))
    }

    @Test
    fun `segment kind words`() {
        assertEquals("pemanasan", PhraseBuilder.segmentKind(SegmentKind.WARMUP, VoiceLanguage.ID))
        assertEquals("jalan", PhraseBuilder.segmentKind(SegmentKind.WALK, VoiceLanguage.ID))
        assertEquals("cool down", PhraseBuilder.segmentKind(SegmentKind.COOLDOWN, VoiceLanguage.EN))
    }

    @Test
    fun `gps lost and recovered phrases`() {
        assertEquals("Sinyal GPS hilang.", PhraseBuilder.gpsLost(VoiceLanguage.ID))
        assertEquals("GPS signal lost.", PhraseBuilder.gpsLost(VoiceLanguage.EN))
        assertEquals("Sinyal GPS pulih.", PhraseBuilder.gpsRecovered(VoiceLanguage.ID))
        assertEquals("GPS signal recovered.", PhraseBuilder.gpsRecovered(VoiceLanguage.EN))
    }

    @Test
    fun `countdown words A01 and A05`() {
        assertEquals("Tiga", PhraseBuilder.countdownWord(3, VoiceLanguage.ID))
        assertEquals("Three", PhraseBuilder.countdownWord(3, VoiceLanguage.EN))
        assertEquals("Lima", PhraseBuilder.countdownWord(5, VoiceLanguage.ID))
    }

    @Test
    fun `auto pause and resume phrases A16 A17`() {
        assertEquals("Dijeda otomatis.", PhraseBuilder.autoPaused(VoiceLanguage.ID))
        assertEquals("Auto-paused.", PhraseBuilder.autoPaused(VoiceLanguage.EN))
        assertEquals("Dilanjutkan.", PhraseBuilder.autoResumed(VoiceLanguage.ID))
        assertEquals("Resumed.", PhraseBuilder.autoResumed(VoiceLanguage.EN))
    }

    @Test
    fun `segment start without target omits the target A03`() {
        val noTarget = Segment("Jalan", SegmentKind.WALK, Limit.Duration(120), null)
        val id = PhraseBuilder.segmentStart(2, 5, noTarget, VoiceLanguage.ID)
        assertTrue(!id.contains("Target pace"), "no target-pace phrase")
        val en = PhraseBuilder.segmentStart(2, 5, noTarget, VoiceLanguage.EN)
        assertTrue(!en.contains("Target pace"), "no target-pace phrase")
    }

    @Test
    fun `R2 range target is spoken as a band`() {
        val range = Segment("Tempo", SegmentKind.RUN, Limit.Distance(1000), 390, targetBound = PaceTargetBound.RANGE, targetPace2SecPerKm = 400)
        val id = PhraseBuilder.segmentStart(1, 1, range, VoiceLanguage.ID)
        assertTrue(id.contains("6 menit 30 sampai 6 menit 40"), "id range: $id")
        val en = PhraseBuilder.segmentStart(1, 1, range, VoiceLanguage.EN)
        assertTrue(en.contains("6 minutes 30 seconds to 6 minutes 40 seconds"), "en range: $en")
    }
}

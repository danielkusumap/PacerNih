package com.daniel.runcoach.engine.model

import com.daniel.runcoach.engine.voice.VoiceLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutTextFormatterTest {

    private fun fmt(workout: Workout, language: VoiceLanguage = VoiceLanguage.ID) =
        WorkoutTextFormatter.format(workout, language)

    @Test
    fun intervalWorkoutMatchesFormat() {
        val workout = Workout(
            id = "", name = "Interval 6×400",
            items = listOf(
                Segment("Pemanasan", SegmentKind.WARMUP, Limit.Distance(1500), 510, targetBound = PaceTargetBound.MIN),
                Repeat(times = 6, children = listOf(
                    Segment("Lari", SegmentKind.RUN, Limit.Distance(400), 390),
                    Segment("Pemulihan", SegmentKind.RECOVERY, Limit.Distance(200), null),
                )),
                Segment("Pendinginan", SegmentKind.COOLDOWN, Limit.Distance(1000), 510, targetBound = PaceTargetBound.MIN),
            ),
        )
        val expected = """
            Interval 6×400
            Pemanasan 1,5 km · ≥ 8:30 /km
            Ulangi 6×:
              Lari 400 m · ≤ 6:30 /km
              Pemulihan 200 m
            Pendinginan 1 km · ≥ 8:30 /km
        """.trimIndent()
        assertEquals(expected, fmt(workout))
    }

    @Test
    fun runWalkUntilDistanceMatchesFormat() {
        val workout = Workout(
            id = "", name = "Run/Walk",
            items = listOf(
                Repeat(untilTotalDistanceM = 5000, children = listOf(
                    Segment("Lari", SegmentKind.RUN, Limit.Duration(360), null),
                    Segment("Jalan", SegmentKind.WALK, Limit.Duration(120), null),
                )),
            ),
        )
        val expected = """
            Run/Walk
            Ulangi sampai 5 km:
              Lari 6 mnt
              Jalan 2 mnt
        """.trimIndent()
        assertEquals(expected, fmt(workout))
    }

    @Test
    fun freeRunHasTwoLines() {
        val workout = Workout(
            id = "", name = "Lari Bebas",
            items = listOf(Segment("Lari bebas", SegmentKind.RUN, Limit.Open, null)),
        )
        assertEquals("Lari Bebas\nLari bebas tanpa batas", fmt(workout))
    }

    @Test
    fun englishUsesDotDecimal() {
        val workout = Workout(
            id = "", name = "Tempo",
            items = listOf(Segment("Warm up", SegmentKind.WARMUP, Limit.Distance(1500), 510, targetBound = PaceTargetBound.MIN)),
        )
        assertEquals("Tempo\nWarm up 1.5 km · ≥ 8:30 /km", fmt(workout, VoiceLanguage.EN))
    }

    @Test
    fun durationSegmentFormatting() {
        val workout = Workout(
            id = "", name = "W",
            items = listOf(
                Segment("A", SegmentKind.RUN, Limit.Duration(45), null),
                Segment("B", SegmentKind.RUN, Limit.Duration(150), null),
                Segment("C", SegmentKind.RUN, Limit.Duration(3600), null),
            ),
        )
        assertEquals("W\nA 45 dtk\nB 2 mnt 30 dtk\nC 1 jam 0 mnt", fmt(workout))
    }
}

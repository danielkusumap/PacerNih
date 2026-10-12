package com.daniel.runcoach.engine.alert

import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.runner.AlertEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlertEngineTest {

    private fun input(
        paceLive: Double?,
        projected: Double? = null,
        target: Int? = 420,
        fastMargin: Int? = 20,
        timeBased: Boolean = true,
        segmentLengthSec: Double = 600.0,
        segmentLengthM: Double = 5000.0,
        segmentDistanceM: Double = 200.0,
        segmentMovingSec: Double = 60.0,
        scopeDistM: Double = 500.0,
        remainingSegmentSec: Double = 500.0,
        required: Double? = null,
        running: Boolean = true,
        gpsLost: Boolean = false,
        open: Boolean = false,
        bound: com.daniel.runcoach.engine.model.PaceTargetBound = com.daniel.runcoach.engine.model.PaceTargetBound.MAX,
        slow: Int? = null,
    ) = CorrectionInput(
        segmentTargetSecPerKm = target,
        fastMarginSec = fastMargin,
        segmentOpen = open,
        timeBased = timeBased,
        segmentLengthM = segmentLengthM,
        segmentLengthSec = segmentLengthSec,
        segmentDistanceM = segmentDistanceM,
        segmentMovingSec = segmentMovingSec,
        scopeDistM = scopeDistM,
        remainingSegmentSec = remainingSegmentSec,
        paceLiveSecPerKm = paceLive,
        projectedSecPerKm = projected,
        requiredSecPerKm = required,
        running = running,
        gpsLost = gpsLost,
        targetBound = bound,
        targetPace2SecPerKm = slow,
    )

    @Test
    fun `a stable runner gets no correction alerts`() {
        val engine = AlertEngine()
        for (t in 0..10_000 step 1_000) {
            assertNull(engine.evaluate(input(paceLive = 410.0), t.toLong()))
        }
        assertEquals(CorrectionState.STABLE, engine.state())
    }

    @Test
    fun `slowing down raises one too slow then repeats every 25 seconds`() {
        val engine = AlertEngine()
        engine.evaluate(input(paceLive = 410.0), 0)
        val events = mutableListOf<Pair<Long, AlertEvent>>()
        for (t in 1_000..40_000 step 1_000) {
            engine.evaluate(input(paceLive = 435.0), t.toLong())?.let { events += t.toLong() to it }
        }
        assertEquals(2, events.size)
        assertTrue(events[0].second is AlertEvent.TooSlow)
        assertEquals(6_000L, events[0].first)
        assertEquals(31_000L, events[1].first)
    }

    @Test
    fun `returning to target raises exactly one back to stable`() {
        val engine = AlertEngine()
        engine.evaluate(input(paceLive = 410.0), 0)
        for (t in 1_000..6_000 step 1_000) engine.evaluate(input(paceLive = 435.0), t.toLong())

        var stable: AlertEvent? = null
        for (t in 7_000..20_000 step 1_000) {
            val event = engine.evaluate(input(paceLive = 410.0), t.toLong())
            if (event != null) stable = event
        }
        assertEquals(AlertEvent.BackToStable, stable)
    }

    @Test
    fun `running 20 seconds faster than target raises too fast`() {
        val engine = AlertEngine()
        engine.evaluate(input(paceLive = 420.0), 0)
        var event: AlertEvent? = null
        for (t in 1_000..7_000 step 1_000) {
            event = engine.evaluate(input(paceLive = 390.0), t.toLong())
            if (event != null) break
        }
        assertTrue(event is AlertEvent.TooFast)
    }

    @Test
    fun `a short segment gets no correction alerts`() {
        val engine = AlertEngine()
        for (t in 0..10_000 step 1_000) {
            assertNull(
                engine.evaluate(input(paceLive = 435.0, timeBased = false, segmentLengthM = 200.0), t.toLong()),
            )
        }
    }

    @Test
    fun `no correction alerts in the first 100 m or 20 s`() {
        val engine = AlertEngine()
        assertNull(engine.evaluate(input(paceLive = 435.0, segmentDistanceM = 50.0), 20_000))
        assertNull(engine.evaluate(input(paceLive = 435.0, segmentMovingSec = 10.0), 20_000))
    }

    @Test
    fun `no correction alerts in the last 15 seconds`() {
        val engine = AlertEngine()
        for (t in 0..10_000 step 1_000) {
            assertNull(engine.evaluate(input(paceLive = 435.0, remainingSegmentSec = 10.0), t.toLong()))
        }
    }

    @Test
    fun `a segment end outranks a pace correction`() {
        val segment = Segment("Interval", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = 420)
        val picked = AlertPriority.pick(
            listOf(AlertEvent.TooSlow(435.0, 420, null), AlertEvent.SegmentEnd(1, segment)),
        )
        assertTrue(picked is AlertEvent.SegmentEnd)
    }

    @Test
    fun `R2 a min target also guards the slow side`() {
        val engine = AlertEngine()
        val bound = com.daniel.runcoach.engine.model.PaceTargetBound.MIN
        engine.evaluate(input(paceLive = 520.0, target = 510, bound = bound), 0)
        var event: AlertEvent? = null
        for (t in 1_000..7_000 step 1_000) {
            event = engine.evaluate(input(paceLive = 540.0, target = 510, bound = bound), t.toLong())
            if (event != null) break
        }
        assertTrue(event is AlertEvent.TooSlow, "min target raises too slow when far slower than target+margin")
    }

    @Test
    fun `R2 a range fires too fast below the fast bound and too slow above the slow bound`() {
        val range = com.daniel.runcoach.engine.model.PaceTargetBound.RANGE
        val fast = AlertEngine()
        fast.evaluate(input(paceLive = 395.0, target = 390, bound = range, slow = 400), 0)
        var event: AlertEvent? = null
        for (t in 1_000..7_000 step 1_000) {
            event = fast.evaluate(input(paceLive = 380.0, target = 390, bound = range, slow = 400), t.toLong())
            if (event != null) break
        }
        assertTrue(event is AlertEvent.TooFast)

        val slowEngine = AlertEngine()
        slowEngine.evaluate(input(paceLive = 395.0, target = 390, bound = range, slow = 400), 0)
        var slowEvent: AlertEvent? = null
        for (t in 1_000..7_000 step 1_000) {
            slowEvent = slowEngine.evaluate(input(paceLive = 412.0, target = 390, bound = range, slow = 400), t.toLong())
            if (slowEvent != null) break
        }
        assertTrue(slowEvent is AlertEvent.TooSlow)
    }
}

package com.daniel.runcoach

import com.daniel.runcoach.engine.model.PaceTargetBound
import com.daniel.runcoach.ui.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** R2 fix: the shared target label must show a range for RANGE segments everywhere. */
class FormattersRangeTest {

    @Test
    fun rangeSegmentShowsBothBounds() {
        assertEquals("6:30–6:40", Formatters.segmentTarget(390, 400, PaceTargetBound.RANGE))
    }

    @Test
    fun maxAndMinUseTheSymbol() {
        assertEquals("≤ 6:30", Formatters.segmentTarget(390, null, PaceTargetBound.MAX))
        assertEquals("≥ 8:30", Formatters.segmentTarget(510, null, PaceTargetBound.MIN))
    }

    @Test
    fun noTargetIsNull() {
        assertNull(Formatters.segmentTarget(null, null, PaceTargetBound.MAX))
    }

    @Test
    fun recordTimeAndPaceAgree() {
        // 1_692_900 ms = 1692.9 s -> rounded 1693 s = 28:13; 5 km pace = 1693/5 = 338.6 -> 339 -> 5:39.
        assertEquals("28:13", Formatters.recordTime(1_692_900L))
        assertEquals("5:39", Formatters.recordPace(1_692_900L, 5000.0))
    }
}

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
}

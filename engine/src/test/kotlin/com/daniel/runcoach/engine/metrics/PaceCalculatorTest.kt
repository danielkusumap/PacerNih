package com.daniel.runcoach.engine.metrics

import com.daniel.runcoach.engine.roundHalfUp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaceCalculatorTest {

    @Test
    fun `speed converts to pace and back`() {
        assertEquals(333.33, PaceCalculator.paceSecPerKmFromSpeed(3.0)!!, 0.01)
        assertEquals(2.7777, PaceCalculator.speedMpsFromPace(360.0)!!, 0.001)
        assertNull(PaceCalculator.paceSecPerKmFromSpeed(0.0))
        assertNull(PaceCalculator.speedMpsFromPace(0.0))
    }

    @Test
    fun `average pace needs a minimum distance`() {
        assertEquals(360.0, PaceCalculator.averagePaceSecPerKm(360.0, 1000.0)!!, 1e-9)
        assertNull(PaceCalculator.averagePaceSecPerKm(20.0, 40.0))
    }

    @Test
    fun `scope is the current kilometre when per-km checking is on`() {
        assertEquals(PaceScope(2000.0, 1000.0), PaceCalculator.scope(5000.0, 2400.0, perKmCheck = true))
        assertEquals(PaceScope(4000.0, 1000.0), PaceCalculator.scope(5000.0, 4900.0, perKmCheck = true))
        // Last partial kilometre.
        assertEquals(PaceScope(1000.0, 500.0), PaceCalculator.scope(1500.0, 1200.0, perKmCheck = true))
    }

    @Test
    fun `scope is the whole segment when per-km checking is off or the segment is short`() {
        assertEquals(PaceScope(0.0, 5000.0), PaceCalculator.scope(5000.0, 2400.0, perKmCheck = false))
        assertEquals(PaceScope(0.0, 800.0), PaceCalculator.scope(800.0, 400.0, perKmCheck = true))
    }

    @Test
    fun `projected pace blends elapsed and remaining time`() {
        assertEquals(416.0, PaceCalculator.projectedPaceSecPerKm(410.0, 170.0, 1000.0, 600.0)!!, 1e-9)
        assertNull(PaceCalculator.projectedPaceSecPerKm(410.0, 170.0, 1000.0, 0.0))
    }

    @Test
    fun `required pace is null when unrealistic`() {
        assertEquals(323.333, PaceCalculator.requiredPaceSecPerKm(330.0, 680.0, 5000.0, 3000.0)!!, 0.01)
        // Target already beaten by more than the elapsed time.
        assertNull(PaceCalculator.requiredPaceSecPerKm(330.0, 1700.0, 5000.0, 3000.0))
        // No remaining distance.
        assertNull(PaceCalculator.requiredPaceSecPerKm(330.0, 100.0, 5000.0, 0.0))
        // Would require faster than 2:30/km.
        assertNull(PaceCalculator.requiredPaceSecPerKm(140.0, 0.0, 1000.0, 1000.0))
    }

    @Test
    fun `round half up follows the plan`() {
        assertEquals(420L, roundHalfUp(420.4))
        assertEquals(421L, roundHalfUp(420.5))
        assertEquals(329L, roundHalfUp(328.947))
        assertEquals(333L, roundHalfUp(333.33))
    }
}

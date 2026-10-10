package com.daniel.runcoach.engine.gps

import kotlin.test.Test
import kotlin.test.assertEquals

/** V19 and F25: distance calibration is derived without compounding and clamped to 0.95..1.05. */
class CalibrationCalculatorTest {

    @Test
    fun v19_factorFromMeasuredAndActual() {
        // 2950 m measured with factor 1.0, real distance 3.00 km -> 3000 / 2950.
        val first = CalibrationCalculator.factorFor(3000.0, 2950.0, 1.0)
        assertEquals(3000.0 / 2950.0, first, 1e-9)

        // 3000 m measured with the factor above, real 3.00 km -> same factor, no compounding.
        val second = CalibrationCalculator.factorFor(3000.0, 3000.0, first)
        assertEquals(first, second, 1e-9)
    }

    @Test
    fun factorIsClampedToTheAllowedRange() {
        assertEquals(1.05, CalibrationCalculator.factorFor(3500.0, 3000.0, 1.0), 1e-9)
        assertEquals(0.95, CalibrationCalculator.factorFor(2500.0, 3000.0, 1.0), 1e-9)
    }

    @Test
    fun degenerateInputsReturnNeutralFactor() {
        assertEquals(1.0, CalibrationCalculator.factorFor(0.0, 3000.0, 1.0), 1e-9)
        assertEquals(1.0, CalibrationCalculator.factorFor(3000.0, 0.0, 1.0), 1e-9)
    }
}

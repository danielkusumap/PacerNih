package com.daniel.runcoach.engine.gps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HaversineTest {

    @Test
    fun `same point is zero`() {
        assertEquals(0.0, haversineM(0.0, 0.0, 0.0, 0.0), 1e-9)
    }

    @Test
    fun `one thousandth of a degree of latitude is about 111 metres`() {
        val d = haversineM(0.0, 0.0, 0.001, 0.0)
        assertEquals(111.19, d, 0.5)
    }

    @Test
    fun `distance is symmetric`() {
        val ab = haversineM(-8.58, 116.10, -8.59, 116.11)
        val ba = haversineM(-8.59, 116.11, -8.58, 116.10)
        assertTrue(kotlin.math.abs(ab - ba) < 1e-9)
    }
}

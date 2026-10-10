package com.daniel.runcoach.engine.gps

import kotlin.test.Test
import kotlin.test.assertEquals

class GpsFixQualityTest {

    @Test
    fun `no fix is searching`() {
        assertEquals(GpsFixQuality.SEARCHING, gpsFixQuality(null))
    }

    @Test
    fun `accuracy at or better than the good fix threshold is ready`() {
        assertEquals(GpsFixQuality.READY, gpsFixQuality(8.0))
        assertEquals(GpsFixQuality.READY, gpsFixQuality(12.0))
    }

    @Test
    fun `worse accuracy is weak`() {
        assertEquals(GpsFixQuality.WEAK, gpsFixQuality(15.0))
    }
}

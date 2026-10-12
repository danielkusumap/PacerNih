package com.daniel.runcoach.engine.result

import kotlin.test.Test
import kotlin.test.assertEquals

class MilestoneTimesTest {

    private fun track(totalM: Int, stepM: Int = 100, paceSecPerKm: Double = 360.0): List<DistTimePoint> {
        val points = mutableListOf(DistTimePoint(0.0, 0L))
        var timeMs = 0.0
        var distanceM = 0.0
        while (distanceM < totalM) {
            val previous = distanceM
            distanceM = minOf(distanceM + stepM, totalM.toDouble())
            timeMs += (distanceM - previous) * paceSecPerKm
            points += DistTimePoint(distanceM, timeMs.toLong())
        }
        return points
    }

    @Test
    fun onlyReachedMilestonesAreReturned() {
        val result = MilestoneTimes.compute(track(10_000))
        assertEquals(listOf(1000.0, 5000.0, 10000.0), result.map { it.meters })
        assertEquals(360_000.0, result[0].durationMs.toDouble(), 50.0)
        assertEquals(360.0, result[0].paceSecPerKm, 0.5)
        assertEquals(1_800_000.0, result[1].durationMs.toDouble(), 200.0)
    }

    @Test
    fun halfAndFullMarathonAreIncludedWhenReached() {
        val hm = MilestoneTimes.compute(track(22_000))
        assertEquals(listOf(1000.0, 5000.0, 10000.0, 15000.0, 21097.5), hm.map { it.meters })
        val fm = MilestoneTimes.compute(track(43_000))
        assertEquals(42195.0, fm.last().meters)
    }

    @Test
    fun aShortSessionHasNoMilestones() {
        assertEquals(emptyList(), MilestoneTimes.compute(track(900)))
    }
}

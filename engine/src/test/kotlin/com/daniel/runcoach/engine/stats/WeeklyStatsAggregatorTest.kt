package com.daniel.runcoach.engine.stats

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeeklyStatsAggregatorTest {

    private val zone = "Asia/Jakarta"
    private fun at(date: String, time: String) =
        LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(ZoneId.of(zone)).toInstant().toEpochMilli()

    /** V16: distance-weighted average pace, not the mean of paces. */
    @Test
    fun averagePaceIsDistanceWeighted() {
        val sessions = listOf(
            WeeklySession(at("2026-10-06", "06:00"), zone, 5000.0, 1800.0),
            WeeklySession(at("2026-10-07", "06:00"), zone, 10000.0, 3300.0),
        )
        val week = WeeklyStatsAggregator.forWeek(sessions, LocalDate.parse("2026-10-05"))
        assertEquals(340.0, week.avgPaceSecPerKm!!, 1e-9) // 5100s / 15km
        assertEquals(2, week.sessionCount)
    }

    /** V17: week boundaries in Asia/Jakarta (Monday start). */
    @Test
    fun weekBoundariesFollowMonday() {
        val sunday = WeeklySession(at("2026-10-11", "23:30"), zone, 5000.0, 1800.0)
        val monday = WeeklySession(at("2026-10-12", "00:30"), zone, 5000.0, 1800.0)
        assertEquals(LocalDate.parse("2026-10-05"), weekStart(sunday))
        assertEquals(LocalDate.parse("2026-10-12"), weekStart(monday))
    }

    @Test
    fun emptyWeekIsNoughtWithNullPace() {
        val week = WeeklyStatsAggregator.forWeek(emptyList(), LocalDate.parse("2026-10-05"))
        assertEquals(0.0, week.totalDistanceM, 1e-9)
        assertNull(week.avgPaceSecPerKm)
        assertNull(week.passRatePercent)
        assertEquals(7, week.dailyDistanceM.size)
    }

    @Test
    fun deltaIsRelativeToThePreviousWeek() {
        val sessions = listOf(
            WeeklySession(at("2026-10-06", "06:00"), zone, 5000.0, 1800.0),
            WeeklySession(at("2026-09-29", "06:00"), zone, 3000.0, 1200.0),
        )
        val weeks = WeeklyStatsAggregator.lastWeeks(sessions, at("2026-10-06", "06:00"), zone, count = 2)
        assertEquals(3000.0, weeks[0].totalDistanceM, 1e-9)
        assertEquals(5000.0, weeks[1].totalDistanceM, 1e-9)
        assertEquals(2000.0, weeks[1].deltaM!!, 1e-9)
    }

    private fun weekStart(session: WeeklySession) =
        LocalDate.ofInstant(java.time.Instant.ofEpochMilli(session.startedAtEpochMs), ZoneId.of(zone))
            .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
}

package com.daniel.runcoach.engine.stats

import com.daniel.runcoach.engine.roundHalfUp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** A session as the weekly aggregator sees it (plan F21). */
data class WeeklySession(
    val startedAtEpochMs: Long,
    val zoneId: String,
    val distanceM: Double,
    val movingSec: Double,
    val segPass: Int = 0,
    val segFail: Int = 0,
    val segBorderline: Int = 0,
)

/** Aggregates for one Monday-to-Sunday week (plan F21). */
data class WeekStats(
    val weekStart: LocalDate,
    val totalDistanceM: Double,
    val totalMovingSec: Double,
    val sessionCount: Int,
    val longestM: Double,
    val avgPaceSecPerKm: Double?,
    val passRatePercent: Int?,
    val dailyDistanceM: List<Double>,
    val deltaM: Double?,
    val deltaPct: Double?,
)

/**
 * Weekly statistics (plan 6.11, F21). Weeks start Monday. Sessions under 100 m are ignored. Average pace is
 * distance-weighted (total moving time / total km), and empty weeks report nought with a null pace.
 */
object WeeklyStatsAggregator {

    private const val MIN_SESSION_M = 100.0

    fun forWeek(sessions: List<WeeklySession>, weekStart: LocalDate, previousWeek: WeekStats? = null): WeekStats {
        val inWeek = sessions
            .filter { it.distanceM >= MIN_SESSION_M }
            .filter { weekStartOf(it) == weekStart }

        val totalDistance = inWeek.sumOf { it.distanceM }
        val totalMoving = inWeek.sumOf { it.movingSec }
        val daily = DoubleArray(7)
        inWeek.forEach { session ->
            val dayIndex = Instant.ofEpochMilli(session.startedAtEpochMs).atZone(ZoneId.of(session.zoneId)).toLocalDate().dayOfWeek.value - 1
            daily[dayIndex] += session.distanceM
        }
        val segPass = inWeek.sumOf { it.segPass }
        val segFail = inWeek.sumOf { it.segFail }
        val segBorder = inWeek.sumOf { it.segBorderline }
        val passDenominator = segPass + segFail + segBorder
        val deltaM = previousWeek?.let { totalDistance - it.totalDistanceM }
        val deltaPct = previousWeek?.takeIf { it.totalDistanceM > 0 }?.let { 100.0 * (totalDistance - it.totalDistanceM) / it.totalDistanceM }

        return WeekStats(
            weekStart = weekStart,
            totalDistanceM = totalDistance,
            totalMovingSec = totalMoving,
            sessionCount = inWeek.size,
            longestM = inWeek.maxOfOrNull { it.distanceM } ?: 0.0,
            avgPaceSecPerKm = if (totalDistance > 0) totalMoving / (totalDistance / 1000.0) else null,
            passRatePercent = if (passDenominator > 0) roundHalfUp(100.0 * segPass / passDenominator).toInt() else null,
            dailyDistanceM = daily.toList(),
            deltaM = deltaM,
            deltaPct = deltaPct,
        )
    }

    /** The last [count] weeks up to the week containing [referenceEpochMs], oldest first, empty weeks included. */
    fun lastWeeks(sessions: List<WeeklySession>, referenceEpochMs: Long, zoneId: String, count: Int = 12): List<WeekStats> {
        val currentWeek = Instant.ofEpochMilli(referenceEpochMs).atZone(ZoneId.of(zoneId)).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val result = mutableListOf<WeekStats>()
        var previous: WeekStats? = null
        for (offset in (count - 1) downTo 0) {
            val weekStart = currentWeek.minusWeeks(offset.toLong())
            val stats = forWeek(sessions, weekStart, previous)
            result += stats
            previous = stats
        }
        return result
    }

    private fun weekStartOf(session: WeeklySession): LocalDate =
        Instant.ofEpochMilli(session.startedAtEpochMs).atZone(ZoneId.of(session.zoneId)).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

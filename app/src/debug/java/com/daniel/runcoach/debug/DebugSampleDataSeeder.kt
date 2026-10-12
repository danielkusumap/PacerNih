package com.daniel.runcoach.debug

import android.content.Context
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.TrackPointEntity
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.PaceTargetBound
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutExpander
import com.daniel.runcoach.engine.model.WorkoutJson
import com.daniel.runcoach.engine.result.DistTimePoint
import com.daniel.runcoach.engine.result.SessionResultsCalculator
import java.time.ZoneId

/**
 * Debug-only sample data (B1). Lives in `src/debug/`, so release builds contain none of it. Every row uses an id
 * starting with `mock_`. Fill/clear is manual from S15.
 *
 * Sample sessions are generated as a real distance/time curve and their segment/split/record numbers are computed
 * from that curve (not fabricated), so the records page always matches the session history.
 */
object DebugSampleDataSeeder : SampleDataSeeder {

    override suspend fun fill(context: Context) {
        clearSample(context)
        seedAll(context)
    }

    override suspend fun clearSample(context: Context) {
        val sessions = SessionRepository(context)
        sessions.allSessions().filter { it.id.startsWith(PREFIX) }.forEach { sessions.delete(it.id) }
        val templates = WorkoutTemplateRepository(context)
        templates.all().filter { it.id.startsWith(PREFIX) }.forEach { templates.delete(it.id) }
    }

    private suspend fun seedAll(context: Context) {
        val sessions = SessionRepository(context)
        seedTemplates(context)
        seedSessions(context, sessions)
    }

    private suspend fun seedTemplates(context: Context) {
        val repo = WorkoutTemplateRepository(context)
        repo.ensureBuiltIn()
        if (repo.byId("mock_tempo") == null) repo.create(tempo5k.copy(id = "mock_tempo"))
        if (repo.byId("mock_interval") == null) repo.create(interval6x400.copy(id = "mock_interval"))
    }

    private suspend fun seedSessions(context: Context, repository: SessionRepository) {
        val zone = ZoneId.systemDefault().id
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        val persistence = RunPersistence(context)

        data class Seed(val daysAgo: Int, val name: String, val workout: Workout, val distanceM: Double, val paceSecPerKm: Double)

        val seeds = listOf(
            Seed(2, "Lari Pagi", stable5k, 5030.0, 342.0),
            Seed(4, "Tempo Sore", tempo5k, 7000.0, 358.0),
            Seed(7, "Recovery Ringan", freeRun, 3010.0, 372.0),
            Seed(11, "Long Run", freeRun, 10050.0, 370.0),
            Seed(15, "Interval 6x400", interval6x400, 5600.0, 352.0),
            Seed(21, "Lari Pagi", stable5k, 5000.0, 360.0),
        )

        seeds.forEachIndexed { i, seed ->
            val started = now - seed.daysAgo * day
            val id = "$PREFIX${seed.daysAgo}_$i"
            val (curve, track) = buildRun(seed.workout, seed.distanceM, seed.paceSecPerKm, id)
            val results = SessionResultsCalculator.compute(seed.workout, curve)
            val distanceM = curve.last().cumulativeDistanceM
            val movingMs = curve.last().elapsedMs
            val entity = SessionEntity(
                id = id,
                templateId = null,
                templateNameSnapshot = seed.workout.name,
                workoutSnapshotJson = WorkoutJson.encode(seed.workout),
                name = seed.name,
                nameIsCustom = false,
                note = if (i % 2 == 0) "Terasa enak, napas stabil." else null,
                rpe = 5 + (i % 4),
                excludeFromRecords = false,
                startedAtEpochMs = started,
                zoneId = zone,
                endedAtEpochMs = started + movingMs,
                elapsedMs = movingMs,
                movingMs = movingMs,
                pausedMs = 0L,
                totalDistanceM = distanceM,
                avgPaceSecPerKm = if (distanceM > 0) movingMs / distanceM else null,
                segPass = results.segments.count { it.status == "PASS" },
                segFail = results.segments.count { it.status == "FAIL" },
                segBorderline = results.segments.count { it.status == "BORDERLINE" },
                segNoTarget = results.segments.count { it.status == "NO_TARGET" },
                planCompleted = true,
                goalReached = false,
                distanceCalibrationFactorUsed = 1.0,
                droppedSampleCount = i,
                updatedAt = started,
            )
            repository.saveSession(entity, results)
            persistence.appendTrackPoints(track)
        }
    }

    /**
     * Walks the expanded plan at each segment's target pace (or [defaultPaceSecPerKm]) up to [targetDistanceM],
     * producing a consistent cumulative distance/time curve plus a matching straight-line GPS track.
     */
    private fun buildRun(
        workout: Workout,
        targetDistanceM: Double,
        defaultPaceSecPerKm: Double,
        sessionId: String,
    ): Pair<List<DistTimePoint>, List<TrackPointEntity>> {
        val expanded = runCatching { WorkoutExpander.expandFinite(workout.items) }.getOrDefault(emptyList())
        val curve = mutableListOf(DistTimePoint(0.0, 0L))
        val track = mutableListOf<TrackPointEntity>()
        val baseLat = -8.5833
        val baseLon = 116.1167
        val step = 25.0
        var distanceM = 0.0
        var timeMs = 0.0
        var idx = 0
        track += TrackPointEntity(
            sessionId = sessionId,
            idx = 0,
            timeMs = 0L,
            lat = baseLat,
            lon = baseLon,
            accuracyM = 6.0,
            speedMps = 1000.0 / defaultPaceSecPerKm,
            segmentIdx = 1,
        )
        idx = 1

        expanded.forEachIndexed { index, segment ->
            val pace = segment.targetPaceSecPerKm?.toDouble() ?: defaultPaceSecPerKm
            val segmentMeters = when (val l = segment.limit) {
                is Limit.Distance -> l.meters.toDouble()
                is Limit.Duration -> (l.seconds * 1000.0) / pace
                Limit.Open -> (targetDistanceM - distanceM).coerceAtLeast(0.0)
            }
            var remaining = segmentMeters
            while (remaining > 0.0 && distanceM < targetDistanceM) {
                val stepM = minOf(step, remaining, targetDistanceM - distanceM)
                if (stepM <= 0.0) break
                // milliseconds per metre = seconds per km (pace).
                timeMs += stepM * pace
                distanceM += stepM
                remaining -= stepM
                curve += DistTimePoint(distanceM, timeMs.toLong())
                track += TrackPointEntity(
                    sessionId = sessionId,
                    idx = idx,
                    timeMs = timeMs.toLong(),
                    lat = baseLat + distanceM / 111_195.0,
                    lon = baseLon,
                    accuracyM = 6.0,
                    speedMps = if (pace > 0) 1000.0 / pace else 3.0,
                    segmentIdx = index + 1,
                )
                idx++
            }
        }
        return curve to track
    }

    private val stable5k = Workout(
        id = "", name = "Stable 5 km",
        items = listOf(Segment("Lari 5 km", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 342)),
    )
    private val tempo5k = Workout(
        id = "", name = "Tempo 5 km",
        items = listOf(
            Segment("Pemanasan", SegmentKind.WARMUP, Limit.Distance(1000), targetPaceSecPerKm = null),
            Segment("Tempo", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 340),
            Segment("Pendinginan", SegmentKind.COOLDOWN, Limit.Distance(1000), targetPaceSecPerKm = null),
        ),
    )
    private val freeRun = Workout(
        id = "", name = "Lari Bebas",
        items = listOf(Segment("Lari Bebas", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null)),
    )
    private val interval6x400 = Workout(
        id = "", name = "Interval 6x400",
        items = listOf(
            Segment("Pemanasan", SegmentKind.WARMUP, Limit.Distance(1000), targetPaceSecPerKm = null),
            Repeat(
                times = 6,
                children = listOf(
                    Segment("Interval", SegmentKind.RUN, Limit.Distance(400), targetPaceSecPerKm = 300),
                    Segment("Pemulihan", SegmentKind.RECOVERY, Limit.Distance(200), targetPaceSecPerKm = 505, targetBound = PaceTargetBound.MIN),
                ),
            ),
            Segment("Pendinginan", SegmentKind.COOLDOWN, Limit.Distance(1000), targetPaceSecPerKm = null),
        ),
    )

    private const val PREFIX = "mock_"
}

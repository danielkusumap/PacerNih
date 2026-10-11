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
import com.daniel.runcoach.engine.result.BestEffort
import com.daniel.runcoach.engine.result.BestEffortKey
import com.daniel.runcoach.engine.result.KmSplit
import com.daniel.runcoach.engine.result.SegmentResultData
import com.daniel.runcoach.engine.result.SegmentStatus
import com.daniel.runcoach.engine.result.SessionResultsData
import java.time.ZoneId

/**
 * Debug-only sample data (B1). Lives in `src/debug/`, so release builds contain none of it. Every row uses an id
 * starting with `mock_`. Seeding happens once per install, guarded by a flag that data wipes never clear.
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

        data class Seed(val daysAgo: Int, val name: String, val workout: Workout, val distanceM: Double, val paceSecPerKm: Double, val failSegments: Set<Int>)

        val seeds = listOf(
            Seed(2, "Lari Pagi", stable5k, 5030.0, 342.0, emptySet()),
            Seed(4, "Tempo Sore", tempo5k, 7000.0, 358.0, setOf(2)),
            Seed(7, "Recovery Ringan", freeRun, 3010.0, 372.0, emptySet()),
            Seed(11, "Long Run", freeRun, 10050.0, 370.0, emptySet()),
            Seed(15, "Interval 6x400", interval6x400, 5600.0, 352.0, setOf(6, 10)),
            Seed(21, "Lari Pagi", stable5k, 5000.0, 360.0, emptySet()),
        )

        seeds.forEachIndexed { i, seed ->
            val started = now - seed.daysAgo * day
            val distanceM = seed.distanceM
            val movingMs = (distanceM / 1000.0 * seed.paceSecPerKm * 1000).toLong()
            val id = "${PREFIX}${seed.daysAgo}_$i"
            val results = resultsFor(seed.workout, distanceM, seed.paceSecPerKm, seed.failSegments)
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
                avgPaceSecPerKm = seed.paceSecPerKm,
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
            persistence.appendTrackPoints(trackFor(id, distanceM, started, movingMs))
        }
    }

    private val stable5k = Workout(
        id = "", name = "Stable 5 km",
        items = listOf(Segment("Lari 5 km", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 342)),
    )
    private val tempo5k = Workout(
        id = "", name = "Tempo 5 km",
        items = listOf(
            Segment("Pemanasan", SegmentKind.WARMUP, Limit.Distance(1000), targetPaceSecPerKm = 505, targetBound = PaceTargetBound.MIN),
            Segment("Tempo", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 340),
            Segment("Pendinginan", SegmentKind.COOLDOWN, Limit.Distance(1000), targetPaceSecPerKm = 505, targetBound = PaceTargetBound.MIN),
        ),
    )
    private val freeRun = Workout(
        id = "", name = "Lari Bebas",
        items = listOf(Segment("Lari Bebas", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null)),
    )
    private val interval6x400 = Workout(
        id = "", name = "Interval 6x400",
        items = listOf(
            Segment("Pemanasan", SegmentKind.WARMUP, Limit.Distance(1000), targetPaceSecPerKm = 505, targetBound = PaceTargetBound.MIN),
            Repeat(
                times = 6,
                children = listOf(
                    Segment("Interval", SegmentKind.RUN, Limit.Distance(400), targetPaceSecPerKm = 300),
                    Segment("Pemulihan", SegmentKind.RECOVERY, Limit.Distance(200), targetPaceSecPerKm = 505, targetBound = PaceTargetBound.MIN),
                ),
            ),
            Segment("Pendinginan", SegmentKind.COOLDOWN, Limit.Distance(1000), targetPaceSecPerKm = 505, targetBound = PaceTargetBound.MIN),
        ),
    )

    /** Builds one segment result per expanded segment plus km splits and best efforts for the whole session. */
    private fun resultsFor(workout: Workout, distanceM: Double, paceSecPerKm: Double, failSegments: Set<Int>): SessionResultsData {
        val expanded = runCatching { WorkoutExpander.expandFinite(workout.items) }.getOrDefault(emptyList())
        var cursor = 0.0
        val segments = expanded.mapIndexed { index, seg ->
            val meters = (seg.limit as? Limit.Distance)?.meters?.toDouble() ?: (distanceM - cursor).coerceAtLeast(0.0)
            val pace = seg.targetPaceSecPerKm?.toDouble() ?: paceSecPerKm
            val durationMs = (meters / 1000.0 * pace * 1000).toLong()
            cursor += meters
            val hasTarget = seg.targetPaceSecPerKm != null
            val status = when {
                !hasTarget -> "NO_TARGET"
                (index + 1) in failSegments -> "FAIL"
                else -> "PASS"
            }
            val delta = if (hasTarget && status == "FAIL") (pace * 0.06).toLong() else 0L
            SegmentResultData(
                idx = index + 1,
                label = seg.label,
                kind = seg.kind.name,
                limitType = when (seg.limit) {
                    is Limit.Distance -> "distance"
                    is Limit.Duration -> "duration"
                    Limit.Open -> "open"
                },
                limitValue = (seg.limit as? Limit.Distance)?.meters?.toDouble() ?: (seg.limit as? Limit.Duration)?.seconds?.toDouble() ?: 0.0,
                targetPace = seg.targetPaceSecPerKm,
                distanceM = meters,
                durationMs = durationMs,
                avgPace = if (meters > 0) durationMs / 1000.0 / (meters / 1000.0) else null,
                deltaSec = delta,
                status = status,
            )
        }

        val kmCount = (distanceM / 1000.0).toInt()
        val splits = (1..kmCount).map { km ->
            val isPartial = km == kmCount && distanceM % 1000.0 > 10.0
            val kmDistance = if (isPartial) distanceM - (kmCount - 1) * 1000.0 else 1000.0
            val pace = paceSecPerKm + ((km % 5) - 2) * 3.0
            KmSplit(
                index = km,
                isPartial = isPartial,
                distanceM = kmDistance,
                durationMs = (kmDistance / 1000.0 * pace * 1000).toLong(),
                paceSecPerKm = pace,
                targetSecPerKm = paceSecPerKm.toInt(),
                deltaSec = (pace - paceSecPerKm).toLong(),
                status = if (km in failSegments) SegmentStatus.FAIL else SegmentStatus.PASS,
            )
        }

        val efforts = buildList {
            if (distanceM >= 1000) add(BestEffort(BestEffortKey.K1, (paceSecPerKm * 0.97 * 1000).toLong(), 200.0))
            if (distanceM >= 5000) add(BestEffort(BestEffortKey.K5, (paceSecPerKm * 0.99 * 5000).toLong(), 400.0))
            if (distanceM >= 10000) add(BestEffort(BestEffortKey.K10, (paceSecPerKm * 10000).toLong(), 300.0))
        }
        return SessionResultsData(segments, splits, efforts)
    }

    private fun trackFor(sessionId: String, distanceM: Double, startedAtMs: Long, movingMs: Long): List<TrackPointEntity> {
        val points = 80
        val baseLat = -8.5833
        val baseLon = 116.1167
        return (0 until points).map { i ->
            val f = i.toDouble() / (points - 1)
            val angle = f * 2 * Math.PI
            val radiusM = 240.0 + 80 * Math.sin(angle * 2)
            val lat = baseLat + (radiusM * Math.cos(angle)) / 111_195.0
            val lon = baseLon + (radiusM * Math.sin(angle)) / (111_195.0 * Math.cos(Math.toRadians(baseLat)))
            TrackPointEntity(
                sessionId = sessionId,
                idx = i,
                timeMs = (f * movingMs).toLong(),
                lat = lat,
                lon = lon,
                accuracyM = 6.0,
                speedMps = if (movingMs > 0) distanceM / (movingMs / 1000.0) else 3.0,
                segmentIdx = 1,
            )
        }
    }

    private const val PREFIX = "mock_"
}

package com.daniel.runcoach.engine.model

/**
 * Builders for the editor's preset generator (plan D02). Pure; each returns a workout that passes
 * [WorkoutValidator]. Ids are left blank so the repository assigns one on save.
 */
object WorkoutPresets {

    private fun seg(label: String, kind: SegmentKind, limit: Limit, target: Int?) =
        Segment(label, kind, limit, targetPaceSecPerKm = target)

    fun stableRun(distanceM: Int, paceSecPerKm: Int): Workout = Workout(
        id = "",
        name = "Stable $distanceM m",
        items = listOf(seg("Run", SegmentKind.RUN, Limit.Distance(distanceM), paceSecPerKm)),
    )

    /** [kilometers] km, starting at [startPaceSecPerKm], each km [stepSecPerKm] faster, optional warm up/cool down. */
    fun progressiveTempo(
        kilometers: Int,
        startPaceSecPerKm: Int,
        stepSecPerKm: Int,
        warmUpMeters: Int = 0,
        coolDownMeters: Int = 0,
    ): Workout {
        val items = mutableListOf<Block>()
        if (warmUpMeters > 0) {
            items += seg("Warm up", SegmentKind.WARMUP, Limit.Distance(warmUpMeters), null)
        }
        repeat(kilometers) { index ->
            items += seg("Tempo ${index + 1}", SegmentKind.RUN, Limit.Distance(1000), startPaceSecPerKm - index * stepSecPerKm)
        }
        if (coolDownMeters > 0) {
            items += seg("Cooldown", SegmentKind.COOLDOWN, Limit.Distance(coolDownMeters), null)
        }
        return Workout(id = "", name = "Progressive tempo $kilometers km", items = items)
    }

    fun interval(
        warmUpMeters: Int,
        work: Limit,
        workTargetSecPerKm: Int,
        recovery: Limit,
        recoveryTargetSecPerKm: Int?,
        repetitions: Int,
        coolDownMeters: Int,
    ): Workout {
        val items = mutableListOf<Block>()
        if (warmUpMeters > 0) items += seg("Warm up", SegmentKind.WARMUP, Limit.Distance(warmUpMeters), null)
        items += Repeat(
            times = repetitions,
            children = listOf(
                seg("Interval", SegmentKind.RUN, work, workTargetSecPerKm),
                seg("Recovery", SegmentKind.RECOVERY, recovery, recoveryTargetSecPerKm),
            ),
        )
        if (coolDownMeters > 0) items += seg("Cooldown", SegmentKind.COOLDOWN, Limit.Distance(coolDownMeters), null)
        return Workout(id = "", name = "Interval", items = items)
    }

    fun runWalk(runSeconds: Int, walkSeconds: Int, goalDistanceM: Int): Workout = Workout(
        id = "",
        name = "Run/walk ${goalDistanceM / 1000} km",
        goalDistanceM = goalDistanceM,
        items = listOf(
            Repeat(
                untilTotalDistanceM = goalDistanceM,
                children = listOf(
                    seg("Run", SegmentKind.RUN, Limit.Duration(runSeconds), null),
                    seg("Walk", SegmentKind.WALK, Limit.Duration(walkSeconds), null),
                ),
            ),
        ),
    )
}

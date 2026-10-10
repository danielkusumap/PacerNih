package com.daniel.runcoach.engine.model

/** The five worked examples from section 5 of the plan, shared by all model tests. */
object Samples {

    fun stable5k() = Workout(
        id = "stable5k",
        name = "Stable 5 km",
        items = listOf(
            Segment("5 km", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 420),
        ),
    )

    fun interval() = Workout(
        id = "interval",
        name = "Interval 200/400",
        items = listOf(
            Segment("Warm up", SegmentKind.WARMUP, Limit.Distance(1500), targetPaceSecPerKm = 480),
            Repeat(
                times = 3,
                children = listOf(
                    Segment("Interval", SegmentKind.RUN, Limit.Distance(200), targetPaceSecPerKm = 390),
                    Segment("Recovery", SegmentKind.RECOVERY, Limit.Distance(400), targetPaceSecPerKm = 360),
                ),
            ),
            Segment("Cooldown", SegmentKind.COOLDOWN, Limit.Distance(1000), targetPaceSecPerKm = 480),
        ),
    )

    fun tempo() = Workout(
        id = "tempo",
        name = "Progressive tempo",
        items = listOf(
            Segment("Tempo 1", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = 490),
            Segment("Tempo 2", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = 450),
            Segment("Tempo 3", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = 430),
            Segment("Tempo 4", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = 410),
            Segment("Cooldown", SegmentKind.COOLDOWN, Limit.Distance(1000), targetPaceSecPerKm = 480),
        ),
    )

    fun runWalk() = Workout(
        id = "runwalk",
        name = "Run/walk 5 km",
        goalDistanceM = 5000,
        items = listOf(
            Repeat(
                untilTotalDistanceM = 5000,
                children = listOf(
                    Segment("Run", SegmentKind.RUN, Limit.Duration(360), targetPaceSecPerKm = null),
                    Segment("Walk", SegmentKind.WALK, Limit.Duration(120), targetPaceSecPerKm = null),
                ),
            ),
        ),
    )

    fun freeRun() = Workout(
        id = "free",
        name = "Lari Bebas",
        items = listOf(
            Segment("Lari Bebas", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null),
        ),
    )

    fun all(): List<Workout> = listOf(stable5k(), interval(), tempo(), runWalk(), freeRun())
}

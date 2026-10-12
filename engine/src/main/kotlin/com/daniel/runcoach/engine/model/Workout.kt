package com.daniel.runcoach.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A structured workout: an ordered list of blocks executed from top to bottom. */
@Serializable
data class Workout(
    val id: String,
    val name: String,
    val items: List<Block>,
    val goalDistanceM: Int? = null,
)

@Serializable
sealed interface Block

/**
 * One workout step. [targetPaceSecPerKm] is the slowest acceptable pace (a ceiling):
 * faster than it still passes. [fastMarginSec] is how much faster than the target pace
 * triggers the "too fast" alert; null disables that alert. [perKmCheck] only has effect
 * when the limit is a distance of at least 1000 m and a target pace is set.
 */
@Serializable
@SerialName("segment")
data class Segment(
    val label: String,
    val kind: SegmentKind,
    val limit: Limit,
    val targetPaceSecPerKm: Int?,
    val fastMarginSec: Int? = 20,
    val perKmCheck: Boolean = true,
    val targetBound: PaceTargetBound = PaceTargetBound.MAX,
    /**
     * R2: the second pace bound for [PaceTargetBound.RANGE]. With RANGE, [targetPaceSecPerKm] is the **fast**
     * bound and this is the **slow** bound (fast < slow). Null for MAX/MIN.
     */
    val targetPace2SecPerKm: Int? = null,
) : Block

/**
 * Repeats [children]. Exactly one of [times] or [untilTotalDistanceM] must be set.
 * The distance-bounded form is expanded lazily while running (see [WorkoutExpander]).
 * Children are always [Segment]s, so repeats cannot nest.
 */
@Serializable
@SerialName("repeat")
data class Repeat(
    val times: Int? = null,
    val untilTotalDistanceM: Int? = null,
    val children: List<Segment>,
) : Block

@Serializable
sealed interface Limit {
    /** Ends after [meters] metres. */
    @Serializable
    @SerialName("distance")
    data class Distance(val meters: Int) : Limit

    /** Ends after [seconds] seconds. */
    @Serializable
    @SerialName("duration")
    data class Duration(val seconds: Int) : Limit

    /** No limit: runs until the user stops. Only valid as the last segment, outside a repeat. */
    @Serializable
    @SerialName("open")
    data object Open : Limit
}

@Serializable
enum class SegmentKind { WARMUP, RUN, WALK, COOLDOWN, RECOVERY }

/**
 * Which way a target pace is enforced.
 * - [MAX] (default, for run/work) is a ceiling: the average pace must be **at most** the target (faster passes).
 * - [MIN] (for warm-up, cool-down, recovery) is a floor: the average pace must be **at least** the target, i.e.
 *   you may not go faster than it.
 * - [RANGE] (R2) keeps the pace inside a band: [Segment.targetPaceSecPerKm] is the fast bound and
 *   [Segment.targetPace2SecPerKm] is the slow bound (fast < slow). Alerts fire on both sides.
 */
@Serializable
enum class PaceTargetBound { MAX, MIN, RANGE }

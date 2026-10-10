package com.daniel.runcoach.engine.model

/** Specific, testable reasons a workout can be rejected. UI maps these to localized text. */
enum class WorkoutErrorCode {
    WORKOUT_EMPTY,
    GOAL_DISTANCE_NOT_POSITIVE,
    REPEAT_BOUND_REQUIRED,
    REPEAT_BOUND_CONFLICT,
    REPEAT_TIMES_NOT_POSITIVE,
    REPEAT_UNTIL_NOT_POSITIVE,
    REPEAT_CHILDREN_EMPTY,
    OPEN_IN_REPEAT,
    OPEN_NOT_LAST,
    DISTANCE_NOT_POSITIVE,
    DURATION_NOT_POSITIVE,
    TARGET_PACE_OUT_OF_RANGE,
}

data class WorkoutError(val code: WorkoutErrorCode, val path: String) {
    override fun toString(): String = "$code at $path"
}

/** Validates the rules in section 5 of the plan. Pure, side-effect free. */
object WorkoutValidator {

    const val MIN_TARGET_PACE_SEC_PER_KM = 150 // 2:30 /km
    const val MAX_TARGET_PACE_SEC_PER_KM = 1200 // 20:00 /km

    fun validate(workout: Workout): List<WorkoutError> {
        val errors = mutableListOf<WorkoutError>()
        if (workout.items.isEmpty()) {
            errors += WorkoutError(WorkoutErrorCode.WORKOUT_EMPTY, "items")
        }
        val goal = workout.goalDistanceM
        if (goal != null && goal <= 0) {
            errors += WorkoutError(WorkoutErrorCode.GOAL_DISTANCE_NOT_POSITIVE, "goalDistanceM")
        }

        val lastIndex = workout.items.lastIndex
        workout.items.forEachIndexed { index, block ->
            when (block) {
                is Segment -> validateSegment(
                    segment = block,
                    path = "items[$index]",
                    errors = errors,
                    openError = if (index == lastIndex) null else WorkoutErrorCode.OPEN_NOT_LAST,
                )

                is Repeat -> validateRepeat(block, "items[$index]", errors)
            }
        }
        return errors
    }

    private fun validateRepeat(repeat: Repeat, path: String, errors: MutableList<WorkoutError>) {
        val times = repeat.times
        val until = repeat.untilTotalDistanceM
        if (times == null && until == null) {
            errors += WorkoutError(WorkoutErrorCode.REPEAT_BOUND_REQUIRED, path)
        }
        if (times != null && until != null) {
            errors += WorkoutError(WorkoutErrorCode.REPEAT_BOUND_CONFLICT, path)
        }
        if (times != null && times <= 0) {
            errors += WorkoutError(WorkoutErrorCode.REPEAT_TIMES_NOT_POSITIVE, path)
        }
        if (until != null && until <= 0) {
            errors += WorkoutError(WorkoutErrorCode.REPEAT_UNTIL_NOT_POSITIVE, path)
        }
        if (repeat.children.isEmpty()) {
            errors += WorkoutError(WorkoutErrorCode.REPEAT_CHILDREN_EMPTY, path)
        }
        repeat.children.forEachIndexed { childIndex, child ->
            validateSegment(
                segment = child,
                path = "$path.children[$childIndex]",
                errors = errors,
                openError = WorkoutErrorCode.OPEN_IN_REPEAT,
            )
        }
    }

    private fun validateSegment(
        segment: Segment,
        path: String,
        errors: MutableList<WorkoutError>,
        openError: WorkoutErrorCode?,
    ) {
        when (val limit = segment.limit) {
            is Limit.Distance -> if (limit.meters <= 0) {
                errors += WorkoutError(WorkoutErrorCode.DISTANCE_NOT_POSITIVE, "$path.limit")
            }

            is Limit.Duration -> if (limit.seconds <= 0) {
                errors += WorkoutError(WorkoutErrorCode.DURATION_NOT_POSITIVE, "$path.limit")
            }

            Limit.Open -> if (openError != null) {
                errors += WorkoutError(openError, "$path.limit")
            }
        }

        val pace = segment.targetPaceSecPerKm
        if (pace != null && pace !in MIN_TARGET_PACE_SEC_PER_KM..MAX_TARGET_PACE_SEC_PER_KM) {
            errors += WorkoutError(WorkoutErrorCode.TARGET_PACE_OUT_OF_RANGE, "$path.targetPaceSecPerKm")
        }
    }
}

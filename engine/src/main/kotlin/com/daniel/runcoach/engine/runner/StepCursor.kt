package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.model.Block
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment

/**
 * Walks a workout's [Block] list into a flat stream of [Segment]s (plan 5, F27).
 *
 * `times` repeats are walked child by child and repeated the given number of times. A distance-bounded
 * repeat (`untilTotalDistanceM`) cycles its children until the caller's current total distance reaches
 * the bound, checked after each child — so the segment in progress always finishes first. Repeats never
 * nest (children are plain segments).
 *
 * Stateful and single-use; not thread safe. `hasNext` reports whether another segment exists without
 * consuming one.
 */
class StepCursor(private val items: List<Block>) {

    private class RepeatState {
        var timesLeft = 0
        var childIndex = 0
        var started = false
    }

    private val repeats = Array(items.size) { RepeatState() }
    private var itemIndex = 0

    init {
        items.forEachIndexed { index, block ->
            if (block is Repeat) repeats[index].timesLeft = block.times ?: Int.MAX_VALUE
        }
    }

    /** Next segment, or null when the plan is finished. [totalDistanceM] drives distance-bounded repeats. */
    fun next(totalDistanceM: Double): Segment? {
        while (itemIndex < items.size) {
            when (val item = items[itemIndex]) {
                is Segment -> {
                    itemIndex++
                    return item
                }

                is Repeat -> {
                    val state = repeats[itemIndex]
                    val until = item.untilTotalDistanceM
                    if (until != null && state.started && totalDistanceM >= until) {
                        itemIndex++
                        continue
                    }
                    if (state.childIndex < item.children.size) {
                        state.started = true
                        return item.children[state.childIndex++]
                    }
                    // Round finished.
                    if (item.times != null) {
                        state.timesLeft--
                        if (state.timesLeft <= 0) {
                            itemIndex++
                            continue
                        }
                        state.childIndex = 0
                    } else {
                        state.childIndex = 0 // distance-bounded: start another round
                    }
                }
            }
        }
        return null
    }

    /** Whether a further segment exists, without advancing. */
    fun hasNext(totalDistanceM: Double): Boolean = peek(totalDistanceM) != null

    /** The next segment without consuming it, or null when the plan is finished. */
    fun peek(totalDistanceM: Double): Segment? {
        val savedItem = itemIndex
        val savedTimes = IntArray(repeats.size) { repeats[it].timesLeft }
        val savedChild = IntArray(repeats.size) { repeats[it].childIndex }
        val savedStarted = BooleanArray(repeats.size) { repeats[it].started }

        val result = next(totalDistanceM)

        itemIndex = savedItem
        repeats.forEachIndexed { index, state ->
            state.timesLeft = savedTimes[index]
            state.childIndex = savedChild[index]
            state.started = savedStarted[index]
        }
        return result
    }
}

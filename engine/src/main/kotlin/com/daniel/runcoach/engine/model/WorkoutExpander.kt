package com.daniel.runcoach.engine.model

/**
 * Expands [Block]s into a flat list of [Segment]s.
 *
 * [expandFinite] and [stepCountOrNull] only work when every [Repeat] has a fixed [Repeat.times];
 * the distance-bounded form (`untilTotalDistanceM`) has no fixed length and is handled by
 * [lazySteps], which the runner consumes until the goal distance is reached.
 */
object WorkoutExpander {

    fun expandFinite(items: List<Block>): List<Segment> {
        val out = mutableListOf<Segment>()
        for (block in items) {
            when (block) {
                is Segment -> out += block
                is Repeat -> {
                    val times = block.times
                        ?: error("unbounded repeat requires lazySteps(), not expandFinite()")
                    repeat(times) { out += block.children }
                }
            }
        }
        return out
    }

    /** Total number of expanded segments, or null if any repeat is distance-bounded. */
    fun stepCountOrNull(items: List<Block>): Int? {
        var total = 0
        for (block in items) {
            when (block) {
                is Segment -> total += 1
                is Repeat -> {
                    val times = block.times ?: return null
                    total += times * block.children.size
                }
            }
        }
        return total
    }

    /**
     * Yields segments in execution order. Fixed repeats are yielded the given number of times;
     * a distance-bounded repeat yields its children forever, so callers must stop consuming
     * once the goal distance is reached. Never throws on a workout that passes validation.
     */
    fun lazySteps(items: List<Block>): Sequence<Segment> = sequence {
        for (block in items) {
            when (block) {
                is Segment -> yield(block)
                is Repeat -> {
                    val times = block.times
                    if (times != null) {
                        repeat(times) { for (child in block.children) yield(child) }
                    } else {
                        while (true) {
                            for (child in block.children) yield(child)
                        }
                    }
                }
            }
        }
    }
}
